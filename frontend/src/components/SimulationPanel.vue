<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElAlert, ElButton, ElCheckbox, ElDatePicker, ElForm, ElFormItem, ElInputNumber, ElOption, ElSelect } from 'element-plus'
import 'element-plus/es/components/alert/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/checkbox/style/css'
import 'element-plus/es/components/date-picker/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/select/style/css'
import { api } from '../api'
import GeoCanvas from './GeoCanvas.vue'

const devices = ref([]), fences = ref([]), preview = ref([]), busy = ref(false), error = ref(''), result = ref()
const form = reactive({ deviceId: '', scenario: 'LINE', startAt: localInput(Date.now() - 9 * 60000), intervalSeconds: 60, count: 10, startLon: 120.1600, startLat: 30.2500, endLon: 120.1690, endLat: 30.2570, fenceId: '', radiusM: 500, heartbeat: true })
const boundDevices = computed(() => devices.value.filter(d => d.enabled && d.elderId))
const selectedFence = computed(() => fences.value.find(f => String(f.id) === String(form.fenceId)))

function localInput(ms) { return new Date(ms + 8 * 3600000).toISOString().slice(0, 16) }
function offset(lon, lat, eastM, northM) { return { longitude: lon + eastM / (111195 * Math.cos(lat * Math.PI / 180)), latitude: lat + northM / 111195 } }
function isoAt(i) { return new Date(new Date(form.startAt + ':00+08:00').getTime() + i * Number(form.intervalSeconds) * 1000).toISOString() }
function fixed(value) { return Number(Number(value).toFixed(7)) }
function makePreview() {
  error.value = '';result.value = null
  const count = form.scenario === 'MANUAL' ? 1 : Math.max(2, Math.min(500, Number(form.count) || 2))
  if (!form.deviceId) { preview.value = [];error.value = '请选择已经绑定老人的启用腕表';return }
  if (!form.startAt || Number(form.intervalSeconds) < 1) { preview.value = [];error.value = '开始时间和上报间隔不正确';return }
  const batch = [], fence = selectedFence.value
  for (let i = 0; i < count; i++) {
    const ratio = count === 1 ? 0 : i / (count - 1);let longitude, latitude
    if (form.scenario === 'LINE' || form.scenario === 'MANUAL') {
      longitude = Number(form.startLon) + (Number(form.endLon) - Number(form.startLon)) * ratio
      latitude = Number(form.startLat) + (Number(form.endLat) - Number(form.startLat)) * ratio
    } else if (form.scenario === 'CIRCLE') {
      const p = offset(Number(form.startLon), Number(form.startLat), Math.cos(ratio * Math.PI * 2) * Number(form.radiusM), Math.sin(ratio * Math.PI * 2) * Number(form.radiusM));({ longitude, latitude } = p)
    } else {
      if (!fence) { preview.value = [];error.value = '越界返回场景需要选择电子围栏';return }
      const journey = ratio <= .5 ? ratio * 2 : (1 - ratio) * 2
      const p = offset(Number(fence.centerLon), Number(fence.centerLat), journey * Number(fence.radiusM) * 1.5, 0);({ longitude, latitude } = p)
    }
    batch.push({ id: i + 1, eventId: `sim-${Date.now()}-${i + 1}`, longitude: fixed(longitude), latitude: fixed(latitude), recordedAt: isoAt(i), label: `预览点 ${i + 1}` })
  }
  preview.value = batch
}
async function load() {
  busy.value = true;error.value = ''
  try { const [d, f] = await Promise.all([api('/devices/distribution'), api('/geofences')]);devices.value = d;fences.value = f;if (!form.deviceId && boundDevices.value.length) form.deviceId = boundDevices.value[0].id;if (!form.fenceId && f.length) form.fenceId = f[0].id;makePreview() }
  catch (e) { error.value = e.status === 404 ? '数据模拟功能只在 demo 环境开放。' : e.message }
  finally { busy.value = false }
}
async function inject() {
  makePreview();if (!preview.value.length || error.value) return
  busy.value = true;error.value = '';result.value = null
  try {
    const points = preview.value.map(({ eventId, longitude, latitude, recordedAt }) => ({ eventId, longitude, latitude, recordedAt }))
    const batchResult = points.length === 1
      ? await api('/demo/locations', { method: 'POST', data: { deviceId: Number(form.deviceId), ...points[0] } })
      : await api('/demo/locations/batch', { method: 'POST', data: { deviceId: Number(form.deviceId), points } })
    if (form.heartbeat) await api('/demo/heartbeats', { method: 'POST', data: { deviceId: Number(form.deviceId), eventId: `sim-heartbeat-${Date.now()}`, recordedAt: new Date().toISOString() } })
    result.value = points.length === 1 ? { received: 1, accepted: 1, duplicates: 0 } : batchResult
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
watch(() => form.scenario, makePreview)
onMounted(load)
</script>

<template>
  <section class="simulation-layout">
    <el-form class="panel simulation-form" label-position="top" @submit.prevent="inject">
      <div class="panel-title"><h2>定位数据模拟</h2><el-button :loading="busy" @click="load">刷新设备</el-button></div>
      <p class="section-note">仅供 demo 环境联调。选择已绑定腕表，预览后一次注入最多 500 个 WGS84 定位点。</p>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false"/><el-alert v-if="result" :title="`注入完成：接收 ${result.received} 点，新增 ${result.accepted} 点，重复 ${result.duplicates} 点。`" type="success" show-icon :closable="false"/>
      <div class="simulation-fields">
        <el-form-item label="腕表"><el-select v-model="form.deviceId" placeholder="请选择" @change="makePreview"><el-option v-for="d in boundDevices" :key="d.id" :label="`${d.serialNo} · ${d.elderName}`" :value="d.id"/></el-select></el-form-item>
        <el-form-item label="场景"><el-select v-model="form.scenario"><el-option label="手动单点" value="MANUAL"/><el-option label="直线轨迹" value="LINE"/><el-option label="环形轨迹" value="CIRCLE"/><el-option label="越界并返回" value="FENCE_CROSSING"/></el-select></el-form-item>
        <el-form-item v-if="form.scenario === 'FENCE_CROSSING'" label="电子围栏"><el-select v-model="form.fenceId" @change="makePreview"><el-option v-for="f in fences" :key="f.id" :label="f.name" :value="f.id"/></el-select></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm" format="YYYY/MM/DD HH:mm" @change="makePreview"/></el-form-item>
        <el-form-item v-if="form.scenario !== 'MANUAL'" label="定位点数"><el-input-number v-model="form.count" :min="2" :max="500" :controls="false" @change="makePreview"/></el-form-item>
        <el-form-item v-if="form.scenario !== 'MANUAL'" label="间隔 秒"><el-input-number v-model="form.intervalSeconds" :min="1" :max="3600" :controls="false" @change="makePreview"/></el-form-item>
        <template v-if="form.scenario !== 'FENCE_CROSSING'">
          <el-form-item :label="form.scenario === 'CIRCLE' ? '圆心经度' : '起点经度'"><el-input-number v-model="form.startLon" :min="-180" :max="180" :precision="7" :step="0.0001" :controls="false" @change="makePreview"/></el-form-item>
          <el-form-item :label="form.scenario === 'CIRCLE' ? '圆心纬度' : '起点纬度'"><el-input-number v-model="form.startLat" :min="-90" :max="90" :precision="7" :step="0.0001" :controls="false" @change="makePreview"/></el-form-item>
          <template v-if="form.scenario === 'LINE'"><el-form-item label="终点经度"><el-input-number v-model="form.endLon" :min="-180" :max="180" :precision="7" :step="0.0001" :controls="false" @change="makePreview"/></el-form-item><el-form-item label="终点纬度"><el-input-number v-model="form.endLat" :min="-90" :max="90" :precision="7" :step="0.0001" :controls="false" @change="makePreview"/></el-form-item></template>
          <el-form-item v-if="form.scenario === 'CIRCLE'" label="半径 米"><el-input-number v-model="form.radiusM" :min="10" :max="5000" :controls="false" @change="makePreview"/></el-form-item>
        </template>
      </div>
      <el-checkbox v-model="form.heartbeat" class="simulation-check">同时发送一次当前心跳</el-checkbox>
      <div class="simulation-actions"><el-button :disabled="busy" @click="makePreview">重新预览</el-button><el-button native-type="submit" type="primary" :loading="busy" :disabled="!preview.length">{{ `注入 ${preview.length} 个定位点` }}</el-button></div>
    </el-form>
    <div><GeoCanvas :points="preview" :fences="form.scenario === 'FENCE_CROSSING' && selectedFence ? [selectedFence] : []" trajectory :index="Math.max(0, preview.length - 1)"/><section class="panel preview-summary"><h2>批量轨迹预览</h2><p>首点：{{ preview[0]?.longitude ?? '—' }}, {{ preview[0]?.latitude ?? '—' }}</p><p>末点：{{ preview.at(-1)?.longitude ?? '—' }}, {{ preview.at(-1)?.latitude ?? '—' }}</p><p>时间：{{ preview[0]?.recordedAt ?? '—' }} 至 {{ preview.at(-1)?.recordedAt ?? '—' }}</p></section></div>
  </section>
</template>

<style scoped>
.simulation-layout{display:grid;grid-template-columns:minmax(330px,1fr) minmax(0,1.5fr);gap:23px}.simulation-form{margin:0;--el-color-primary:#0d908d;--el-color-success:#1a8c65;--el-border-radius-base:8px}.simulation-fields{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:18px}.simulation-fields :deep(.el-form-item){margin-bottom:0}.simulation-fields :deep(.el-select),.simulation-fields :deep(.el-date-editor),.simulation-fields :deep(.el-input-number){width:100%}.simulation-form :deep(.el-alert){margin-top:14px}.simulation-check{margin:20px 0}.simulation-actions{display:flex;justify-content:flex-end;gap:12px;border-top:1px solid var(--border);padding-top:20px}.preview-summary p{font-size:.82rem;color:#657d85;margin:8px 0;overflow-wrap:anywhere}.preview-summary h2{margin-bottom:14px}@media(max-width:1000px){.simulation-layout{grid-template-columns:1fr}}@media(max-width:600px){.simulation-fields{grid-template-columns:1fr}}
</style>
