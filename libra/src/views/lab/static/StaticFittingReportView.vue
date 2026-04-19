<template>
  <div class="report-page">
    <div class="container">

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="radar-spinner"></div>
        <p>Aries 正在生成生物力学诊断报告...</p>
      </div>

      <!-- 报告主体 -->
      <div v-else-if="report" class="report-content fade-in">

        <!-- 1. 顶部：匹配度总览 -->
        <header class="report-hero glass-card">
          <div class="hero-text">
            <span class="tag">BIOMECHANICAL DIAGNOSIS</span>
            <h1 class="title">调教诊断报告</h1>
            <div class="summary-box">
              <i class="fas fa-quote-left quote-icon"></i>
              <p class="summary-text">{{ report.summary }}</p>
            </div>
          </div>

          <div class="score-dashboard">
            <div class="score-ring">
              <svg viewBox="0 0 100 100">
                <circle class="ring-bg" cx="50" cy="50" r="45"></circle>
                <!-- 动态计算的环形进度条 -->
                <circle class="ring-fill" cx="50" cy="50" r="45" :style="scoreStyle"></circle>
              </svg>
              <div class="score-center">
                <span class="score-num" :class="statusClass(report.matchScore)">{{ report.matchScore }}</span>
                <span class="score-label">MATCH SCORE</span>
              </div>
            </div>
            <div class="match-level" :class="statusClass(report.matchScore)">
              {{ report.matchLevel }}
            </div>
          </div>
        </header>

        <div class="report-layout">
          <!-- 左侧：主要诊断数据 -->
          <div class="main-column">

            <!-- 2. 车架几何适用性 (不可变基础) -->
            <section class="section-block">
              <h3 class="section-title"><i class="fas fa-vector-square"></i> 车架骨架匹配 (Frame Compatibility)</h3>
              <p class="section-desc">验证当前车架尺码是否符合您的身体比例。若此处存在严重偏差，后期调教效果将受限。</p>

              <div class="compatibility-grid two-col">
                <!-- Stack 匹配 -->
                <div class="compat-card" v-if="report.frameStackCheck">
                  <h4 class="compat-title">Stack 匹配</h4>
                  <div class="compat-body">
                    <div class="compat-status">
                      <span v-if="report.frameStackCheck.status === 'PERFECT'" class="status-tag tag-yellow">完美契合</span>
                      <span v-else-if="report.frameStackCheck.status === 'ADJUST'" class="status-tag tag-green">建议微调</span>
                      <span v-else-if="report.frameStackCheck.status === 'WARNING'" class="status-tag tag-red">严重偏差</span>
                    </div>
                    <div class="compat-values">
                      <div class="value-item">
                        <span class="current">{{ report.frameStackCheck.currentValue || '--' }}</span>
                        <span class="value-label">当前</span>
                      </div>
                      <span class="sep">/</span>
                      <div class="value-item">
                        <span class="ideal">{{ report.frameStackCheck.idealValue }}{{ report.frameStackCheck.unit }}</span>
                        <span class="value-label">推荐</span>
                      </div>
                    </div>
                    <p class="compat-reason">{{ report.frameStackCheck.reason }}</p>
                  </div>
                </div>

                <!-- Reach 匹配 -->
                <div class="compat-card" v-if="report.frameReachCheck">
                  <h4 class="compat-title">Reach 匹配</h4>
                  <div class="compat-body">
                    <div class="compat-status">
                      <span v-if="report.frameReachCheck.status === 'PERFECT'" class="status-tag tag-yellow">完美契合</span>
                      <span v-else-if="report.frameReachCheck.status === 'ADJUST'" class="status-tag tag-green">建议微调</span>
                      <span v-else-if="report.frameReachCheck.status === 'WARNING'" class="status-tag tag-red">严重偏差</span>
                    </div>
                    <div class="compat-values">
                      <div class="value-item">
                        <span class="current">{{ report.frameReachCheck.currentValue || '--' }}</span>
                        <span class="value-label">当前</span>
                      </div>
                      <span class="sep">/</span>
                      <div class="value-item">
                        <span class="ideal">{{ report.frameReachCheck.idealValue }}{{ report.frameReachCheck.unit }}</span>
                        <span class="value-label">推荐</span>
                      </div>
                    </div>
                    <p class="compat-reason">{{ report.frameReachCheck.reason }}</p>
                  </div>
                </div>
              </div>
            </section>

            <!-- 3. 调教设定明细 (可调项目) -->
            <section class="section-block">
              <h3 class="section-title"><i class="fas fa-sliders-h"></i> 设定偏差分析 (Setup Deviations)</h3>
              <p class="section-desc">对比您当前的单车设定与 Aries 计算出的黄金生物力学坐标。</p>

              <!-- 7个字段方框网格 -->
              <div class="deviation-grid">
                <div v-for="(field, index) in deviationFields" :key="index" class="deviation-card">
                  <h4 class="field-name">{{ field.name }}</h4>
                  <div class="field-values">
                    <div class="v-item">
                      <span class="v-label">当前</span>
                      <span class="v-current">{{ field.current }}<template v-if="field.current !== '--'">{{ field.unit }}</template></span>
                    </div>
                    <div class="v-item">
                      <span class="v-label">推荐</span>
                      <span class="v-ideal">{{ field.ideal }}<template v-if="field.ideal !== '--'">{{ field.unit }}</template></span>
                    </div>
                    <div class="v-item" :class="field.deltaClass">
                      <span class="v-label">偏差</span>
                      <span class="v-delta">{{ field.delta === '--' ? '--' : (field.delta > 0 ? '+' : '') + field.delta + field.unit }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

          </div>

          <!-- 右侧：器材与操作区 -->
          <aside class="side-column">

            <!-- 4. 器材建议 (购物车) -->
            <div class="equip-card glass-card sticky-card">
              <h3 class="side-title"><i class="fas fa-shopping-bag"></i> 器材更换建议</h3>

              <ul v-if="report.equipmentSuggestions && report.equipmentSuggestions.length > 0" class="equip-list">
                <li v-for="(suggest, idx) in report.equipmentSuggestions" :key="idx">
                  <i class="fas fa-wrench icon-gold"></i>
                  <span>{{ suggest }}</span>
                </li>
              </ul>
              <div v-else class="equip-empty">
                <i class="fas fa-check-circle"></i>
                <p>当前硬件配置合理，无需更换核心零件，请专注于细微刻度调整。</p>
              </div>

              <div class="action-group">
                <!-- 引导用户去车库修改数据 -->
                <button class="btn btn-primary full-width" @click="goToGarage">
                  去车库调整数据 <i class="fas fa-tools"></i>
                </button>
                <button class="btn btn-outline full-width" @click="router.push('/lab')">
                  返回实验室
                </button>
              </div>
            </div>

          </aside>
        </div>

      </div>

      <!-- 异常状态 -->
      <div v-else class="error-state">
        <i class="fas fa-exclamation-triangle"></i>
        <p>无法获取诊断报告，请确保您已生成过基准方案。</p>
        <button class="btn btn-primary" @click="router.push('/lab')">返回实验室</button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getFittingReport, getFittingReportById } from '@/api/staticFitting.js';
