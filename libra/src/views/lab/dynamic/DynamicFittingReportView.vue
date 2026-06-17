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
        <p>{{ PROJECT_CONFIG.MASCOT }} 正在调取您的动态分析报告及全部视角序列...</p>
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
            <div class="report-legend">
              <span class="l-item"><i class="status-dot ok"></i> 正常</span>
              <span class="l-item"><i class="status-dot adjust"></i> 建议调整 (ADJUST)</span>
              <span class="l-item" title="建议调整代表该项生物力学指标偏离了职业理想参考范围"><i class="fas fa-info-circle"></i></span>
            </div>
            
            <!-- Overall Share Button -->
            <button class="btn btn-outline" style="margin-top:20px" @click="handleShare">
              <i class="fas fa-share-alt"></i> 生成极客分享海报
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

              <!-- Metrics Groups -->
              <div v-for="(group, gIdx) in getMetricGroups(viewType)" :key="gIdx" class="metric-group-wrapper">
                <h4 v-if="group.title" class="group-title" style="margin-top: 5px; margin-bottom: 20px; font-size: 1.15rem; color: var(--primary-color);">{{ group.title }}</h4>
                <div class="metric-grid">
                  <div v-for="m in group.metrics" :key="m.label" class="m-card" :class="m.status">
                    <div class="m-head">
                      <span class="m-label">{{ m.label }}</span>
                      <span class="m-status-text" v-if="m.status !== 'OK'">{{ m.status }}</span>
                    </div>
                    <div class="m-vals">
                      <div class="v-group">
                        <span class="v-label">实测</span>
                        <span class="v-num" :class="{'out-of-range': m.status !== 'OK'}">{{ m.currentValue }}<span class="v-unit">{{ getUnit(m.label, viewType) }}</span></span>
                      </div>
                      <div class="v-group">
                        <span class="v-label">理想</span>
                        <span class="v-num ideal-num">{{ m.idealRange && m.idealRange !== '--' ? m.idealRange : '--' }}<span class="v-unit">{{ getUnit(m.label, viewType) }}</span></span>
                      </div>
                    </div>
                    <p v-if="getDescription(m.label)" class="m-desc-text">{{ getDescription(m.label) }}</p>
                    <!-- 调车建议 -->
                    <p class="m-advice" v-if="m.advice && m.status !== 'OK'">
                      <i class="fas fa-wrench advice-icon bike-icon"></i>
                      <span>{{ m.advice }}</span>
                    </p>
                    <!-- 骑车习惯/动作纠正建议 -->
                    <p class="m-advice rider-advice" v-if="m.riderAdvice && m.status !== 'OK'">
                      <i class="fas fa-child advice-icon rider-icon"></i>
                      <span>{{ m.riderAdvice }}</span>
                    </p>
                  </div>
                </div>
              </div>

              <!-- Snapshots & Evidence -->
              
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

    <!-- 5. Sharing Preview Modal -->
    <Transition name="modal-fade">
      <div v-if="showSharePreview" class="share-preview-overlay" @click.self="showSharePreview = false">
        <div class="preview-card glass-card">
          <div class="preview-header">
            <h4>海报预览</h4>
            <button @click="showSharePreview = false">&times;</button>
          </div>
          <div class="preview-content">
            <img v-if="posterImage" :src="posterImage" alt="Poster Image" class="generated-poster">
            <div v-else class="loading-state">
              <i class="fas fa-spinner fa-spin"></i> 正在生成高阶海报...
            </div>
          </div>
          <div class="preview-footer">
            <button class="btn btn-outline" @click="downloadPoster" :disabled="!posterImage">
              <i class="fas fa-download"></i> 保存到相册
            </button>
            <p class="share-hint">提示：长按图片可直接保存或发送给朋友</p>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Hidden Poster for Capture -->
    <div class="hidden-poster" style="position: absolute; top: 0; left: -9999px; pointer-events: none;">
      <DynamicSharingPoster v-if="report" ref="posterRef" :report="report" :snapshotsMap="snapshotsMap" />
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
import { PROJECT_CONFIG } from '@/config/constants';

import DynamicSkeletonPlayer from './components/DynamicSkeletonPlayer.vue';
import PedalingAnalysisSection from './components/PedalingAnalysisSection.vue';
import DynamicSharingPoster from '../components/DynamicSharingPoster.vue';

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

