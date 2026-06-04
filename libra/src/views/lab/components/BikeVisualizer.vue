<template>
  <!-- 单车添加或修改时右侧指示�?-->
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
        
        <!-- 小箭头（用于把立长度等） -->
        <marker id="arrow-small" markerWidth="7" markerHeight="7" refX="4" refY="2" orient="auto" markerUnits="strokeWidth">
          <path d="M0,0 L0,4 L6,2 z" fill="var(--accent-gold)"/>
        </marker>
        
        <!-- 垫圈柔和发光滤镜 -->
        <filter id="spacerGlow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="3" result="blur"/>
          <feFlood flood-color="#D4AF37" flood-opacity="0.6" result="color"/>
          <feComposite in="color" in2="blur" operator="in" result="glow"/>
          <feMerge>
            <feMergeNode in="glow"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
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
          <!-- 核心修复：y 坐标增加 0.1 偏移，确保渲�?-->
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
                marker-end="url(#arrow)"
                :transform="!isRoadType ? 'rotate(-5, ' + (c.saddle.x + 40) + ', ' + (c.saddle.y + 20) + ')' : ''"/>
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

        <!-- SADDLE TILT (座垫角度) -->
        <template v-if="activePart === 'saddleTilt'">
          <!-- 水平参考虚线 -->
          <path :d="`M${c.saddle.x - 20},${c.saddle.y} L${c.saddle.x + 220},${c.saddle.y}`" class="helper-dashed"/>
          <!-- 座垫实际角度线-->
          <path :d="`M${c.saddle.x - 10},${c.saddle.y} L${c.saddle.x + 90},${c.saddle.y - 15}`" class="indicator-glow"
                marker-end="url(#arrow)"/>
          <!-- 角度弧线 -->
          <path :d="`M${c.saddle.x + 70},${c.saddle.y} A35,35 0 0 0 ${c.saddle.x + 68},${c.saddle.y - 11}`" class="indicator-glow"
                stroke-width="3"/>
          <text :x="c.saddle.x + 105" :y="c.saddle.y - 20" class="metric-label">TILT (角度)</text>
        </template>

        <!-- SADDLE WIDTH (座垫宽度) - 替换背景图，无需额外标注 -->

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
          <!-- 座垫高度水平参考虚线 -->
          <path :d="`M${c.saddle.x},${c.saddle.y} H${c.htTop.x + 30}`" class="helper-dashed"/>
          <!-- 车把中心水平参考虚线 -->
          <path :d="`M${c.htTop.x - 20},${c.htTop.y - 85} H${c.stemEnd.x + 20}`" class="helper-dashed"/>
          <!-- 落差金色垂直指示线 -->
          <line :x1="c.htTop.x + 25" :y1="c.saddle.y" :x2="c.htTop.x + 25.5" :y2="c.htTop.y - 85" 
                stroke="var(--accent-gold)" stroke-width="3" stroke-dasharray="6,4"/>
          <text :x="c.htTop.x + 40" :y="c.saddle.y + 20" class="metric-label">DROP</text>
          <text :x="c.htTop.x + 40" :y="c.saddle.y + 40" class="metric-label" font-size="14">把立落差</text>
        </template>

        <!-- STEM LENGTH (把立长度) -->
        <template v-if="activePart === 'stemLength'">
          <!-- 把立长度指示线 -->
          <path :d="isRoadType 
            ? `M${c.htTop.x - 30},${c.htTop.y - 60} L${c.stemEnd.x - 45},${c.stemEnd.y - 70}` 
            : `M${c.htTop.x - 15},${c.htTop.y - 15} L${c.htTop.x + (c.stemEnd.x - c.htTop.x)/4},${c.stemEnd.y - 17}`" 
                class="indicator-glow" marker-end="url(#arrow-small)"
                :transform="!isRoadType ? 'rotate(-15, ' + (c.htTop.x - 15) + ', ' + (c.htTop.y - 15) + ')' : ''"/>
          <text :x="c.htTop.x + (c.stemEnd.x - c.htTop.x)/2 - 20" :y="c.htTop.y - 55" class="metric-label">STEM (把立长度)</text>
        </template>

        <!-- STEM ANGLE (把立角度) -->
        <template v-if="activePart === 'stemAngle'">
          <!-- 水平参考虚线 -->
          <path :d="isRoadType 
            ? `M${c.htTop.x - 40},${c.htTop.y - 65} L${c.stemEnd.x - 20},${c.htTop.y - 65}` 
            : `M${c.htTop.x - 20},${c.htTop.y + -10} L${c.stemEnd.x},${c.htTop.y + 5}`" 
                class="helper-dashed"/>
          <!-- 把立角度指示线 -->
          <path :d="isRoadType 
            ? `M${c.htTop.x - 30},${c.htTop.y - 60} L${c.stemEnd.x - 45},${c.stemEnd.y - 85}` 
            : `M${c.htTop.x - 30},${c.htTop.y - 10} L${c.stemEnd.x - 45},${c.stemEnd.y - 35}`" 
                class="indicator-glow" marker-end="url(#arrow-small)"/>
          <!-- 角度弧线 -->
          <path :d="isRoadType 
            ? `M${c.htTop.x + 20},${c.htTop.y - 65} A20,20 0 0 0 ${c.htTop.x + 18},${c.htTop.y - 72}` 
            : `M${c.htTop.x + 20},${c.htTop.y - 15} A20,20 0 0 0 ${c.htTop.x + 18},${c.htTop.y - 22}`" 
                class="indicator-glow" stroke-width="3"/>
          <text :x="c.htTop.x + (c.stemEnd.x - c.htTop.x)/2 - 25" :y="c.htTop.y - 75" class="metric-label">STEM ANGLE (角度)</text>
        </template>

        <!-- SPACERS HEIGHT (垫圈高度) -->
        <template v-if="activePart === 'spacersHeight'">
          <!-- 垫圈高度指示线 -->
          <path :d="`M${c.htTop.x - 20},${c.htTop.y - 20} L${c.htTop.x - 20},${c.htTop.y - 50}`" class="indicator-glow"
                marker-start="url(#arrow-small)" marker-end="url(#arrow-small)"/>
          <!-- 垫圈区域高亮矩形（带动画效果） -->
          <rect :x="c.htTop.x - 28" :y="c.htTop.y - 50" :width="28" :height="12"
                fill="rgba(212, 175, 55, 0.4)" stroke="var(--accent-gold)" stroke-width="2.5" rx="2"
                :transform="'rotate(-15, ' + (c.htTop.x - 14) + ', ' + (c.htTop.y - 44) + ')'"
                filter="url(#spacerGlow)">
            <animate attributeName="opacity" values="0.4;1;0.4" dur="2s" repeatCount="indefinite"/>
          </rect>
          <text :x="c.htTop.x + 5" :y="c.htTop.y - 30" class="metric-label">SPACERS (垫圈)</text>
        </template>

        <!-- 五通中心原点 -->
        <circle :cx="c.bb.x" :cy="c.bb.y" r="8" fill="var(--accent-gold)" stroke="white" stroke-width="2"/>
      </g>
    </svg>

    <div class="visual-hint" v-if="!activePart">请聚焦左侧输入框查看测量图纸</div>
  </div>