import { showToast } from '@/utils/toast.js';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const report = ref(null);
const bikeId = ref(route.query.bikeId);
const resultId = ref(route.query.resultId || route.query.fittingResultId); // 支持 resultId 或 fittingResultId

// 动画属性：环形进度条的填充计算
const scoreStyle = computed(() => {
  if (!report.value) return {};
  const score = report.value.matchScore || 0;
  const circumference = 2 * Math.PI * 45; // r=45
  const offset = circumference - (score / 100) * circumference;
  return {
    strokeDasharray: circumference,
    strokeDashoffset: offset
  };
});

// 根据状态返回颜色类名
const statusClass = (scoreOrStatus) => {
  if (typeof scoreOrStatus === 'number') {
    if (scoreOrStatus >= 90) return 'text-success';
    if (scoreOrStatus >= 70) return 'text-warning';
    return 'text-danger';
  } else {
    // 对应 'PERFECT', 'ADJUST', 'WARNING'
    if (scoreOrStatus === 'PERFECT') return 'text-success';
    if (scoreOrStatus === 'ADJUST') return 'text-warning';
    if (scoreOrStatus === 'WARNING') return 'text-danger';
    return 'text-muted';
  }
};

// 状态中文翻译
const getStatusText = (status) => {
  const map = {
    'PERFECT': '完美契合',
    'ADJUST': '建议微调',
    'WARNING': '严重偏差'
  };
  return map || '未知';
};

