<template>
  <div class="compare-page">
    <div class="container lab-container">

      <!-- 1. 顶部操作栏 -->
      <!-- 1. 顶部操作栏 -->
      <div class="toolbar-row" v-scroll-reveal>
        <div class="text-group">
          <div class="back-link" @click="$router.back()">
            <i class="fas fa-arrow-left"></i> <span>返回档案列表</span>
          </div>
          <h1 class="page-title">对比分析</h1>
          <p class="page-subtitle">BIOMECHANICAL COMPARISON / 深度差异矩阵报告</p>
        </div>
        <div class="tabs-group">
          <div class="actions">
            <button class="btn btn-outline btn-sm" @click="handlePrint">
              <i class="fas fa-print"></i> 打印报告
            </button>
          </div>
        </div>
      </div>

      <div v-if="loading" class="lab-loading">
        <i class="fas fa-circle-notch fa-spin"></i> 正在进行交叉比对分析...
      </div>

      <div v-else-if="compData" class="compare-main animate-fade">

        <!-- 2. 核心机能：对比雷达图 -->
        <section class="radar-section glass-card" v-scroll-reveal>
          <div class="radar-info">
            <h3 class="card-title">机能维度解析</h3>
            <div class="legend">
              <div class="l-item base">
                <span class="dot"></span>
                历史: {{ formatDate(compData.baseRecord.createTime) }}
              </div>
              <div class="l-item target">
                <span class="dot"></span>
                当前: {{ formatDate(compData.targetRecord.createTime) }}
              </div>
            </div>
          </div>
          <div ref="radarRef" class="radar-chart"></div>
        </section>

        <!-- 3. 详细参数差异矩阵 -->
        <section class="matrix-section glass-card" v-scroll-reveal>
          <h3 class="card-title">参数差异矩阵</h3>
          <div class="table-responsive">
            <table class="diff-table">
              <thead>
              <tr>
                <th>评估维度</th>
                <th>历史快照</th>
                <th>当前状态</th>
                <th>进化趋势</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="item in compData.diffMatrix" :key="item.label">
                <td class="dim-info">
                  <span class="label">{{ item.label }}</span>
                  <span class="significance">{{ item.significance }}</span>
                </td>
                <td class="val-muted">{{ item.baseVal }}<small>{{ item.unit }}</small></td>
                <td class="val-strong">{{ item.targetVal }}<small>{{ item.unit }}</small></td>
                <td>
                  <div class="trend-badge" :class="item.trend">
                    <i :class="getTrendIcon(item.trend)"></i>
                    <span class="delta">{{ item.delta > 0 ? '+' : '' }}{{ item.delta }}</span>
                  </div>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- 4. 专家诊断建议 -->
        <section class="expert-conclusion glass-card" v-scroll-reveal>
          <div class="conclusion-header">
            <i class="fas fa-stethoscope"></i>
            <span>Fittor 专家进化诊断</span>
          </div>
          <p class="conclusion-text">{{ compData.evolutionSummary }}</p>
        </section>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { compareUserBody } from '@/api/userBody.js';
import * as echarts from 'echarts/core';
import { RadarChart } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([RadarChart, CanvasRenderer]);
import dayjs from 'dayjs';

const route = useRoute();
const compData = ref(null);
const loading = ref(true);
const radarRef = ref(null);
let myChart = null;

const formatDate = (d) => dayjs(d).format('YYYY-MM-DD');
const getTrendIcon = (t) => t === 'UP' ? 'fas fa-caret-up' : (t === 'DOWN' ? 'fas fa-caret-down' : 'fas fa-minus');

/**
 * 初始化雷达图
 */
const initRadar = () => {
  if (!radarRef.value) return;
  myChart = echarts.init(radarRef.value);

  const b = compData.value.baseRecord;
  const t = compData.value.targetRecord;

  const option = {
    color: ['#B5A495', '#5C6E58'],
    radar: {
      indicator: [
        { name: '柔韧性', max: 100 },
        { name: '稳定性', max: 100 },
        { name: '胸椎旋转', max: 90 },
        { name: '胸椎伸展', max: 45 },
        { name: '举臂角度', max: 180 }
      ],
      shape: 'circle',
      splitNumber: 4,
      axisName: { color: '#787673', fontWeight: 600 },
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } },
      splitArea: { show: false }
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: [b.flexibilityScore, b.stabilityScore, b.thoracicRotationAngle, b.thoracicExtensionAngle, b.armRaiseAngle],
          name: '历史',
          lineStyle: { type: 'dashed', width: 1 },
          areaStyle: { color: 'rgba(181, 164, 149, 0.1)' }
        },
        {
          value: [t.flexibilityScore, t.stabilityScore, t.thoracicRotationAngle, t.thoracicExtensionAngle, t.armRaiseAngle],
          name: '当前',
          symbol: 'circle', symbolSize: 6,
          lineStyle: { width: 3 },
          areaStyle: { color: 'rgba(92, 110, 88, 0.25)' }
        }
      ]
    }]
  };
  myChart.setOption(option);
};

