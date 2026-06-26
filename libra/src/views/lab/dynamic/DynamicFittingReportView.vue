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
              <span class="l-item"><i class="status-dot warn"></i> 余量受限</span>
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
                        <span class="v-num" :class="{'needs-adjust': m.status === 'ADJUST', 'out-of-range': m.status === 'WARNING'}">{{ m.currentValue }}<span class="v-unit">{{ getUnit(m.label, viewType) }}</span></span>
                      </div>
                      <div class="v-group">
                        <span class="v-label">理想</span>
                        <span class="v-num ideal-num">{{ m.idealRange && m.idealRange !== '--' ? m.idealRange : '--' }}<span class="v-unit">{{ getUnit(m.label, viewType) }}</span></span>
                      </div>
                    </div>
                    <p v-if="getDescription(m.label)" class="m-desc-text">{{ getDescription(m.label) }}</p>
                    <!-- 调车建议 -->
                    <div class="m-advice bike-advice-box" v-if="m.advice && m.status !== 'OK'">
                      <i class="fas fa-wrench advice-icon bike-icon"></i>
                      <span>{{ m.advice }}</span>
                    </div>
                    <!-- 骑车习惯/动作纠正建议 -->
                    <div class="m-advice rider-advice-box" v-if="m.riderAdvice && m.status !== 'OK'">
                      <i class="fas fa-child advice-icon rider-icon"></i>
                      <span>{{ m.riderAdvice }}</span>
                    </div>
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

          <!-- 骑车习惯纠正建议汇总 -->
          <div class="rider-habits-card" v-if="allRiderAdvices.length">
            <h4><i class="fas fa-person-running"></i> 骑车习惯 &amp; 动作纠正要点</h4>
            <div class="rider-habits-list">
              <div class="habit-item" v-for="(item, idx) in allRiderAdvices.slice(0, 3)" :key="idx">
                <div class="habit-header">
                  <span class="habit-badge">{{ getViewTitle(item.viewType) }}</span>
                  <span class="habit-label">{{ item.label }}</span>
                </div>
                <p class="habit-text">{{ item.advice }}</p>
              </div>
            </div>
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
            <div v-else-if="report" class="live-poster-stage">
              <DynamicSharingPoster ref="posterRef" :report="report" :snapshotsMap="snapshotsMap" />
              <div v-if="isPosterGenerating" class="generating-badge">
                <i class="fas fa-spinner fa-spin"></i>
                正在生成高清分享图...
              </div>
              <div v-if="posterError" class="poster-inline-error">
                <i class="fas fa-circle-exclamation"></i>
                {{ posterError }}
              </div>
            </div>
            <div v-else class="loading-state error-state">
              <i class="fas fa-circle-exclamation"></i> {{ posterError || '海报暂未显示，请重新生成' }}
            </div>
          </div>
          <div class="preview-footer">
            <button v-if="!posterImage && !isPosterGenerating" class="btn btn-outline" @click="handleShare">
              <i class="fas fa-rotate-right"></i> 重新生成
            </button>
            <button v-else class="btn btn-outline" @click="downloadPoster" :disabled="!posterImage">
              <i class="fas fa-download"></i> 保存到相册
            </button>
            <p class="share-hint">提示：长按图片可直接保存或发送给朋友</p>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { getDiagnosisResult, getSnapshots, getMotionData } from '@/api/dynamicFitting.js';
import { showToast } from '@/utils/toast.js';
import * as echarts from 'echarts';
import dayjs from 'dayjs';
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

