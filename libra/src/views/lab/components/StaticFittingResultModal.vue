<template>
  <Transition name="modal-fade">
    <!-- 1. 遮罩层：冲突修复，使用专属 class 并 Teleport 到 body -->
    <div v-if="visible" class="fitting-result-overlay" @click.self="close">
      <div class="result-container glass-card">

        <!-- 2. 头部：分数与画像标签 -->
        <header class="result-header">
          <div class="score-radial">
            <svg viewBox="0 0 36 36" class="circular-chart" :class="scoreLevelClass">
              <path class="circle-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
              <path class="circle" :stroke-dasharray="`${hasThoracicScore ? result.thoracicScore : 0}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
            </svg>
            <div class="score-content">
              <span class="score-val">{{ displayThoracicScore }}</span>
              <span class="score-unit">柔韧分</span>
            </div>
          </div>

          <div class="header-main">
            <h3>静态 fitting 基准 <span class="badge-date">{{ formatDate(result.fittingTime) }}</span></h3>
            <div class="tags-row">
              <span v-for="tag in result.riderTags" :key="tag" class="status-tag">
                <i class="fas fa-check-double"></i> {{ tag }}
              </span>
              <span class="type-tag" v-if="hasThoracicScore && result.thoracicScore < 30">
                <i class="fas fa-shield-alt"></i> 康复防护模式已开启
              </span>
            </div>
            <!-- 痛点标签 -->
            <div class="pain-tags" v-if="result.painLabels && result.painLabels.length > 0">
              <span class="pain-tag" v-for="label in result.painLabels" :key="label">
                [{{ label }}]
              </span>
            </div>
          </div>
          <button class="close-x" @click="close">&times;</button>
        </header>

        <!-- 3. 数据网格 (Bento Grid) -->
        <div class="scroll-body">
          <!-- 建议实车设定 -->
          <div class="setup-module">
            <h4 class="module-title"><i class="fas fa-sliders-h"></i> 建议实车设定</h4>
            <div class="metrics-grid">
            <!-- 组 A: 下肢动力 -->
            <div class="metric-group">
              <h4 class="group-label">POWER BASE / 下肢动力</h4>
              <div class="lower-body-grid">
                <div class="data-card">
                  <span class="l">建议座高</span>
                  <span class="v">{{ result.recSeatHeight }}<small>mm</small></span>
                </div>
                <div class="data-card">
                  <span class="l">座垫后移</span>
                  <span class="v">{{ result.recSetback }}<small>mm</small></span>
                </div>
                <div class="data-card">
                  <span class="l">曲柄长度</span>
                  <span class="v">{{ result.recCrankLength }}<small>mm</small></span>
                </div>
                <div class="data-card">
                  <span class="l">座垫推荐宽</span>
                  <span class="v">{{ result.recSaddleWidth }}<small>mm</small></span>
                </div>
                <div class="data-card wide">
                  <span class="l">
                    推荐车架Stack
                    <span class="info-tooltip-wrapper">
                      <i class="info-icon fas fa-exclamation-circle"></i>
                      <span class="tooltip-text">这是车架几何参考，不是日常调车刻度；实际把高请看车把高度与垫圈高度</span>
                    </span>
                  </span>
                  <span class="v">{{ result.recFrameStack }}<small>mm</small></span>
                </div>
              </div>
            </div>

            <!-- 组 B: 上肢座舱 -->
            <div class="metric-group">
              <h4 class="group-label">COCKPIT / 上肢座舱</h4>
              <div class="lower-body-grid">
                <div class="data-card">
                  <span class="l">车把高度</span>
                  <span class="v">{{ result.targetBarHeight }}<small>mm</small></span>
                </div>
                <div class="data-card">
                  <span class="l">车把落差</span>
                  <span class="v">{{ result.recHandlebarDrop }}<small>mm</small></span>
                </div>
                <div class="data-card">
                  <span class="l">垫圈高度</span>
                  <span class="v">{{ formatRecommendedValue(result.recSpacerHeight) }}<small v-if="result.recSpacerHeight != null">mm</small></span>
                </div>
                <div class="data-card">
                  <span class="l">把立长度</span>
                  <span class="v">{{ formatRecommendedValue(result.recStemLength) }}<small v-if="result.recStemLength != null">mm</small></span>
                </div>
                <div class="data-card wide">
                  <span class="l">
                    推荐前伸Reach
                    <span class="info-tooltip-wrapper">
                      <i class="info-icon fas fa-exclamation-circle"></i>
                      <span class="tooltip-text">该前伸可通过调整把立长度与座垫前后位置实现</span>
                    </span>
                  </span>
                  <span class="v">{{ result.recReachPlus }}<small>mm</small></span>
                </div>
              </div>
            </div>
            </div>
          </div>

          <!-- 4. 深度诊断文案列表 -->
          <div class="diagnosis-section">
            <h4 class="section-title">Fitter 深度诊断报告</h4>
            <div class="diag-list">
              <div v-for="item in diagnosisCards" :key="item.key" class="diag-card">
                <div class="diag-header">
                  <span class="diag-part">{{ item.part }}</span>
                  <span class="diag-feature" v-if="item.bodyFeature">{{ item.bodyFeature }}</span>
                </div>
                <!-- white-space: pre-line 关键：处理文本中的 \n -->
                <p class="diag-logic">{{ item.adjustmentLogic }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 5. 底部操作区 -->
        <footer class="result-footer">
          <div class="footer-info">
            <i class="fas fa-database"></i>
            {{ PROJECT_CONFIG.MASCOT }} Engine Analysis Complete.
          </div>
          <div class="footer-btns">
            <button class="btn-zen-text" @click="handleShare">
              <i class="fas fa-share-alt"></i> 生成分享海报
            </button>
            <button class="btn-zen-text" @click="close">稍后调整</button>
            <button class="btn-zen-primary" @click="confirmToReport">
              获取实车对比诊断 <i class="fas fa-chevron-right"></i>
            </button>
          </div>
        </footer>

      </div>
    </div>
  </Transition>

  <Transition name="modal-fade">
    <!-- 6. Sharing Preview Modal -->
    <div v-if="showSharePreview" class="share-preview-overlay" @click.self="showSharePreview = false">
      <div class="preview-card">
        <div class="preview-header">
          <h4>海报预览</h4>
          <button @click="showSharePreview = false">&times;</button>
        </div>
        <div class="preview-content">
          <img v-if="posterImage" :src="posterImage" alt="Poster Image" class="generated-poster">
          <div v-else class="loading-state">
            <i class="fas fa-spinner fa-spin"></i> 正在生成硬核海报...
          </div>
        </div>
        <div class="preview-footer">
          <button class="btn-download" @click="downloadPoster" :disabled="!posterImage">
            <i class="fas fa-download"></i> 保存到相册
          </button>
          <p class="share-hint">长按图片也可直接发送给好友</p>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Hidden Poster for Capture -->
  <div class="hidden-poster">
    <SharingPoster v-if="result" ref="posterRef" :result="result" />
  </div>
</template>

<script setup>
import {computed, ref, nextTick} from 'vue';
import { PROJECT_CONFIG } from '@/config/constants';
import SharingPoster from './StaticSharingPoster.vue';

const props = defineProps({
  visible: Boolean,
  result: {type: Object, default: () => ({})}
});

const emit = defineEmits(['update:visible', 'to-report']);

const posterRef = ref(null);
const posterImage = ref(null);
const showSharePreview = ref(false);

const close = () => emit('update:visible', false);
const showInfo = (msg) => {
  alert(msg);
};
const confirmToReport = () => {
  close();
  emit('to-report');
};

const handleShare = async () => {
  showSharePreview.value = true;
  posterImage.value = null; // Reset

  // Wait for DOM
  await nextTick();

  if (posterRef.value) {
    // Small delay to ensure ECharts animation or rendering is stable
    setTimeout(async () => {
      const img = await posterRef.value.generateImage();
      posterImage.value = img;
    }, 500);
  }
};

const downloadPoster = () => {
  if (!posterImage.value) return;
  const link = document.createElement('a');
  link.href = posterImage.value;
  link.download = `RideTrace_Fitting_${props.result.fittingResultId || Date.now()}.png`;
  link.click();
};

const hasThoracicScore = computed(() => {
  return props.result.thoracicScore !== null && props.result.thoracicScore !== undefined;
});

const displayThoracicScore = computed(() => {
  return hasThoracicScore.value ? props.result.thoracicScore : '--';
});

const scoreLevelClass = computed(() => {
  if (!hasThoracicScore.value) return 'level-neutral';
  const score = props.result.thoracicScore || 0;
  if (score < 30) return 'level-danger';
  if (score < 70) return 'level-warning';
  return 'level-success';
});

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return dateStr.split(' ')[0]; // 仅显示日期
};

