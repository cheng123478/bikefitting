<template>
  <div class="poster-wrapper">
    <!-- Hidden poster for rendering -->
    <div class="poster-container" ref="posterRef">
      
      <!-- A. Header: Brand & ID -->
      <header class="poster-header">
        <div class="brand-badge">
          <span class="brand-cn">{{ PROJECT_CONFIG.NAME }} LAB</span>
          <span class="brand-en">AI DYNAMIC BIOMECHANICS</span>
        </div>
      </header>

      <main class="poster-body">
        
        <!-- B. AI Shutter Viewport: Snapshot or SVG skeleton fallback -->
        <div class="visual-viewport-card">
          <!-- 1. Real snapshot (pre-overlaid with skeleton from backend) -->
          <img 
            v-if="bestSnapshot" 
            :src="bestSnapshot.url" 
            crossorigin="anonymous"
            class="snapshot-img" 
            alt="AI Biomechanical Snapshot"
          />

          <!-- 2. Fallback SVG Skeleton lines (drawn locally using pts data) -->
          <svg v-else-if="staticSkeletonPts" class="skeleton-svg" viewBox="0 0 320 220">
            <!-- Tech radar backdrop -->
            <g opacity="0.08">
              <line x1="20" y1="110" x2="300" y2="110" stroke="var(--text-main)" stroke-width="1" stroke-dasharray="3,3" />
              <line x1="160" y1="20" x2="160" y2="200" stroke="var(--text-main)" stroke-width="1" stroke-dasharray="3,3" />
              <circle cx="160" cy="110" r="70" fill="none" stroke="var(--text-main)" stroke-width="1" stroke-dasharray="3,3" />
            </g>

            <g v-if="activeViewType === 'side'">
              <line v-if="staticSkeletonPts.EAR && staticSkeletonPts.SHOULDER" 
                    :x1="staticSkeletonPts.EAR.x * 320" :y1="staticSkeletonPts.EAR.y * 220" 
                    :x2="staticSkeletonPts.SHOULDER.x * 320" :y2="staticSkeletonPts.SHOULDER.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.SHOULDER && staticSkeletonPts.HIP" 
                    :x1="staticSkeletonPts.SHOULDER.x * 320" :y1="staticSkeletonPts.SHOULDER.y * 220" 
                    :x2="staticSkeletonPts.HIP.x * 320" :y2="staticSkeletonPts.HIP.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.SHOULDER && staticSkeletonPts.ELBOW" 
                    :x1="staticSkeletonPts.SHOULDER.x * 320" :y1="staticSkeletonPts.SHOULDER.y * 220" 
                    :x2="staticSkeletonPts.ELBOW.x * 320" :y2="staticSkeletonPts.ELBOW.y * 220" 
                    stroke="var(--primary-light)" stroke-width="2.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.ELBOW && staticSkeletonPts.WRIST" 
                    :x1="staticSkeletonPts.ELBOW.x * 320" :y1="staticSkeletonPts.ELBOW.y * 220" 
                    :x2="staticSkeletonPts.WRIST.x * 320" :y2="staticSkeletonPts.WRIST.y * 220" 
                    stroke="var(--primary-light)" stroke-width="2.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.HIP && staticSkeletonPts.KNEE" 
                    :x1="staticSkeletonPts.HIP.x * 320" :y1="staticSkeletonPts.HIP.y * 220" 
                    :x2="staticSkeletonPts.KNEE.x * 320" :y2="staticSkeletonPts.KNEE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="4" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.KNEE && staticSkeletonPts.ANKLE" 
                    :x1="staticSkeletonPts.KNEE.x * 320" :y1="staticSkeletonPts.KNEE.y * 220" 
                    :x2="staticSkeletonPts.ANKLE.x * 320" :y2="staticSkeletonPts.ANKLE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="4" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.ANKLE && staticSkeletonPts.HEEL" 
                    :x1="staticSkeletonPts.ANKLE.x * 320" :y1="staticSkeletonPts.ANKLE.y * 220" 
                    :x2="staticSkeletonPts.HEEL.x * 320" :y2="staticSkeletonPts.HEEL.y * 220" 
                    stroke="var(--primary-color)" stroke-width="2.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.HEEL && staticSkeletonPts.TOE" 
                    :x1="staticSkeletonPts.HEEL.x * 320" :y1="staticSkeletonPts.HEEL.y * 220" 
                    :x2="staticSkeletonPts.TOE.x * 320" :y2="staticSkeletonPts.TOE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="2.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.TOE && staticSkeletonPts.ANKLE" 
                    :x1="staticSkeletonPts.TOE.x * 320" :y1="staticSkeletonPts.TOE.y * 220" 
                    :x2="staticSkeletonPts.ANKLE.x * 320" :y2="staticSkeletonPts.ANKLE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="2.5" stroke-linecap="round" />

              <circle v-for="key in ['EAR', 'SHOULDER', 'ELBOW', 'WRIST', 'HIP', 'KNEE', 'ANKLE', 'HEEL', 'TOE']" 
                      v-if="staticSkeletonPts[key]"
                      :key="key"
                      :cx="staticSkeletonPts[key].x * 320" 
                      :cy="staticSkeletonPts[key].y * 220" 
                      r="5" 
                      fill="var(--accent-gold)" 
                      stroke="#FFF" 
                      stroke-width="1.5" />
            </g>

            <g v-else>
              <line v-if="staticSkeletonPts.L_SHOULDER && staticSkeletonPts.R_SHOULDER" 
                    :x1="staticSkeletonPts.L_SHOULDER.x * 320" :y1="staticSkeletonPts.L_SHOULDER.y * 220" 
                    :x2="staticSkeletonPts.R_SHOULDER.x * 320" :y2="staticSkeletonPts.R_SHOULDER.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.L_HIP && staticSkeletonPts.R_HIP" 
                    :x1="staticSkeletonPts.L_HIP.x * 320" :y1="staticSkeletonPts.L_HIP.y * 220" 
                    :x2="staticSkeletonPts.R_HIP.x * 320" :y2="staticSkeletonPts.R_HIP.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.L_SHOULDER && staticSkeletonPts.L_HIP" 
                    :x1="staticSkeletonPts.L_SHOULDER.x * 320" :y1="staticSkeletonPts.L_SHOULDER.y * 220" 
                    :x2="staticSkeletonPts.L_HIP.x * 320" :y2="staticSkeletonPts.L_HIP.y * 220" 
                    stroke="var(--primary-color)" stroke-width="2" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.R_SHOULDER && staticSkeletonPts.R_HIP" 
                    :x1="staticSkeletonPts.R_SHOULDER.x * 320" :y1="staticSkeletonPts.R_SHOULDER.y * 220" 
                    :x2="staticSkeletonPts.R_HIP.x * 320" :y2="staticSkeletonPts.R_HIP.y * 220" 
                    stroke="var(--primary-color)" stroke-width="2" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.L_HIP && staticSkeletonPts.L_KNEE" 
                    :x1="staticSkeletonPts.L_HIP.x * 320" :y1="staticSkeletonPts.L_HIP.y * 220" 
                    :x2="staticSkeletonPts.L_KNEE.x * 320" :y2="staticSkeletonPts.L_KNEE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.R_HIP && staticSkeletonPts.R_KNEE" 
                    :x1="staticSkeletonPts.R_HIP.x * 320" :y1="staticSkeletonPts.R_HIP.y * 220" 
                    :x2="staticSkeletonPts.R_KNEE.x * 320" :y2="staticSkeletonPts.R_KNEE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.L_KNEE && staticSkeletonPts.L_ANKLE" 
                    :x1="staticSkeletonPts.L_KNEE.x * 320" :y1="staticSkeletonPts.L_KNEE.y * 220" 
                    :x2="staticSkeletonPts.L_ANKLE.x * 320" :y2="staticSkeletonPts.L_ANKLE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3.5" stroke-linecap="round" />
              <line v-if="staticSkeletonPts.R_KNEE && staticSkeletonPts.R_ANKLE" 
                    :x1="staticSkeletonPts.R_KNEE.x * 320" :y1="staticSkeletonPts.R_KNEE.y * 220" 
                    :x2="staticSkeletonPts.R_ANKLE.x * 320" :y2="staticSkeletonPts.R_ANKLE.y * 220" 
                    stroke="var(--primary-color)" stroke-width="3.5" stroke-linecap="round" />

              <line v-if="staticSkeletonPts.L_SHOULDER && staticSkeletonPts.R_SHOULDER && staticSkeletonPts.L_HIP && staticSkeletonPts.R_HIP"
                    :x1="((staticSkeletonPts.L_SHOULDER.x + staticSkeletonPts.R_SHOULDER.x) / 2) * 320"
                    :y1="((staticSkeletonPts.L_SHOULDER.y + staticSkeletonPts.R_SHOULDER.y) / 2) * 220"
                    :x2="((staticSkeletonPts.L_HIP.x + staticSkeletonPts.R_HIP.x) / 2) * 320"
                    :y2="((staticSkeletonPts.L_HIP.y + staticSkeletonPts.R_HIP.y) / 2) * 220"
                    stroke="var(--accent-gold)" stroke-width="2" stroke-dasharray="3,3" />

              <circle v-for="key in ['L_SHOULDER', 'R_SHOULDER', 'L_HIP', 'R_HIP', 'L_KNEE', 'R_KNEE', 'L_ANKLE', 'R_ANKLE']" 
                      v-if="staticSkeletonPts[key]"
                      :key="key"
                      :cx="staticSkeletonPts[key].x * 320" 
                      :cy="staticSkeletonPts[key].y * 220" 
                      r="5" 
                      fill="var(--accent-gold)" 
                      stroke="#FFF" 
                      stroke-width="1.5" />
            </g>
          </svg>

          <!-- Technical shutter bounds & view overlay -->
          <div class="view-shutter-overlay">
            <span class="view-type-badge">{{ activeViewType.toUpperCase() }} VIEW</span>
            <span v-if="keyMetric" class="primary-angle-badge">
              {{ keyMetric.label }}: {{ keyMetric.currentValue }}{{ getUnit(keyMetric.label) }}
            </span>
          </div>
        </div>

        <!-- C. User Info Bar -->
        <div class="rider-profile-bar">
          <div class="rider-info-left">
            <span class="rider-name">{{ posterUsername }}</span>
            <span class="rider-date">{{ reportDate }}</span>
          </div>
          <div class="rider-title-badge">
            <span>{{ riderDynamicTitle }}</span>
          </div>
        </div>

        <!-- D. Bento Grid -->
        <div class="bento-layout">
          <!-- Card 1: Match Score -->
          <div class="bento-card score-bento">
            <span class="bento-label">综合适配评分</span>
            <div class="bento-score-body">
              <span class="score-val" :class="scoreClass">{{ calculatedScore }}</span>
              <div class="score-sub-box">
                <span class="score-unit">分</span>
                <span class="score-level" :class="scoreClass">{{ scoreLabel }}</span>
              </div>
            </div>
          </div>

          <!-- Card 2: Biomechanical Performance -->
          <div class="bento-card performance-bento">
            <template v-if="hasPedalingData">
              <span class="bento-label">双侧发力平衡度</span>
              <div class="bento-balance-bar">
                <div class="left-ratio" :style="{ width: safePedaling.leftPowerRatio + '%' }">
                  <span>L {{ safePedaling.leftPowerRatio }}%</span>
                </div>
                <div class="right-ratio" :style="{ width: safePedaling.rightPowerRatio + '%' }">
                  <span>R {{ safePedaling.rightPowerRatio }}%</span>
                </div>
              </div>
              <div class="bento-sub-metric">
                <span>平均踏频: <strong>{{ safePedaling.cadenceRpm || 90 }} RPM</strong></span>
              </div>
            </template>
            <template v-else>
              <span class="bento-label">核心力线参数</span>
              <div class="bento-list-metrics" v-if="fallbackMetrics.length > 0">
                <div class="fallback-item" v-for="m in fallbackMetrics.slice(0, 2)" :key="m.label">
                  <span class="m-lbl">{{ m.label }}</span>
                  <span class="m-val">{{ m.currentValue }}{{ getUnit(m.label) }}</span>
                </div>
              </div>
            </template>
          </div>

          <!-- Card 3: Diagnosis (Full Width) -->
          <div class="bento-card diagnosis-bento">
            <span class="bento-label">Aries AI 核心诊断意见</span>
            <div class="diagnosis-insight-body">
              <i class="fas" :class="injuryWarning ? 'fa-exclamation-triangle warning-icon' : 'fa-rocket success-icon'"></i>
              <p class="diagnosis-desc">{{ criticalInsightText }}</p>
            </div>
          </div>
        </div>

      </main>

      <!-- F. Footer -->
      <footer class="poster-footer">
        <div class="footer-left-info">
          <p class="slogan-cn">{{ PROJECT_CONFIG.SHARE_SLOGAN_DYNAMIC }}</p>
          <p class="sub-slogan-cn">RideTrace AI · 守护您的每一次踩踏</p>
        </div>
        <div class="qr-code-box">
          <qrcode-vue
            :value="shareUrl"
            :size="60"
            level="H"
            background="#fff"
            foreground="#2B2A29"
          />
        </div>
      </footer>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue';
