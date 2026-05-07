<template>
  <!-- 包装容器：如果已经生成了图片，就直接展示图片；否则展示 DOM 用于截图 -->
  <div class="poster-wrapper">
    <!-- 生成后的纯图片展示区 (用于用户长按保存) -->
    <img
        v-if="cachedPosterImage"
        :src="cachedPosterImage"
        class="final-poster-image"
        alt="Fitting Report Poster"
    />

    <!-- 原始 DOM 渲染区 (截图前展示，生成图片后隐藏，避免重复渲染) -->
    <div
        v-show="!cachedPosterImage"
        class="poster-container"
        ref="posterRef"
    >
      <!-- 1. 顶部：品牌与用户信息 -->
      <header class="poster-header">
        <div class="brand-badge">
          <span class="brand-cn">骑迹</span>
          <span class="brand-en">RideTrace</span>
        </div>
        <div class="user-meta">
          <div class="user-info">
            <span class="username">{{ safeResult.username || 'Aries Rider' }}</span>
            <span class="report-date">{{ formatDate(safeResult.fittingTime) }}</span>
          </div>
          <!-- 推荐使用真实图片，这里用首字母代替 FontAwesome -->
          <div class="avatar-circle">
            {{ (safeResult.username || 'A').charAt(0).toUpperCase() }}
          </div>
        </div>
      </header>

      <!-- 2. 主视觉：分数与雷达图 -->
      <main class="poster-body">
        <div class="score-card">
          <div class="score-info">
            <p class="score-label">FITTING GRADE</p>
            <h1 class="fitting-score">{{ calculatedScore }}</h1>
          </div>
          <!-- 雷达图容器缩小并靠右 -->
          <div ref="chartRef" class="radar-chart-mini"></div>
        </div>

        <!-- 3. 核心洞察：硬核数据矩阵 (骑士最爱分享的内容) -->
        <div class="metrics-grid">
          <div class="metric-item">
            <span class="m-label">建议座高</span>
            <span class="m-value">{{ safeResult.recSeatHeight || '745' }} <small>mm</small></span>
          </div>
          <div class="metric-item">
            <span class="m-label">座垫后飘</span>
            <span class="m-value">{{ safeResult.recSaddleSetback || '50' }} <small>mm</small></span>
          </div>
          <div class="metric-item">
            <span class="m-label">把立长度</span>
            <span class="m-value">{{ safeResult.recStemLength || '100' }} <small>mm</small></span>
          </div>
          <div class="metric-item">
            <span class="m-label">曲柄建议</span>
            <span class="m-value">{{ safeResult.recCrankLength || '170' }} <small>mm</small></span>
          </div>
        </div>

        <!-- 4. 专家诊断 -->
        <div class="verdict-section">
          <svg class="quote-icon" viewBox="0 0 24 24" width="20" height="20">
            <path fill="#D4AF37" d="M14.017 21v-7.391c0-5.704 3.731-9.57 8.983-10.609l.995 2.151c-2.432.917-3.995 3.638-3.995 5.849h4v10h-9.983zm-14.017 0v-7.391c0-5.704 3.748-9.57 9-10.609l.996 2.151c-2.433.917-3.996 3.638-3.996 5.849h3.983v10h-9.983z"/>
          </svg>
          <p class="verdict-text">{{ expertVerdict }}</p>
        </div>
      </main>

      <!-- 5. 底部：二维码与 Slogan -->
      <footer class="poster-footer">
        <div class="footer-left">
          <p class="slogan">每一次发力，皆有迹可循。</p>
          <p class="sub-slogan">RideTrace · 智能单车静态几何适配</p>
        </div>
        <div class="qr-code-section">
          <qrcode-vue
              :value="shareUrl"
              :size="55"
              level="H"
              background="#fff"
              foreground="#334030"
          />
          <span class="qr-text">扫码体验智能调教</span>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import * as echarts from 'echarts';
import html2canvas from 'html2canvas';
import QrcodeVue from 'qrcode.vue';
import request from '@/api/request.js';

const props = defineProps({
  result: {
    type: Object,
    default: null
  }
});

const posterRef = ref(null);
const chartRef = ref(null);
const cachedPosterImage = ref(null); // 图片缓存机制
const inviteCode = ref('');
let myChart = null;

// 防御性计算属性，避免 result 为 null 时报错
const safeResult = computed(() => props.result || {});

// 动态生成分享链接
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

const calculatedScore = computed(() => {
  return safeResult.value.thoracicScore || 85;
});

const expertVerdict = computed(() => {
  if (safeResult.value.details && safeResult.value.details.length > 0) {
    return safeResult.value.details[0].adjustmentLogic;
  }
  return "躯干角度与踩踏发力区间均处于黄金分割点，建议保持当前几何设定，通过增加核心力量训练进一步提升功率输出。";
});