const formatRecommendedValue = (value) => {
  return value === null || value === undefined || value === '' ? '暂无推荐' : value;
};

const asArray = (value) => Array.isArray(value) ? value : [];

const hasTag = (tags, keyword) => {
  return tags.some(tag => String(tag).includes(keyword));
};

const detailText = (details) => {
  return details
      .map(item => `${item.part || ''}${item.bodyFeature || ''}${item.adjustmentLogic || ''}`)
      .join(' ');
};

const formatMm = (value) => {
  return value === null || value === undefined || value === '' ? '暂无推荐' : `${value}mm`;
};

const diagnosisCards = computed(() => {
  const res = props.result || {};
  const tags = asArray(res.riderTags);
  const details = asArray(res.details);
  const rawText = detailText(details);
  const painLabels = asArray(res.painLabels);
  const cards = [];

  if (painLabels.length > 0) {
    const painText = painLabels.slice(0, 3).join('、');
    cards.push({
      key: 'pain',
      part: '疼痛保护',
      bodyFeature: painText,
      adjustmentLogic: `你这次反馈了 ${painText}，推荐值会优先降低相关区域的压力。先按建议座高、前伸和落差小幅调整，骑 2-3 次后观察疼痛是否下降；如果疼痛持续或加重，先暂停强度训练。`
    });
  }

  const dataWarnings = details.filter(item => item.bodyFeature === '数据缺失' || item.part === '数据质量');
  if (dataWarnings.length > 0) {
    const warningText = dataWarnings
        .map(item => item.adjustmentLogic)
        .filter(Boolean)
        .slice(0, 2)
        .join(' ');
    cards.push({
      key: 'data',
      part: '数据确认',
      bodyFeature: '影响精度',
      adjustmentLogic: warningText || '部分测量数据缺失，本次结果可以作为初始参考；补齐后重新生成会更贴近你的身体情况。'
    });
  }

  let cockpitReason = '这次座舱目标会按你的上肢长度、躯干比例和柔韧性来定，不需要先填写当前把立数据。';
  let cockpitFeature = '座舱落点';
  if (hasTag(tags, '腹部空间优化-增强')) {
    cockpitFeature = '腹部空间优先';
    cockpitReason = '你的腹前空间需求较高，本次推荐会进一步缩短前伸并抬高把位，让髋腹夹角和呼吸空间更安全。';
  } else if (hasTag(tags, '腹部空间优化')) {
    cockpitFeature = '降低压迫';
    cockpitReason = '你的腰高比提示需要更多髋腹空间，本次推荐会适度减少前伸和落差，降低大腿顶腹、腹部压迫和深把位呼吸受限。';
  } else if (res.thoracicScore !== null && res.thoracicScore !== undefined && res.thoracicScore < 60) {
    cockpitFeature = '保护腰背肩颈';
    cockpitReason = '你的胸椎和肩部灵活性不适合过激进的低趴姿态，本次车头目标会更保守，减少腰背和肩颈代偿。';
  }
  cards.push({
    key: 'cockpit',
    part: '上肢座舱',
    bodyFeature: cockpitFeature,
    adjustmentLogic: `${cockpitReason} 建议优先按两个最容易落地的车头项调整：把立 ${formatMm(res.recStemLength)}，垫圈 ${formatMm(res.recSpacerHeight)}；车把落差目标 ${formatMm(res.recHandlebarDrop)}。如果实车把立角度与常见安装差异较大，请以车把高度和落差目标为准。`
  });

  const lowerActions = [
    `座高 ${formatMm(res.recSeatHeight)}`,
    `座垫后移 ${formatMm(res.recSetback)}`,
    `曲柄 ${formatMm(res.recCrankLength)}`
  ];
  cards.push({
    key: 'lower-body',
    part: '下肢发力',
    bodyFeature: '先调座垫',
    adjustmentLogic: `你最先应该确认座垫位置：${lowerActions.join('，')}。座高负责膝关节伸展，后移量负责发力重心；先从这些基础值开始，再根据骑行中的膝盖和骨盆稳定反馈微调。`
  });

  if (rawText.includes('核心') || rawText.includes('稳定') || rawText.includes('塌腰')) {
    cards.push({
      key: 'body-core',
      part: '身体短板',
      bodyFeature: '核心稳定',
      adjustmentLogic: '你需要把核心稳定性一起纳入调整：如果长时间骑行容易塌腰、骨盆晃动，先用短组平板支撑、死虫、鸟狗和侧桥建立耐力，每周 2-3 次即可。'
    });
  } else if (res.thoracicScore !== null && res.thoracicScore !== undefined && res.thoracicScore < 70) {
    cards.push({
      key: 'body-flex',
      part: '身体短板',
      bodyFeature: '柔韧性',
      adjustmentLogic: '你的柔韧性还不适合直接追求大落差。建议把胸椎伸展、髋屈肌和腘绳肌拉伸作为日常补弱，每次 8-12 分钟，车上姿态会更容易稳定。'
    });
  }

  if (res.recHandlebarWidth || res.recSaddleWidth) {
    cards.push({
      key: 'equipment',
      part: '器材规格',
      bodyFeature: '按身体宽度选',
      adjustmentLogic: `把宽建议 ${formatMm(res.recHandlebarWidth)}，座垫宽度建议 ${formatMm(res.recSaddleWidth)}。这些不是越激进越好，核心目标是让肩部不过度外展、坐骨有稳定支撑。`
    });
  }

  if (cards.length === 0) {
    cards.push({
      key: 'fallback',
      part: '本次结论',
      bodyFeature: '初始基准',
      adjustmentLogic: '本次已生成静态 fitting 初始基准。建议先按上方推荐值调整，再用 2-3 次短距离骑行观察膝盖、腰背、肩颈和手部压力。'
    });
  }

  return cards.slice(0, 4);
});
</script>

