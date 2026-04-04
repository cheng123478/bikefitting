<template>
  <div class="dynamic-report-page">
    <div class="container report-container">

      <!-- 1. Top Navigation & Meta -->
      <header class="report-header fade-up">
        <button class="back-link" @click="$router.push('/lab/archives/dynamic')">
          <i class="fas fa-chevron-left"></i> 返回存档
        </button>
        <div class="meta-info">
          <span class="date">{{ report?.createTime ? formatDate(report.createTime) : '--' }}</span>
        </div>
      </header>

      <div v-if="loading" class="loading-full">
        <div class="pulse-loader"></div>
        <p>Aries 正在调取您的动态轨迹数据...</p>
      </div>

      <div v-else-if="report" class="report-body">

        <!-- 2. Hero Component: Score & Summary -->
        <section class="hero-section glass-card fade-up delay-1">
          <div class="score-orbit">
            <svg viewBox="0 0 100 100" class="ring-svg">
              <circle class="ring-bg" cx="50" cy="50" r="45"></circle>
              <circle class="ring-fill" cx="50" cy="50" r="45" :style="scoreRingStyle"></circle>
            </svg>
            <div class="score-content">
              <span class="value">{{ report.matchScore }}</span>
              <span class="label">MATCH SCORE</span>
            </div>
          </div>
          <div class="summary-content">
            <h2 class="level-tag" :class="getScoreClass(report.matchScore)">{{ report.matchLevel }}</h2>
            <p class="summary-text">{{ report.summary }}</p>
            <div class="quick-stats">
              <div class="stat"><span class="l">采集视角</span><span class="v">{{ report.viewsCount }}</span></div>
              <div class="stat"><span class="l">骑行单车</span><span class="v">{{ report.bikeName }}</span></div>
            </div>
          </div>
        </section>

        <!-- 3. Sub-reports (Side, Front, Back) -->
        <div class="view-reports fade-up delay-2">

          <!-- 侧面视角核心数据 -->
          <div v-if="report.sideMetrics?.length" class="view-item glass-card">
            <h3 class="view-title"><i class="fas fa-bicycle"></i> 侧面视角 (核心几何)</h3>
            <div class="metric-grid">
              <div v-for="m in report.sideMetrics" :key="m.label" class="m-card" :class="m.status">
                <div class="m-head">
                  <span class="m-label">{{ m.label }}</span>
                  <span class="m-status-text">{{ m.status }}</span>
                </div>
                <div class="m-vals">
                  <div class="v-group">
                    <span class="v-label">实测</span>
                    <span class="v-num">{{ m.currentValue }}°</span>
                  </div>
                  <div class="v-group">
                    <span class="v-label">理想</span>
                    <span class="v-num">{{ m.idealRange }}</span>
                  </div>
                </div>
                <p class="m-advice">{{ m.advice }}</p>
              </div>
            </div>
          </div>

          <!-- 正面视角数据 -->
          <div v-if="report.frontMetrics?.length" class="view-item glass-card">
            <h3 class="view-title"><i class="fas fa-arrows-alt-h"></i> 正面视角 (稳定性/风阻)</h3>
            <div class="metric-grid">
              <div v-for="m in report.frontMetrics" :key="m.label" class="m-card" :class="m.status">
                <div class="m-head"><span class="m-label">{{ m.label }}</span></div>
                <div class="m-vals">
                  <div class="v-group"><span class="v-label">偏差</span><span class="v-num">{{ m.currentValue }}</span></div>
                </div>
                <p class="m-advice">{{ m.advice }}</p>
              </div>
            </div>
          </div>

          <!-- 背面视角数据 -->
          <div v-if="report.backMetrics?.length" class="view-item glass-card">
            <h3 class="view-title"><i class="fas fa-user"></i> 背面视角 (力量输出/稳定性)</h3>
            <div class="metric-grid">
              <div v-for="m in report.backMetrics" :key="m.label" class="m-card" :class="m.status">
                <div class="m-head"><span class="m-label">{{ m.label }}</span></div>
                <div class="m-vals">
                  <div class="v-group"><span class="v-label">偏差</span><span class="v-num">{{ m.currentValue }}</span></div>
                </div>
                <p class="m-advice">{{ m.advice }}</p>
              </div>
            </div>
          </div>

        </div>

        <!-- 4. Evidence Gallery: Snapshots -->
        <section class="gallery-section fade-up delay-3">
          <h3 class="section-label">视觉证据 (Evidence)</h3>
          <div v-if="snapshotsLoading" class="mini-loading"><i class="fas fa-spinner fa-spin"></i></div>
          <div v-else class="snapshot-grid">
            <div v-for="snap in snapshots" :key="snap.url" class="snap-card glass-card">
              <img :src="snap.url" :alt="snap.label">
              <div class="snap-info">
                <span class="label">{{ snap.label }}</span>
                <span class="metric">{{ snap.metricValue }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 5. Deep Analysis: Pro Chart -->
        <section class="pro-section fade-up delay-4">
          <div class="section-header">
            <h3 class="section-label">关节轨迹深度分析 (Pro Analysis)</h3>
            <div class="chart-controls">
              <button
                v-for="v in availableMotionViews"
                :key="v"
                class="btn-v"
                :class="{ active: activeView === v }"
                @click="switchView(v)"
              >
                {{ v }}
              </button>
            </div>
          </div>

          <div class="chart-container glass-card">
            <div v-if="motionLoading" class="chart-loading">
              <i class="fas fa-circle-notch fa-spin"></i>
              <span>正在拉取海量时序数据...</span>
            </div>
            <div ref="chartRef" class="main-chart" :style="{ opacity: motionLoading ? 0 : 1 }"></div>
          </div>
        </section>

        <!-- 6. Equipment & Actions -->
        <footer class="report-footer fade-up delay-5">
          <div class="equip-card glass-card" v-if="report.equipmentSuggestions?.length">
            <h4><i class="fas fa-tools"></i> 器材更换/调整建议</h4>
            <ul>
              <li v-for="s in report.equipmentSuggestions" :key="s">{{ s }}</li>
            </ul>
          </div>
          <div class="action-bar">
            <button class="btn btn-outline" @click="window.print()"><i class="fas fa-print"></i> 打印报告</button>
            <button class="btn btn-primary" @click="$router.push('/lab/garage/edit?id=' + report.bikeId)">进入车库调整配置</button>
          </div>
        </footer>

      </div>

      <div v-else class="error-msg">
        <i class="fas fa-heart-broken"></i>
        <h3>报告由于超时或损坏无法加载</h3>
        <button class="btn btn-primary" @click="$router.back()">返回重试</button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { getDiagnosisResult, getSnapshots, getMotionData } from '@/api/dynamicFitting.js';
import { showToast } from '@/utils/toast.js';
import * as echarts from 'echarts';
import dayjs from 'dayjs';

const route = useRoute();
const sessionId = route.query.sessionId;

const loading = ref(true);
const report = ref(null);
const snapshots = ref([]);
const snapshotsLoading = ref(false);

const motionLoading = ref(false);
const activeView = ref('SIDE');
const availableMotionViews = ['SIDE', 'FRONT', 'BACK'];
const chartRef = ref(null);
let myChart = null;

const formatDate = (d) => dayjs(d).format('YYYY-MM-DD HH:mm:ss');

const scoreRingStyle = computed(() => {
  if (!report.value) return {};
  const score = report.value.matchScore || 0;
  const circ = 2 * Math.PI * 45;
  return {
    strokeDasharray: circ,
    strokeDashoffset: circ - (score / 100) * circ
  };
});

const getScoreClass = (s) => {
  if (s >= 90) return 'text-success';
  if (s >= 70) return 'text-warning';
  return 'text-danger';
};

const initReport = async () => {
  if (!sessionId) {
    showToast('缺少 Session ID', 'error');
    return;
  }
  loading.value = true;
  try {
    const res = await getDiagnosisResult(sessionId);
    report.value = res.data || res;

    // Determine the default view based on available data
    let defaultView = 'SIDE';
    if (report.value.sideMetrics?.length) {
      defaultView = 'SIDE';
    } else if (report.value.frontMetrics?.length) {
      defaultView = 'FRONT';
    } else if (report.value.backMetrics?.length) {
      defaultView = 'BACK';
    }

    // Initialize with the dynamic default view
    switchView(defaultView);
  } catch (e) {
    showToast('读取诊断详情失败', 'error');
  } finally {
    loading.value = false;
  }
};

const loadSnapshots = async (view) => {
  activeView.value = view;
  snapshotsLoading.value = true;
  try {
    const res = await getSnapshots(sessionId, view);
    snapshots.value = res.data || res;
  } catch (e) {
    console.error('Snapshots failed');
  } finally {
    snapshotsLoading.value = false;
  }
};

const switchView = (view) => {
  loadMotionData(view);
  loadSnapshots(view.toLowerCase());
};

const loadMotionData = async (view) => {
  activeView.value = view;
  motionLoading.value = true;
  try {
    const res = await getMotionData(sessionId, view);
    let data = res.data || res;

    // Parse the JSON string from the VO
    if (data && typeof data.dataJson === 'string') {
      data = JSON.parse(data.dataJson);
    }

    renderChart(data, view);
  } catch (e) {
    showToast('拉取详细轨迹失败', 'warning');
  } finally {
    motionLoading.value = false;
  }
};

const renderChart = (data, view) => {
  if (!chartRef.value) return;
  if (!myChart) myChart = echarts.init(chartRef.value);

  // 根据视角构造不同的 ECharts 配置
  let option = {};
  if (view === 'SIDE') {
    option = {
      title: { text: '侧面视角 - 关节伸展度动态', textStyle: { color: '#888', fontSize: 13 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['膝角', '髋角'], bottom: 0 },
      xAxis: { type: 'category', data: data.kneeAngles?.map((_, i) => i) || [] },
      yAxis: { type: 'value', min: 30, max: 180 },
      series: [
        { name: '膝角', type: 'line', data: data.kneeAngles, smooth: true, color: '#5C6E58' },
        { name: '髋角', type: 'line', data: data.hipAngles, smooth: true, color: '#D4AF37' }
      ]
    };
  } else {
    // 简化处理其他视角
    option = {
      title: { text: `${view} 视角稳定性数据`, textStyle: { color: '#888', fontSize: 13 } },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category' },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: Object.values(data)[0] || [] }]
    };
  }

  myChart.setOption(option, true);
};

onMounted(() => {
  initReport();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (myChart) myChart.dispose();
});

const handleResize = () => myChart?.resize();

</script>

<style scoped>
.dynamic-report-page { padding: 120px 0 80px; min-height: 100vh; background: transparent; }
.report-container { max-width: 1100px; }

/* Header */
.report-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.back-link { background: none; border: none; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 8px; font-size: 0.95rem; }
.back-link:hover { color: var(--primary-color); }
.meta-info { display: flex; gap: 20px; color: var(--text-muted); font-size: 0.8rem; font-family: monospace; }

/* Loader */
.loading-full { height: 60vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20px; color: var(--text-muted); }
.pulse-loader { width: 40px; height: 40px; border: 4px solid var(--primary-light); border-radius: 50%; border-top-color: var(--primary-color); animation: spin 0.8s infinite linear; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Hero Section */
.hero-section { display: flex; align-items: center; gap: 50px; padding: 50px; margin-bottom: 40px; border-left: 8px solid var(--primary-color); }
.score-orbit { position: relative; width: 160px; height: 160px; flex-shrink: 0; }
.ring-svg { width: 100%; height: 100%; }
.ring-bg { fill: none; stroke: rgba(0,0,0,0.05); stroke-width: 6; }
.ring-fill { fill: none; stroke: var(--primary-color); stroke-width: 6; stroke-linecap: round; transform: rotate(-90deg); transform-origin: 50% 50%; transition: stroke-dashoffset 1s ease-out; }
.score-content { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.score-content .value { font-family: var(--font-serif); font-size: 3.5rem; font-weight: 800; line-height: 1; color: var(--text-main); }
.score-content .label { font-size: 0.7rem; color: var(--text-muted); letter-spacing: 2px; }

.summary-content { flex: 1; }
.level-tag { font-family: var(--font-serif); font-size: 2.2rem; margin-bottom: 15px; }
.summary-text { font-size: 1rem; color: var(--text-body); font-style: italic; line-height: 1.6; margin-bottom: 25px; border-left: 2px solid var(--accent-gold); padding-left: 20px; }
.quick-stats { display: flex; gap: 40px; }
.stat { display: flex; flex-direction: column; }
.stat .l { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 4px; }
.stat .v { font-weight: 700; color: var(--text-main); }

/* View Reports */
.view-reports { margin-bottom: 40px; display: grid; gap: 30px; }
.view-title { font-size: 1.2rem; color: var(--text-main); margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }
.metric-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.m-card { padding: 20px; border-radius: 12px; border: 1px solid rgba(0,0,0,0.05); }
.m-card.ADJUST { background: rgba(212, 175, 55, 0.03); border-left: 4px solid var(--accent-gold); }
.m-card.WARNING { background: rgba(255, 77, 79, 0.03); border-left: 4px solid #ff4d4f; }
.m-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.m-label { font-size: 1rem; font-weight: 700; color: var(--text-main); }
.m-status-text { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; opacity: 0.6; }

.m-vals { display: flex; gap: 30px; margin-bottom: 15px; }
.v-group { display: flex; flex-direction: column; }
.v-label { font-size: 0.7rem; color: var(--text-muted); }
.v-num { font-family: var(--font-serif); font-size: 1.4rem; font-weight: 700; color: var(--primary-color); }

.m-advice { font-size: 0.85rem; color: var(--text-muted); line-height: 1.5; border-top: 1px dotted #ddd; padding-top: 10px; }

/* Snapshots */
.gallery-section { margin-bottom: 50px; }
.section-label { font-size: 1.3rem; color: var(--text-main); margin-bottom: 20px; border-left: 4px solid var(--primary-color); padding-left: 15px; }
.snapshot-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px; }
.snap-card { padding: 10px; border-radius: 12px; }
.snap-card img { width: 100%; aspect-ratio: 4/3; object-fit: cover; border-radius: 8px; margin-bottom: 10px; border: 1px solid #eee; }
.snap-info { display: flex; justify-content: space-between; align-items: center; padding: 0 5px; }
.snap-info .label { font-size: 0.8rem; color: var(--text-muted); }
.snap-info .metric { font-weight: 700; color: var(--primary-color); font-size: 0.9rem; }

/* Pro Analysis */
.pro-section { margin-bottom: 50px; }
.pro-section .section-header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 20px; }
.chart-controls { display: flex; gap: 10px; }
.btn-v { padding: 6px 15px; border-radius: 20px; border: 1px solid #ddd; background: #fff; cursor: pointer; transition: 0.3s; font-size: 0.85rem; }
.btn-v.active { background: var(--primary-color); color: #fff; border-color: var(--primary-color); }

.chart-container { height: 400px; padding: 25px; position: relative; }
.chart-loading { position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 15px; color: var(--text-muted); z-index: 10; }
.main-chart { width: 100%; height: 100%; transition: opacity 0.5s; }

/* Footer */
.report-footer { display: grid; gap: 30px; }
.equip-card { padding: 30px; background: #333; color: #fff; }
.equip-card h4 { color: var(--accent-gold); margin-bottom: 20px; font-size: 1.1rem; }
.equip-card ul { padding-left: 20px; }
.equip-card li { margin-bottom: 12px; font-size: 0.95rem; opacity: 0.9; }
.action-bar { display: flex; gap: 20px; justify-content: center; }

.text-success { color: var(--success-color); }
.text-warning { color: var(--warning-color); }
.text-danger { color: #ff4d4f; }

@media (max-width: 768px) {
  .hero-section { flex-direction: column; padding: 30px; text-align: center; gap: 30px; }
  .summary-text { padding-left: 0; border-left: none; }
  .quick-stats { justify-content: center; }
  .metric-grid { grid-template-columns: 1fr; }
  .action-bar { flex-direction: column; }
}
</style>