<template>
  <div class="poster-wrapper">
    <img
        v-if="cachedPosterImage"
        :src="cachedPosterImage"
        class="final-poster-image"
        alt="Fitting Report Poster"
    />

    <div
        v-show="!cachedPosterImage"
        class="poster-container"
        ref="posterRef"
    >
      <header class="poster-header">
        <div class="brand-badge">
          <span class="brand-cn">{{ PROJECT_CONFIG.NAME }}</span>
          <span class="brand-en">{{ PROJECT_CONFIG.NAME_EN }} LAB</span>
        </div>
        <div class="user-info">
          <span class="username">{{ posterUsername }}</span>
          <span class="rider-title">{{ riderTitle }}</span>
        </div>
      </header>

      <main class="poster-body">
        
        <div class="score-badge-section">
          <div class="score-badge">
            <span class="badge-label">综合适配评估</span>
            <div class="badge-value-wrapper">
              <span class="badge-val">{{ calculatedScore }}</span>
              <span class="badge-unit">分</span>
            </div>
          </div>
        </div>

        <div class="chart-section">
          <div ref="radarChartRef" class="radar-chart"></div>
        </div>

        <div class="conclusion-section">
          <div class="aha-moment">
            <div class="aha-card danger">
              <div class="aha-icon"><i class="fas fa-heartbeat"></i> 临床体征诊断</div>
              <p class="aha-text">{{ diagnosisText }}</p>
            </div>
            <div class="aha-card info">
              <div class="aha-icon"><i class="fas fa-file-medical-alt"></i> 骑士处方提要</div>
              <p class="aha-text">{{ prescriptionText }}</p>
            </div>
          </div>
        </div>
      </main>

      <footer class="poster-footer">
        <div class="footer-left">
          <p class="slogan">你的单车是在帮你，还是毁你？</p>
          <p class="sub-slogan">长按扫码，开启你的专属单车几何体检 👉</p>
        </div>
        <div class="qr-code-section">
          <qrcode-vue
              :value="shareUrl"
              :size="56"
              level="H"
              background="#fff"
              foreground="#1D1D1F"
          />
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick, markRaw } from 'vue';
import html2canvas from 'html2canvas';
import QrcodeVue from 'qrcode.vue';
import * as echarts from 'echarts';
import request from '@/api/request.js';
import { PROJECT_CONFIG } from '@/config/constants';
import { useUserStore } from '@/stores/user';

const props = defineProps({
  result: {
    type: Object,
    default: null
  }
});

const userStore = useUserStore();
const posterRef = ref(null);
const radarChartRef = ref(null);
const cachedPosterImage = ref(null);
const inviteCode = ref('');
let chartInstance = null;

const safeResult = computed(() => props.result || {});

// === Username Logic ===
const posterUsername = computed(() => {
  if (safeResult.value.username) return safeResult.value.username;
  try {
    const userInfoStr = localStorage.getItem('user_info');
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      return userInfo.name;
    }
  } catch (e) {
    console.warn('Failed to parse user_info from localStorage', e);
  }
  return `${PROJECT_CONFIG.MASCOT} Rider`;
});

// === Radar Data Derivation ===
const radarDimensions = computed(() => {
  const res = safeResult.value;
  const details = res.details || [];
  const detailsStr = JSON.stringify(details);
  const tagsStr = JSON.stringify(res.riderTags || []);
  const painCount = res.painLabels?.length || 0;

  // 1. 柔韧素质
  const flex = res.thoracicScore || 80;
  
  // 2. 核心力量
  let core = 85;
  if (detailsStr.includes('核心') || detailsStr.includes('稳定') || detailsStr.includes('预警')) {
    core -= 20;
  } else if (flex > 80) {
    core += 5; // 如果柔韧好，核心通常给点福利
  }

  // 3. 抗伤痛指数
  const painResist = Math.max(30, 100 - (painCount * 15));

  // 4. 下肢杠杆
  let legLeverage = 80;
  if (detailsStr.includes('长股骨') || tagsStr.includes('长股骨') || detailsStr.includes('发力')) {
    legLeverage += 15;
  }

  // 5. 器材适配
  let equipFit = 90;
  if (detailsStr.includes('规格') || detailsStr.includes('把立') || detailsStr.includes('更换') || detailsStr.includes('升级')) {
    equipFit -= 15;
  }

  return {
    flexibility: flex,
    core: core,
    painResistance: painResist,
    legLeverage: legLeverage,
    equipmentFit: equipFit
  };
});

