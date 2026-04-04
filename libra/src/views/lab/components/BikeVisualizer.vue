<template>
  <!-- 单车添加或修改时右侧指示图 -->
  <div class="bike-visualizer-container">
    <svg viewBox="0 0 850 450" class="bike-svg" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <!-- 强力发光滤镜 -->
        <filter id="neonGlow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="2.5" result="blur"/>
          <feFlood flood-color="#D4AF37" flood-opacity="1" result="color"/>
          <feComposite in="color" in2="blur" operator="in" result="glow"/>
          <feMerge>
            <feMergeNode in="glow"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>

        <!-- 箭头 -->
        <marker id="arrow" markerWidth="10" markerHeight="10" refX="5" refY="3" orient="auto" markerUnits="strokeWidth">
          <path d="M0,0 L0,6 L9,3 z" fill="var(--accent-gold)"/>
        </marker>
      </defs>

      <!-- 底图 -->
      <image
          :href="currentBgImage"
          x="0" y="0" width="850" height="450"
          class="base-image"
      />

      <!-- 动态指引层 (全部改用 path 绘制) -->
      <g fill="none" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">

        <!-- REACH (L) -->
        <template v-if="activePart === 'frameReach' || activePart === 'reach'">
          <path :d="`M${c.bb.x},${c.groundY} V${c.stackY}`" class="helper-dashed"/>
          <!-- 核心修复：y 坐标增加 0.1 偏移，确保渲染 -->
          <path :d="`M${c.bb.x},${c.stackY} L${c.htTop.x},${c.stackY + 0.1}`" class="indicator-glow"
                marker-end="url(#arrow)" stroke-width="3" />
          <path :d="`M${c.htTop.x},${c.stackY} V${c.htTop.y}`" class="helper-solid"/>
          <text :x="c.bb.x + (c.htTop.x - c.bb.x)/2" :y="c.stackY - 15" class="metric-label">L (REACH)</text>
        </template>

        <!-- STACK (M) -->
        <template v-if="activePart === 'frameStack' || activePart === 'stack'">
          <path :d="`M${c.htTop.x},${c.stackY} H${c.bb.x}`" class="helper-dashed"/>
          <path :d="`M${c.bb.x},${c.bb.y} L${c.bb.x + 0.1},${c.stackY}`" class="indicator-glow" stroke-width="3" />
          <text :x="c.bb.x - 60" :y="c.stackY + (c.bb.y - c.stackY)/2" class="metric-label">M (STACK)</text>
        </template>

        <!-- STA 立管角度 (B) -->
        <template v-if="activePart === 'seatTubeAngle' || activePart === 'seat_angle'">
          <path :d="`M${c.rearAxle.x},${c.bb.y} H${c.bb.x + 50}`" class="helper-dashed"/>
          <path :d="`M${c.bb.x},${c.bb.y} L${c.stTop.x},${c.stTop.y}`" class="indicator-glow"/>
          <path :d="`M${c.bb.x - 40},${c.bb.y} A40,40 0 0 1 ${c.bb.x - 15},${c.bb.y - 35}`" class="indicator-glow"
                stroke-width="3"/>
          <text :x="c.bb.x - 110" :y="c.bb.y - 15" class="metric-label">B (STA)</text>
        </template>

        <!-- HTA 头管角度 (E) -->
        <template v-if="activePart === 'headTubeAngle'">
          <path :d="`M${c.frontAxle.x - 260},${c.frontAxle.y} H${c.htTop.x + 80}`" class="helper-dashed"/>
          <path :d="`M${c.frontAxle.x},${c.frontAxle.y} L${c.htTop.x},${c.htTop.y}`" class="indicator-glow"/>
          <path :d="`M${c.frontAxle.x - 50},${c.frontAxle.y} A50,50 0 0 1 ${c.frontAxle.x - 20},${c.frontAxle.y - 45}`"
                class="indicator-glow" stroke-width="3"/>
          <text :x="c.frontAxle.x - 120" :y="c.frontAxle.y - 15" class="metric-label">E (HTA)</text>
        </template>

        <!-- SADDLE HEIGHT (A) -->
        <template v-if="activePart === 'saddleHeight' || activePart === 'saddle_height'">
          <path :d="`M${c.bb.x},${c.bb.y} L${c.saddle.x + 40},${c.saddle.y + 20}`" class="indicator-glow"
                marker-end="url(#arrow)"/>
          <text :x="c.saddle.x - 40" :y="c.saddle.y + 60" class="metric-label">A (HEIGHT)</text>
        </template>

        <!-- SETBACK (C) -->
        <template v-if="activePart === 'saddleSetback' || activePart === 'setback'">
          <path :d="`M${c.bb.x},${c.bb.y} V${c.saddle.y}`" class="helper-dashed"/>
          <path :d="`M${c.bb.x},${c.saddle.y+1} L${c.saddle.x - 20},${c.saddle.y}`" class="indicator-glow"
                marker-end="url(#arrow)"/>
          <circle :cx="c.saddle.x - 20" :cy="c.saddle.y" r="5" fill="var(--accent-gold)"/>
          <text :x="c.bb.x - 50" :y="c.saddle.y - 10" class="metric-label">C (SETBACK)</text>
        </template>

        <!-- BB DROP (J) -->
        <template v-if="activePart === 'bbDrop'">
          <path :d="`M${c.rearAxle.x},${c.bb.y - 40} L${c.frontAxle.x},${c.bb.y - 40}`" class="helper-dashed" />
          <path :d="`M${c.bb.x},${c.bb.y - 40} L${c.bb.x + 0.5},${c.bb.y}`"
              class="indicator-glow" stroke-width="3" />
          <path :d="`M${c.bb.x - 160},${c.bb.y} L${c.bb.x + 220},${c.bb.y}`" class="helper-dashed" />
          <text :x="c.bb.x + 10" :y="c.bb.y - 15" class="metric-label">J (DROP)</text>
        </template>

        <!-- WHEELBASE (H) -->
        <template v-if="activePart === 'wheelBase'">
          <path :d="`M${c.rearAxle.x + 20},${c.groundY - 1} L${c.frontAxle.x - 20},${c.groundY}`" class="indicator-glow"
                marker-start="url(#arrow)" marker-end="url(#arrow)"/>
          <path :d="`M${c.rearAxle.x},${c.rearAxle.y} V${c.groundY}`" class="helper-dashed"/>
          <path :d="`M${c.frontAxle.x},${c.frontAxle.y} V${c.groundY}`" class="helper-dashed"/>
          <text :x="c.bb.x" :y="c.groundY + 25" class="metric-label">H (WHEELBASE)</text>
        </template>

        <!-- standOverHeight (SH/Q) -->
        <template v-if="activePart === 'standOverHeight'">
          <path :d="`M${c.bb.x + 1},${c.standoverHeight} L${c.bb.x},${c.groundY}`"
                class="indicator-glow" stroke-width="3" />
          <path :d="`M${c.rearAxle.x + 20},${c.groundY - 1} L${c.frontAxle.x - 20},${c.groundY}`" class="helper-dashed"/>
          <text :x="c.bb.x + 10" :y="c.bb.y - 15" class="metric-label">SH/Q(跨高)</text>
        </template>

        <!-- HANDLEBAR WIDTH (N) - 右侧子图 -->
        <template v-if="activePart === 'handlebarWidth' || activePart === 'bar_width'">
          <path :d="`M${c.barDetail.x1},${c.barDetail.y+1} L${c.barDetail.x2},${c.barDetail.y}`" class="indicator-glow"
                marker-start="url(#arrow)" marker-end="url(#arrow)"/>
          <text :x="c.barDetail.cx" :y="c.barDetail.y + 35" class="metric-label">N (WIDTH)</text>
        </template>

        <!-- CRANK LENGTH (P) - 右侧子图 -->
        <template v-if="activePart === 'crankLength' || activePart === 'crank'">
          <path :d="`M${c.crankDetail.cx},${c.crankDetail.cy} L${c.crankDetail.endX},${c.crankDetail.endY}`"
                class="indicator-glow"/>
          <text :x="c.crankDetail.endX + 15" :y="c.crankDetail.endY + 15" class="metric-label">P</text>
        </template>

        <!-- HANDLEBAR DROP (车把落差) -->
        <template v-if="activePart === 'handlebarDrop'">
          <path :d="`M${c.saddle.x},${c.saddle.y} H${c.htTop.x + 30}`" class="helper-dashed"/>
          <path :d="`M${c.htTop.x + 20},${c.saddle.y} V${c.htTop.y}`" class="indicator-glow"
                marker-end="url(#arrow)"/>
          <text :x="c.htTop.x + 35" :y="c.saddle.y + (c.htTop.y - c.saddle.y)/2" class="metric-label">DROP</text>
        </template>

        <!-- 五通中心原点 -->
        <circle :cx="c.bb.x" :cy="c.bb.y" r="8" fill="var(--accent-gold)" stroke="white" stroke-width="2"/>
      </g>
    </svg>

    <div class="visual-hint" v-if="!activePart">请聚焦左侧输入框查看测量图纸</div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import bgRoad from '@/assets/image/road-bike.png';
