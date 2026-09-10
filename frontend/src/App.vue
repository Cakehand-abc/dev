<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick, defineAsyncComponent } from 'vue'
import { api, refreshCsrf, setTokens, clearTokens, getTokens } from './api'
import Chart from './components/Chart.vue'
import GeoCanvas from './components/GeoCanvas.vue'
import AiAssistantOrb from './components/AiAssistantOrb.vue'
const SimulationPanel = defineAsyncComponent(() => import('./components/SimulationPanel.vue'))

const menus = [{ id: 'home', label: '首页总览', icon: '◈' }, { id: 'population', label: '人口信息分析', icon: '◷' }, { id: 'health', label: '健康检测分析', icon: '♡' }, { id: 'followup', label: '医生随访分析', icon: '▤' }, { id: 'devices', label: '腕表分布', icon: '⌖' }, { id: 'fences', label: '电子围栏', icon: '◎' }, { id: 'trajectory', label: '轨迹回放', icon: '↝' }, { id: 'simulation', label: '数据模拟', icon: '⌁' }, { id: 'doctors', label: '医生管理', icon: '✚' }, { id: 'users', label: '账号管理', icon: '⚙' }]
const titles = { home: '医养服务运行总览', population: '人口信息统计分析', health: '健康检测统计分析', followup: '医生随访统计分析', devices: '腕表分布', fences: '电子围栏', trajectory: '轨迹回放', simulation: '数据模拟与轨迹注入', doctors: '医生管理', users: '账号管理' }
const descriptions = { home: '汇集人口、健康、随访与设备数据，掌握服务运行情况。', population: '了解在册老人的年龄结构、性别与地区分布。', health: '查看检测趋势和异常记录，跟进老人的健康服务需求。', followup: '追踪随访计划、完成情况与逾期事项。', devices: '查看腕表最后定位、绑定关系及心跳状态。', fences: '管理活动范围，查看并处理越界事件。', trajectory: '按时间查看定位记录，回放老人活动轨迹。', simulation: '手动生成定位点或批量轨迹，用于演示和设备联调。', doctors: '维护参与医养服务的医生信息。', users: '配置管理员、业务人员和只读分析员。' }
const page = ref(location.hash.slice(1) || 'home'), me = ref(), loading = ref(false), ready = ref(false), error = ref(''), success = ref(''), loginBusy = ref(false)
const username = ref(''), password = ref(''), search = ref(''), pageNo = ref(1), total = ref(0), rows = ref([]), summary = ref({}), refs = reactive({ regions: [], elders: [], doctors: [] })
const allFences = ref([]), deviceRows = ref([]), alerts = ref([]), selectedDevice = ref(), points = ref([]), index = ref(0), playing = ref(false), speed = ref(1), alertTotal = ref(0), alertPage = ref(1)
const roleNames = { ADMIN: '管理员', OPERATOR: '业务人员', ANALYST: '只读分析员' }
const canWrite = computed(() => me.value && me.value.role !== 'ANALYST')
const canAdmin = computed(() => me.value?.role === 'ADMIN')
const visibleMenus = computed(() => menus.filter(m => !['users','simulation'].includes(m.id) || canAdmin.value))
const localDate = (offset = 0) => { const d = new Date(Date.now() + offset * 86400000); return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).format(d) }
const isoNow = () => new Date(Date.now() + 8 * 3600000).toISOString().slice(0, 16)
const filters = reactive({ regionId: '', startDate: localDate(-29), endDate: localDate(), elderId: '', doctorId: '', abnormal: '', status: '', trajectoryDate: localDate() })
function nextDay(date) { return new Date(new Date(date + 'T00:00:00+08:00').getTime() + 86400000 + 8 * 3600000).toISOString().slice(0, 10) }
const timeQuery = computed(() => ({ start: filters.startDate + 'T00:00:00+08:00', end: nextDay(filters.endDate) + 'T00:00:00+08:00', regionId: filters.regionId }))
const fmt = value => value == null ? '—' : String(value).replace('T', ' ').replace(/\+08:00$/, '').slice(0, 19)
const n = value => Number(value || 0).toLocaleString('zh-CN')
const label = value => ({ ACTIVE: '在册', ARCHIVED: '已归档', MALE: '男', FEMALE: '女', UNKNOWN: '未知', ONLINE: '在线', OFFLINE: '离线', DISABLED: '已停用', PENDING: '待随访', COMPLETED: '已完成', CANCELED: '已取消', ADMIN: '管理员', OPERATOR: '业务人员', ANALYST: '分析员' }[value] || value)
const optionsElders = computed(() => refs.elders.filter(e => e.status === 'ACTIVE').map(e => [e.id, `${e.name} · ${e.code}`]))
const optionsDoctors = computed(() => refs.doctors.filter(d => d.enabled).map(d => [d.id, d.name]))
const regionName = id => refs.regions.find(r => String(r.id) === String(id))?.name || '—'
const mapPoints = computed(() => deviceRows.value.filter(d => d.location).map(d => ({ ...d.location, id: d.id, status: d.status, label: `${d.elderName || '未绑定'} · ${d.serialNo}` })))
const selectedFences = computed(() => page.value === 'trajectory' ? allFences.value.filter(f => f.elderIds.map(String).includes(String(filters.elderId))) : allFences.value)
const gaps = computed(() => points.value.filter((p, i, all) => i && new Date(p.recordedAt) - new Date(all[i - 1].recordedAt) > 600000).length)
let serial = 0, timer
async function safe(fn) { error.value = ''; success.value = ''; try { return await fn() } catch (e) { error.value = e.message; return null } }
async function allPages(path) { const first = await api(path, { query: { size: 100 } }); const result = [...first.records]; for (let p = 2; p <= Math.ceil(Number(first.total) / 100); p++) result.push(...(await api(path, { query: { page: p, size: 100 } })).records); return result }
async function loadRefs() { const [regions, elders, doctors] = await Promise.all([api('/regions'), allPages('/elders'), allPages('/doctors')]); Object.assign(refs, { regions, elders, doctors }); if (!filters.elderId && elders.length) filters.elderId = elders[0].id }
async function load() {
  if (!me.value) return
  const turn = ++serial; loading.value = true; error.value = ''; playing.value = false
  try {
    const q = { ...timeQuery.value, page: pageNo.value, size: 10 }, id = page.value
    let result, stats = {}
    if (id === 'home') { const data = await api('/dashboard', { query: q }); if (turn !== serial) return; summary.value = data; return }
    if (id === 'population') [result, stats] = await Promise.all([api('/elders', { query: { ...q, keyword: search.value, status: filters.status } }), api('/statistics/population', { query: { regionId: filters.regionId } })])
    if (id === 'health') [result, stats] = await Promise.all([api('/health-records', { query: { ...q, abnormal: filters.abnormal } }), api('/statistics/health', { query: q })])
    if (id === 'followup') [result, stats] = await Promise.all([api('/followup-plans', { query: { ...q, doctorId: filters.doctorId, status: filters.status } }), api('/statistics/followups', { query: { ...q, doctorId: filters.doctorId } })])
    if (id === 'devices') { const [d, f] = await Promise.all([api('/devices/distribution', { query: { regionId: filters.regionId, status: filters.status } }), api('/geofences')]); if (turn !== serial) return; deviceRows.value = d; allFences.value = f; rows.value = d; total.value = d.length; return }
    if (id === 'fences') { const [f, a] = await Promise.all([api('/geofences'), api('/geofence-alerts', { query: { ...q, page: alertPage.value, handled: filters.status } })]); if (turn !== serial) return; allFences.value = f; rows.value = f; total.value = f.length; alerts.value = a.records; alertTotal.value = Number(a.total); return }
    if (id === 'trajectory') { const f = await api('/geofences'); if (turn !== serial) return; allFences.value = f; if (filters.elderId) { const p = await api(`/elders/${filters.elderId}/trajectory`, { query: { start: filters.trajectoryDate + 'T00:00:00+08:00', end: nextDay(filters.trajectoryDate) + 'T00:00:00+08:00' } }); if (turn !== serial) return; points.value = p; index.value = 0 } return }
    if (id === 'simulation') return
    if (id === 'doctors') result = await api('/doctors', { query: q })
    if (id === 'users') result = await api('/users', { query: q })
    if (turn !== serial) return; rows.value = result?.records || []; total.value = Number(result?.total || 0); summary.value = stats
  } catch (e) { if (turn === serial) { rows.value = []; points.value = []; summary.value = {}; error.value = e.message } }
  finally { if (turn === serial) loading.value = false }
}
function navigate(id) { page.value = id; location.hash = id }
watch(page, () => { pageNo.value = 1; alertPage.value = 1; filters.status = ''; search.value = ''; selectedDevice.value = null; rows.value = []; summary.value = {}; load() })
function applyFilters() { pageNo.value = 1; alertPage.value = 1; load() }
function resetFilters() { Object.assign(filters, { regionId: '', startDate: localDate(-29), endDate: localDate(), doctorId: '', abnormal: '', status: '' }); search.value = ''; applyFilters() }
async function login() {
  loginBusy.value = true
  await safe(async () => {
    await refreshCsrf()
    const res = await api('/auth/login', {
      method: 'POST',
      data: { username: username.value, password: password.value }
    })
    if (res?.accessToken) {
      setTokens(res)
    }
    me.value = res?.user || res
    password.value = ''
    await refreshCsrf()
    if (page.value === 'users' && me.value.role !== 'ADMIN') page.value = 'home'
    await loadRefs()
    await load()
  })
  loginBusy.value = false
}