// === Dynamic Texts ===
const riderTitle = computed(() => {
  const d = radarDimensions.value;
  let max = -1;
  let title = '[普通骑手]';
  
  if (d.flexibility > max) { max = d.flexibility; title = '[柔韧度 S 级]'; }
  if (d.core > max) { max = d.core; title = '[核心王者]'; }
  if (d.painResistance > max) { max = d.painResistance; title = '[无伤行者]'; }
  if (d.legLeverage > max) { max = d.legLeverage; title = '[功率机器]'; }
  if (d.equipmentFit > max) { max = d.equipmentFit; title = '[原车绝配]'; }
  
  return title;
});

const calculatedScore = computed(() => {
  return safeResult.value.matchScore || safeResult.value.thoracicScore || 85;
});

const diagnosisText = computed(() => {
  const details = safeResult.value.details || [];
  const painDetail = details.find(d => 
    d.bodyFeature && (d.bodyFeature.includes('痛') || d.bodyFeature.includes('酸') || d.bodyFeature.includes('麻'))
  );
  if (painDetail) {
    return `检测到该骑士存在【${painDetail.bodyFeature}】隐患，原车几何设定对局部肌肉与神经产生异常压迫。`;
  }
  return '躯干与核心体征表现良好，但目前车辆的粗略设定可能正在制约其发力效率。';
});

const prescriptionText = computed(() => {
  const res = safeResult.value;
  const fields = [];
  let previewItem = '';
  
  if (res.recSeatHeight) { fields.push('座高'); if(!previewItem) previewItem = `建议座高调整至 ${res.recSeatHeight}mm`; }
  if (res.recSetback) { fields.push('座垫位置'); if(!previewItem) previewItem = `建议座垫后移 ${res.recSetback}mm`; }
  if (res.recReachPlus) { fields.push('前伸量'); if(!previewItem) previewItem = `建议前伸量(Reach+)调整至 ${res.recReachPlus}mm`; }
  if (res.recHandlebarDrop) { fields.push('落差'); if(!previewItem) previewItem = `建议车把落差调整为 ${res.recHandlebarDrop}mm`; }
  if (res.recStemLength) { fields.push('把立'); }
  if (res.recCrankLength) { fields.push('曲柄'); }
  
  const count = fields.length || 3;
  
  if (previewItem) {
    return `【关键参数公开】${previewItem}。系统已针对该骑士分析其余 ${Math.max(1, count - 1)} 项专属防伤痛调教指令。`;
  }
  return `${PROJECT_CONFIG.MASCOT}-已针对其关键部位，分析了 ${count} 项专属防伤痛调教指令。`;
});

const shareUrl = computed(() => {
  const origin = window.location.origin;
  if (inviteCode.value) {
    return `${origin}/?inviter=${inviteCode.value}`;
  }
  return `${origin}/`;
});

const fetchInviteCode = async () => {
  try {
    const res = await request.get('/membership/invite-code');
    if (res.code === 200) {
      inviteCode.value = res.data.inviteCode;
    }
  } catch (e) {
    console.error('获取邀请码失败', e);
  }
};