import html2canvas from 'html2canvas';
import QrcodeVue from 'qrcode.vue';
import request from '@/api/request.js';
import { PROJECT_CONFIG } from '@/config/constants';
import dayjs from 'dayjs';

const props = defineProps({
  report: {
    type: Object,
    default: () => ({})
  },
  snapshotsMap: {
    type: Object,
    default: () => ({})
  }
});

const posterRef = ref(null);
const inviteCode = ref('');

// Fetch invite code
const fetchInviteCode = async () => {
  try {
    const res = await request.get('/membership/invite-code');
    if (res.code === 200) {
      inviteCode.value = res.data.inviteCode;
    }
  } catch (e) {
    console.warn('获取邀请码失败', e);
  }
};
fetchInviteCode();

const shareUrl = computed(() => {
  const origin = window.location.origin;
  if (inviteCode.value) {
    return `${origin}/?inviter=${inviteCode.value}`;
  }
  return `${origin}/`;
});

// Username & Date
const posterUsername = computed(() => {
  if (props.report.username) return props.report.username;
  try {
    const userInfoStr = localStorage.getItem('user_info');
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      return userInfo.name;
    }
  } catch (e) {}
  return `${PROJECT_CONFIG.MASCOT} Rider`;
});

const reportDate = computed(() => {
  if (props.report.createTime) {
    return dayjs(props.report.createTime).format('YYYY.MM.DD');
  }
  return dayjs().format('YYYY.MM.DD');
});

