<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])
const props = defineProps({ rows: { type: Array, default: () => [] }, type: { default: 'bar' }, title: String })
const host = ref(); let chart, observer
function render() {
  if (!chart) return
  const base = { color: ['#168a89', '#56b7a3', '#edb45b', '#7497b4', '#9aa9c8', '#cf8c9f'], textStyle: { fontFamily: 'Microsoft YaHei, sans-serif', fontSize: 14 }, tooltip: { trigger: props.type === 'pie' ? 'item' : 'axis' }, animationDuration: 350 }
  const data = props.rows.map(row => ({ name: String(row.name).slice(0, 10), value: Number(row.value) }))
  chart.setOption(props.type === 'pie' ? { ...base, legend: { bottom: 0, textStyle: { fontSize: 12 }, formatter: name => `${name} ${data.find(d => d.name === name)?.value ?? 0}` }, series: [{ type: 'pie', radius: ['42%', '65%'], center: ['50%', '43%'], label: { show: false }, data }] } : {
    ...base, grid: { left: 48, right: 20, top: 25, bottom: 60 }, xAxis: { type: 'category', data: data.map(d => d.name), axisLabel: { fontSize: 12, interval: data.length > 10 ? 'auto' : 0, rotate: data.length > 4 ? 25 : 0 }, axisLine: { lineStyle: { color: '#bcccd0' } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf2f3' } } }, series: [{ type: props.type, data: data.map(d => d.value), barMaxWidth: 36, smooth: false, areaStyle: props.type === 'line' ? { opacity: .08 } : undefined, itemStyle: { borderRadius: props.type === 'bar' ? [5, 5, 0, 0] : 0 } }],
  }, true)
}
onMounted(() => { chart = echarts.init(host.value); observer = new ResizeObserver(() => chart.resize()); observer.observe(host.value); render() })
watch(() => [props.rows, props.type], render, { deep: true })
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose() })
</script>
<template><section class="panel chart-panel"><h2>{{ title }}</h2><div v-if="!rows.length" class="chart-empty">当前条件下暂无数据</div><div ref="host" class="chart" role="img" :aria-label="title + '：' + rows.map(r => r.name + ' ' + r.value).join('，')"></div></section></template>
