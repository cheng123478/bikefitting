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
        <p>Aries 正在调取您的动态分析报告及全部视角序列...</p>
      </div>

      <div v-else-if="report" class="report-body">

        <!-- 2. Meta Section: Score & Summary -->
        <section class="report-hero glass-card fade-up delay-1">
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
            
            <!-- Overall Share Button -->
            <button class="btn btn-outline" style="margin-top:20px" @click="shareReport">
              <i class="fas fa-share-alt"></i> 分享完整报告
            </button>
          </div>
        </section>

        <!-- [NEW] Pedaling Pro Analysis Section -->
        <PedalingAnalysisSection v-if="report.pedalingData" :data="report.pedalingData" />

        <!-- 3. Sequential Perspectives -->
        <div class="view-sequence-container fade-up delay-2">
          
          <template v-for="viewType in availableViews" :key="viewType">
            <section class="view-section glass-card">
              <div class="view-header">
                <h2>
                  <i :class="getViewIcon(viewType)"></i>
                  {{ getViewTitle(viewType) }}视角分析
                </h2>
                <span class="badge">{{ viewType.toUpperCase() }}</span>
              </div>

              <!-- Metrics -->
              <div class="metric-grid">
                <div v-for="m in getMetricsForView(viewType)" :key="m.label" class="m-card" :class="m.status">
                  <div class="m-head">
                    <span class="m-label">{{ m.label }}</span>
                    <span class="m-status-text" v-if="m.status !== 'OK'">{{ m.status }}</span>
                  </div>
                  <div class="m-vals">
                    <div class="v-group">
                      <span class="v-label">实测</span>
                      <span class="v-num">{{ m.currentValue }}<span v-if="viewType === 'side'">°</span></span>
                    </div>
                    <div class="v-group" v-if="m.idealRange && m.idealRange !== '--'">
                      <span class="v-label">理想</span>
                      <span class="v-num">{{ m.idealRange }}</span>
                    </div>
                  </div>
                  <p class="m-advice" v-if="m.advice && m.status !== 'OK'">{{ m.advice }}</p>
                </div>
              </div>

              <!-- Snapshots & Evidence (暂时隐藏) -->
              <!-- 
              <div class="evidence-block" v-if="snapshotsMap[viewType]?.length">
                <h4 class="sub-label"><i class="fas fa-camera"></i> 捕捉帧 (Evidence)</h4>
                <div class="snapshot-grid">
                  <div v-for="snap in snapshotsMap[viewType]" :key="snap.url" class="snap-card">
                    <img :src="snap.url" :alt="snap.label">
                    <div class="snap-info">
                      <span class="label">{{ getReadableLabel(snap.label) }}</span>
                      <span class="metric" v-if="snap.metricValue">{{ snap.metricValue }}</span>
                    </div>
                  </div>
                </div>
              </div>
              -->

              <!-- Pro Chart & Skeleton Animation -->
              <div class="pro-analysis-row">
                <div class="col-animation">
                  <DynamicSkeletonPlayer 
                     :viewType="viewType" 
                     :motionData="motionDataMap[viewType]"
                     :metrics="getMetricsForView(viewType)"
                   />
                </div>
                <div class="col-chart">
                  <div class="chart-container sub-glass-card">
                     <div :ref="el => setChartRef(el, viewType)" class="echarts-dom"></div>
                  </div>
                </div>
              </div>

            </section>
          </template>

        </div>

        <!-- 4. Equipment & Actions -->
        <footer class="report-footer fade-up delay-4">
          <div class="equip-card glass-card" v-if="report.equipmentSuggestions?.length">
            <h4><i class="fas fa-tools"></i> 器材更换/调整建议</h4>
            <ul>
              <li v-for="s in report.equipmentSuggestions" :key="s">{{ s }}</li>
            </ul>
          </div>
          <div class="action-bar">
            <button class="btn btn-outline" @click="window.print()"><i class="fas fa-print"></i> 打印简报</button>
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
import html2canvas from 'html2canvas';

import DynamicSkeletonPlayer from './components/DynamicSkeletonPlayer.vue';
import PedalingAnalysisSection from './components/PedalingAnalysisSection.vue';

const route = useRoute();
const sessionId = route.query.sessionId;

const loading = ref(true);
const report = ref(null);

const availableViews = ref([]);
const snapshotsMap = ref({});
const motionDataMap = ref({});

const echartInstances = {};
const chartRefs = {};

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

const getViewTitle = (vt) => {
  const map = { 'side': '侧面', 'front': '正面', 'back': '背面' };
  return map[vt.toLowerCase()] || vt;
};