const shortReportId = computed(() => {
  if (props.report.id) return String(props.report.id).substring(0, 8).toUpperCase();
  if (props.report.sessionId) return String(props.report.sessionId).substring(0, 8).toUpperCase();
  return Math.random().toString(36).substr(2, 8).toUpperCase();
});

// View adaptation & snapshot selector
const activeViewType = computed(() => {
  if (props.report.sideData || props.report.sideMetrics) return 'side';
  if (props.report.frontData || props.report.frontMetrics) return 'front';
  if (props.report.backData || props.report.backMetrics) return 'back';
  return 'side'; // fallback
});

const bestSnapshot = computed(() => {
  const view = activeViewType.value;
  const list = props.snapshotsMap && props.snapshotsMap[view];
  if (!list || list.length === 0) return null;
  
  const priority = ['k_ext', 'bdc', 'max_wobble', 'max_drop'];
  for (const key of priority) {
    const match = list.find(s => s.label && s.label.toLowerCase().includes(key));
    if (match) return match;
  }
  return list[0];
});

// Skeleton coordinate points parser (fallback)
const staticSkeletonPts = computed(() => {
  const view = activeViewType.value;
  const viewData = props.report[`${view}Data`] || {};
  let motionJson = viewData.motionDataJson;
  if (!motionJson && props.report.motionDataMap) {
    motionJson = props.report.motionDataMap[view]?.dataJson;
  }
  
  if (motionJson) {
    try {
      const frames = typeof motionJson === 'string' ? JSON.parse(motionJson) : motionJson;
      if (frames && frames.length > 0) return frames[0].pts || null;
    } catch (e) {
      console.warn('解析分享海报骨骼数据失败', e);
    }
  }
  return null;
});

