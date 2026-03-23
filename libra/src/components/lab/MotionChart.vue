<template>
  <div class="motion-chart-container glass-card">
    <div class="chart-header">
      <h5 class="chart-title">{{ title }}</h5>
      <div class="chart-legend">
        <span v-for="item in series" :key="item.name" class="legend-item">
          <span class="dot" :style="{ background: item.color }"></span>
          {{ item.name }}
        </span>
      </div>
    </div>
    <div ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';

const props = defineProps({
  title: String,
  data: {
    type: Array, // [{ x: timestamp, y: value, ... }]
    required: true
  },
  xAxisKey: { type: String, default: 'timestamp' },
  series: {
    type: Array, // [{ key: 'kneeAngle', name: '膝角', color: '#5C6E58' }]
    required: true
  },
  yAxisName: String
});

const chartRef = ref(null);
let myChart = null;

const initChart = () => {
  if (!chartRef.value) return;
  
  myChart = echarts.init(chartRef.value);
  updateChart();
};

const updateChart = () => {
  if (!myChart || !props.data.length) return;

  const option = {
    grid: { left: '8%', right: '4%', bottom: '15%', top: '10%' },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#EEE',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    xAxis: {
      type: 'category',
      data: props.data.map(d => d[props.xAxisKey]),
      axisLine: { lineStyle: { color: '#EEE' } },
      axisLabel: { color: '#999', fontSize: 10, interval: Math.floor(props.data.length / 5) }
    },
    yAxis: {
      type: 'value',
      name: props.yAxisName,
      splitLine: { lineStyle: { color: '#F5F5F5', type: 'dashed' } },
      axisLabel: { color: '#999' }
    },
    series: props.series.map(s => ({
      name: s.name,
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: props.data.map(d => d[s.key]),
      itemStyle: { color: s.color },
      lineStyle: { width: 3, cap: 'round' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: s.color + '33' },
          { offset: 1, color: s.color + '00' }
        ])
      }
    }))
  };

  myChart.setOption(option);
};

onMounted(() => {
  initChart();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  myChart?.dispose();
});

const handleResize = () => {
  myChart?.resize();
};

watch(() => props.data, updateChart, { deep: true });
</script>

<style scoped>
.motion-chart-container {
  padding: 20px;
  margin-bottom: 20px;
  background: #FFF;
  border-radius: 16px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.chart-title {
  margin: 0;
  font-size: 1rem;
  color: var(--text-main);
  font-family: var(--font-serif);
}
.chart-legend {
  display: flex;
  gap: 15px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--text-muted);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.chart-body {
  height: 240px;
  width: 100%;
}
</style>