const getViewIcon = (vt) => {
  const map = { 'side': 'fa-bicycle', 'front': 'fa-arrows-alt-h', 'back': 'fa-user' };
  return map[vt.toLowerCase()] || 'fa-info-circle';
};

const getMetricsForView = (vt) => {
  if (!report.value) return [];
  vt = vt.toLowerCase();
  if (vt === 'side') return report.value.sideData?.metrics || report.value.sideMetrics || [];
  if (vt === 'front') return report.value.frontData?.metrics || report.value.frontMetrics || [];
  if (vt === 'back') return report.value.backData?.metrics || report.value.backMetrics || [];
  return [];
};

const getReadableLabel = (tag) => {
  const labels = {
    'bdc': '下死点(B.D.C)',
    'tdc': '上死点(T.D.C)',
    'k_ext': '最大伸展',
    'max_wobble': '最大横摆',
    'max_tilt': '最大倾斜',
    'max_sway': '骨盆最大摇摆',
    'max_drop': '最大下坠'
  };
  return labels[tag] || tag;
};

const setChartRef = (el, viewType) => {
  if (el) {
    chartRefs[viewType] = el;
  }
};

const initReport = async () => {
  if (!sessionId) {
    showToast('缺少 Session ID', 'error');
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const res = await getDiagnosisResult(sessionId);
    report.value = res.data || res;

    const possibleViews = ['side', 'front', 'back'];
    const activeViews = [];

    // Step 1: Sequential or parallel check for each view type's data
    await Promise.all(possibleViews.map(async (vt) => {
      const metrics = getMetricsForView(vt);
      if (metrics.length > 0) {
        activeViews.push(vt);
        await loadViewData(vt); // Pre-load details
        return;
      }
      
      // If no metrics, check if there are snapshots or motion sequence data
      try {
        const [snapRes, motionRes] = await Promise.all([
          getSnapshots(sessionId, vt),
          getMotionData(sessionId, vt)
        ]);
        const snapshots = snapRes.data || snapRes;
        const motion = motionRes.data || motionRes;
        
        if ((snapshots && snapshots.length > 0) || (motion && motion.dataJson)) {
          activeViews.push(vt);
          snapshotsMap.value[vt] = snapshots;
          if (motion && typeof motion.dataJson === 'string') {
             motion.parsedData = JSON.parse(motion.dataJson);
          }
          motionDataMap.value[vt] = motion;
        }
      } catch (e) {
        console.warn(`Check failed for ${vt}`, e);
      }
    }));

    // Ensure order is preserved for consistent report (side -> front -> back)
    availableViews.value = possibleViews.filter(v => activeViews.includes(v));

    nextTick(() => {
      availableViews.value.forEach(vt => renderEchart(vt));
    });

  } catch (e) {
    console.error(e);
    showToast('读取诊断详情失败', 'error');
  } finally {
    loading.value = false;
  }
};

const loadViewData = async (viewType) => {
  try {
    const [snapRes, motionRes] = await Promise.all([
      getSnapshots(sessionId, viewType.toLowerCase()),
      getMotionData(sessionId, viewType.toLowerCase())
    ]);
    
    snapshotsMap.value[viewType] = snapRes.data || snapRes;
    
    let motionData = motionRes.data || motionRes;
    if (motionData && typeof motionData.dataJson === 'string') {
       // Deep copy and parse to inject into Echarts more easily, 
       // but keep the string for the Player
       motionData.parsedData = JSON.parse(motionData.dataJson);
    }
    motionDataMap.value[viewType] = motionData;
  } catch (e) {
    console.warn(`Failed to load extended data for ${viewType}`);
  }
};