onMounted(async () => {
  const ids = route.query.ids?.split(',');
  if (!ids || ids.length < 2) return;

  try {
    const res = await compareUserBody(ids[0], ids[1]);
    compData.value = res.data || res;
    setTimeout(() => initRadar(), 100); // 确保 DOM 已渲染
  } catch (e) {
    console.error("对比数据加载失败", e);
  } finally {
    loading.value = false;
  }

  window.addEventListener('resize', () => myChart?.resize());
});

onUnmounted(() => {
  window.removeEventListener('resize', () => myChart?.resize());
  myChart?.dispose();
});

const handlePrint = () => window.print();
</script>

<style scoped>
.compare-page {
  padding-top: var(--header-height);
  min-height: 100vh;
  background: transparent;
}

.lab-container {
  padding-top: 40px;
  padding-bottom: 80px;
}

/* Toolbar */
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.page-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  margin-bottom: 5px;
  color: var(--text-main);
  line-height: 1.1;
}

.page-subtitle {
  font-size: 1rem;
  color: var(--text-muted);
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: var(--primary-color);
  margin-bottom: 12px;
  cursor: pointer;
  font-weight: 600;
  transition: 0.3s;
  opacity: 0.8;
}

.back-link:hover {
  opacity: 1;
  transform: translateX(-4px);
}

.compare-main { display: flex; flex-direction: column; gap: 30px; }

/* 雷达图区块 */
.radar-section { padding: 40px; display: flex; align-items: center; gap: 50px; }
.radar-info { flex: 0.4; }
.radar-chart { flex: 1; height: 450px; }

.legend { margin-top: 30px; display: flex; flex-direction: column; gap: 12px; }
.l-item { display: flex; align-items: center; gap: 10px; font-size: 0.9rem; color: var(--text-muted); }
.l-item .dot { width: 12px; height: 12px; border-radius: 50%; }
.l-item.base .dot { border: 1px dashed var(--secondary-color); background: rgba(181, 164, 149, 0.2); }
.l-item.target .dot { background: var(--primary-color); box-shadow: 0 0 10px rgba(92, 110, 88, 0.3); }

/* 差异矩阵表 */
.matrix-section { padding: 40px; }
.diff-table { width: 100%; border-collapse: collapse; margin-top: 20px; }
.diff-table th { text-align: left; padding: 15px 20px; font-size: 0.85rem; color: var(--text-muted); border-bottom: 2px solid var(--bg-page); }
.diff-table td { padding: 25px 20px; border-bottom: 1px solid var(--bg-page); }

.dim-info .label { display: block; font-weight: 700; color: var(--text-main); font-size: 1rem; }
.dim-info .significance { display: block; font-size: 0.75rem; color: var(--primary-light); margin-top: 5px; font-style: italic; }

.val-muted { color: var(--text-muted); font-size: 0.95rem; }
.val-strong { font-weight: 700; color: var(--text-main); font-size: 1.1rem; }

.trend-badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 4px; font-weight: 700; }
.trend-badge.UP { color: var(--success-color); background: rgba(110, 133, 101, 0.1); }
.trend-badge.DOWN { color: var(--danger-color); background: rgba(193, 140, 126, 0.1); }
.trend-badge.STABLE { color: #999; background: #F5F5F5; }

/* 专家建议 */
.expert-conclusion { padding: 40px; border-left: 6px solid var(--primary-color); }
.conclusion-header { display: flex; align-items: center; gap: 12px; font-weight: 800; color: var(--primary-color); font-size: 1.1rem; margin-bottom: 20px; }
.conclusion-text { font-size: 1.1rem; line-height: 1.8; color: var(--text-body); font-family: var(--font-serif); font-style: italic; }

/* 打印优化 */
@media print {
  .navbar, .compare-header, .btn { display: none !important; }
  .compare-page { padding: 0; }
  .glass-card { box-shadow: none; border: 1px solid #EEE; }
}

@media (max-width: 1024px) {
  .radar-section { flex-direction: column; text-align: center; }
  .radar-chart { width: 100%; }
}
</style>