import bgMtb from '@/assets/image/mtb-bike.png';

const props = defineProps({
  activePart: String,
  bikeType: Number
});

const isRoadType = computed(() => [1, 3, 4].includes(props.bikeType));
const currentBgImage = computed(() => isRoadType.value ? bgRoad : bgMtb);

/**
 * 最终校准坐标系 (Based on BB Point)
 */
const coords = {
  // 公路车坐标系
  road: {
    bb: {x: 310, y: 310},         // 原点 (已确认)

    // 关键点位
    rearAxle: {x: 145, y: 310},   // 后轴 (BB Drop=0视感)
    frontAxle: {x: 540, y: 310},  // 前轴

    htTop: {x: 470, y: 140},      // 头管顶 (Stack/Reach 终点)
    stackY: 140,                    // Stack 高度水平线

    stTop: {x: 270, y: 155},      // 立管顶 (上管交汇处)
    saddle: {x: 200, y: 40},      // 座垫中心

    stemEnd: {x: 550, y: 120},    // 把立末端
    groundY: 400,                   // 地面线
    standOverHeight: 150,

    // 右侧子图
    barDetail: {x1: 710, x2: 815, y: 115, cx: 730},
    crankDetail: {cx: 740, cy: 300, endX: 800, endY: 350}
  },

  // 山地车坐标系
  mtb: {
    bb: {x: 310, y: 300},         // 原点 (已确认)

    rearAxle: {x: 160, y: 280},
    frontAxle: {x: 520, y: 280},

    htTop: {x: 440, y: 100},      // MTB头管较高
    stackY: 120,

    stTop: {x: 270, y: 160},
    saddle: {x: 240, y: 90},

    stemEnd: {x: 580, y: 100},
    groundY: 410,
    standOverHeight: 175,

    // 右侧子图
    barDetail: {x1: 650, x2: 780, y: 120, cx: 735},
    crankDetail: {cx: 680, cy: 280, endX: 730, endY: 330}
  }
};