const formatDate = (dateStr) => {
  if (!dateStr) return new Date().toLocaleDateString().replace(/\//g, '-');
  return dateStr.split(' ')[0];
};

const initChart = async () => {
  // 确保 DOM 已经渲染
  await nextTick();
  if (!chartRef.value || cachedPosterImage.value) return;

  if (myChart) myChart.dispose();
  myChart = echarts.init(chartRef.value);

  const option = {
    radar: {
      indicator:[
        { name: '柔韧', max: 100 },
        { name: '稳定', max: 100 },
        { name: '效率', max: 100 },
        { name: '比例', max: 100 },
        { name: '核心', max: 100 }
      ],
      shape: 'polygon', // 改为多边形，视觉更硬核
      radius: '65%',
      splitNumber: 3,
      axisName: { color: '#787673', fontSize: 9 },
      splitLine: { lineStyle: { color: 'rgba(92, 110, 88, 0.2)' } },
      splitArea: { show: false },
      axisLine: { lineStyle: { color: 'rgba(92, 110, 88, 0.2)' } }
    },
    series: [{
      type: 'radar',
      data:[{
        value:[
          safeResult.value.flexibility || 85,
          safeResult.value.stability || 80,
          safeResult.value.efficiency || 90,
          safeResult.value.proportion || 88,
          safeResult.value.core || 75
        ],
        symbol: 'circle',
        symbolSize: 4,
        itemStyle: { color: '#D4AF37' },
        areaStyle: { color: 'rgba(212, 175, 55, 0.4)' },
        lineStyle: { width: 1.5, color: '#D4AF37' }
      }]
    }]
  };

  myChart.setOption(option);
};

onMounted(() => {
  if (props.result) initChart();
  fetchInviteCode();
});

watch(() => props.result, (newVal) => {
  if (newVal) {
    cachedPosterImage.value = null; // 数据变化时清空缓存
    initChart();
  }
}, { deep: true });

// 暴露给父组件的方法
defineExpose({
  generateImage: async () => {
    // 1. 如果已经生成过，直接返回缓存！避免重复消耗性能
    if (cachedPosterImage.value) {
      return cachedPosterImage.value;
    }

    // 2. 确保图表完全渲染完毕
    await nextTick();
    if (!posterRef.value) return null;

    try {
      // 为了保证 html2canvas 截图时 Echarts 动画已完成
      if (myChart) {
        myChart.setOption({ animation: false }); // 临时关闭动画
      }

      const canvas = await html2canvas(posterRef.value, {
        useCORS: true,
        scale: 3, // 提高至 3 倍图，保证字体和二维码极度清晰
        backgroundColor: '#F3F2ED',
        logging: false
      });

      const imgDataUrl = canvas.toDataURL('image/png');

      // 3. 将生成的图片存入局部状态，DOM 会自动替换为 <img> 标签
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
/* 包装器 */
.poster-wrapper {
  width: 375px;
  background-color: transparent;
  display: flex;
  justify-content: center;
}

/* 最终图片展示（供长按保存） */
.final-poster-image {
  width: 100%;
  height: auto;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0,0,0,0.15);
  display: block;
}

/* 海报 DOM 结构 */
.poster-container {
  width: 375px;
  background: linear-gradient(135deg, #F9F8F5 0%, #EFEEDB 100%);
  padding: 30px;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #334030;
}

.poster-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid rgba(92, 110, 88, 0.15);
  padding-bottom: 15px;
}

.brand-badge {
  display: flex;
  flex-direction: column;
}
.brand-cn { font-size: 1.4rem; font-weight: 800; color: #5C6E58;}
.brand-en { font-size: 0.65rem; font-weight: 600; color: #D4AF37; letter-spacing: 1px;}

.user-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-info {
  text-align: right;
  display: flex;
  flex-direction: column;
}
.username { font-size: 0.9rem; font-weight: 700; color: #334030;}
.report-date { font-size: 0.65rem; color: #787673;}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #5C6E58;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.1rem;
}

/* 分数与图表区 */
.score-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.score-info {
  flex: 1;
}
.score-label {
  font-size: 0.7rem;
  letter-spacing: 2px;
  color: #787673;
  margin: 0 0 5px 0;
  font-weight: 600;
}
.fitting-score {
  font-size: 4.5rem;
  font-weight: 900;
  color: #D4AF37;
  margin: 0;
  line-height: 1;
  text-shadow: 2px 2px 0px rgba(212, 175, 55, 0.2);
}

.radar-chart-mini {
  width: 140px;
  height: 140px;
}

/* 硬核数据矩阵 */
.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  margin-bottom: 30px;
}

.metric-item {
  background: #FFF;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.03);
  display: flex;
  flex-direction: column;
  border-left: 3px solid #5C6E58;
}

.m-label {
  font-size: 0.65rem;
  color: #787673;
  margin-bottom: 4px;
}
.m-value {
  font-size: 1.2rem;
  font-weight: 800;
  color: #334030;
}
.m-value small {
  font-size: 0.6rem;
  font-weight: normal;
  color: #999;
}

/* 专家诊断 */
.verdict-section {
  position: relative;
  background: rgba(212, 175, 55, 0.08);
  padding: 20px 20px 20px 40px;
  border-radius: 8px;
  margin-bottom: 40px;
}
.quote-icon {
  position: absolute;
  top: 15px;
  left: 12px;
  opacity: 0.5;
}
.verdict-text {
  font-size: 0.85rem;
  line-height: 1.6;
  margin: 0;
  color: #5C6E58;
  font-weight: 500;
}

/* 底部 */
.poster-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.slogan {
  font-size: 0.9rem;
  font-weight: 800;
  color: #334030;
  margin: 0 0 6px 0;
}
.sub-slogan {
  font-size: 0.6rem;
  color: #787673;
  margin: 0;
}

.qr-code-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qr-mock {
  width: 55px;
  height: 55px;
  background: #fff;
  border: 1px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #999;
}

.qr-text {
  font-size: 0.55rem;
  color: #787673;
}
</style>