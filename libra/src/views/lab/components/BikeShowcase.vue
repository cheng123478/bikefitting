<template>
  <div class="showcase-overlay" @click.self="$emit('close')">
    <!-- 1. 顶部操作栏 -->
    <div class="showcase-header">
      <div class="bike-title-box">
        <span class="en-label">BIKE SHOWCASE / DIGITAL TWIN</span>
        <h2 class="brand-text">{{ bikeName }}</h2>
        <p class="specs-text">{{ bike.modelYear }} · {{ bike.frameSize }} · {{ getBikeTypeName(bike.bikeType) }}</p>
      </div>
      <button class="btn-close-circle" @click="$emit('close')">
        <i class="fas fa-times"></i>
      </button>
    </div>

    <div class="showcase-container">
      <!-- 2. 左侧面板 -->
      <aside class="side-panel left-panel">
        <div class="metric-card glass-card" v-scroll-reveal="1">
          <label class="card-label">几何性格 / STR</label>
          <div class="main-value">{{ calculateSTR(bike) }}</div>
          <div class="str-meter">
            <div class="meter-bar">
              <div class="pointer" :style="{ left: calcStrPosition(calculateSTR(bike)) + '%' }"></div>
            </div>
            <div class="meter-labels">
              <span>Race</span><span>Endurance</span>
            </div>
          </div>
        </div>

        <div class="metric-card glass-card" v-scroll-reveal="2">
          <label class="card-label">核心触点规格</label>
          <ul class="mini-specs">
            <li><span>曲柄长度</span><strong>{{ bike.crankLength || '--' }}mm</strong></li>
            <li><span>座垫宽度</span><strong>{{ bike.saddleWidth || '--' }}mm</strong></li>
            <li><span>把横宽度</span><strong>{{ bike.handlebarWidth || '--' }}mm</strong></li>
          </ul>
        </div>

        <button v-if="bike.status === 1" class="btn-link-retire" @click="handleRetire">
          <i class="fas fa-archive"></i> 退役这辆单车
        </button>
        <button v-else-if="bike.status === 2" class="btn-link-restore" @click="handleRestore">
          <i class="fas fa-undo"></i> 恢复这辆单车
        </button>
      </aside>

      <!-- 3. 中间视觉区 - 1408*736 像素级对齐引擎 -->
      <main class="bike-visual-zone">
        <div class="visual-wrapper">
          <img :src="currentVisualImg" class="bike-silhouette" :class="{ 'dimmed': activePart }" />

          <!-- viewBox 与图片原始尺寸 1408*736 完全同步 -->
          <svg viewBox="0 0 1408 736" class="overlay-svg-layer">
            <defs>
              <filter id="goldGlow">
                <feGaussianBlur stdDeviation="4" result="blur" />
                <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
              </filter>
            </defs>

            <!-- 五通中心原点 (BB) -->
            <circle :cx="c.bb.x" :cy="c.bb.y" r="10" class="bb-point" />

            <!-- 动态流光指标线 -->
            <g v-if="activePart" class="indicator-group">
              <path :d="indicatorPath" class="path-glow" />
              <!-- 悬浮数值标签 -->
              <g :transform="`translate(${labelPos.x}, ${labelPos.y})`">
                <rect x="-60" y="-20" width="120" height="40" rx="6" fill="var(--accent-gold)" />
                <text x="0" y="7" class="svg-tag-text">{{ activeLabel }}</text>
              </g>
            </g>
          </svg>
        </div>
      </main>

      <!-- 4. 右侧参数面板 - 分组滚动列表 -->
      <aside class="side-panel right-panel">
        <div class="data-scroll-box">

          <!-- 分组一：调教设定 -->
          <div class="data-group">
            <h4 class="group-title">调教设定 (Setup)</h4>
            <div class="metric-row" v-for="m in setupMetrics" :key="m.key"
                 @mouseenter="activePart = m.key" @mouseleave="activePart = null">
              <span class="l">{{ m.label }}</span>
              <span class="v" :class="{active: activePart === m.key}">
                {{ bike[m.key] || '--' }}<small>mm</small>
              </span>
            </div>
          </div>

          <!-- 分组二：车架几何 -->
          <div class="data-group">
            <h4 class="group-title">车架几何 (Geometry)</h4>
            <div class="metric-row" v-for="m in geoMetrics" :key="m.key"
                 @mouseenter="activePart = m.key" @mouseleave="activePart = null">
              <span class="l">{{ m.label }}</span>
              <span class="v" :class="{active: activePart === m.key}">
                {{ bike[m.key] || '--' }}<small>{{ m.unit || 'mm' }}</small>
              </span>
            </div>
          </div>
        </div>

        <button v-if="bike.status === 1" class="btn-gold-action" @click="handleEdit">
          <i class="fas fa-sliders-h"></i> 校准物理参数
        </button>
      </aside>
    </div>
    <div class="global-grid-bg"></div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { showConfirm, showToast } from '@/utils/toast';
import { retireUserBike, restoreUserBike } from '@/api/userBike.js';

import imgRoad from '@/assets/image/road-bike.png';
import imgMtb from '@/assets/image/mtb-bike.png';