// Pedaling statistics
const safePedaling = computed(() => props.report.pedalingData || {});
const hasPedalingData = computed(() => Object.keys(safePedaling.value).length > 0);

const riderDynamicTitle = computed(() => {
  if (hasPedalingData.value) {
    const p = safePedaling.value;
    if (p.upstrokeDragIndex < 15) return '踩踏平顺大师';
    if (Math.abs((p.leftPowerRatio || 50) - 50) <= 0.5) return '双侧发力对称王者';
    if ((p.lateralWobbleLeft || 0) < 2.0 && (p.lateralWobbleRight || 0) < 2.0) return '完美膝盖稳定者';
    if ((p.cadenceRpm || 0) > 95) return '踏频狂魔';
    return '高能输出机器';
  }
  return '进阶探索骑士';
});

// Score & evaluation
const calculatedScore = computed(() => props.report.matchScore || 85);
const scoreClass = computed(() => {
  if (calculatedScore.value >= 90) return 'text-success';
  if (calculatedScore.value >= 75) return 'text-warning';
  return 'text-danger';
});
const scoreLabel = computed(() => props.report.matchLevel || '待优化');

const fallbackMetrics = computed(() => {
  const m = [];
  if (props.report.sideMetrics) m.push(...props.report.sideMetrics);
  else if (props.report.sideData && props.report.sideData.metrics) m.push(...props.report.sideData.metrics);
  
  if (props.report.frontMetrics) m.push(...props.report.frontMetrics);
  else if (props.report.frontData && props.report.frontData.metrics) m.push(...props.report.frontData.metrics);
  
  return m.filter(x => x.label.includes('角') || x.label.includes('Tilt') || x.label.includes('KOPS'));
});