</template>

<script setup>
import {computed, ref} from 'vue';
import bgRoad from '@/assets/image/road-bike.png';
import bgMtb from '@/assets/image/mtb-bike.png';
import saddleWidthImg from '@/assets/image/saddleWidth.png';
import flatPedalImg from '@/assets/image/flatPedal.png';
import roadLockImg from '@/assets/image/roadLock.png';
import mountainLockImg from '@/assets/image/mountain.png';

const showSaddleWidthTooltip = ref(false);

const props = defineProps({
  activePart: String,
  bikeType: Number,
  pedalType: Number
});

const isRoadType = computed(() => [1, 3, 4].includes(props.bikeType));
const currentBgImage = computed(() => {
  // 座垫宽度
  if (props.activePart === 'saddleWidth') {
    return saddleWidthImg;
  }
  // 踏板类型
  if (props.activePart === 'pedalType') {
    if (props.pedalType === 1) return flatPedalImg;
    if (props.pedalType === 2) return roadLockImg;
    if (props.pedalType === 3) return mountainLockImg;
  }
  // 默认自行车背景
  return isRoadType.value ? bgRoad : bgMtb;
});

/**
 * 最终校准坐标系 (Based on BB Point)
 */
const coords = {
  // 公路车坐标系
  road: {
    bb: {x: 310, y: 310},         // 原点 

    // 关键点位
    rearAxle: {x: 145, y: 310},   // 后轴 (BB Drop=0视感)
    frontAxle: {x: 540, y: 310},  // 前轴

    htTop: {x: 470, y: 140},      // 头管(Stack/Reach 终点)
    stackY: 140,                    // Stack 高度水平

    stTop: {x: 270, y: 155},      // 立管(上管交汇)
    saddle: {x: 200, y: 40},      // 座垫中心

    stemEnd: {x: 550, y: 120},    // 把立末端
    groundY: 400,                   // 地面
    standOverHeight: 150,

    // 右侧子图
    barDetail: {x1: 710, x2: 815, y: 115, cx: 730},
    crankDetail: {cx: 740, cy: 300, endX: 800, endY: 350},
    saddleWidthDetail: {x1: 690, x2: 790, y: 200, cx: 740}
  },

  // 山地车坐标系
  mtb: {
    bb: {x: 310, y: 300},         // 原点 

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
    crankDetail: {cx: 680, cy: 280, endX: 730, endY: 330},
    saddleWidthDetail: {x1: 690, x2: 790, y: 200, cx: 740}
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

/* 核心：金色流动线*/
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

.spacer-highlight {
  filter: url(#spacerGlow);
  animation: glowPulse 2s infinite alternate;
}

@keyframes glowPulse {
  from {
    opacity: 0.3;
  }
  to {
    opacity: 0.6;
  }
}

</style>
