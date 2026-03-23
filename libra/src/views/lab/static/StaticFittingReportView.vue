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

              <div class="frame-grid">
                <!-- 堆高 Stack -->
                <div class="frame-card glass-card" v-if="report.frameStackCheck">
                  <div class="fc-header">
                    <h4>{{ report.frameStackCheck.label }}</h4>
                    <span class="status-badge" :class="report.frameStackCheck.status">
                      {{ getStatusText(report.frameStackCheck.status) }}
                    </span>
                  </div>
                  <div class="fc-data">
                    <div class="val">现状: {{ report.frameStackCheck.currentValue || '--' }} <small>{{ report.frameStackCheck.unit }}</small></div>
                    <div class="val ideal">理想: {{ report.frameStackCheck.idealValue }} <small>{{ report.frameStackCheck.unit }}</small></div>
                  </div>
                  <p class="fc-reason">{{ report.frameStackCheck.reason }}</p>
                </div>

                <!-- 前伸 Reach -->
                <div class="frame-card glass-card" v-if="report.frameReachCheck">
                  <div class="fc-header">
                    <h4>{{ report.frameReachCheck.label }}</h4>
                    <span class="status-badge" :class="report.frameReachCheck.status">
                      {{ getStatusText(report.frameReachCheck.status) }}
                    </span>
                  </div>
                  <div class="fc-data">
                    <div class="val">现状: {{ report.frameReachCheck.currentValue || '--' }} <small>{{ report.frameReachCheck.unit }}</small></div>
                    <div class="val ideal">理想: {{ report.frameReachCheck.idealValue }} <small>{{ report.frameReachCheck.unit }}</small></div>
                  </div>
                  <p class="fc-reason">{{ report.frameReachCheck.reason }}</p>
                </div>
              </div>
            </section>

            <!-- 3. 调教设定明细 (可调项目) -->
            <section class="section-block">
              <h3 class="section-title"><i class="fas fa-sliders-h"></i> 设定偏差分析 (Setup Deviations)</h3>
              <p class="section-desc">对比您当前的单车设定与 Aries 计算出的黄金生物力学坐标。</p>

              <div class="setup-list">
                <div v-for="(metric, index) in report.setupDetails" :key="index" class="setup-item glass-card">

                  <div class="item-head">
                    <div class="item-title">
                      <span class="dot" :class="metric.status"></span>
                      <h4>{{ metric.label }}</h4>
                    </div>
                    <div class="item-action" :class="metric.status">{{ metric.action }}</div>
                  </div>

                  <div class="item-compare">
                    <div class="c-box current">
                      <span class="c-label">当前设定</span>
                      <span class="c-val">{{ metric.currentValue || '--' }}<small>{{ metric.unit }}</small></span>
                    </div>
                    <div class="c-arrow"><i class="fas fa-arrow-right"></i></div>
                    <div class="c-box ideal">
                      <span class="c-label">理想建议</span>
                      <span class="c-val">{{ metric.idealValue }}<small>{{ metric.unit }}</small></span>
                    </div>
                    <div class="c-delta" :class="metric.status">
                      <span class="d-label">偏差</span>
                      <span class="d-val">{{ metric.delta > 0 ? '+' : '' }}{{ metric.delta }}</span>
                    </div>
                  </div>

                  <div class="item-reason"><i class="fas fa-info-circle"></i> {{ metric.reason }}</div>
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
.frame-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.frame-card { padding: 25px; }
.fc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.fc-header h4 { margin: 0; font-size: 1.1rem; color: var(--text-main); }
.status-badge { font-size: 0.75rem; padding: 4px 10px; border-radius: 12px; font-weight: bold; }
.status-badge.PERFECT { background: #E8F5E9; color: #2E7D32; }
.status-badge.ADJUST { background: #FFF3E0; color: #E65100; }
.status-badge.WARNING { background: #FFEBEE; color: #C62828; }
.fc-data { display: flex; gap: 20px; margin-bottom: 15px; }
.fc-data .val { font-size: 0.9rem; color: var(--text-muted); }
.fc-data .ideal { color: var(--primary-color); font-weight: bold; }
.fc-reason { font-size: 0.85rem; color: var(--text-muted); border-top: 1px dashed #EEE; padding-top: 15px; margin: 0; }

/* Setup Detail List */
.setup-list { display: flex; flex-direction: column; gap: 20px; }
.setup-item { padding: 25px; border-left: 4px solid transparent; transition: 0.3s; }
.setup-item:hover { border-left-color: var(--primary-color); transform: translateX(5px); }

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
  .frame-grid { grid-template-columns: 1fr; }
  .item-compare { flex-wrap: wrap; gap: 15px; }
  .c-delta { border-left: none; border-top: 1px solid #eee; padding-left: 0; padding-top: 15px; width: 100%; text-align: center; flex: none; }
}
</style>