const initChart = () => {
  if (!radarChartRef.value) return;
  if (chartInstance) {
    chartInstance.dispose();
  }
  chartInstance = markRaw(echarts.init(radarChartRef.value, null, { renderer: 'svg' })); 
  
  // 动态读取 CSS 主题色变量，实现 ECharts 图表与页面主题全自动联动
  let themeColor = '#0A84FF';
  if (posterRef.value) {
    const style = getComputedStyle(posterRef.value);
    const customColor = style.getPropertyValue('--primary-color').trim();
    if (customColor) {
      themeColor = customColor;
    }
  }
  
  const d = radarDimensions.value;
  const option = {
    animation: false, 
    radar: {
      indicator: [
        { name: '脊柱柔韧', max: 100 },
        { name: '核心支撑', max: 100 },
        { name: '抗伤痛值', max: 100 },
        { name: '下肢杠杆', max: 100 },
        { name: '器材适配', max: 100 }
      ],
      shape: 'polygon',
      center: ['50%', '50%'],
      radius: '65%',
      splitNumber: 4,
      axisName: {
        color: '#86868B', // Secondary text
        fontSize: 11,
        fontWeight: 'bold',
      },
      splitArea: {
        areaStyle: {
          color: ['#F9F9FB', '#F4F5F7', '#EFEFF2', '#EAECEF'],
        }
      },
      axisLine: {
        lineStyle: { color: 'rgba(0,0,0,0.05)' } 
      },
      splitLine: {
        lineStyle: { color: 'rgba(0,0,0,0.08)' }
      }
    },
    series: [
      {
        name: 'Clinical Stats',
        type: 'radar',
        data: [
          {
            value: [d.flexibility, d.core, d.painResistance, d.legLeverage, d.equipmentFit],
            name: '能力分布',
            itemStyle: {
              color: themeColor
            },
            areaStyle: {
              color: themeColor,
              opacity: 0.15 // 利用 opacity 做透明度，完美适配任意格式的主题色（Hex/RGB）
            },
            lineStyle: {
              width: 2,
              color: themeColor
            }
          }
        ]
      }
    ]
  };
  
  chartInstance.setOption(option);
};

onMounted(() => {
  fetchInviteCode();
  nextTick(() => {
    initChart();
  });
});

watch(() => props.result, (newVal) => {
  if (newVal) {
    cachedPosterImage.value = null;
    nextTick(() => {
      initChart();
    });
  }
}, { deep: true });

defineExpose({
  generateImage: async () => {
    if (cachedPosterImage.value) {
      return cachedPosterImage.value;
    }

    await nextTick();
    if (!posterRef.value) return null;

    try {
      // 【终极方案】将元素临时挂载到视口绝对左上角，脱离所有父元素的 margin/padding/scroll/transform 干扰
      const originalPosition = posterRef.value.style.position;
      const originalTop = posterRef.value.style.top;
      const originalLeft = posterRef.value.style.left;
      const originalZIndex = posterRef.value.style.zIndex;

      posterRef.value.style.position = 'fixed';
      posterRef.value.style.top = '0px';
      posterRef.value.style.left = '0px';
      posterRef.value.style.zIndex = '-9999'; // 放在最底层，用户无感知

      // 稍微等待 DOM 和 Echarts 渲染更新
      await new Promise(resolve => setTimeout(resolve, 300));

      const rect = posterRef.value.getBoundingClientRect();
      const canvas = await html2canvas(posterRef.value, {
        useCORS: true,
        scale: 3,
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
        windowWidth: 375, // Lock the capture window width
      });

      // 恢复原状
      posterRef.value.style.position = originalPosition;
      posterRef.value.style.top = originalTop;
      posterRef.value.style.left = originalLeft;
      posterRef.value.style.zIndex = originalZIndex;

      const imgDataUrl = canvas.toDataURL('image/png');
      cachedPosterImage.value = imgDataUrl;

      return imgDataUrl;
    } catch (err) {
      console.error('生成海报失败:', err);
      return null;
    }
  }
});
</script>

<style scoped>
.poster-wrapper {
  width: 375px;
  background-color: transparent;
  display: flex;
  justify-content: center;
}