const getMetricGroups = (vt) => {
  const metrics = getMetricsForView(vt);
  if (vt === 'front') {
    // 彻底过滤掉正面视图中的“膝部伸展角度”（存在严重透视畸变缺失参考意义且会误导买家）
    const cleanMetrics = metrics.filter(m => !m.label.includes('伸展度') && !m.label.includes('双膝平均') && !m.label.toLowerCase().includes('kneeangle'));
    
    const groupA = cleanMetrics.filter(m => m.label.includes('肩') || m.label.includes('骨盆') || m.label.includes('脊柱') || m.label.includes('躯干') || m.label.includes('Tilt'));
    const groupB = cleanMetrics.filter(m => !groupA.includes(m) && (m.label.includes('横摆') || m.label.includes('Wobble') || m.label.includes('膝')));
    const groupC = cleanMetrics.filter(m => !groupA.includes(m) && !groupB.includes(m) && (m.label.includes('肘') || m.label.includes('Flare')));
    const other = cleanMetrics.filter(m => !groupA.includes(m) && !groupB.includes(m) && !groupC.includes(m));
    
    const res = [];
    if (groupA.length) res.push({ title: '🎯 身体中轴稳定分析', metrics: groupA });
    if (groupB.length) res.push({ title: '🦵 下肢力线及控制分析', metrics: groupB });
    if (groupC.length) res.push({ title: '💪 上肢操控及气动分析', metrics: groupC });
    if (other.length) res.push({ title: '📌 其他特征参数', metrics: other });
    return res.length > 0 ? res : [{ title: '', metrics: cleanMetrics }];
  }
  
  return [{ title: '', metrics: metrics }];
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

const getUnit = (label, viewType) => {
  if (label.includes('角') || label.includes('倾') || label.includes('Tilt') || label.includes('度')) return '\u00B0';
  // 【修复 P13】以下三项均为归一化百分比值（已除以人体参照物尺寸），单位应为 % 而非 px/mm
  if (label.includes('横摆') || label.includes('外张') || label.includes('摇摆') || label.includes('Flare')) return '%';
  if (label.includes('跨度') || label.includes('偏离')) return 'mm';
  if (label.includes('下坠')) return '%';
  // Fallback
  return viewType === 'side' ? '\u00B0' : '';
};

const getDescription = (label) => {
  if (label.includes('肩')) return '反映上身稳定性。角度过大通常意味着核心力量不足或存在耸肩代偿。';
  if (label.includes('骨盆侧倾') || label.includes(' Pelvis Tilt')) return '骨盆的大幅扭动通常暗示坐垫过高、过窄或双腿伸展不均衡。';
  if (label.includes('横摆') || label.includes('Wobble')) return '膝关节的横向偏移会大幅损耗功率，并增加髂胫束（ITB）受损风险。';
  if (label.includes('外张') || label.includes('肘')) return '反映风阻截面。外张过大通常由于车把过宽或上半身过于紧张。';
  if (label.includes('脊柱') || label.includes('躯干')) return '反映躯干与车辆水平面的夹角，接近理想范围骑行效率更佳。';
  if (label.includes('伸展度')) return '决定座管高度的核心指标。过高易引发膝后痛，过低易引发膝前痛。';
  if (label.includes('踝角') || label.includes('Ankle')) return '反映脚踝在踩踏过程中的代偿。过大通常意味着存在“垫脚尖”式发力。';
  if (label.includes('髋角') || label.includes('Hip')) return '反映大腿与腹部的夹角。过小（上死点）会产生撞击感并压迫呼吸。';
  if (label.includes('踏频')) return '高踏频有利于减轻肌肉负担，但过高的踏频若无稳定核心支撑则会产生身体跳动。';
  if (label.includes('KOPS')) return '反映膝盖与脚踏轴心的垂直力线关系，关系到力量传导效率。';
  return '';
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
          snapshotsMap.value[vt] = (snapshots || []).filter(s => s && s.url);
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
    
    const snapshots = snapRes.data || snapRes;
    snapshotsMap.value[viewType] = (snapshots || []).filter(s => s && s.url);
    
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
const showSharePreview = ref(false);
const posterImage = ref(null);
const posterRef = ref(null);

const handleShare = async () => {
  showSharePreview.value = true;
  posterImage.value = null; // Reset

  await nextTick();

  if (posterRef.value) {
    try {
      showToast('正在生成硬核海报，请稍候...', 'info');
      // Timeout is used to ensure DOM rendering completes safely
      setTimeout(async () => {
        const img = await posterRef.value.generateImage();
        posterImage.value = img;
        if (img) showToast('海报生成成功', 'success');
        else showToast('海报生成失败', 'error');
      }, 500);
    } catch (e) {
      console.error(e);
      showToast('海报生成发生异常', 'error');
    }
  }
};

const downloadPoster = () => {
  if (!posterImage.value) return;
  const link = document.createElement('a');
  link.href = posterImage.value;
  link.download = `RideTrace_Dynamic_Fitting_${new Date().getTime()}.jpg`;
  link.click();
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
.report-hero { display: flex; align-items: center; gap: 40px; padding: 35px 40px; margin-bottom: 30px; border-left: 6px solid var(--primary-color); background: linear-gradient(135deg, var(--bg-surface) 0%, rgba(92, 110, 88, 0.03) 100%); }
.score-orbit { position: relative; width: 120px; height: 120px; flex-shrink: 0; }
.ring-svg { width: 100%; height: 100%; }
.ring-bg { fill: none; stroke: rgba(92, 110, 88, 0.1); stroke-width: 8; }
.ring-fill { fill: none; stroke: var(--primary-color); stroke-width: 8; stroke-linecap: round; transform: rotate(-90deg); transform-origin: 50% 50%; transition: stroke-dashoffset 1.2s ease-out; filter: drop-shadow(0 0 8px rgba(92, 110, 88, 0.3)); }
.score-content { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.score-content .value { font-family: var(--font-serif); font-size: 2.5rem; font-weight: 800; line-height: 1; color: var(--text-main); }
.score-content .label { font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; text-transform: uppercase; margin-top: 4px; display: block; }

.summary-content { flex: 1; }
.level-tag { font-family: var(--font-serif); font-size: 1.2rem; margin-bottom: 10px; padding: 6px 16px; display: inline-block; border-radius: 20px; background: rgba(92, 110, 88, 0.08); color: var(--primary-color); }
.summary-text { font-size: 1rem; color: var(--text-body); font-style: italic; line-height: 1.6; margin-bottom: 18px; border-left: 3px solid var(--accent-gold); padding-left: 20px; }
.quick-stats { display: flex; gap: 35px; }
.stat { display: flex; flex-direction: column; padding: 12px 20px; background: rgba(0,0,0,0.02); border-radius: 10px; }
.stat .l { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 6px; text-transform: uppercase; letter-spacing: 1px; }
.stat .v { font-weight: 700; color: var(--text-main); font-size: 1.1rem; }

/* View Sequence (Vertical Stack) */
.view-sequence-container { display: flex; flex-direction: column; gap: 30px; margin-bottom: 50px; }
.view-section { padding: 40px; background: var(--bg-surface); border-radius: 16px; border: 1px solid rgba(92, 110, 88, 0.1); box-shadow: 0 4px 20px rgba(0,0,0,0.03); }
.view-header { display: flex; align-items: center; gap: 15px; margin-bottom: 35px; border-bottom: 2px solid rgba(92, 110, 88, 0.1); padding-bottom: 20px;}
.view-header h2 { font-size: 1.5rem; color: var(--text-main); display: flex; align-items: center; gap: 12px; margin: 0; font-weight: 600; }
.view-header h2 i { color: var(--primary-color); font-size: 1.3rem; }
.badge { background: var(--primary-color); padding: 6px 14px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; color: white; letter-spacing: 1px;}

.report-legend { display: flex; gap: 20px; margin-top: 10px; font-size: 0.8rem; color: var(--text-muted); align-items: center; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; margin-right: 6px; }
.status-dot.ok { background: #52c41a; box-shadow: 0 0 6px rgba(82, 196, 26, 0.4); }
.status-dot.adjust { background: var(--accent-gold); box-shadow: 0 0 6px rgba(212, 175, 55, 0.4); }

.metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 22px; margin-bottom: 35px; }
.m-card { padding: 24px; border-radius: 14px; border: 1px solid rgba(92, 110, 88, 0.12); background: white; transition: all 0.3s ease; overflow: hidden; }
.m-card:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(92, 110, 88, 0.08); }
.m-card.ADJUST { background: linear-gradient(135deg, rgba(212, 175, 55, 0.05) 0%, rgba(212, 175, 55, 0.02) 100%); border-left: 5px solid var(--accent-gold); }
.m-card.WARNING { background: linear-gradient(135deg, rgba(255, 77, 79, 0.05) 0%, rgba(255, 77, 79, 0.02) 100%); border-left: 5px solid #ff4d4f; }
.m-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.m-label { font-size: 1rem; font-weight: 700; color: var(--text-main); }
.m-status-text { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; padding: 3px 10px; border-radius: 12px; }
.m-card.ADJUST .m-status-text { background: rgba(212, 175, 55, 0.15); color: var(--accent-gold); }
.m-card.WARNING .m-status-text { background: rgba(255, 77, 79, 0.15); color: #ff4d4f; }

.m-vals { display: flex; gap: 40px; margin-bottom: 18px; }
.v-group { display: flex; flex-direction: column; }
.v-label { font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px; }
.v-num { font-family: var(--font-serif); font-size: 1.5rem; font-weight: 700; color: var(--primary-color); }
.v-num.out-of-range { color: #ff4d4f; }

.m-desc-text { font-size: 0.8rem; color: #888; margin-bottom: 8px; line-height: 1.4; padding: 10px 12px; background: rgba(92, 110, 88, 0.03); border-radius: 8px; border-left: 3px solid rgba(92, 110, 88, 0.15); }
.m-advice { font-size: 0.85rem; color: var(--text-body); line-height: 1.6; border-top: 1px dotted rgba(92, 110, 88, 0.15); padding-top: 12px; margin-top: 4px; font-style: italic; display: flex; align-items: flex-start; gap: 8px; }
.m-advice .advice-icon { flex-shrink: 0; margin-top: 2px; font-size: 0.9rem; }
.m-advice .bike-icon { color: var(--primary-color); }
.m-advice.rider-advice { border-top: 1px dotted rgba(212, 175, 55, 0.2); background: linear-gradient(135deg, rgba(212, 175, 55, 0.04) 0%, rgba(212, 175, 55, 0.01) 100%); border-radius: 0 0 8px 8px; margin: 0 -12px -12px -12px; padding: 10px 16px; border-left: 3px solid var(--accent-gold); font-style: normal; }
.m-advice .rider-icon { color: var(--accent-gold); }
.v-unit { font-size: 0.9rem; margin-left: 2px; color: inherit; }
.ideal-num { color: var(--text-muted) !important; }

.evidence-block { margin-bottom: 45px; }
.sub-label { font-size: 1.1rem; color: var(--text-main); margin-bottom: 20px; display: flex; align-items: center; gap: 10px; font-weight: 600; }
.sub-label i { color: var(--primary-color); }
.snapshot-grid { display: flex; gap: 20px; overflow-x: auto; padding-bottom: 15px; }
.snap-card { width: 220px; flex-shrink: 0; background: white; padding: 12px; border-radius: 14px; border: 1px solid rgba(92, 110, 88, 0.12); transition: all 0.3s ease; }
.snap-card:hover { transform: translateY(-3px); box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
.snap-card img { width: 100%; aspect-ratio: 4/3; object-fit: cover; border-radius: 10px; margin-bottom: 12px; background: rgba(0,0,0,0.03); }
.snap-info { display: flex; justify-content: space-between; align-items: center; padding: 0 4px; }
.snap-info .label { font-size: 0.8rem; color: var(--text-muted); font-weight: 600;}
.snap-info .metric { font-weight: 700; color: var(--accent-gold); font-size: 0.95rem; font-family: var(--font-serif); }

.pro-analysis-row { display: grid; grid-template-columns: 1fr 1fr; gap: 25px; align-items: stretch;}
.sub-glass-card { background: white; border: 1px solid rgba(92, 110, 88, 0.12); border-radius: 14px; padding: 24px; display: flex; align-items: center; justify-content: center; }
.echarts-dom { width: 100%; height: 380px; }

/* Footer */
.report-footer { display: grid; gap: 30px; }
.equip-card { padding: 35px; background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%); color: rgba(255,255,255,0.9); border-radius: 16px; border: 1px solid rgba(255,255,255,0.05); }
.equip-card h4 { color: var(--accent-gold); margin-bottom: 20px; font-size: 1.2rem; display: flex; align-items: center; gap: 10px; }
.equip-card h4 i { font-size: 1.1rem; }
.equip-card ul { padding-left: 20px; }
.equip-card li { margin-bottom: 12px; font-size: 0.95rem; position: relative; padding-left: 15px; }
.equip-card li::before { content: '✦'; color: var(--accent-gold); position: absolute; left: 0; }
.action-bar { display: flex; gap: 20px; justify-content: center; padding: 25px; background: var(--bg-surface); border-top: 1px solid rgba(92, 110, 88, 0.1); }

.btn { padding: 14px 28px; border-radius: 25px; border: 2px solid var(--primary-color); background: transparent; color: var(--primary-color); font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 10px; transition: all 0.3s ease; font-size: 0.95rem; }
.btn:hover { background: var(--primary-color); color: white; transform: translateY(-2px); box-shadow: 0 6px 20px rgba(92, 110, 88, 0.2); }
.btn-outline { border-color: rgba(92, 110, 88, 0.3); color: var(--text-muted); }
.btn-outline:hover { border-color: var(--primary-color); background: var(--primary-color); color: white; }
.btn-primary { background: linear-gradient(135deg, var(--primary-color) 0%, #4a7c59 100%); border: none; color: white; }
.btn-primary:hover { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(92, 110, 88, 0.3); }

.text-success { color: var(--success-color); }
.text-warning { color: var(--warning-color); }
.text-danger { color: #ff4d4f; }

/* --- 5. 响应式适配 --- */
@media (max-width: 900px) {
  .pro-analysis-row { grid-template-columns: 1fr; gap: 30px; }
  .echarts-dom { height: 300px; }
}

@media (max-width: 768px) {
  .dynamic-report-page { padding: 80px 0 40px; }
  
  /* Header 适配 */
  .report-header { margin-bottom: 25px; }
  .meta-info { display: none; } /* 移动端隐藏日期 meta */

  /* Hero 适配 */
  .report-hero { 
    flex-direction: column; 
    padding: 30px 20px; 
    text-align: center; 
    gap: 25px; 
    border-left: none;
    border-top: 4px solid var(--primary-color);
  }
  .score-orbit { width: 110px; height: 110px; }
  .summary-text { 
    padding-left: 0; 
    border-left: none; 
    font-size: 0.9rem;
    margin: 15px 0;
  }
  .report-legend { justify-content: center; flex-wrap: wrap; }
  .quick-stats { justify-content: center; gap: 30px; }

  /* 指标卡片适配 */
  .metric-grid { grid-template-columns: 1fr; gap: 15px; }
  .m-card { padding: 18px; }
  .m-vals { gap: 20px; justify-content: space-between; }
  .v-num { font-size: 1.2rem; }
  
  /* 捕捉帧横向滑动优化 */
  .snap-card { width: 160px; }

  /* 底部操作 */
  .view-section { padding: 25px 15px; border-radius: 16px; }
  .action-bar { flex-direction: column; gap: 12px; width: 100%; }
  .btn { width: 100%; justify-content: center; padding: 14px; }
  
  .equip-card { padding: 20px; border-radius: 12px; }
}

@media (max-width: 480px) {
  .report-hero { padding: 25px 15px; }
  .level-tag { font-size: 1.1rem; }
  .m-vals { flex-direction: row; }
  .v-group { flex: 1; }
}

/* Sharing Preview Modal Styles */
.share-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(43, 42, 41, 0.85); /* Dark overlay */
  backdrop-filter: blur(10px);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.preview-card {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 40px 100px rgba(0,0,0,0.5);
  padding: 0;
}

.preview-header {
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: var(--border-glass);
}

.preview-header h4 { margin: 0; font-family: var(--font-serif); font-size: 1.2rem; }
.preview-header button { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--text-muted); }
.preview-header button:hover { color: var(--text-main); }

.preview-content {
  padding: 20px;
  background: #F9F9F7;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.generated-poster {
  width: 100%;
  max-width: 320px;
  height: auto;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
  display: block;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  color: var(--primary-color);
  font-family: var(--font-serif);
  font-size: 1.1rem;
}

.preview-footer {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  background: var(--bg-surface);
}

.share-hint {
  font-size: 0.8rem;
  color: var(--text-muted);
  margin: 0;
}

.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.3s ease;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}
</style>