const getUnit = (label) => {
  if (label.includes('角') || label.includes('Tilt')) return '°';
  if (label.includes('距') || label.includes('偏离') || label.includes('横摆')) return 'mm';
  return '';
};

const keyMetric = computed(() => {
  const list = fallbackMetrics.value;
  if (!list || list.length === 0) return null;
  const match = list.find(m => m.label.includes('伸展') || m.label.includes('前倾') || m.label.includes('侧倾'));
  return match || list[0];
});

// AI Insights
const injuryWarning = computed(() => {
  if (hasPedalingData.value) {
    const lWobble = safePedaling.value.lateralWobbleLeft || 0;
    const rWobble = safePedaling.value.lateralWobbleRight || 0;
    const maxWobble = Math.max(lWobble, rWobble);
    const badLeg = maxWobble === rWobble ? '右膝' : '左膝';
    if (maxWobble > 5.0) {
      return `检测到${badLeg}存在 ${maxWobble.toFixed(1)}mm 侧向晃动，处于半月板磨损高危区，建议微调锁鞋。`;
    }
  } else {
     const ext = fallbackMetrics.value.find(m => m.label.includes('伸展度'));
     if (ext && ext.currentValue > 150) return `膝关节伸展度 ${ext.currentValue}° 偏大，长期骑行极易引发膝后侧十字韧带拉伤。`;
     if (ext && ext.currentValue < 130) return `膝关节伸展度 ${ext.currentValue}° 偏小，髌骨关节腔压力过大，易引发膝前痛。`;
  }
  return null;
});