// 计算偏差字段数据
const deviationFields = computed(() => {
  if (!report.value) return [];

  const fields = [];
  const adjustments = report.value.mechanicalAdjustments || [];

  // 字段配置与后端 label 的映射
  const fieldConfig = [
    { key: 'seatHeight', name: '建议座高', unit: 'mm', label: 'Saddle Height (座垫高度)' },
    { key: 'setback', name: '坐垫后移', unit: 'mm', label: 'Setback (座垫后移)' },
    { key: 'crankLength', name: '曲柄长度', unit: 'mm', label: 'Crank Length (曲柄长度)' },
    { key: 'saddleWidth', name: '坐垫推荐宽', unit: 'mm', label: 'Saddle Width (坐垫推荐宽)' },
    { key: 'handlebarDrop', name: '车把落差', unit: 'mm', label: 'Handlebar Drop (车把落差)' },
    { key: 'spacerHeight', name: '垫圈高度', unit: 'mm', label: 'Spacer Height (垫圈高度)' },
    { key: 'stemLength', name: '把立长度', unit: 'mm', label: 'Stem Length (把立长度)' }
  ];

  // 从 mechanicalAdjustments 中提取当前值和推荐值
  fieldConfig.forEach(config => {
    const item = adjustments.find(a => a.label === config.label);
    const hasData = !!item;
    const currentVal = hasData ? item.currentValue : null;
    const idealVal = hasData ? item.idealValue : null;
    const delta = (currentVal != null && idealVal != null) ? idealVal - currentVal : null;

    let deltaClass = 'neutral';
    if (delta != null) {
      if (Math.abs(delta) <= 5) deltaClass = 'good';
      else if (Math.abs(delta) <= 15) deltaClass = 'warning';
      else deltaClass = 'danger';
    }

    fields.push({
      name: config.name,
      unit: config.unit,
      current: currentVal != null ? currentVal : '--',
      ideal: idealVal != null ? idealVal : '--',
      delta: delta != null ? delta : '--',
      deltaClass: deltaClass
    });
  });

  return fields;
});

const goToGarage = () => {
  router.push({ path: '/lab/garage/edit', query: { id: bikeId.value } });
};
// 初始化获取数据
onMounted(async () => {
  try {
    let res;
    if (resultId.value) {
      // 优先按结果ID拉取（分享场景）
      res = await getFittingReportById(resultId.value, { silent: true });
    } else if (bikeId.value) {
      // 兼容旧的按车辆拉取最新（实验室流程场景）
      res = await getFittingReport(bikeId.value, { silent: true });
    } else {
      showToast('缺少报告标识', 'error');
      router.push('/lab');
      return;
    }

    // 适配后端 Result 封装
    report.value = res.data || res;
  } catch (error) {
    console.error('获取报告失败:', error);
    // 只有在没有 report 数据时才显示错误提示，且由组件内部统一处理
    if (!report.value) {
      showToast(error.message || '报告获取失败', 'error');
    }
  } finally {
    // 稍微延迟关闭 loading，让动画更平滑
    setTimeout(() => { loading.value = false; }, 500);
  }
});
</script>

<style scoped>
.report-page {
  padding: 120px 0 80px;
  background: transparent;
  min-height: 100vh;
}

/* Loading */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  color: var(--text-muted);
}
.radar-spinner {
  width: 50px; height: 50px;
  border: 3px solid rgba(92, 110, 88, 0.2);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}
@keyframes spin { 100% { transform: rotate(360deg); } }

/* Hero Header */
.report-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40px;
  margin-bottom: 40px;
  border-left: 6px solid var(--primary-color);
}
.hero-text .tag { font-size: 0.8rem; letter-spacing: 2px; color: var(--text-muted); font-weight: bold; }
.hero-text .title { font-size: 2.5rem; font-family: var(--font-serif); margin: 10px 0 20px; color: var(--text-main); }
.summary-box { display: flex; gap: 15px; max-width: 600px; background: rgba(0,0,0,0.02); padding: 15px 20px; border-radius: 8px; }
.quote-icon { color: var(--accent-gold); font-size: 1.5rem; margin-top: 5px; }
.summary-text { font-size: 1.05rem; color: var(--text-muted); line-height: 1.6; font-style: italic; }

