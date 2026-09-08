let csrf
export async function refreshCsrf() {
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
  const body = await response.json()
  if (!response.ok) throw new Error(body.message || '无法获取安全令牌')
  csrf = body.data
}
export async function api(path, { method = 'GET', data, query } = {}) {
  const params = new URLSearchParams()
  Object.entries(query || {}).forEach(([key, value]) => { if (value !== '' && value != null) params.set(key, value) })
  if (method !== 'GET' && !csrf) await refreshCsrf()
  const response = await fetch('/api/v1' + path + (params.size ? '?' + params : ''), {
    method, credentials: 'same-origin', headers: { 'Content-Type': 'application/json', ...(method === 'GET' ? {} : { [csrf.headerName]: csrf.token }) },
    body: data == null ? undefined : JSON.stringify(data),
  })
  const body = await response.json()
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/auth/')) window.dispatchEvent(new Event('session-expired'))
    const error = new Error(body.message || '请求失败'); error.status = response.status; throw error
  }
  return body.data
}