const renderEchart = (viewType) => {
  const dom = chartRefs[viewType];
  const motionData = motionDataMap.value[viewType];
  if (!dom || !motionData || !motionData.parsedData) return;

  if (!echartInstances[viewType]) {
    echartInstances[viewType] = echarts.init(dom);
  }
  const myChart = echartInstances[viewType];
  
  const parsed = motionData.parsedData;
  let option = {};

  if (viewType === 'side') {
    // Collect knee/hip arrays from frameData
    const kneeAngles = parsed.map(f => f.metrics?.kneeAngle || 0);
    const hipAngles = parsed.map(f => f.metrics?.hipAngle || 0);
    const timeAxis = parsed.map(f => (f.ts/1000).toFixed(1));

    option = {
      title: { text: '时序伸展角度', textStyle: { color: '#888', fontSize: 12 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['膝角', '髋角'], bottom: 0, icon: 'circle' },
      grid: { left: '10%', right: '5%', bottom: '15%', top: '15%' },
      xAxis: { type: 'category', data: timeAxis, boundaryGap: false },
      yAxis: { type: 'value', min: 30, max: 180 },
      series: [
        { name: '膝角', type: 'line', data: kneeAngles, smooth: true, lineStyle: { color: '#5C6E58', width: 2 }, symbol: 'none' },
        { name: '髋角', type: 'line', data: hipAngles, smooth: true, lineStyle: { color: '#D4AF37', width: 2 }, symbol: 'none' }
      ]
    };
  } else if (viewType === 'front') {
    const lWobbles = parsed.map(f => f.metrics?.leftKneeWobble || 0);
    const rWobbles = parsed.map(f => f.metrics?.rightKneeWobble || 0);
    const timeAxis = parsed.map(f => (f.ts/1000).toFixed(1));

    option = {
      title: { text: '膝部横摆率 (%)', textStyle: { color: '#888', fontSize: 12 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['左膝', '右膝'], bottom: 0, icon: 'circle' },
      grid: { left: '10%', right: '5%', bottom: '15%', top: '15%' },
      xAxis: { type: 'category', data: timeAxis, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [
        { name: '左膝', type: 'line', data: lWobbles, smooth: true, lineStyle: { color: '#007BFF', width: 2 }, symbol: 'none' },
        { name: '右膝', type: 'line', data: rWobbles, smooth: true, lineStyle: { color: '#DC3545', width: 2 }, symbol: 'none' }
      ]
    };
  } else if (viewType === 'back') {
    const sway = parsed.map(f => f.metrics?.pelvisSway || 0);
    const timeAxis = parsed.map(f => (f.ts/1000).toFixed(1));

    option = {
      title: { text: '骨盆摇摆时序', textStyle: { color: '#888', fontSize: 12 } },
      tooltip: { trigger: 'axis' },
      grid: { left: '10%', right: '5%', bottom: '15%', top: '15%' },
      xAxis: { type: 'category', data: timeAxis, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [
        { name: '骨盆摇摆', type: 'line', data: sway, smooth: true, areaStyle: { color: 'rgba(212, 175, 55, 0.2)' }, lineStyle: { color: '#D4AF37' }, symbol: 'none' }
      ]
    };
  }

  myChart.setOption(option, true);
};

// Handle Overall Share (generating an image of the report)
const shareReport = async () => {
  try {
    const reportDom = document.querySelector('.report-container');
    if (!reportDom) return;
    showToast('正在生成报告长图，请稍候...','info');
    const canvas = await html2canvas(reportDom, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff'
    });
    const url = canvas.toDataURL('image/jpeg', 0.9);
    const a = document.createElement('a');
    a.href = url;
    a.download = `FittingReport_${new Date().getTime()}.jpg`;
    a.click();
    showToast('报告生成成功','success');
  } catch(e) {
    console.error(e);
    showToast('生成分享图片失败','error');
  }
};

const handleResize = () => {
  Object.values(echartInstances).forEach(ins => ins.resize());
};

onMounted(() => {
  initReport();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  Object.values(echartInstances).forEach(ins => {
    if (ins) ins.dispose();
  });
});

</script>

<style scoped>
.dynamic-report-page { padding: 100px 0 60px; min-height: 100vh; background: transparent; }
.report-container { max-width: 1100px; }

/* Header */
.report-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.back-link { background: none; border: none; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 8px; font-size: 0.95rem; }
.back-link:hover { color: var(--primary-color); }
.meta-info { display: flex; gap: 20px; color: var(--text-muted); font-size: 0.8rem; font-family: monospace; }

/* Loader */
.loading-full { height: 60vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20px; color: var(--text-muted); }
.pulse-loader { width: 40px; height: 40px; border: 4px solid var(--primary-light); border-radius: 50%; border-top-color: var(--primary-color); animation: spin 0.8s infinite linear; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Report Hero Section */
.report-hero { display: flex; align-items: center; gap: 25px; padding: 12px 25px; margin-bottom: 15px; border-left: 6px solid var(--primary-color); }
.score-orbit { position: relative; width: 90px; height: 90px; flex-shrink: 0; }
.ring-svg { width: 100%; height: 100%; }
.ring-bg { fill: none; stroke: rgba(0,0,0,0.05); stroke-width: 6; }
.ring-fill { fill: none; stroke: var(--primary-color); stroke-width: 6; stroke-linecap: round; transform: rotate(-90deg); transform-origin: 50% 50%; transition: stroke-dashoffset 1s ease-out; }
.score-content { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.score-content .value { font-family: var(--font-serif); font-size: 1.8rem; font-weight: 800; line-height: 1; color: var(--text-main); }
.score-content .label { font-size: 0.65rem; color: var(--text-muted); letter-spacing: 2px; }

.summary-content { flex: 1; }
.level-tag { font-family: var(--font-serif); font-size: 1.3rem; margin-bottom: 4px; }
.summary-text { font-size: 0.95rem; color: var(--text-body); font-style: italic; line-height: 1.5; margin-bottom: 10px; border-left: 2px solid var(--accent-gold); padding-left: 15px; }
.quick-stats { display: flex; gap: 20px; }
.stat { display: flex; flex-direction: column; }
.stat .l { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 4px; }
.stat .v { font-weight: 700; color: var(--text-main); }

/* View Sequence (Vertical Stack) */
.view-sequence-container { display: flex; flex-direction: column; gap: 25px; margin-bottom: 50px; }
.view-section { padding: 40px; }
.view-header { display: flex; align-items: center; gap: 15px; margin-bottom: 30px; border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 15px;}
.view-header h2 { font-size: 1.4rem; color: var(--text-main); display: flex; align-items: center; gap: 12px; margin: 0; }
.view-header h2 i { color: var(--primary-color); }
.badge { background: rgba(0,0,0,0.05); padding: 4px 10px; border-radius: 12px; font-size: 0.7rem; font-weight: 700; color: var(--text-muted); letter-spacing: 1px;}

.metric-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; margin-bottom: 30px; }
.m-card { padding: 20px; border-radius: 12px; border: 1px solid rgba(0,0,0,0.05); background: var(--bg-surface); }
.m-card.ADJUST { background: rgba(212, 175, 55, 0.03); border-left: 4px solid var(--accent-gold); }
.m-card.WARNING { background: rgba(255, 77, 79, 0.03); border-left: 4px solid #ff4d4f; }
.m-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.m-label { font-size: 0.95rem; font-weight: 700; color: var(--text-main); }
.m-status-text { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; opacity: 0.6; }

.m-vals { display: flex; gap: 30px; margin-bottom: 15px; }
.v-group { display: flex; flex-direction: column; }
.v-label { font-size: 0.7rem; color: var(--text-muted); }
.v-num { font-family: var(--font-serif); font-size: 1.3rem; font-weight: 700; color: var(--primary-color); }

.m-advice { font-size: 0.85rem; color: var(--text-muted); line-height: 1.5; border-top: 1px dotted rgba(0,0,0,0.1); padding-top: 10px; margin-top: auto;}

.evidence-block { margin-bottom: 40px; }
.sub-label { font-size: 1rem; color: var(--text-main); margin-bottom: 15px; }
.snapshot-grid { display: flex; gap: 15px; overflow-x: auto; padding-bottom: 10px; }
.snap-card { width: 200px; flex-shrink: 0; background: var(--bg-surface); padding: 8px; border-radius: 10px; border: 1px solid rgba(0,0,0,0.05); }
.snap-card img { width: 100%; aspect-ratio: 4/3; object-fit: cover; border-radius: 6px; margin-bottom: 8px; }
.snap-info { display: flex; justify-content: space-between; align-items: center; padding: 0 4px; }
.snap-info .label { font-size: 0.75rem; color: var(--text-muted); font-weight: bold;}
.snap-info .metric { font-weight: 700; color: var(--accent-gold); font-size: 0.85rem; }

.pro-analysis-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; align-items: stretch;}
.sub-glass-card { background: var(--bg-surface); border: 1px solid rgba(0,0,0,0.05); border-radius: 12px; padding: 20px; display: flex; align-items: center; justify-content: center;}
.echarts-dom { width: 100%; height: 350px; }

/* Footer */
.report-footer { display: grid; gap: 30px; }
.equip-card { padding: 30px; background: var(--sidebar-bg, #1a1a1a); color: rgba(255,255,255,0.9); border-radius: 16px; }
.equip-card h4 { color: var(--accent-gold); margin-bottom: 20px; font-size: 1.1rem; }
.equip-card ul { padding-left: 20px; }
.equip-card li { margin-bottom: 12px; font-size: 0.95rem; }
.action-bar { display: flex; gap: 20px; justify-content: center; }

.text-success { color: var(--success-color); }
.text-warning { color: var(--warning-color); }
.text-danger { color: #ff4d4f; }

@media (max-width: 900px) {
  .pro-analysis-row { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .report-hero { flex-direction: column; padding: 30px; text-align: center; gap: 30px; }
  .summary-text { padding-left: 0; border-left: none; }
  .quick-stats { justify-content: center; }
  .metric-grid { grid-template-columns: 1fr; }
  .action-bar { flex-direction: column; }
  .view-section { padding: 20px; }
}
</style>