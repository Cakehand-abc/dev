let csrf

/**
 * 刷新获取当前会话绑定的 CSRF 令牌（兼容非 Bearer 请求与传统表单）。
 */
export async function refreshCsrf() {
  try {
    const response = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
    const body = await response.json()
    if (response.ok) {
      csrf = body.data
    }
  } catch (e) {
    // CSRF 获取失败时不中断请求流程，请求将尝试依赖 Bearer 令牌鉴权
  }
}

/**
 * 从 localStorage 中读取存储的双 Token。
 *
 * @returns {{ accessToken: string | null, refreshToken: string | null }}
 */
export function getTokens() {
  return {
    accessToken: localStorage.getItem('care_access_token'),
    refreshToken: localStorage.getItem('care_refresh_token')
  }
}

/**
 * 将双 Token 持久化存储到 localStorage。
 *
 * @param {{ accessToken?: string, refreshToken?: string }} tokens
 */
export function setTokens({ accessToken, refreshToken } = {}) {
  if (accessToken) localStorage.setItem('care_access_token', accessToken)
  if (refreshToken) localStorage.setItem('care_refresh_token', refreshToken)
}

/**
 * 清除本地保存的双 Token。
 */
export function clearTokens() {
  localStorage.removeItem('care_access_token')
  localStorage.removeItem('care_refresh_token')
}

// 刷新状态锁与重试等待队列，避免多个并发请求同时触发多次刷新
let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

function onRefreshFailed(err) {
  refreshSubscribers.forEach(cb => cb(null, err))
  refreshSubscribers = []
}

/**
 * 使用本地 RefreshToken 调用后端刷新端点获取新的 AccessToken。
 */
async function doRefreshToken() {
  const { refreshToken } = getTokens()
  if (!refreshToken) {
    clearTokens()
    throw new Error('会话已过期，请重新登录')
  }

  const response = await fetch('/api/v1/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  })
  const body = await response.json()
  if (!response.ok) {
    clearTokens()
    const err = new Error(body.message || '刷新登录态失败')
    err.status = response.status
    throw err
  }
  setTokens(body.data)
  return body.data.accessToken
}

/**
 * 统一 API 请求封装，支持 Bearer 令牌自动注入、401 自动无感刷新重试以及 CSRF 适配。
 *
 * @param {string} path 接口路径（如 '/elders'）
 * @param {object} options 请求选项
 * @returns {Promise<any>}
 */
export async function api(path, { method = 'GET', data, query, _retry = false } = {}) {
  const params = new URLSearchParams()
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== '' && value != null) params.set(key, value)
  })

  const { accessToken } = getTokens()
  if (!accessToken && method !== 'GET' && !csrf) {
    await refreshCsrf()
  }

  const headers = { 'Content-Type': 'application/json' }
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }
  if (csrf && csrf.headerName && csrf.token && method !== 'GET') {
    headers[csrf.headerName] = csrf.token
  }

  const response = await fetch('/api/v1' + path + (params.size ? '?' + params : ''), {
    method,
    credentials: 'same-origin',
    headers,
    body: data == null ? undefined : JSON.stringify(data),
  })

  const body = await response.json().catch(() => ({}))

  if (!response.ok) {
    // 捕获 401 认证异常，尝试执行无感刷新重放
    if (response.status === 401 && !path.startsWith('/auth/login') && !path.startsWith('/auth/refresh') && !_retry) {
      const { refreshToken } = getTokens()
      if (refreshToken) {
        if (!isRefreshing) {
          isRefreshing = true
          try {
            const newToken = await doRefreshToken()
            isRefreshing = false
            onRefreshed(newToken)
            return api(path, { method, data, query, _retry: true })
          } catch (refreshErr) {
            isRefreshing = false
            onRefreshFailed(refreshErr)
            window.dispatchEvent(new Event('session-expired'))
            throw refreshErr
          }
        } else {
          // 当前已有并发请求在刷新中，加入等待队列，刷新完成后使用新 Token 重放当前请求
          return new Promise((resolve, reject) => {
            refreshSubscribers.push((newToken, err) => {
              if (err) {
                reject(err)
              } else {
                resolve(api(path, { method, data, query, _retry: true }))
              }
            })
          })
        }
      } else {
        if (!path.startsWith('/auth/')) {
          window.dispatchEvent(new Event('session-expired'))
        }
      }
    }

    if (response.status === 401 && !path.startsWith('/auth/')) {
      window.dispatchEvent(new Event('session-expired'))
    }

    const error = new Error(body.message || '请求失败')
    error.status = response.status
    throw error
  }

  return body.data
}