const c = computed(() => isRoadType.value ? coords.road : coords.mtb);
</script>

<style scoped>
.bike-visualizer-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #FFFFFF;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.bike-svg {
  width: 100%;
  height: auto;
  overflow: visible;
}

.base-image {
  opacity: 0.85;
  filter: contrast(1.1);
}

/* 核心：金色流动线条 (纯循环，无卡顿) */
.indicator-glow {
  stroke: var(--accent-gold);
  stroke-dasharray: 12, 6;
  animation: flowLoop 1s linear infinite;
  filter: url(#neonGlow);
}

/* 辅助虚线 */
.helper-dashed {
  stroke: #888;
  stroke-width: 1.5;
  stroke-dasharray: 4, 4;
  opacity: 0.6;
}

.helper-solid {
  stroke: #999;
  stroke-width: 1;
}

/* 标注文字 */
.metric-label {
  fill: var(--accent-gold);
  font-family: 'Lato', sans-serif;
  font-weight: 900;
  font-size: 18px;
  paint-order: stroke;
  stroke: white;
  stroke-width: 5px;
  stroke-linecap: round;
  stroke-linejoin: round;
}

@keyframes flowLoop {
  from {
    stroke-dashoffset: 18;
  }
  to {
    stroke-dashoffset: 0;
  }
}

.visual-hint {
  position: absolute;
  bottom: 15px;
  font-size: 0.75rem;
  color: #AAA;
  letter-spacing: 0.2em;
  font-weight: 700;
  text-transform: uppercase;
}
</style>