// 汇总所有视角的车手自身动作纠正建议
const allRiderAdvices = computed(() => {
  if (!report.value) return [];
  const views = ['side', 'front', 'back'];
  const advices = [];
  views.forEach(vt => {
    const metrics = getMetricsForView(vt);
    metrics.forEach(m => {
      if (m.riderAdvice && m.status !== 'OK') {
        advices.push({ label: m.label, advice: m.riderAdvice, viewType: vt });
      }
    });
  });
  return advices;
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
    // 正面视图不展示膝角伸展值：2D 正面透视缺少深度，容易误导。
    const cleanMetrics = metrics.filter(m => !m.label.includes('伸展度') && !m.label.includes('双膝平均') && !m.label.toLowerCase().includes('kneeangle'));
    
    const groupA = cleanMetrics.filter(m => m.label.includes('肩') || m.label.includes('骨盆') || m.label.includes('脊柱') || m.label.includes('躯干') || m.label.includes('Tilt'));
    const groupB = cleanMetrics.filter(m => !groupA.includes(m) && (m.label.includes('横摆') || m.label.includes('Wobble') || m.label.includes('膝')));
    const groupC = cleanMetrics.filter(m => !groupA.includes(m) && !groupB.includes(m) && (m.label.includes('肘') || m.label.includes('Flare')));
    const other = cleanMetrics.filter(m => !groupA.includes(m) && !groupB.includes(m) && !groupC.includes(m));
    
    const res = [];
    if (groupA.length) res.push({ title: '身体中轴稳定分析', metrics: groupA });
    if (groupB.length) res.push({ title: '下肢力线及控制分析', metrics: groupB });
    if (groupC.length) res.push({ title: '上肢操控及气动分析', metrics: groupC });
    if (other.length) res.push({ title: '其他特征参数', metrics: other });
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
  if (label.includes('踏频')) return 'RPM';
  if (label.includes('角') || label.includes('倾') || label.includes('Tilt') || label.includes('度')) return '\u00B0';
  // 【修复 P13】以下三项均为归一化百分比值（已除以人体参照物尺寸），单位应为 % 而非 px/mm
  if (label.includes('KOPS') || label.includes('横摆') || label.includes('外张') || label.includes('摇摆') || label.includes('下坠') || label.includes('Flare')) return '%';
  if (label.includes('跨度') || label.includes('偏离')) return 'mm';
  // Fallback
  return viewType === 'side' ? '\u00B0' : '';
};

const getDescription = (label) => {
  if ((label.includes('骨盆') && label.includes('倾')) || label.includes(' Pelvis Tilt')) return '反映踩踏时骨盆左右晃动的幅度，数值越大核心越不稳。';
  if (label.includes('肩')) return '反映肩膀左右倾斜的程度，数值越大上半身越不稳定。';
  if (label.includes('横摆') || label.includes('Wobble')) return '反映膝盖横向偏移的幅度，数值越大腿部力线越偏。';
  if (label.includes('外张') || label.includes('肘')) return '反映手肘向外张开的幅度，数值越大风阻越大。';
  if (label.includes('脊柱') || label.includes('躯干')) return '反映躯干与水平面的夹角，数值越小姿势越低趴。';
  if (label.includes('伸展度')) return '反映膝盖在踩踏最低点的伸展程度，判断座高的核心指标。';
  if (label.includes('踝') || label.includes('Ankle')) return '反映脚踝在踏板最低点时的姿态，偏高脚尖下压、偏低脚跟下沉。';
  if (label.includes('髋角') || label.includes('Hip')) return '反映大腿与腹部的夹角，数值越小越容易顶到腹部。';
  if (label.includes('踏频')) return '反映每分钟踩踏圈数，数值越高腿部负担越轻。';
  if (label.includes('KOPS')) return '反映膝盖相对踏板轴的前后位置，偏前则膝盖超轴、偏后则膝盖落后。';
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
        { name: '左膝', type: 'line', data: lWobbles, smooth: true, lineStyle: { color: '#5C6E58', width: 2 }, symbol: 'none' },
        { name: '右膝', type: 'line', data: rWobbles, smooth: true, lineStyle: { color: '#B5A495', width: 2 }, symbol: 'none' }
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
const isPosterGenerating = ref(false);
const posterError = ref('');

const isDisplayablePosterImage = (value) => {
  return typeof value === 'string'
    && /^data:image\/(png|jpe?g|webp|svg\+xml)/i.test(value)
    && value.length > 256;
};

const handleShare = async () => {
  posterImage.value = null;
  posterError.value = '';
  isPosterGenerating.value = true;
  showSharePreview.value = true;

  await nextTick();
  await new Promise(resolve => setTimeout(resolve, 120));

  if (!posterRef.value || !posterRef.value.generateImage) {
    isPosterGenerating.value = false;
    posterError.value = '海报组件正在准备，请重新生成';
    showToast('海报组件正在准备，请重新生成', 'info');
    return;
  }

  try {
    const img = await posterRef.value.generateImage();
    if (isDisplayablePosterImage(img)) {
      posterImage.value = img;
      showToast('海报生成成功', 'success');
    } else {
      posterError.value = '海报暂未显示，请重新生成';
      showToast('海报暂未显示，请重新生成', 'info');
    }
  } catch (e) {
    console.error('生成海报异常:', e);
    posterError.value = '海报生成未完成，请重新生成';
    showToast('海报生成未完成，请重新生成', 'info');
  } finally {
    isPosterGenerating.value = false;
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
.loading-full { height: 60vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 24px; color: var(--text-muted); }
.loading-full p {
  font-family: var(--font-serif);
  font-size: 1.05rem;
  font-style: italic;
  letter-spacing: 0.03em;
}
.pulse-loader {
  width: 60px; height: 60px;
  position: relative;
}
.pulse-loader::before {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 2px solid rgba(92, 110, 88, 0.08);
  border-top-color: rgba(92, 110, 88, 0.25);
  animation: spin 2s linear infinite;
}
.pulse-loader::after {
  content: '';
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  border: 2.5px solid rgba(92, 110, 88, 0.12);
  border-top-color: var(--primary-color);
  animation: spin 1.2s linear infinite reverse;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Report Hero Section */
.report-hero { display: flex; align-items: center; gap: 40px; padding: 35px 40px; margin-bottom: 30px; border-left: 6px solid var(--primary-color); background: linear-gradient(135deg, var(--bg-surface) 0%, rgba(92, 110, 88, 0.03) 100%); }
.score-orbit { position: relative; width: 140px; height: 140px; flex-shrink: 0; }
.ring-svg { width: 100%; height: 100%; }
.ring-bg { fill: none; stroke: rgba(92, 110, 88, 0.1); stroke-width: 8; }
.ring-fill { fill: none; stroke: var(--primary-color); stroke-width: 8; stroke-linecap: round; transform: rotate(-90deg); transform-origin: 50% 50%; transition: stroke-dashoffset 1.2s ease-out; filter: drop-shadow(0 0 8px rgba(92, 110, 88, 0.3)); }
.score-content { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.score-content .value { font-family: var(--font-serif); font-size: 2.5rem; font-weight: 800; line-height: 1; color: var(--text-main); }
.score-content .label { font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; text-transform: uppercase; margin-top: 4px; display: block; }

.summary-content { flex: 1; }
.level-tag { font-family: var(--font-serif); font-size: 1.1rem; margin-bottom: 16px; padding: 6px 18px; display: inline-block; border-radius: 20px; background: rgba(92, 110, 88, 0.08); color: var(--primary-color); font-weight: 600; }
.level-tag.text-success { background: rgba(110, 133, 101, 0.1); color: var(--success-color); }
.level-tag.text-warning { background: rgba(212, 175, 55, 0.12); color: var(--accent-gold); }
.level-tag.text-danger { background: rgba(193, 140, 126, 0.12); color: var(--danger-color); }
.summary-text { font-size: 1rem; color: var(--text-body); font-style: italic; line-height: 1.6; margin-bottom: 18px; border-left: 3px solid var(--accent-gold); padding-left: 20px; }
.quick-stats { display: flex; gap: 24px; }
.stat { display: flex; flex-direction: column; padding: 14px 22px; background: rgba(92, 110, 88, 0.04); border-radius: 12px; border: 1px solid rgba(92, 110, 88, 0.08); min-width: 120px; }
.stat .l { font-size: 0.7rem; color: var(--text-muted); margin-bottom: 6px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 600; }
.stat .v { font-weight: 700; color: var(--text-main); font-size: 1.15rem; font-family: var(--font-serif); }

/* View Sequence (Vertical Stack) */
.view-sequence-container { display: flex; flex-direction: column; gap: 30px; margin-bottom: 50px; }
.view-section { padding: 40px; background: var(--bg-surface); border-radius: 16px; border: 1px solid rgba(92, 110, 88, 0.1); box-shadow: 0 4px 20px rgba(0,0,0,0.03); position: relative; overflow: hidden; }
.view-section::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-light), transparent);
  opacity: 0.5;
}
.view-header { display: flex; align-items: center; gap: 15px; margin-bottom: 35px; border-bottom: 2px solid rgba(92, 110, 88, 0.1); padding-bottom: 20px;}
.view-header h2 { font-size: 1.5rem; color: var(--text-main); display: flex; align-items: center; gap: 12px; margin: 0; font-weight: 600; }
.view-header h2 i { color: var(--primary-color); font-size: 1.3rem; }
.badge { background: var(--primary-color); padding: 6px 14px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; color: white; letter-spacing: 1px;}

.report-legend { display: flex; gap: 20px; margin-top: 10px; font-size: 0.8rem; color: var(--text-muted); align-items: center; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; margin-right: 6px; }
.status-dot.ok { background: #52c41a; box-shadow: 0 0 6px rgba(82, 196, 26, 0.4); }
.status-dot.adjust { background: var(--accent-gold); box-shadow: 0 0 6px rgba(212, 175, 55, 0.4); }
.status-dot.warn { background: var(--danger-color); box-shadow: 0 0 6px rgba(193, 140, 126, 0.4); }

.metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 22px; margin-bottom: 35px; }
.m-card { padding: 24px; border-radius: 14px; border: 1px solid rgba(92, 110, 88, 0.12); background: white; transition: all 0.3s ease; overflow: hidden; }
.m-card:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(92, 110, 88, 0.08); border-color: rgba(92, 110, 88, 0.2); }
.m-card.ADJUST { background: linear-gradient(135deg, rgba(212, 175, 55, 0.05) 0%, rgba(212, 175, 55, 0.02) 100%); border-left: 5px solid var(--accent-gold); }
.m-card.WARNING { background: linear-gradient(135deg, rgba(193, 140, 126, 0.06) 0%, rgba(193, 140, 126, 0.02) 100%); border-left: 5px solid var(--danger-color); }
.m-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.m-label { font-size: 1rem; font-weight: 700; color: var(--text-main); }
.m-status-text { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; padding: 3px 10px; border-radius: 12px; }
.m-card.ADJUST .m-status-text { background: rgba(212, 175, 55, 0.15); color: var(--accent-gold); }
.m-card.WARNING .m-status-text { background: rgba(193, 140, 126, 0.15); color: var(--danger-color); }

.m-vals { display: flex; gap: 40px; margin-bottom: 18px; }
.v-group { display: flex; flex-direction: column; }
.v-label { font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px; }
.v-num { font-family: var(--font-serif); font-size: 1.5rem; font-weight: 700; color: var(--primary-color); }
.v-num.needs-adjust { color: var(--accent-gold); }
.v-num.out-of-range { color: var(--danger-color); }

.m-desc-text { font-size: 0.8rem; color: #888; margin-bottom: 8px; line-height: 1.4; padding: 10px 12px; background: rgba(92, 110, 88, 0.03); border-radius: 8px; border-left: 3px solid rgba(92, 110, 88, 0.15); }
.m-advice { font-size: 0.84rem; line-height: 1.65; display: flex; align-items: flex-start; gap: 10px; padding: 12px 14px; margin-top: 10px; border-radius: 10px; }
.m-advice .advice-icon { flex-shrink: 0; margin-top: 2px; font-size: 0.9rem; }
.m-advice .bike-icon { color: var(--primary-color); }
.m-advice .rider-icon { color: var(--accent-gold); }
/* 调车建议：绿色主题卡片 */
.m-advice.bike-advice-box { background: rgba(92, 110, 88, 0.04); border-left: 3px solid var(--primary-color); color: var(--text-body); }
/* 骑车习惯建议：金色主题卡片 */
.m-advice.rider-advice-box { background: linear-gradient(135deg, rgba(212, 175, 55, 0.06) 0%, rgba(212, 175, 55, 0.02) 100%); border-left: 3px solid var(--accent-gold); color: var(--text-body); }
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
.equip-card { padding: 30px; background: var(--bg-surface); border-radius: var(--radius-md); border: var(--border-glass); }
.equip-card h4 { color: var(--primary-color); margin-bottom: 20px; font-size: 1.2rem; display: flex; align-items: center; gap: 10px; }
.equip-card h4 i { font-size: 1.1rem; color: var(--accent-gold); }
.equip-card ul { padding-left: 0; list-style: none; }
.equip-card li { margin-bottom: 12px; font-size: 0.95rem; color: var(--text-body); display: flex; gap: 10px; align-items: flex-start; line-height: 1.5; }
.equip-card li::before { content: '\2726'; color: var(--accent-gold); flex-shrink: 0; margin-top: 1px; }
.action-bar { display: flex; gap: 20px; justify-content: center; padding: 25px; background: var(--bg-surface); border-top: 1px solid rgba(92, 110, 88, 0.1); }

/* Use global btn styles; only override scoped specifics */
.action-bar .btn {
  padding: 14px 32px;
  border-radius: var(--radius-pill);
  font-weight: 600;
  font-size: 0.95rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all var(--duration-normal) var(--ease-out-expo);
}

/* 骑车习惯总结卡片样式 */
.rider-habits-card {
  padding: 28px 30px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05) 0%, rgba(255, 255, 255, 1) 100%);
  border: 1px solid rgba(212, 175, 55, 0.25);
  border-radius: 16px;
}
.rider-habits-card h4 {
  color: #b48b00;
  margin-bottom: 12px;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  gap: 10px;
}
.rider-habits-card h4 i {
  font-size: 1.1rem;
  color: var(--accent-gold);
}
.rider-habits-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
  padding-right: 8px;
}
.rider-habits-list::-webkit-scrollbar {
  width: 4px;
}
.rider-habits-list::-webkit-scrollbar-thumb {
  background: rgba(212, 175, 55, 0.25);
  border-radius: 4px;
}
.habit-item {
  background: white;
  border-radius: 10px;
  padding: 12px 16px;
  border: 1px solid rgba(212, 175, 55, 0.18);
}
.habit-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.habit-badge {
  background: rgba(212, 175, 55, 0.12);
  color: #b48b00;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: 600;
  flex-shrink: 0;
}
.habit-label {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--text-main);
}
.habit-text {
  font-size: 0.82rem;
  line-height: 1.6;
  color: var(--text-body);
  margin: 0;
}
.btn-outline { border-color: rgba(92, 110, 88, 0.3); color: var(--text-muted); background: transparent; border: 1px solid currentColor; }
.btn-outline:hover { border-color: var(--primary-color); background: var(--primary-color); color: white; transform: translateY(-2px); box-shadow: 0 6px 20px rgba(92, 110, 88, 0.2); }
.btn-primary { background: linear-gradient(135deg, var(--primary-color) 0%, #4a7c59 100%); border: none; color: white; }
.btn-primary:hover { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(92, 110, 88, 0.3); }

.text-success { color: var(--success-color); }
.text-warning { color: var(--warning-color); }
.text-danger { color: var(--danger-color); }

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
  .score-orbit { width: 130px; height: 130px; }
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
  max-width: 480px;
  max-height: 94vh;
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
  padding: 18px;
  background: #F2EFEA;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 420px;
  max-height: calc(94vh - 146px);
  overflow: auto;
}

.generated-poster {
  width: 100%;
  max-width: 375px;
  height: auto;
  border-radius: 10px;
  box-shadow: var(--shadow-md);
  display: block;
}

.live-poster-stage {
  position: relative;
  width: 375px;
  max-width: 100%;
  display: flex;
  justify-content: center;
}

.generating-badge {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(43, 42, 41, 0.82);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(0,0,0,0.22);
  white-space: nowrap;
  pointer-events: none;
}

.poster-inline-error {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(194, 65, 12, 0.92);
  color: #fff;
  font-size: 0.82rem;
  text-align: center;
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

.error-state {
  color: var(--danger-color);
  text-align: center;
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

@media (max-width: 460px) {
  .share-preview-overlay {
    padding: 10px;
  }

  .preview-content {
    padding: 10px;
  }
}
</style>