const props = defineProps({ bike: Object });
const emit = defineEmits(['close', 'refresh']);
const router = useRouter();
const activePart = ref(null);

// --- 1. 5 种车型坐标配置表 (针对 1408*736 底图) ---
const bikeConfigs = {
  1: { // ROAD
    img: imgRoad,
    bb: { x: 510, y: 512 }, saddle: { x: 420, y: 155 }, ht: { x: 960, y: 260 }, bar: { x: 1080, y: 260 }
  },
  2: { // MTB
    img: imgMtb,
    bb: { x: 520, y: 490 }, saddle: { x: 450, y: 180 }, ht: { x: 920, y: 220 }, bar: { x: 1050, y: 220 }
  },
  3: { // GRAVEL (参考Road但略高)
    img: imgRoad,
    bb: { x: 655, y: 565 }, saddle: { x: 420, y: 170 }, ht: { x: 960, y: 240 }, bar: { x: 1080, y: 240 }
  },
  4: { // TT (铁三车座垫靠前)
    img: imgRoad,
    bb: { x: 680, y: 560 }, saddle: { x: 600, y: 155 }, ht: { x: 980, y: 280 }, bar: { x: 1150, y: 280 }
  },
  5: { // CITY (城市车座垫靠后，把手极高)
    img: imgMtb,
    bb: { x: 640, y: 550 }, saddle: { x: 380, y: 200 }, ht: { x: 900, y: 150 }, bar: { x: 1000, y: 120 }
  }
};

const c = computed(() => bikeConfigs[props.bike.bikeType] || bikeConfigs[1]);
const currentVisualImg = computed(() => c.value.img);
const bikeName = computed(() => props.bike.brandModel || `${props.bike.brand || ''} ${props.bike.model || ''}`.trim() || '未命名单车');

// --- 2. 动态分析路径 ---
const indicatorPath = computed(() => {
  if (!activePart.value) return "";
  const { bb, saddle, ht, bar } = c.value;
  const paths = {
    saddleHeight: `M${bb.x},${bb.y} L${saddle.x},${saddle.y}`,
    saddleSetback: `M${bb.x},${bb.y} V${saddle.y} H${saddle.x}`,
    stemLength: `M${ht.x},${ht.y} L${bar.x},${bar.y}`,
    handlebarDrop: `M${saddle.x},${saddle.y} H${bar.x} V${bar.y}`,
    frameStack: `M${bb.x},${bb.y} V${ht.y} H${ht.x}`,
    frameReach: `M${bb.x},${ht.y} H${ht.x}`,
    spacersHeight: `M${ht.x},${ht.y} V${ht.y + 40}`,
    wheelBase: `M${bb.x - 400},${bb.y + 60} H${bb.x + 450}`
  };
  return paths[activePart.value] || "";
});

const labelPos = computed(() => {
  const { bb, saddle, ht } = c.value;
  const posMap = {
    saddleHeight: { x: (bb.x + saddle.x)/2 - 80, y: (bb.y + saddle.y)/2 },
    saddleSetback: { x: bb.x - 80, y: saddle.y - 30 },
    frameStack: { x: bb.x - 100, y: (bb.y + ht.y)/2 },
    frameReach: { x: (bb.x + ht.x)/2, y: ht.y - 40 }
  };
  return posMap[activePart.value] || { x: 704, y: 368 };
});

const activeLabel = computed(() => activePart.value?.replace(/([A-Z])/g, ' $1').toUpperCase());

// --- 3. 页面逻辑保持 ---
const setupMetrics = [
  { label: '座垫高度', key: 'saddleHeight' },
  { label: '座垫后移', key: 'saddleSetback' },
  { label: '把立长度', key: 'stemLength' },
  { label: '车头落差', key: 'handlebarDrop' },
  { label: '垫圈高度', key: 'spacersHeight' }
];
const geoMetrics = [
  { label: '车架堆高 Stack', key: 'frameStack' },
  { label: '车架前伸 Reach', key: 'frameReach' },
  { label: '立管角度 STA', key: 'seatTubeAngle', unit: '°' },
  { label: '五通下沉 BB Drop', key: 'bbDrop' },
  { label: '轴距 Wheelbase', key: 'wheelBase' }
];

const calculateSTR = (b) => (b.frameStack && b.frameReach) ? (b.frameStack / b.frameReach).toFixed(2) : '--';
const calcStrPosition = (str) => str === '--' ? 50 : Math.max(0, Math.min(100, ((parseFloat(str) - 1.35) / 0.25) * 100));
const getBikeTypeName = (t) => ({ 1: 'ROAD', 2: 'MTB', 3: 'GRAVEL', 4: 'TT', 5: 'CITY' }[t] || 'BIKE');
const handleEdit = () => { emit('close'); router.push({ path: '/lab/garage/edit', query: { id: props.bike.id } }); };

const handleRetire = async () => {
  if (await showConfirm(`确定要将 ${bikeName.value} 退役吗？`, '单车退役')) {
    try { await retireUserBike(props.bike.id); showToast('已退役', 'success'); emit('refresh'); emit('close'); }
    catch (e) { showToast('失败', 'error'); }
  }
};