const powerPotential = computed(() => {
  if (hasPedalingData.value) {
     const drag = safePedaling.value.upstrokeDragIndex || 0;
     if (drag > 20) return `目前上提对抗阻力（Drag: ${drag}）偏高。若优化拉力协同，输出功率预计能提升 +5%！`;
  }
  return '躯干与下肢控制极佳，当前发力几何已接近黄金甜区，功率正获得无损释放！';
});

const criticalInsightText = computed(() => {
  const warning = injuryWarning.value;
  if (warning) {
    return warning.length > 45 ? warning.substring(0, 45) + '...' : warning;
  }
  const potential = powerPotential.value;
  return potential.length > 45 ? potential.substring(0, 45) + '...' : potential;
});

// Export generation function
defineExpose({
  generateImage: async () => {
    await nextTick();
    if (!posterRef.value) return null;

    try {
      if (document.fonts && document.fonts.ready) {
        await document.fonts.ready;
      }

      const originalPosition = posterRef.value.style.position;
      const originalTop = posterRef.value.style.top;
      const originalLeft = posterRef.value.style.left;
      const originalZIndex = posterRef.value.style.zIndex;

      posterRef.value.style.position = 'fixed';
      posterRef.value.style.top = '0px';
      posterRef.value.style.left = '0px';
      posterRef.value.style.zIndex = '-9999';

      await new Promise(resolve => setTimeout(resolve, 300)); // DOM update

      const rect = posterRef.value.getBoundingClientRect();
      const canvas = await html2canvas(posterRef.value, {
        useCORS: true,
        scale: 3.0, // scale for high density exporting
        backgroundColor: '#FFFFFF',
        logging: false,
        // 由于设定了 position: fixed 且 top: 0, left: 0，元素在克隆画布中的绝对坐标为 (0, 0)
        // 直接从 (0, 0) 截取，避免因页面滚动 scrollY 导致的海报顶部被截断和底部空白问题
        x: 0,
        y: 0,
        width: rect.width,
        height: rect.height,
        scrollX: 0,
        scrollY: 0,
      });

      // Restore style
      posterRef.value.style.position = originalPosition;
      posterRef.value.style.top = originalTop;
      posterRef.value.style.left = originalLeft;
      posterRef.value.style.zIndex = originalZIndex;

      return canvas.toDataURL('image/jpeg', 0.9);
    } catch (err) {
      console.error('海报生成失败:', err);
      return null;
    }
  }
});
</script>

<style scoped>
.poster-wrapper {
  position: relative;
  background-color: transparent;
}

.poster-container {
  width: 375px;
  background-color: #F4F3EE;
  background-image: radial-gradient(rgba(92, 110, 88, 0.08) 1.2px, transparent 1.2px);
  background-size: 14px 14px;
  position: relative;
  overflow: hidden;
  padding: 30px 24px 24px 24px;
  box-sizing: border-box;
  font-family: var(--font-sans);
  color: var(--text-main);
  display: flex;
  flex-direction: column;
}

/* Subtle organic texture paper mask */
.poster-container::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.15;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  z-index: 0;
  pointer-events: none;
}

.poster-container > * {
  position: relative;
  z-index: 1;
}

.poster-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.brand-badge {
  display: flex;
  flex-direction: column;
}

.brand-cn {
  font-family: var(--font-serif);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--primary-color);
  letter-spacing: 1px;
}

.brand-en {
  font-size: 0.55rem;
  font-weight: 700;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.report-id {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--text-muted);
  background: rgba(0, 0, 0, 0.04);
  padding: 4px 8px;
  border-radius: 4px;
  font-family: monospace;
}

.poster-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Analytical Shutter Viewport */
.visual-viewport-card {
  position: relative;
  width: 100%;
  height: 220px;
  background: #1E2320;
  border-radius: var(--radius-md);
  border: 1px solid rgba(0, 0, 0, 0.08);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* HUD corners */
.visual-viewport-card::before {
  content: '';
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
  z-index: 2;
}

.snapshot-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.skeleton-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.view-shutter-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 20px;
  pointer-events: none;
}