<style scoped>
.fitting-result-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(15px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.result-container {
  width: 100%;
  max-width: 800px;
  background: #fff;
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 40px 100px rgba(0, 0, 0, 0.3);
}

/* Header */
.result-header {
  padding: 30px 40px;
  display: flex;
  align-items: center;
  gap: 30px;
  background: linear-gradient(to right, #F9F9F7, #FFF);
  border-bottom: 1px solid #EEE;
  position: relative;
}

.score-radial {
  position: relative;
  width: 70px;
  height: 70px;
}

.circular-chart {
  width: 100%;
  height: 100%;
}

.circle-bg {
  fill: none;
  stroke: #eee;
  stroke-width: 3;
}

.circle {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 1s ease;
}

/* 分数颜色定义 */
.level-neutral .circle {
  stroke: #B8B8B0;
}

.level-danger .circle {
  stroke: #C18C7E;
}

.level-warning .circle {
  stroke: #D4AF37;
}

.level-success .circle {
  stroke: #5C6E58;
}

.score-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-val {
  font-size: 1.5rem;
  font-weight: bold;
  line-height: 1;
}

.score-unit {
  font-size: 0.5rem;
  color: #999;
  margin-top: 2px;
}

.header-main h3 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 1.8rem;
}

.badge-date {
  font-size: 0.8rem;
  font-weight: normal;
  color: #AAA;
  margin-left: 10px;
}

.tags-row {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.status-tag {
  font-size: 0.7rem;
  background: #F4F6F3;
  color: #5C6E58;
  padding: 3px 10px;
  border-radius: 4px;
  font-weight: 600;
}

.type-tag {
  font-size: 0.7rem;
  background: #FFF1F0;
  color: #CF1322;
  padding: 3px 10px;
  border-radius: 4px;
  font-weight: 600;
}

.pain-tags {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.pain-tag {
  color: #CF1322;
  font-size: 0.85rem;
  font-weight: bold;
}

.close-x {
  position: absolute;
  right: 25px;
  top: 25px;
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #CCC;
}

/* Scrollable Body */
.scroll-body {
  padding: 40px;
  max-height: 65vh;
  overflow-y: auto;
}

.setup-module {
  margin-bottom: 40px;
}

.module-title {
  font-size: 0.85rem;
  color: #555;
  letter-spacing: 1px;
  margin: 0 0 16px 4px;
  font-weight: 600;
}

.module-title i {
  margin-right: 6px;
  color: #8B7355;
}

.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 25px;
}

.metric-group {
  background: #F9F9F7;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.lower-body-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.data-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 10px;
  min-height: 68px;
  border: 1px solid #E8E8E4;
  border-radius: 10px;
  background: #FFFFFF;
  transition: all 0.2s ease;
  cursor: default;
  box-sizing: border-box;
}

.data-card:hover {
  border-color: #C8C8C0;
  background: #FAFAF5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.data-card:hover .v {
  color: #5C6E58;
}

.data-card.wide {
  grid-column: 1 / -1;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: #5C6E58;
  border-color: #5C6E58;
  border-radius: 12px;
}

.data-card.wide .l {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 1px;
}

.data-card.wide .info-icon {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.5);
  cursor: help;
  transition: color 0.2s ease;
}

.data-card.wide .info-icon:hover {
  color: #FFFFFF;
}

.info-tooltip-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.tooltip-text {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%);
  width: max-content;
  max-width: 220px;
  padding: 10px 14px;
  background: #FFFFFF;
  color: #333;
  font-size: 0.7rem;
  line-height: 1.5;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  opacity: 0;
  visibility: hidden;
  transition: all 0.25s ease;
  z-index: 100;
  letter-spacing: 0;
}