const handleRestore = async () => {
  if (await showConfirm(`确定要将 ${bikeName.value} 重新投入现役吗？`, '恢复单车')) {
    try {
      await restoreUserBike(props.bike.id);
      showToast('已恢复至车库', 'success');
      emit('refresh');
      emit('close');
    } catch (e) {
      showToast('恢复失败', 'error');
    }
  }
};
</script>

<style scoped>
/* 1. 基础容器：加深背景，确保对比度 */
.showcase-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(18, 18, 16, 0.98); backdrop-filter: blur(30px);
  color: #fff; display: flex; flex-direction: column;
}

/* 2. Header：找回原本精致的叉和标题 */
.showcase-header { padding: 40px 60px; display: flex; justify-content: space-between; align-items: flex-start; }
.en-label { font-size: 0.7rem; color: var(--accent-gold); letter-spacing: 4px; font-weight: 800; }
.brand-text { font-size: 2.8rem; line-height: 1.1; margin: 8px 0; }
.specs-text { font-size: 0.9rem; color: rgba(255,255,255,0.4); text-transform: uppercase; }

.btn-close-circle {
  width: 48px; height: 48px; border-radius: 50%; border: 1px solid rgba(255,255,255,0.2);
  background: transparent; color: #fff; cursor: pointer; transition: 0.3s;
}
.btn-close-circle:hover { background: #fff; color: #000; transform: rotate(90deg); }

/* 3. 布局：三栏网格 */
.showcase-container {
  flex: 1; display: grid; grid-template-columns: 320px 1fr 380px; gap: 40px;
  padding: 0 60px 60px; align-items: center; overflow: hidden;
}

/* 4. 左侧：高对比度卡片 */
.metric-card { padding: 25px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; margin-bottom: 25px; }
.card-label { font-size: 0.7rem; color: rgba(255,255,255,0.3); text-transform: uppercase; margin-bottom: 12px; display: block; }
.main-value { font-family: var(--font-serif); font-size: 3.2rem; color: #fff; }

.mini-specs li { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 0.95rem; }
.mini-specs span { color: rgba(255,255,255,0.4); }

.btn-link-retire { background: none; border: none; color: rgba(255,255,255,0.2); font-size: 0.85rem; cursor: pointer; margin-top: 15px; text-decoration: underline; }
.btn-link-retire:hover { color: #ff4d4f; }
.btn-link-restore { background: none; border: none; color: var(--accent-gold); font-size: 0.85rem; cursor: pointer; margin-top: 15px; text-decoration: underline; opacity: 0.6; }
.btn-link-restore:hover { opacity: 1; }

/* 5. 视觉区：数字化身对齐 */
.visual-wrapper { position: relative; width: 100%; max-width: 1000px; margin: 0 auto; aspect-ratio: 1408 / 736; }
.bike-silhouette { width: 100%; height: 100%; object-fit: contain; transition: 0.6s; }
.dimmed { opacity: 0.8; filter: grayscale(1) brightness(0.4); }

.overlay-svg-layer { position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; }
.bb-point { fill: var(--accent-gold); filter: drop-shadow(0 0 12px var(--accent-gold)); }
.path-glow {
  fill: none; stroke: var(--accent-gold); stroke-width: 5.5;
  stroke-dasharray: 15, 10; animation: dash 1s linear infinite;
  filter: url(#goldGlow);
}
@keyframes dash { to { stroke-dashoffset: -25; } }
.svg-tag-text { fill: #000; font-size: 18px; font-weight: 900; text-anchor: middle; font-family: 'Lato', sans-serif; }

/* 6. 右侧：参数滚动列表 */
.data-scroll-box { overflow-y: auto; padding-right: 20px; max-height: 70vh; }
.data-scroll-box::-webkit-scrollbar { width: 3px; }
.data-scroll-box::-webkit-scrollbar-thumb { background: var(--accent-gold); }

.data-group { margin-bottom: 35px; }
.group-title { font-size: 0.8rem; color: var(--accent-gold); margin-bottom: 15px; letter-spacing: 2px; text-transform: uppercase; border-bottom: 1px solid rgba(212,175,55,0.2); padding-bottom: 8px; }

.metric-row { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid rgba(255,255,255,0.08); transition: 0.3s; cursor: pointer; }
.metric-row:hover { background: rgba(255,255,255,0.03); padding-left: 10px; }
.metric-row .l { font-size: 1rem; color: rgba(255,255,255,0.5); }
.metric-row .v { font-weight: 700; color: #fff; font-size: 1.2rem; }
.metric-row .v.active { color: var(--accent-gold); transform: scale(1.1); }

.btn-gold-action { background: var(--accent-gold); color: #000; border: none; padding: 18px; border-radius: 12px; font-weight: 800; cursor: pointer; width: 100%; transition: 0.3s; }
.btn-gold-action:hover { background: #fff; transform: translateY(-3px); }

.global-grid-bg { position: absolute; inset: 0; z-index: -1; opacity: 0.05; background-image: linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px); background-size: 60px 60px; }
</style>