/* Score Dashboard */
.score-dashboard { text-align: center; }
.score-ring { position: relative; width: 140px; height: 140px; margin: 0 auto; }
.ring-bg { stroke: var(--primary-color); stroke-width: 8; fill: none; }
.ring-fill { stroke: var(--primary-color); stroke-width: 8; fill: none; stroke-linecap: round; transform: rotate(-90deg); transform-origin: 50% 50%; transition: stroke-dashoffset 1.5s var(--ease-out-expo); }
.score-center { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); display: flex; flex-direction: column; }
.score-num { font-size: 2.5rem; font-family: var(--font-serif); font-weight: bold; line-height: 1; }
.score-label { font-size: 0.65rem; color: var(--text-muted); margin-top: 5px; letter-spacing: 1px; }
.match-level { margin-top: 15px; font-weight: bold; font-size: 1.1rem; }

/* Layout Grid */
.report-layout { display: grid; grid-template-columns: 1fr 350px; gap: 40px; }
.section-block { margin-bottom: 50px; }
.section-title { font-size: 1.4rem; color: var(--text-main); margin-bottom: 8px; border-bottom: 2px solid rgba(0,0,0,0.05); padding-bottom: 10px; }
.section-desc { font-size: 0.9rem; color: var(--text-muted); margin-bottom: 25px; }