.view-type-badge {
  font-size: 0.65rem;
  font-weight: 800;
  color: #FFF;
  background: rgba(0, 0, 0, 0.6);
  padding: 4px 8px;
  border-radius: 4px;
  letter-spacing: 1px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.primary-angle-badge {
  font-size: 0.65rem;
  font-weight: 800;
  color: #FFF;
  background: var(--primary-color);
  padding: 4px 8px;
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

/* Rider Profile Info Bar */
.rider-profile-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: var(--shadow-xs);
}

.rider-info-left {
  display: flex;
  flex-direction: column;
}

.rider-name {
  font-family: var(--font-serif);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-main);
}

.rider-date {
  font-size: 0.7rem;
  color: var(--text-muted);
  font-family: monospace;
}

.rider-title-badge {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--accent-gold);
  background: rgba(212, 175, 55, 0.08);
  border: 1px solid rgba(212, 175, 55, 0.15);
  padding: 4px 10px;
  border-radius: 20px;
}

/* Bento Grid */
.bento-layout {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 12px;
}

.bento-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 14px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-xs);
}

.bento-label {
  font-size: 0.65rem;
  font-weight: 800;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

/* Bento Card: Score */
.score-bento {
  justify-content: space-between;
}

.bento-score-body {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-val {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  font-weight: 800;
  line-height: 0.9;
}

.score-sub-box {
  display: flex;
  flex-direction: column;
}

.score-unit {
  font-size: 0.65rem;
  color: var(--text-muted);
  font-weight: bold;
}

.score-level {
  font-size: 0.8rem;
  font-weight: 800;
}

/* Bento Card: Performance */
.performance-bento {
  justify-content: center;
}

.bento-balance-bar {
  display: flex;
  height: 18px;
  border-radius: 20px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.05);
  position: relative;
  margin-bottom: 8px;
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.left-ratio, .right-ratio {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFF;
  font-size: 0.65rem;
  font-weight: 800;
}

.left-ratio {
  background: var(--primary-light);
}

.right-ratio {
  background: var(--primary-color);
}

.bento-sub-metric {
  font-size: 0.7rem;
  color: var(--text-muted);
  text-align: center;
}

.bento-sub-metric strong {
  color: var(--text-main);
}

.bento-list-metrics {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fallback-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
}

.fallback-item .m-lbl {
  color: var(--text-muted);
}

.fallback-item .m-val {
  font-weight: 700;
  color: var(--text-main);
}

/* Bento Card: Diagnosis (Full Width) */
.diagnosis-bento {
  grid-column: span 2;
  gap: 6px;
}

.diagnosis-insight-body {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.diagnosis-insight-body i {
  margin-top: 3px;
  font-size: 1rem;
}

.warning-icon {
  color: var(--danger-color);
}

.success-icon {
  color: var(--primary-color);
}

.diagnosis-desc {
  font-size: 0.75rem;
  line-height: 1.5;
  margin: 0;
  color: var(--text-body);
  font-weight: 500;
}

/* Referral Growth Banner */
.referral-growth-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, var(--primary-dark) 0%, #303B2E 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
  color: #FFF;
}

.gift-icon-box {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-gold);
  font-size: 0.95rem;
}

.referral-text-box h4 {
  font-family: var(--font-sans);
  font-size: 0.75rem;
  font-weight: 700;
  color: #FFF;
  margin: 0 0 2px 0;
  letter-spacing: 0.5px;
}

.referral-text-box p {
  font-size: 0.6rem;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

/* Footer layout */
.poster-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed rgba(92, 110, 88, 0.15);
}

.footer-left-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.slogan-cn {
  font-family: var(--font-serif);
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--primary-color);
  margin: 0;
  line-height: 1.2;
}

.sub-slogan-cn {
  font-size: 0.55rem;
  color: var(--text-muted);
  margin: 0;
  letter-spacing: 0.5px;
}

.qr-code-box {
  padding: 4px;
  background: #FFF;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-shrink: 0;
}
</style>