.tooltip-text::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: #FFFFFF;
}

.info-tooltip-wrapper:hover .tooltip-text {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(-4px);
}

.data-card.wide .v {
  font-size: 1.6rem;
  color: #FFFFFF;
}

.data-card.wide .v small {
  color: rgba(255, 255, 255, 0.7);
}

.data-card.wide:hover {
  background: #4D5F49;
  border-color: #4D5F49;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(92, 110, 88, 0.3);
}

.data-card.wide:hover .v {
  color: #FFFFFF;
}

.data-card .l {
  font-size: 0.65rem;
  color: #999;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.data-card .v {
  font-size: 1.3rem;
  font-weight: bold;
  color: #333;
  font-family: var(--font-serif);
}

.data-card .v small {
  font-size: 0.6rem;
  color: #999;
  margin-left: 2px;
  font-weight: normal;
}

.group-label {
  font-size: 0.7rem;
  color: #AAA;
  letter-spacing: 2px;
  margin: 0 0 15px 0;
}

.data-row.highlight {
  background: #5C6E58;
  color: white;
  padding: 15px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.data-row.highlight.gold {
  background: #D4AF37;
}

.data-row .l {
  font-size: 0.8rem;
  opacity: 0.9;
}

.data-row .v {
  font-size: 2rem;
  font-family: var(--font-serif);
  font-weight: bold;
}

.data-sub-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.data-item {
  display: flex;
  flex-direction: column;
}

.data-item .l {
  font-size: 0.65rem;
  color: #999;
  margin-bottom: 4px;
}

.data-item .v {
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
}

.data-card .v.warning {
  color: #CF1322;
  font-size: 0.8rem;
}

.data-item .v.warning {
  color: #CF1322;
  font-size: 0.8rem;
}

small {
  font-size: 0.6rem;
  margin-left: 2px;
  font-weight: normal;
  color: #999;
}

/* Diagnosis Section */
.diagnosis-section {
  border-top: 1px solid #EEE;
  padding-top: 30px;
}

.section-title {
  font-size: 1rem;
  margin-bottom: 20px;
  color: #555;
  font-weight: 700;
}

.diag-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.diag-card {
  background: #FFF;
  border: 1px solid #F0F0F0;
  padding: 20px;
  border-radius: 12px;
  border-left: 4px solid #5C6E58;
  transition: 0.3s;
}

.diag-card:hover {
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
}

.diag-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.diag-part {
  font-size: 0.7rem;
  background: #5C6E58;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
}

.diag-feature {
  font-size: 0.75rem;
  font-weight: bold;
  color: #333;
}

.diag-logic {
  font-size: 0.85rem;
  color: #666;
  line-height: 1.6;
  margin: 0;
  white-space: pre-line; /* 识别文本中的换行符 */
}

/* Footer */
.result-footer {
  padding: 25px 40px;
  background: #F9F9F7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-info {
  font-size: 0.7rem;
  color: #BBB;
  font-family: monospace;
}

.footer-btns {
  display: flex;
  gap: 20px;
}

.btn-zen-primary {
  background: #D4AF37;
  color: white;
  border: none;
  padding: 12px 25px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.3s;
}

.btn-zen-primary:hover {
  background: #b8972f;
  transform: translateY(-2px);
}

.btn-zen-text {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
}

/* Sharing Preview Styles */
.share-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(10px);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.preview-card {
  background: #FFF;
  border-radius: 20px;
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 40px 100px rgba(0,0,0,0.5);
}

.preview-header {
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #EEE;
}

.preview-header h4 { margin: 0; font-weight: 700; font-family: var(--font-serif); }
.preview-header button { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #999; }

.preview-content {
  padding: 20px;
  background: #F5F5F3;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.generated-poster {
  max-width: 100%;
  max-height: 60vh;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
  border-radius: 8px;
}

.loading-state {
  color: #999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  font-size: 1rem;
}

.preview-footer {
  padding: 20px;
  text-align: center;
}

.btn-download {
  background: #5C6E58;
  color: white;
  border: none;
  padding: 14px 40px;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
  margin-bottom: 12px;
  transition: 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.btn-download:hover:not(:disabled) {
  background: #4a5947;
  transform: translateY(-2px);
}

.btn-download:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.share-hint {
  font-size: 0.75rem;
  color: #999;
  margin: 0;
}

.hidden-poster {
  position: absolute;
  left: -9999px;
  top: 0;
  pointer-events: none;
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .fitting-result-overlay {
    padding: 10px;
  }
  .result-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
    gap: 15px;
  }
  .header-main h3 {
    font-size: 1.4rem;
  }
  .tags-row, .pain-tags {
    flex-wrap: wrap;
  }
  .scroll-body {
    padding: 20px;
    max-height: 70vh;
  }
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .result-footer {
    flex-direction: column;
    padding: 20px;
    gap: 15px;
    text-align: center;
  }
  .footer-btns {
    flex-direction: column;
    width: 100%;
    gap: 10px;
  }
  .footer-btns button {
    width: 100%;
    justify-content: center;
    padding: 12px;
  }
}

/* Modal Transition */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