async function logout() {
  await safe(async () => {
    const { refreshToken } = getTokens()
    await api('/auth/logout', { method: 'POST', data: { refreshToken } }).catch(() => {})
    clearTokens()
    me.value = null
    serial++
    points.value = []
    rows.value = []
    summary.value = {}
    await refreshCsrf()
  })
}

function expire() {
  clearTokens()
  me.value = null
  serial++
  playing.value = false
  error.value = '会话已失效，请重新登录'
}

function hashChange() {
  const route = location.hash.slice(1)
  if (visibleMenus.value.some(m => m.id === route)) page.value = route
}

onMounted(async () => {
  window.addEventListener('session-expired', expire)
  window.addEventListener('hashchange', hashChange)
  await safe(async () => {
    const { accessToken, refreshToken } = getTokens()
    if (accessToken || refreshToken) {
      try {
        me.value = await api('/auth/me')
      } catch (e) {
        if (e.status !== 401) throw e
      }
    } else {
      await refreshCsrf()
    }
    if (me.value) {
      if (!visibleMenus.value.some(m => m.id === page.value)) page.value = 'home'
      await loadRefs()
      await load()
    }
  })
  ready.value = true
  timer = setInterval(() => {
    if (playing.value && index.value < points.value.length - 1)
      index.value = Math.min(points.value.length - 1, index.value + Number(speed.value))
    else
      playing.value = false
  }, 500)
})
onBeforeUnmount(() => { clearInterval(timer); window.removeEventListener('session-expired', expire); window.removeEventListener('hashchange', hashChange) })