/* Frame Check Cards */
.compatibility-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.compatibility-grid.two-col { grid-template-columns: repeat(2, 1fr); }
.compat-card { background: #FFFFFF; border-radius: 12px; padding: 20px; border: 1px solid #E8E8E4; transition: all 0.2s ease; }
.compat-card:hover { border-color: #D0D0C8; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); }
.compat-title { font-size: 0.75rem; color: #999; letter-spacing: 1px; margin: 0 0 15px 0; text-transform: uppercase; }
.compat-body { display: flex; flex-direction: column; gap: 12px; }
.compat-status { font-size: 0.7rem; padding: 4px 10px; border-radius: 12px; font-weight: bold; display: inline-block; width: fit-content; }
.status-tag { padding: 4px 12px; border-radius: 12px; font-size: 0.75rem; font-weight: bold; }
.tag-yellow { background: #FFF8E1; color: #F57F17; border: 1px solid #FFECB3; }
.tag-green { background: #E8F5E9; color: #2E7D32; border: 1px solid #C8E6C9; }
.tag-red { background: #FFEBEE; color: #C62828; border: 1px solid #FFCDD2; }
.compat-values { display: flex; align-items: center; gap: 6px; font-size: 1.4rem; font-family: var(--font-serif); }
.value-item { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.value-item .current { color: #333; font-weight: bold; }
.value-item .ideal { color: #5C6E58; font-weight: bold; }
.value-label { font-size: 0.6rem; color: #999; font-family: var(--font-sans); letter-spacing: 0.5px; }
.compat-values .sep { color: #CCC; font-size: 1rem; }
.compat-reason { font-size: 0.8rem; color: #888; line-height: 1.5; margin: 0; padding-top: 8px; border-top: 1px dashed #EEE; }
.compat-card.overall { background: #5C6E58; border-color: #5C6E58; color: #FFFFFF; }
.compat-card.overall:hover { background: #4D5F49; border-color: #4D5F49; }
.compat-card.overall .compat-title { color: rgba(255, 255, 255, 0.7); }
.overall-score { display: flex; align-items: baseline; gap: 4px; }
.overall-score .score { font-size: 2.2rem; font-weight: bold; font-family: var(--font-serif); }
.overall-score .label { font-size: 0.85rem; opacity: 0.8; }
.overall-section { display: flex; flex-direction: column; gap: 6px; }
.section-label { font-size: 0.7rem; color: rgba(255, 255, 255, 0.6); letter-spacing: 0.5px; }
.overall-desc { font-size: 0.9rem; opacity: 0.95; margin: 0; font-weight: 500; }
.overall-score.PERFECT { color: #A5D6A7; }
.overall-score.ADJUST { color: #FFCC80; }
.overall-score.WARNING { color: #EF9A9A; }

@media (max-width: 768px) {
  .compatibility-grid,
  .compatibility-grid.two-col { grid-template-columns: 1fr; }
}

/* Setup Detail List */
.setup-list { display: flex; flex-direction: column; gap: 20px; }
.setup-item { padding: 25px; border-left: 4px solid transparent; transition: 0.3s; }
.setup-item:hover { border-left-color: var(--primary-color); transform: translateX(5px); }

/* Deviation Grid */
.deviation-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.deviation-card { background: #FFFFFF; border-radius: 12px; padding: 16px; border: 1px solid #E8E8E4; transition: all 0.2s ease; }
.deviation-card:hover { border-color: #D0D0C8; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); }
.field-name { font-size: 0.75rem; color: #999; letter-spacing: 0.5px; margin: 0 0 12px 0; text-transform: uppercase; }
.field-values { display: flex; flex-direction: column; gap: 8px; }
.v-item { display: flex; justify-content: space-between; align-items: center; }
.v-label { font-size: 0.7rem; color: #AAA; }
.v-current { font-size: 1rem; font-weight: bold; color: #333; font-family: var(--font-serif); }
.v-ideal { font-size: 1rem; font-weight: bold; color: #5C6E58; font-family: var(--font-serif); }
.v-delta { font-size: 0.9rem; font-weight: bold; font-family: var(--font-serif); }
.v-item.good .v-delta { color: #2E7D32; }
.v-item.warning .v-delta { color: #F57F17; }
.v-item.danger .v-delta { color: #C62828; }
.v-item.neutral .v-delta { color: #666; }

@media (max-width: 1024px) {
  .deviation-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .deviation-grid { grid-template-columns: repeat(2, 1fr); }
}

.item-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.item-title { display: flex; align-items: center; gap: 10px; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot.PERFECT { background: var(--success-color); }
.dot.ADJUST { background: var(--warning-color); }
.dot.WARNING { background: var(--danger-color); }
.item-title h4 { margin: 0; font-size: 1.2rem; }
.item-action { font-weight: bold; font-size: 0.95rem; }

.item-compare { display: flex; align-items: center; background: #FAFAFA; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
.c-box { flex: 1; text-align: center; }
.c-label { display: block; font-size: 0.75rem; color: var(--text-muted); margin-bottom: 5px; }
.c-val { font-size: 1.8rem; font-family: var(--font-serif); font-weight: bold; color: var(--text-main); }
.c-box.ideal .c-val { color: var(--primary-color); }
.c-arrow { color: #DDD; font-size: 1.2rem; padding: 0 20px; }
.c-delta { flex: 1; text-align: right; border-left: 1px solid #EEE; padding-left: 20px; }
.d-label { display: block; font-size: 0.75rem; color: var(--text-muted); margin-bottom: 5px; }
.d-val { font-size: 1.4rem; font-weight: bold; font-family: var(--font-serif); }

.item-reason { font-size: 0.9rem; color: var(--text-muted); line-height: 1.5; background: rgba(92, 110, 88, 0.05); padding: 12px 15px; border-radius: 6px; }

/* Sidebar: Equipment Suggestions */
.sticky-card { position: sticky; top: 100px; padding: 30px; }
.side-title { font-size: 1.2rem; color: var(--text-main); margin-bottom: 20px; padding-bottom: 10px; border-bottom: 1px solid #EEE; }
.equip-list { list-style: none; padding: 0; margin-bottom: 30px; }
.equip-list li { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 15px; font-size: 0.95rem; color: var(--text-main); line-height: 1.5; }
.icon-gold { color: var(--accent-gold); margin-top: 3px; }

.equip-empty { text-align: center; color: var(--success-color); margin-bottom: 30px; padding: 20px 0; }
.equip-empty i { font-size: 2rem; margin-bottom: 10px; }
.equip-empty p { font-size: 0.9rem; color: var(--text-muted); }

.action-group { display: flex; flex-direction: column; gap: 15px; }
.full-width { width: 100%; justify-content: center; }

/* Utility colors */
.text-success { color: var(--success-color); }
.text-warning { color: var(--warning-color); }
.text-danger { color: var(--danger-color); }

@media (max-width: 1024px) {
  .report-layout { grid-template-columns: 1fr; }
  .sticky-card { position: static; }
  .report-hero { flex-direction: column; text-align: center; gap: 30px; }
  .summary-box { justify-content: center; }
}
@media (max-width: 768px) {
  .item-compare { flex-wrap: wrap; gap: 15px; }
  .c-delta { border-left: none; border-top: 1px solid #eee; padding-left: 0; padding-top: 15px; width: 100%; text-align: center; flex: none; }
}
</style>