.final-poster-image {
  width: 100%;
  height: auto;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.15);
  display: block;
}

.poster-container {
  width: 375px;
  /* 融合微光点缀直接到背景渐变中，彻底去除任何绝对定位的溢出元素，杜绝画布高度意外膨胀 */
  background: radial-gradient(circle at 100% 0%, rgba(10,132,255,0.06) 0%, rgba(255,255,255,0) 55%),
              linear-gradient(135deg, #FFFFFF 0%, #F4F5F7 100%);
  position: relative;
  overflow: hidden;
  padding: 35px 25px 25px 25px;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #1D1D1F;
  display: flex;
  flex-direction: column;
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

.brand-badge { display: flex; flex-direction: column; align-items: flex-start; }
.brand-cn { font-size: 1.1rem; font-weight: 800; color: #1D1D1F;}
.brand-en { font-size: 0.55rem; font-weight: 700; color: #86868B; letter-spacing: 1px;}

.user-info { display: flex; flex-direction: column; gap: 2px; align-items: flex-end; }
.username { font-size: 0.95rem; font-weight: 700; color: #1D1D1F; }
.rider-title { 
  font-size: 0.65rem; color: var(--primary-color); font-weight: 600; 
  background: rgba(10,132,255,0.08);
  padding: 2px 6px; border-radius: 4px;
}

.poster-body {
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* 重新设计的勋章分数 */
.score-badge-section {
  display: flex;
  justify-content: center;
  margin-bottom: 5px;
}
.score-badge {
  display: inline-flex;
  align-items: center;
  background: #FFF;
  border: 1px solid rgba(0,0,0,0.05);
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  border-radius: 30px;
  padding: 4px 16px 4px 6px;
}
.badge-label {
  font-size: 0.65rem;
  color: #86868B;
  font-weight: 600;
  background: #F4F5F7;
  padding: 4px 10px;
  border-radius: 20px;
  margin-right: 10px;
}
.badge-value-wrapper {
  display: flex;
  align-items: baseline;
}
.badge-val {
  font-size: 1.4rem;
  font-weight: 900;
  color: #1D1D1F;
}
.badge-unit {
  font-size: 0.7rem;
  color: #86868B;
  margin-left: 2px;
  font-weight: bold;
}

.chart-section {
  width: 100%;
  height: 250px;
  margin-bottom: 5px;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}
.radar-chart {
  width: 100%; 
  height: 100%;
}

.conclusion-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 25px;
}

.aha-moment {
  display: flex; flex-direction: column; gap: 10px;
}

.aha-card {
  padding: 14px 16px;
  border-radius: 12px;
  background: #FFF;
  border: 1px solid rgba(0,0,0,0.04);
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
}
.aha-card.danger {
  background: #FFF5F5;
  border-left: 3px solid #FF3B30;
}
.aha-card.info {
  background: #F0F8FF;
  border-left: 3px solid var(--primary-color);
}

.aha-icon {
  font-size: 0.75rem; font-weight: 800; margin-bottom: 6px; letter-spacing: 0.5px;
}
.aha-card.danger .aha-icon { color: #FF3B30; }
.aha-card.info .aha-icon { color: var(--primary-color); }

.aha-text {
  font-size: 0.8rem; line-height: 1.6; margin: 0; color: #48484A; font-weight: 500;
}

/* Footer */
.poster-footer { 
  display: flex; justify-content: space-between; align-items: center; 
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid rgba(0,0,0,0.05);
}
.footer-left {
  display: flex; flex-direction: column; gap: 6px;
}
.slogan { 
  font-size: 0.85rem; font-weight: 800; color: #1D1D1F; margin: 0; 
}
.sub-slogan { 
  font-size: 0.6rem; color: var(--primary-color); margin: 0; font-weight: 600;
}
.qr-code-section {
  padding: 4px;
  background: #FFF;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
}
</style>