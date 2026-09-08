<script setup>
import { computed } from 'vue'
const props = defineProps({ points: { type: Array, default: () => [] }, fences: { type: Array, default: () => [] }, trajectory: Boolean, index: { default: 0 } })
const emit = defineEmits(['select'])
const bounds = computed(() => {
  const coordinates = props.points.map(p => [Number(p.longitude), Number(p.latitude)])
  for (const f of props.fences) { const r = Number(f.radiusM) / 111000; coordinates.push([Number(f.centerLon) - r / .86, Number(f.centerLat) - r], [Number(f.centerLon) + r / .86, Number(f.centerLat) + r]) }
  if (!coordinates.length) coordinates.push([120.15, 30.24], [120.17, 30.26])
  const minX = Math.min(...coordinates.map(p => p[0])), maxX = Math.max(...coordinates.map(p => p[0])), minY = Math.min(...coordinates.map(p => p[1])), maxY = Math.max(...coordinates.map(p => p[1]))
  const latitude = (minY + maxY) / 2, cos = Math.cos(latitude * Math.PI / 180)
  const span = Math.max((maxX - minX) * cos, (maxY - minY) * 1.8, .008) * 1.3
  return { cx: (minX + maxX) / 2, cy: latitude, scale: 780 / span, cos }
})
const x = lon => 440 + (Number(lon) - bounds.value.cx) * bounds.value.cos * bounds.value.scale
const y = lat => 260 - (Number(lat) - bounds.value.cy) * bounds.value.scale
const segments = computed(() => {
  const rows = props.points.slice(0, props.index + 1), result = []; let group = []
  for (let i = 0; i < rows.length; i++) {
    if (i && new Date(rows[i].recordedAt) - new Date(rows[i - 1].recordedAt) > 600000) { if (group.length) result.push(group.join(' ')); group = [] }
    group.push(`${x(rows[i].longitude)},${y(rows[i].latitude)}`)
  }
  if (group.length) result.push(group.join(' ')); return result
})
const current = computed(() => props.points[props.index])
</script>
<template><div class="geo-canvas"><div class="map-label"><span class="live-dot"></span>WGS84 坐标示意图 <small>无道路底图</small></div><svg viewBox="0 0 880 520" role="img" aria-label="腕表定位和电子围栏坐标示意图">
  <defs><pattern id="geo-grid" width="44" height="44" patternUnits="userSpaceOnUse"><path d="M 44 0 L 0 0 0 44" fill="none" stroke="#dce9e8" stroke-width="1" /></pattern></defs>
  <rect width="880" height="520" fill="#f0f7f5"/><rect width="880" height="520" fill="url(#geo-grid)"/>
  <g v-for="f in fences" :key="f.id"><circle :cx="x(f.centerLon)" :cy="y(f.centerLat)" :r="Number(f.radiusM) / 111195 * bounds.scale" fill="#168a8914" stroke="#168a89" stroke-width="2" :stroke-dasharray="f.enabled ? '8 5' : '2 8'"/><text :x="x(f.centerLon)" :y="y(f.centerLat) - 12" text-anchor="middle" fill="#23665c" font-size="14">{{ f.name }}</text><circle :cx="x(f.centerLon)" :cy="y(f.centerLat)" r="4" fill="#168a89"/></g>
  <template v-if="trajectory"><polyline v-for="(line, i) in segments" :key="i" :points="line" fill="none" stroke="#1d777c" stroke-width="3" stroke-linejoin="round"/><g v-if="current"><circle :cx="x(current.longitude)" :cy="y(current.latitude)" r="13" fill="#168a8933"/><circle :cx="x(current.longitude)" :cy="y(current.latitude)" r="6" fill="#123c4a" stroke="white" stroke-width="2"/></g></template>
  <g v-else v-for="p in points" :key="p.id" tabindex="0" role="button" :aria-label="p.label || '定位点'" @click="emit('select', p)" @keydown.enter="emit('select', p)"><circle :cx="x(p.longitude)" :cy="y(p.latitude)" r="13" :fill="p.status === 'ONLINE' ? '#168a8922' : '#b8a57b33'"/><circle :cx="x(p.longitude)" :cy="y(p.latitude)" r="6" :fill="p.status === 'ONLINE' ? '#168a89' : '#a28752'" stroke="white" stroke-width="2"/><title>{{ p.label }}</title></g>
  <text x="25" y="490" fill="#54716d" font-size="13">中心 {{ bounds.cx.toFixed(5) }}°E · {{ bounds.cy.toFixed(5) }}°N</text><text x="827" y="35" fill="#54716d" font-size="16">北 ↑</text>
</svg><div v-if="!points.length" class="map-empty">当前没有可显示的定位点</div></div></template>