const modal = ref(), modalTitle = ref(''), form = reactive({}), fields = ref([]), saving = ref(false)
let savePath, saveMethod, afterSave
function field(key, name, type = 'text', options, required = true) { return { key, name, type, options, required } }
function openForm(title, fs, values, path, method = 'POST', callback) { modalTitle.value = title; fields.value = fs; Object.keys(form).forEach(k => delete form[k]); Object.assign(form, values); savePath = path; saveMethod = method; afterSave = callback; modal.value.showModal(); nextTick(() => modal.value.querySelector('input,select,textarea')?.focus()) }
function edit(kind, row) {
  const common = { version: row?.version }, optionsRegions = refs.regions.map(r => [r.id, r.name]); const base = row ? { ...row } : {}; let fs, values, path
  if (kind === 'elder') { fs = [field('code', '档案编号'), field('name', '姓名'), field('gender', '性别', 'select', [['MALE', '男'], ['FEMALE', '女'], ['UNKNOWN', '未知']]), field('birthDate', '出生日期', 'date', null, false), field('regionId', '所属片区', 'select', optionsRegions), field('phone', '联系电话', 'text', null, false)]; values = { code: base.code || '', name: base.name || '', gender: base.gender || 'UNKNOWN', birthDate: base.birthDate || '', regionId: base.regionId || refs.regions[0]?.id, phone: base.phone || '', ...(row ? common : {}) }; path = '/elders' }
  if (kind === 'doctor') { fs = [field('code', '医生编号'), field('name', '姓名'), field('department', '科室'), field('phone', '电话', 'text', null, false), field('enabled', '启用', 'checkbox')]; values = { code: base.code || '', name: base.name || '', department: base.department || '全科医学', phone: base.phone || '', enabled: base.enabled ?? true, ...(row ? common : {}) }; path = '/doctors' }
  if (kind === 'device') { fs = [field('serialNo', '设备序列号'), field('model', '型号'), field('enabled', '启用', 'checkbox')]; values = { serialNo: base.serialNo || '', model: base.model || '教学模拟腕表', enabled: base.enabled ?? true, ...(row ? common : {}) }; path = '/devices' }
  if (kind === 'fence') { fs = [field('name', '围栏名称'), field('centerLon', '中心经度', 'number'), field('centerLat', '中心纬度', 'number'), field('radiusM', '半径 米', 'number'), field('enabled', '启用', 'checkbox')]; values = { name: base.name || '', centerLon: base.centerLon ?? 120.16, centerLat: base.centerLat ?? 30.25, radiusM: base.radiusM ?? 650, enabled: base.enabled ?? true, ...(row ? common : {}) }; path = '/geofences' }
  if (kind === 'user') { fs = [field('username', '用户名'), field('displayName', '显示名称'), field('role', '角色', 'select', [['ADMIN', '管理员'], ['OPERATOR', '业务人员'], ['ANALYST', '只读分析员']]), field('enabled', '启用', 'checkbox')]; if (!row) fs.push(field('password', '初始密码 至少12字符', 'password')); values = { username: base.username || '', displayName: base.displayName || '', role: base.role || 'ANALYST', enabled: base.enabled ?? true, ...(!row ? { password: '' } : {}) }; path = '/users' }
  openForm(`${row ? '编辑' : '新增'}${{ elder: '老人档案', doctor: '医生', device: '腕表', fence: '电子围栏', user: '账号' }[kind]}`, fs, values, path + (row ? '/' + row.id : ''), row ? 'PUT' : 'POST')
}
function addHealth() { openForm('录入健康检测', [field('elderId', '老人', 'select', optionsElders.value), field('measuredAt', '检测时间', 'datetime-local'), ...[['systolic', '收缩压 mmHg'], ['diastolic', '舒张压 mmHg'], ['heartRate', '心率 次/分'], ['oxygen', '血氧 %'], ['temperature', '体温 ℃']].map(([k, t]) => field(k, t, 'number', null, false))], { elderId: optionsElders.value[0]?.[0], measuredAt: isoNow(), systolic: '', diastolic: '', heartRate: '', oxygen: '', temperature: '' }, '/health-records') }
function addPlan() { openForm('新增随访计划', [field('elderId', '老人', 'select', optionsElders.value), field('doctorId', '负责医生', 'select', optionsDoctors.value), field('dueAt', '计划到期时间', 'datetime-local')], { elderId: optionsElders.value[0]?.[0], doctorId: optionsDoctors.value[0]?.[0], dueAt: isoNow() }, '/followup-plans') }
function complete(row) { openForm('完成随访 · ' + row.elderName, [field('completedAt', '完成时间', 'datetime-local'), field('content', '随访内容', 'textarea'), field('result', '随访结果', 'textarea')], { completedAt: isoNow(), content: '', result: '' }, `/followup-plans/${row.id}/complete`) }
function noteAction(title, path, key = 'reason', extra = {}) { openForm(title, [field(key, '说明', 'textarea')], { [key]: '', ...extra }, path) }
function bind(row) { openForm('绑定腕表 · ' + row.serialNo, [field('elderId', '老人', 'select', optionsElders.value)], { elderId: optionsElders.value[0]?.[0] }, `/devices/${row.id}/bind`) }
function members(row) { openForm('关联围栏成员 · ' + row.name, [field('elderIds', '选择老人 可多选', 'multi', optionsElders.value, false)], { elderIds: row.elderIds.map(String) }, `/geofences/${row.id}/members`, 'PUT') }
async function submit() { saving.value = true; await safe(async () => { const payload = { ...form }; for (const f of fields.value) { if (f.type === 'number') payload[f.key] = payload[f.key] === '' ? null : Number(payload[f.key]); if (f.type === 'datetime-local') payload[f.key] = payload[f.key] + ':00+08:00' } await api(savePath, { method: saveMethod, data: payload }); modal.value.close(); success.value = '已保存'; await loadRefs(); await load(); if (afterSave) await afterSave() }); saving.value = false }
const confirmation = ref(), confirmMessage = ref(''); let confirmed
function confirmAction(message, action) { confirmMessage.value = message; confirmed = action; confirmation.value.showModal() }
async function executeConfirmed() { saving.value = true; await safe(async () => { const value = await confirmed(); confirmation.value.close(); await loadRefs(); await load(); success.value = '操作完成'; if (value?.apiKey) { credentialResult.value = value; await nextTick(); credentialDialog.value.showModal() } }); saving.value = false }
function archive(row) { confirmAction(`归档 ${row.name} 后将结束设备绑定、围栏监测及未完成随访，历史记录保留。`, () => api(`/elders/${row.id}/archive`, { method: 'POST' })) }
function removeElder(row) { confirmAction(`仅允许删除没有健康、随访、设备、定位或围栏记录的误录档案。确认删除 ${row.name}？`, () => api(`/elders/${row.id}`, { method: 'DELETE' })) }
function unbind(row) { confirmAction(`解除 ${row.serialNo} 与 ${row.elderName} 的绑定？历史轨迹会保留。`, () => api(`/devices/${row.id}/unbind`, { method: 'POST' })) }
function issueCredential(row) { confirmAction(`为 ${row.serialNo} 生成新的设备接入密钥？原密钥会立即失效。`, () => api(`/devices/${row.id}/credential`, { method: 'POST' })) }
function resetPassword(row) { openForm('重置密码 · ' + row.displayName, [field('password', '新密码 至少12字符', 'password')], { password: '' }, `/users/${row.id}/reset-password`) }
const credentialDialog = ref(), credentialResult = ref()
const detail = ref(), detailTitle = ref(''), detailRecord = ref(), detailTrend = ref([]), metric = ref('systolic'), detailElderId = ref()
async function showTrend(row) { detailElderId.value = row.elderId; detailTitle.value = row.elderName + ' · 健康趋势'; detailRecord.value = null; await loadTrend(); detail.value.showModal() }
async function loadTrend() { await safe(async () => { const list = await api(`/elders/${detailElderId.value}/health-trend`, { query: { ...timeQuery.value, metric: metric.value } }); detailTrend.value = list.filter(r => r[metric.value] != null).map(r => ({ name: fmt(r.measuredAt).slice(0, 10), value: r[metric.value] })) }) }
function showRecord(row) { detailTitle.value = row.elderName + ' · 随访记录'; detailRecord.value = row.record; detail.value.showModal() }
</script>

<template>
  <div v-if="!ready" class="boot">正在连接医养服务平台…</div>
  <main v-else-if="!me" class="login-screen">
    <section class="login-intro"><div class="brand"><span class="brand-symbol">✚</span><div>智慧医养<small>公共服务平台</small></div></div><div class="login-copy"><p class="eyebrow">医养结合 · 数据决策</p><h1>让服务有依据<br>让关怀可追踪</h1><p>人口、健康、随访与活动轨迹<br>在同一平台形成完整的服务视图。</p><div class="login-modules"><span>人口分析</span><span>健康监测</span><span>随访管理</span><span>活动守护</span></div></div><div class="login-bottom">智慧医养大数据决策分析系统</div></section>
    <section class="login-form-area"><form class="login-form" @submit.prevent="login"><span class="eyebrow">欢迎使用</span><h2>登录工作台</h2><p>请输入管理员分配的账号与密码。</p><label>用户名<input v-model="username" autocomplete="username" required maxlength="64" placeholder="请输入用户名"></label><label>密码<input v-model="password" type="password" autocomplete="current-password" required maxlength="72" placeholder="请输入密码"></label><div v-if="error" class="message error" role="alert">{{ error }}</div><button class="primary login-button" :disabled="loginBusy">{{ loginBusy ? '正在登录…' : '登录平台 →' }}</button><small>首次账号及密码请查阅本地部署说明。</small></form></section>
  </main>
  <div v-else class="app-shell">
    <aside class="sidebar"><a class="brand" href="#home" @click.prevent="navigate('home')"><span class="brand-symbol">✚</span><div>智慧医养<small>大数据决策分析</small></div></a><div class="nav-caption">服务管理</div><nav><button v-for="m in visibleMenus" :key="m.id" :class="{ active: page === m.id }" @click="navigate(m.id)"><span class="nav-icon" aria-hidden="true">{{ m.icon }}</span>{{ m.label }}<span v-if="page === m.id" class="nav-indicator"></span></button></nav><div class="sidebar-footer"><span class="live-dot"></span>医养结合公共服务平台<small>数据汇聚 · 服务协同</small></div></aside>
    <div class="workspace"><header class="topbar"><span>智慧医养大数据公共服务平台 <span class="divider">/</span> <strong>决策分析系统</strong></span><div class="user-menu"><span class="avatar">{{ me.displayName.slice(0, 1) }}</span><span>{{ me.displayName }}<small>{{ roleNames[me.role] }}</small></span><button class="text-button" @click="logout">退出</button></div></header>
      <main class="content"><div class="page-heading"><div><p class="eyebrow">{{ page === 'home' ? '服务运行概况' : '数据与服务' }}</p><h1>{{ titles[page] }}</h1><p>{{ descriptions[page] }}</p></div><div class="date-badge">{{ localDate() }}<small>Asia/Shanghai</small></div></div>
      <div v-if="error" class="message error" role="alert">{{ error }} <button class="text-button" @click="load">重新加载</button></div><div v-if="success" class="message success" role="status">{{ success }}</div>
      <form v-if="!['users', 'doctors', 'simulation'].includes(page)" class="filterbar" @submit.prevent="applyFilters">
        <label v-if="!['fences', 'trajectory'].includes(page)">所属片区<select v-model="filters.regionId"><option value="">全部片区</option><option v-for="r in refs.regions" :key="r.id" :value="r.id">{{ r.name }}</option></select></label>
        <template v-if="['home', 'health', 'followup', 'fences'].includes(page)"><label>开始日期<input v-model="filters.startDate" type="date" required :max="filters.endDate"></label><span class="date-to">至</span><label>结束日期<input v-model="filters.endDate" type="date" required :min="filters.startDate"></label></template>
        <label v-if="page === 'population'">档案检索<input v-model="search" placeholder="姓名或档案编号"></label>
        <label v-if="page === 'health'">检测结果<select v-model="filters.abnormal"><option value="">全部结果</option><option value="true">存在异常</option><option value="false">无异常项</option></select></label>
        <label v-if="page === 'followup'">负责医生<select v-model="filters.doctorId"><option value="">全部医生</option><option v-for="d in refs.doctors" :value="d.id" :key="d.id">{{ d.name }}</option></select></label>
        <label v-if="['population','followup','devices','fences'].includes(page)">状态<select v-model="filters.status"><option value="">全部状态</option><template v-if="page === 'population'"><option value="ACTIVE">在册</option><option value="ARCHIVED">已归档</option></template><template v-if="page === 'followup'"><option value="PENDING">待随访</option><option value="COMPLETED">已完成</option><option value="CANCELED">已取消</option></template><template v-if="page === 'devices'"><option value="ONLINE">在线</option><option value="OFFLINE">离线</option><option value="UNKNOWN">未知</option><option value="DISABLED">已停用</option></template><template v-if="page === 'fences'"><option value="false">告警未处理</option><option value="true">告警已处理</option></template></select></label>
        <template v-if="page === 'trajectory'"><label>老人<select v-model="filters.elderId"><option v-for="e in refs.elders" :value="e.id" :key="e.id">{{ e.name }} · {{ e.code }}</option></select></label><label>回放日期<input v-model="filters.trajectoryDate" type="date" required :max="localDate()"></label></template>
        <button class="primary" :disabled="loading">{{ loading ? '查询中…' : '查询' }}</button><button type="button" class="secondary" @click="resetFilters">重置</button>
      </form>

      <div v-if="loading" class="loading-line" role="status">正在加载最新数据…</div>
      <template v-if="page === 'home' && summary.population">
        <div class="metric-grid"><button class="metric" @click="navigate('population')"><span>在册老人</span><strong>{{ n(summary.population.total) }}<small>人</small></strong><em>查看人口结构 →</em></button><button class="metric" @click="navigate('health')"><span>已检测老人</span><strong>{{ n(summary.health.summary.people) }}<small>人</small></strong><em>当前日期范围内去重人数 →</em></button><button class="metric amber" @click="navigate('health')"><span>健康异常老人</span><strong>{{ n(summary.health.summary.abnormalPeople) }}<small>人</small></strong><em>含至少一项演示阈值异常 →</em></button><button class="metric" @click="navigate('followup')"><span>随访完成率</span><strong>{{ summary.followups.summary.completionRate ?? '—' }}<small>%</small></strong><em>{{ n(summary.followups.summary.completed) }} / {{ n(summary.followups.summary.planned) }} 项计划 →</em></button></div>
        <div class="chart-grid wide-left"><Chart title="健康检测趋势" type="line" :rows="summary.health.trend"/><Chart title="人口年龄结构" :rows="summary.population.age"/></div>
        <div class="chart-grid"><Chart title="片区服务人口" :rows="summary.population.region"/><section class="panel overview-panel"><div class="panel-title"><h2>设备与随访</h2><button class="text-button" @click="navigate('devices')">查看腕表 →</button></div><div class="overview-number"><span class="live-dot"></span><strong>{{ n(summary.devices.ONLINE) }}</strong><span>在线腕表</span></div><div class="overview-list"><div><span>离线腕表</span><strong>{{ n(summary.devices.OFFLINE) }}</strong></div><div><span>心跳未知</span><strong>{{ n(summary.devices.UNKNOWN) }}</strong></div><div><span>逾期未完成随访</span><strong class="warning-text">{{ n(summary.followups.summary.overdue) }}</strong></div><div><span>逾期完成随访</span><strong>{{ n(summary.followups.summary.late) }}</strong></div></div><button class="secondary" @click="navigate('fences')">查看电子围栏事件 →</button></section></div>
      </template>
      <template v-if="page === 'population'"><div class="section-note">当前在册人口 {{ n(summary.total) }} 人。下方列表可包含已归档档案，图表仅统计在册老人。</div><div class="chart-grid three"><Chart title="年龄结构" :rows="summary.age"/><Chart title="性别分布" type="pie" :rows="summary.gender"/><Chart title="地区分布" :rows="summary.region"/></div></template>
      <template v-if="page === 'health' && summary.summary"><div class="metric-grid"><div class="metric"><span>检测记录</span><strong>{{ n(summary.summary.measurements) }}<small>次</small></strong></div><div class="metric"><span>检测人数</span><strong>{{ n(summary.summary.people) }}<small>人</small></strong></div><div class="metric amber"><span>异常人数</span><strong>{{ n(summary.summary.abnormalPeople) }}<small>人</small></strong></div><div class="metric"><span>异常记录</span><strong>{{ n(summary.summary.abnormalRecords) }}<small>条</small></strong></div></div><Chart title="检测次数趋势" type="line" :rows="summary.trend"/><p class="section-note">阈值用于教学演示；缺测不视为正常，结果不用于临床诊断。点击老人姓名查看单项趋势。</p></template>
      <template v-if="page === 'followup' && summary.summary"><div class="metric-grid"><div class="metric"><span>应随访计划</span><strong>{{ n(summary.summary.planned) }}</strong></div><div class="metric"><span>已完成</span><strong>{{ n(summary.summary.completed) }}</strong></div><div class="metric amber"><span>逾期未完成</span><strong>{{ n(summary.summary.overdue) }}</strong></div><div class="metric"><span>完成率</span><strong>{{ summary.summary.completionRate ?? '无计划' }}<small v-if="summary.summary.completionRate != null">%</small></strong></div></div><Chart title="医生已完成随访" :rows="summary.doctors?.map(d => ({name:d.name,value:d.completed}))"/><p class="section-note">分母为所选期间到期且未取消的计划；已完成按查询截止时点统计。</p></template>

      <template v-if="page === 'devices'"><div class="map-layout"><GeoCanvas :points="mapPoints" :fences="allFences" @select="p => selectedDevice = deviceRows.find(d => d.id === p.id)"/><section class="panel map-details"><h2>定位详情</h2><template v-if="selectedDevice"><h3>{{ selectedDevice.elderName || '未绑定老人' }}</h3><dl><dt>设备编号</dt><dd>{{ selectedDevice.serialNo }}</dd><dt>设备状态</dt><dd>{{ label(selectedDevice.status) }}</dd><dt>最后心跳</dt><dd>{{ fmt(selectedDevice.lastSeenAt) }}</dd><dt>最后定位</dt><dd>{{ fmt(selectedDevice.location?.recordedAt) }}</dd><dt>经纬度</dt><dd>{{ selectedDevice.location?.longitude }}<br>{{ selectedDevice.location?.latitude }}</dd></dl></template><p v-else class="muted">点击定位点或下方设备行查看详情。</p><p class="section-note">在线状态以最近 5 分钟内的心跳为准，无定位记录不显示点位。</p></section></div></template>
      <template v-if="page === 'fences'"><GeoCanvas :fences="allFences"/><p class="section-note">修改范围后重新判断下一条有效定位；处理告警与返回围栏为两个独立状态。</p></template>
      <template v-if="page === 'trajectory'"><GeoCanvas :points="points" :fences="selectedFences" trajectory :index="index"/><section class="panel player"><button class="primary" :disabled="!points.length" @click="() => { if(index === points.length - 1) index = 0; playing = !playing }">{{ playing ? '暂停' : '播放' }}</button><input v-model.number="index" type="range" min="0" :max="Math.max(0, points.length - 1)" :disabled="!points.length" aria-label="回放进度" @input="playing = false"><select v-model="speed" aria-label="播放速度"><option :value="1">1 倍速</option><option :value="2">2 倍速</option><option :value="4">4 倍速</option></select><span>{{ points.length ? index + 1 : 0 }} / {{ points.length }} 点</span><strong>{{ fmt(points[index]?.recordedAt) }}</strong></section><p class="section-note">{{ gaps }} 处超过 10 分钟的定位缺口，路线在缺口处断开。连线仅表示相邻定位点，不代表真实道路。单次最多 24 小时、5000 点。</p></template>
      <SimulationPanel v-if="page === 'simulation'"/>

      <section v-if="!['home','trajectory','simulation'].includes(page)" class="panel data-panel"><div class="panel-title"><h2>{{ { population:'老人档案',health:'检测明细',followup:'随访计划',devices:'腕表设备',fences:'围栏配置',doctors:'医生列表',users:'账号列表' }[page] }}<small>{{ n(total) }} 条</small></h2><div v-if="canWrite"><button v-if="page === 'population'" class="primary" @click="edit('elder')">＋ 新增档案</button><button v-if="page === 'health'" class="primary" @click="addHealth">＋ 录入检测</button><button v-if="page === 'followup'" class="primary" @click="addPlan">＋ 新增计划</button><button v-if="page === 'devices'" class="primary" @click="edit('device')">＋ 注册腕表</button><button v-if="page === 'fences'" class="primary" @click="edit('fence')">＋ 新增围栏</button><button v-if="page === 'doctors'" class="primary" @click="edit('doctor')">＋ 新增医生</button><button v-if="page === 'users'" class="primary" @click="edit('user')">＋ 新增账号</button></div></div>
        <div class="table-wrap"><table><thead><tr v-if="page === 'population'"><th>档案编号</th><th>姓名</th><th>性别</th><th>出生日期</th><th>所属片区</th><th>电话</th><th>状态</th><th v-if="canWrite">操作</th></tr><tr v-if="page === 'health'"><th>老人</th><th>检测时间</th><th>血压 mmHg</th><th>心率</th><th>血氧 %</th><th>体温 ℃</th><th>结果</th></tr><tr v-if="page === 'followup'"><th>老人</th><th>负责医生</th><th>到期时间</th><th>状态</th><th>操作</th></tr><tr v-if="page === 'devices'"><th>序列号</th><th>绑定老人</th><th>型号</th><th>心跳状态</th><th>最后心跳</th><th>操作</th></tr><tr v-if="page === 'fences'"><th>名称</th><th>中心经纬度</th><th>半径</th><th>关联人数</th><th>状态</th><th v-if="canWrite">操作</th></tr><tr v-if="page === 'doctors'"><th>编号</th><th>姓名</th><th>科室</th><th>电话</th><th>状态</th><th v-if="canWrite">操作</th></tr><tr v-if="page === 'users'"><th>用户名</th><th>显示名称</th><th>角色</th><th>状态</th><th>操作</th></tr></thead>
          <tbody><tr v-for="row in rows" :key="row.id">
            <template v-if="page === 'population'"><td class="mono">{{ row.code }}</td><td class="strong">{{ row.name }}</td><td>{{ label(row.gender) }}</td><td>{{ row.birthDate || '未知' }}</td><td>{{ regionName(row.regionId) }}</td><td>{{ row.phone || '—' }}</td><td><span class="tag" :class="row.status === 'ARCHIVED' ? 'neutral' : ''">{{ label(row.status) }}</span></td><td v-if="canWrite"><button v-if="row.status === 'ACTIVE'" class="text-button" @click="edit('elder',row)">编辑</button><button v-if="row.status === 'ACTIVE'" class="text-button warning-text" @click="archive(row)">归档</button><button v-if="canAdmin" class="text-button danger-text" @click="removeElder(row)">删除误录</button></td></template>
            <template v-if="page === 'health'"><td><button class="text-button strong" @click="showTrend(row)">{{ row.elderName }}</button></td><td>{{ fmt(row.measuredAt) }}</td><td>{{ row.systolic ?? '—' }} / {{ row.diastolic ?? '—' }}</td><td>{{ row.heartRate ?? '—' }}</td><td>{{ row.oxygen ?? '—' }}</td><td>{{ row.temperature ?? '—' }}</td><td><span class="tag" :class="row.abnormal ? 'warning' : ''">{{ row.abnormal ? '存在异常' : '无异常项' }}</span></td></template>
            <template v-if="page === 'followup'"><td class="strong">{{ row.elderName }}</td><td>{{ row.doctorName }}</td><td>{{ fmt(row.dueAt) }}</td><td><span class="tag" :class="row.overdue || row.late ? 'warning' : row.status === 'CANCELED' ? 'neutral' : ''">{{ row.overdue ? '已逾期' : row.late ? '逾期完成' : label(row.status) }}</span></td><td><template v-if="canWrite && row.status === 'PENDING'"><button class="text-button" @click="complete(row)">完成随访</button><button class="text-button warning-text" @click="noteAction('取消随访',`/followup-plans/${row.id}/cancel`)">取消</button></template><button v-if="row.record" class="text-button" @click="showRecord(row)">查看记录</button></td></template>
            <template v-if="page === 'devices'"><td class="mono">{{ row.serialNo }}</td><td>{{ row.elderName || '未绑定' }}</td><td>{{ row.model }}</td><td><span class="tag" :class="row.status === 'ONLINE' ? '' : 'neutral'">{{ label(row.status) }}</span></td><td>{{ fmt(row.lastSeenAt) }}</td><td><button class="text-button" @click="selectedDevice = row">详情</button><template v-if="canWrite"><button class="text-button" @click="edit('device',row)">编辑</button><button v-if="!row.elderId && row.enabled" class="text-button" @click="bind(row)">绑定</button><button v-if="row.elderId" class="text-button warning-text" @click="unbind(row)">解绑</button></template><button v-if="canAdmin && row.enabled" class="text-button" @click="issueCredential(row)">接入密钥</button></td></template>
            <template v-if="page === 'fences'"><td class="strong">{{ row.name }}</td><td class="mono">{{ row.centerLon }}, {{ row.centerLat }}</td><td>{{ row.radiusM }} 米</td><td>{{ row.elderIds.length }}</td><td><span class="tag" :class="row.enabled ? '' : 'neutral'">{{ row.enabled ? '启用' : '停用' }}</span></td><td v-if="canWrite"><button class="text-button" @click="edit('fence',row)">编辑</button><button class="text-button" @click="members(row)">关联成员</button></td></template>
            <template v-if="page === 'doctors'"><td class="mono">{{ row.code }}</td><td class="strong">{{ row.name }}</td><td>{{ row.department }}</td><td>{{ row.phone || '—' }}</td><td><span class="tag" :class="row.enabled ? '' : 'neutral'">{{ row.enabled ? '启用' : '停用' }}</span></td><td v-if="canWrite"><button class="text-button" @click="edit('doctor',row)">编辑</button></td></template>
            <template v-if="page === 'users'"><td class="mono">{{ row.username }}</td><td class="strong">{{ row.displayName }}</td><td>{{ label(row.role) }}</td><td><span class="tag" :class="row.enabled ? '' : 'neutral'">{{ row.enabled ? '启用' : '停用' }}</span></td><td><button class="text-button" @click="edit('user',row)">编辑</button><button class="text-button" @click="resetPassword(row)">重置密码</button></td></template>
          </tr><tr v-if="!rows.length"><td colspan="8" class="empty-cell">{{ loading ? '正在加载…' : '当前条件下暂无记录' }}</td></tr></tbody></table></div>
        <div v-if="!['devices','fences'].includes(page)" class="pagination"><span>共 {{ n(total) }} 条，每页 10 条</span><div><button class="secondary" :disabled="pageNo === 1 || loading" @click="pageNo--; load()">上一页</button><span>{{ pageNo }} / {{ Math.max(1, Math.ceil(total/10)) }}</span><button class="secondary" :disabled="pageNo * 10 >= total || loading" @click="pageNo++; load()">下一页</button></div></div>
      </section>
      <section v-if="page === 'fences'" class="panel data-panel"><div class="panel-title"><h2>越界事件<small>{{ n(alertTotal) }} 条</small></h2></div><div class="table-wrap"><table><thead><tr><th>老人</th><th>围栏</th><th>触发时间</th><th>返回时间</th><th>监测状态</th><th>处理状态</th><th>操作</th></tr></thead><tbody><tr v-for="a in alerts" :key="a.id"><td>{{ a.elderName }}</td><td>{{ a.fenceName }}</td><td>{{ fmt(a.triggeredAt) }}</td><td>{{ fmt(a.returnedAt) }}</td><td>{{ a.returnedAt ? '已返回' : a.closedAt ? '监测已结束' : '越界中' }}</td><td><span class="tag" :class="a.handledAt ? '' : 'warning'">{{ a.handledAt ? '已处理' : '待处理' }}</span><small v-if="a.handlingNote" class="cell-note">{{ a.handlingNote }}</small></td><td><button v-if="canWrite && !a.handledAt" class="text-button" @click="noteAction('处理越界事件',`/geofence-alerts/${a.id}/handle`,'note',{version:a.version})">处理</button></td></tr><tr v-if="!alerts.length"><td colspan="7" class="empty-cell">当前条件下暂无越界事件</td></tr></tbody></table></div><div class="pagination"><span>第 {{ alertPage }} 页</span><div><button class="secondary" :disabled="alertPage === 1 || loading" @click="alertPage--; load()">上一页</button><button class="secondary" :disabled="alertPage * 10 >= alertTotal || loading" @click="alertPage++; load()">下一页</button></div></div></section>
      <footer class="page-footer">医养数据决策分析 <span>数据按所选条件从服务端查询 · 时间以北京时间为准</span></footer>
      </main>
    </div>
  </div>
  <dialog ref="modal" class="form-dialog" @cancel="() => { if(saving) return }"><form @submit.prevent="submit"><div class="dialog-header"><h2>{{ modalTitle }}</h2><button type="button" class="close-button" aria-label="关闭" @click="modal.close()">×</button></div><div class="dialog-body"><div v-if="error" class="message error" role="alert">{{ error }}</div><label v-for="f in fields" :key="f.key" :class="{ 'check-label': f.type === 'checkbox' }">{{ f.name }}<span v-if="f.required && f.type !== 'checkbox'" class="required-mark">*</span><select v-if="f.type === 'select'" v-model="form[f.key]" :required="f.required"><option v-for="[value, text] in f.options" :key="value" :value="value">{{ text }}</option></select><select v-else-if="f.type === 'multi'" v-model="form[f.key]" multiple size="8"><option v-for="[value,text] in f.options" :key="value" :value="String(value)">{{ text }}</option></select><textarea v-else-if="f.type === 'textarea'" v-model="form[f.key]" :required="f.required" rows="4" maxlength="2000"></textarea><input v-else-if="f.type === 'checkbox'" v-model="form[f.key]" type="checkbox"><input v-else v-model="form[f.key]" :type="f.type" :step="f.type === 'number' ? 'any' : undefined" :required="f.required" :minlength="f.type === 'password' ? 12 : undefined" :maxlength="f.type === 'password' ? 72 : 100" :autocomplete="f.type === 'password' ? 'new-password' : 'off'"><small v-if="f.type === 'multi'">按住 Ctrl 可选择多位老人，清空选择可移除全部成员。</small></label></div><div class="dialog-actions"><button type="button" class="secondary" :disabled="saving" @click="modal.close()">取消</button><button class="primary" :disabled="saving">{{ saving ? '正在保存…' : '保存' }}</button></div></form></dialog>
  <dialog ref="confirmation" class="form-dialog compact"><div class="dialog-header"><h2>确认操作</h2></div><div class="dialog-body"><p>{{ confirmMessage }}</p><div v-if="error" class="message error">{{ error }}</div></div><div class="dialog-actions"><button class="secondary" :disabled="saving" @click="confirmation.close()">取消</button><button class="primary" :disabled="saving" @click="executeConfirmed">确认</button></div></dialog>
  <dialog ref="credentialDialog" class="form-dialog compact"><div class="dialog-header"><h2>设备接入密钥</h2><button class="close-button" aria-label="关闭" @click="credentialDialog.close()">×</button></div><div class="dialog-body"><p>密钥仅显示一次，请立即保存到设备或安全配置中。再次生成会使旧密钥失效。</p><label>设备<span class="mono">{{ credentialResult?.serialNo }}</span></label><label>接入密钥<textarea class="mono" readonly :value="credentialResult?.apiKey" rows="3"></textarea></label></div><div class="dialog-actions"><button class="primary" @click="credentialDialog.close()">我已保存</button></div></dialog>
  <dialog ref="detail" class="form-dialog detail-dialog"><div class="dialog-header"><h2>{{ detailTitle }}</h2><button class="close-button" aria-label="关闭" @click="detail.close()">×</button></div><div class="dialog-body"><template v-if="detailRecord"><dl><dt>完成时间</dt><dd>{{ fmt(detailRecord.completedAt) }}</dd><dt>随访内容</dt><dd>{{ detailRecord.content }}</dd><dt>随访结果</dt><dd>{{ detailRecord.result }}</dd></dl></template><template v-else><label>检测指标<select v-model="metric" @change="loadTrend"><option value="systolic">收缩压 mmHg</option><option value="diastolic">舒张压 mmHg</option><option value="heartRate">心率 次/分</option><option value="oxygen">血氧 %</option><option value="temperature">体温 ℃</option></select></label><Chart title="单项指标趋势" type="line" :rows="detailTrend"/></template></div></dialog>
  <!-- 智慧医养 AI 专属健康守护悬浮球与对话工作台 -->
  <AiAssistantOrb />
</template>
