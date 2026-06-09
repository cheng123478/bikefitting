<template>
  <transition name="modal-pop">
    <!-- 弹窗遮罩：移除背景点击关闭，防止误触 -->
    <div v-if="visible" class="result-modal-overlay">
      <div class="result-modal-card glass-card">
        <!-- 右上角关闭按钮 -->
        <button class="close-btn" @click.stop="handleClose">
          <i class="fas fa-times"></i>
        </button>

        <div class="report-wrapper">
          <!-- 1. 档案构建进度 (Completeness) -->
          <header class="report-header">
            <div class="completeness-box">
              <div class="circle-chart" :style="`--p:${completeness}; --c:var(--primary-color)`">
                <span class="percent">{{ completeness }}%</span>
              </div>
              <div class="header-text">
                <span class="report-label">FITTING ARCHIVE</span>
                <h2 class="report-title">身体图谱解析报告</h2>
                <p class="completeness-hint">
                  {{ completeness < 100 ? '档案部分锁定，完善进阶数据以解锁专家建议' : '生物蓝图已完美构建，所有分析已解锁' }}
                </p>
              </div>
            </div>
          </header>

          <!-- 2. 骑士画像勋章 (Rider Tags) -->
          <div class="tag-row">
            <template v-if="profile?.riderTags && profile.riderTags.length > 0">
              <span v-for="tag in profile.riderTags" :key="tag" class="biometric-tag">
                <i class="fas fa-award"></i> {{ tag }}
              </span>
            </template>
            <div v-else class="tag-placeholder">
              <i class="fas fa-unlock-alt"></i> 录入肢体比例后解锁画像特征
            </div>
          </div>

          <!-- 3. 核心分层诊断内容 -->
          <div class="report-sections">

            <!-- 第一块：机能评分 (需要柔韧性数据) -->
            <section class="info-section" :class="{ 'locked-section': !hasFlexData }">
              <h4 class="section-h4">
                <i :class="hasFlexData ? 'fas fa-check-circle' : 'fas fa-lock'"></i>
                肢体潜力与柔韧评估
              </h4>

              <div v-if="hasFlexData" class="unlocked-content">
                <div class="progress-bars-row">
                  <div class="progress-bar-box">
                    <div class="progress-header">
                      <span class="progress-label">柔韧性</span>
                      <span class="progress-value">{{ profile.flexibilityScore }}分</span>
                    </div>
                    <div class="progress-track">
                      <div class="progress-fill" :style="{ width: profile.flexibilityScore + '%' }"></div>
                    </div>
                  </div>
                  <div class="progress-bar-box">
                    <div class="progress-header">
                      <span class="progress-label">稳定性</span>
                      <span class="progress-value">{{ profile.stabilityScore }}分</span>
                    </div>
                    <div class="progress-track">
                      <div class="progress-fill" :style="{ width: profile.stabilityScore + '%' }"></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else class="lock-overlay" @click="handleReEdit(3)">
                <p>录入胸椎旋转与举臂角度，解锁专业落差(Drop)建议</p>
                <span class="cta-link">去完善数据 <i class="fas fa-chevron-right"></i></span>
              </div>
            </section>

            <!-- 第三块：Fitter 评估结论 -->
            <section class="info-section" :class="{ 'locked-section': !hasFlexData }">
              <h4 class="section-h4">
                <i :class="hasFlexData ? 'fas fa-check-circle' : 'fas fa-lock'"></i>
                Fitter 评估结论
              </h4>

              <div v-if="hasFlexData" class="unlocked-content">
                <!-- 1. 总结 -->
                <div class="evaluation-block summary-block" v-if="profile.summary">
                  <h5 class="block-title">
                    <i class="fas fa-file-alt"></i> 总结
                  </h5>
                  <div class="summary-content">
                    <i class="fas fa-quote-left"></i>
                    <p>{{ profile.summary }}</p>
                  </div>
                </div>

                <!-- 2. 待关注方面 -->
                <div class="evaluation-block findings-block" v-if="profile.findings && profile.findings.length > 0">
                  <h5 class="block-title">
                    <i class="fas fa-exclamation-triangle"></i> 待关注方面
                  </h5>
                  <ul class="findings-list">
                    <li v-for="(finding, index) in profile.findings" :key="index" class="finding-item">
                      <i class="fas fa-circle-info"></i>
                      <span>{{ finding }}</span>
                    </li>
                  </ul>
                </div>

                <!-- 3. 补弱建议 -->
                <div class="evaluation-block advice-block" v-if="profile.advice && profile.advice.length > 0">
                  <h5 class="block-title">
                    <i class="fas fa-lightbulb"></i> 补弱建议
                  </h5>
                  <ul class="advice-list">
                    <li v-for="(item, index) in profile.advice" :key="index" class="advice-item">
                      <i class="fas fa-check-circle"></i>
                      <span>{{ item }}</span>
                    </li>
                  </ul>
                </div>
              </div>

              <div v-else class="lock-overlay" @click="handleReEdit(3)">
                <p>录入胸椎旋转与举臂角度，解锁Fitter专业评估结论</p>
                <span class="cta-link">去完善数据 <i class="fas fa-chevron-right"></i></span>
              </div>
            </section>

          </div>

          <!-- 4. 底部行动点 -->
          <footer class="report-footer">
            <div class="footer-btns">
              <!-- 情况 A：数据不全 -->
              <template v-if="completeness < 100">
                <button class="btn btn-outline" @click="goTo('/lab/body/history')">完成查看</button>
                <button class="btn btn-primary btn-glow" @click.stop="handleReEdit(2)">
                  补全进阶数据 <i class="fas fa-arrow-right"></i>
                </button>
              </template>
              <!-- 情况 B：数据完整 -->
              <template v-else>
                <button class="btn btn-outline" @click="goTo('/lab/body/history')">完成查看</button>
                <button class="btn btn-primary btn-glow" @click.stop="goToGarage">
                  进入我的车库 <i class="fas fa-bicycle"></i>
                </button>
              </template>
            </div>
            <p class="footer-note">提示：身体数据是 Fitting 算法的基准，建议每三个月校准一次。</p>
          </footer>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps({
  visible: Boolean,
  profile: Object
});

const emit = defineEmits(['close']);
const router = useRouter();

// 内部锁：防止弹窗刚打开时因点击事件残留（如 MouseUp）误触关闭
const canClose = ref(false);
watch(() => props.visible, (newVal) => {
  if (newVal) {
    setTimeout(() => {
      canClose.value = true;
    }, 500);
  } else {
    canClose.value = false;
  }
});

/**
 * 1. 计算档案完整度逻辑
 */
const completeness = computed(() => {
  if (!props.profile) return 0;
  let score = 40; // 只要有结果说明已经填了身高跨高 (40%)
  if (props.profile.recSaddleHeight && props.profile.apeIndex) score += 20; // 肢体细节 (20%)
  if (props.profile.flexibilityScore > 0) score += 40; // 柔韧性与画像 (40%)
  return score;
});

const hasFlexData = computed(() => props.profile && props.profile.flexibilityScore > 0);

// 通用跳转映射
const goTo = (path) => {
  if (!canClose.value) return;
  emit('close');
  if (typeof path === 'string') {
    router.push(path);
  } else {
    router.push(path);
  }
};

// 1. 关闭弹窗
const handleClose = () => {
  if (!canClose.value) return;
  emit('close');
};

// 2. 引导完善信息：跳转回录入页，并传递步骤参数
const handleReEdit = (step) => {
  goTo({ path: '/lab/body', query: { step: step } });
};

// 3. 进入下一步业务：我的车库 (位于个人中心)
const goToGarage = () => {
  goTo('/lab/garage');
};
</script>

<style scoped>
/* 遮罩与容器 */
.result-modal-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: var(--bg-overlay); backdrop-filter: blur(15px);
  display: flex; align-items: center; justify-content: center; padding: 20px;
  /* 显式声明以覆盖 main.css 可能存在的冲突 */
  opacity: 1 !important;
  pointer-events: auto !important;
}

.result-modal-card {
  width: 100%; max-width: 620px; background: var(--bg-page);
  border-radius: var(--radius-md); padding: 45px; position: relative;
  box-shadow: var(--shadow-lg); max-height: 90vh; overflow-y: auto;
}

/* 顶部完成度 */
.report-header { margin-bottom: 35px; }
.completeness-box { display: flex; align-items: center; gap: 25px; }

.circle-chart {
  width: 80px; height: 80px; border-radius: 50%;
  background: radial-gradient(closest-side, var(--bg-page) 82%, transparent 0),
  conic-gradient(var(--c) calc(var(--p) * 1%), #E0E0E0 0);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; font-weight: 800; color: var(--primary-color);
}

.report-label { font-size: 0.7rem; letter-spacing: 0.25em; color: var(--primary-light); font-weight: 700; }
.report-title { font-family: var(--font-serif); font-size: 1.8rem; color: var(--text-main); line-height: 1.2; margin: 4px 0; }
.completeness-hint { font-size: 0.85rem; color: var(--text-muted); line-height: 1.4; }

/* 勋章标签 */
.tag-row { display: flex; justify-content: flex-start; gap: 10px; margin-bottom: 35px; min-height: 32px; }
.biometric-tag {
  background: white; 
  border: 1px solid var(--accent-gold); 
  color: var(--accent-gold);
  padding: 5px 14px; 
  border-radius: var(--radius-pill); 
  font-size: 0.8rem;
  font-weight: 700; 
  display: flex; 
  align-items: center; 
  gap: 6px;
  transition: all 0.3s ease;
  cursor: default;
  position: relative;
  overflow: hidden;
}
.biometric-tag::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 215, 0, 0.2), transparent);
  transition: left 0.5s ease;
}
.biometric-tag:hover {
  background: var(--accent-gold);
  color: white;
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.4);
  border-color: var(--accent-gold);
}
.biometric-tag:hover::before {
  left: 100%;
}
.biometric-tag:hover i {
  transform: rotate(15deg) scale(1.2);
}
.biometric-tag i {
  transition: transform 0.3s ease;
}
.tag-placeholder { color: #BBB; font-size: 0.85rem; font-style: italic; }

/* 内容板块 */
.info-section {
  background: white; padding: 22px; border-radius: var(--radius-sm); margin-bottom: 20px;
  border: 1px solid rgba(0,0,0,0.03); position: relative;
}
.section-h4 { font-size: 0.95rem; margin-bottom: 18px; color: var(--text-main); display: flex; align-items: center; gap: 8px; }
.section-h4 i { font-size: 0.85rem; }
.unlocked h4 i { color: var(--success-color); }

.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.metric-item { 
  background: var(--bg-page); 
  padding: 15px; 
  border-radius: 8px; 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}
.metric-item:hover {
  background: white;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(92, 110, 88, 0.15);
  border-color: rgba(92, 110, 88, 0.2);
}
.metric-item label { font-size: 0.8rem; color: var(--text-muted); transition: color 0.3s ease; }
.metric-item:hover label { color: var(--text-main); }
.metric-item .val { 
  font-weight: 700; 
  font-size: 1rem; 
  color: var(--text-main);
  transition: all 0.3s ease;
}
.metric-item:hover .val {
  color: var(--primary-color);
  transform: scale(1.05);
}

/* 锁定状态 */
.locked-section { background: rgba(0,0,0,0.02); border: 1px dashed #DDD; }
.locked-section .section-h4 { color: #AAA; }
.lock-overlay { text-align: center; padding: 10px 0; cursor: pointer; }
.lock-overlay p { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 12px; }
.cta-link { font-size: 0.85rem; color: var(--primary-color); font-weight: 700; text-decoration: underline; text-underline-offset: 4px; }

/* 专家文案 */
.progress-bars-row { display: flex; flex-direction: column; gap: 25px; margin: 25px 0 30px; }
.progress-bar-box { display: flex; flex-direction: column; gap: 10px; }
.progress-header { display: flex; justify-content: space-between; align-items: center; }
.progress-label { font-size: 0.95rem; color: var(--text-main); font-weight: 600; }
.progress-value { font-size: 0.9rem; color: var(--primary-color); font-weight: 700; }
.progress-track {
  width: 100%; height: 12px;
  background: var(--bg-page);
  border-radius: 6px;
  overflow: hidden;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #5C6E58 0%, #8FA08B 50%, #6E8565 100%);
  border-radius: 6px;
  transition: width 0.8s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.3s ease, box-shadow 0.3s ease;
  position: relative;
}
.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, 
    rgba(255, 255, 255, 0.1) 0%, 
    rgba(255, 255, 255, 0.25) 50%, 
    rgba(255, 255, 255, 0.1) 100%);
  border-radius: 6px;
}
.progress-bar-box:hover .progress-fill {
  filter: brightness(1.2);
  box-shadow: 0 0 18px rgba(92, 110, 88, 0.45);
}
.progress-bar-box:hover .progress-fill::after {
  background: linear-gradient(90deg, 
    rgba(255, 255, 255, 0.15) 0%, 
    rgba(255, 255, 255, 0.35) 50%, 
    rgba(255, 255, 255, 0.15) 100%);
}
.progress-bar-box:hover .progress-value {
  transform: scale(1.1);
  color: var(--primary-dark);
}
.progress-value {
  transition: all 0.3s ease;
}

/* Fitter 评估结论样式 */
/* 1. 通用评估区块样式 */
.evaluation-block {
  margin-bottom: 25px;
  padding: 20px;
  background: var(--bg-page);
  border-radius: 8px;
  border-left: 4px solid transparent;
  transition: all 0.3s ease;
}
.evaluation-block:last-child {
  margin-bottom: 0;
}
.block-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.block-title i {
  font-size: 0.9rem;
}

/* 2. 总结区块 - 浅鼠尾草绿 */
.summary-block {
  border-left-color: #a3b39e;
  background: linear-gradient(135deg, #f7f9f6 0%, #eef1ec 100%);
}
.summary-block .block-title i {
  color: #6E8565;
}
.summary-block:hover {
  background: linear-gradient(135deg, #eef1ec 0%, #e5e9e2 100%);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.08);
}
.summary-content {
  position: relative;
  padding-left: 25px;
}
.summary-content > i {
  position: absolute;
  left: 0;
  top: 0;
  opacity: 0.15;
  font-size: 1.2rem;
  color: #6E8565;
}
.summary-content p {
  font-size: 0.92rem;
  line-height: 1.8;
  color: var(--text-body);
  font-style: italic;
  margin: 0;
}

/* 3. 关键问题区块 - 中鼠尾草绿 */
.findings-block {
  border-left-color: #8FA08B;
  background: linear-gradient(135deg, #eef1ec 0%, #e5e9e2 100%);
}
.findings-block .block-title i {
  color: #5C6E58;
}
.findings-block:hover {
  background: linear-gradient(135deg, #e5e9e2 0%, #dce2da 100%);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.12);
}
.findings-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.finding-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 15px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  border-left: 3px solid #8FA08B;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.finding-item:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.15);
  border-left-width: 4px;
}
.finding-item i {
  color: #5C6E58;
  font-size: 0.85rem;
  margin-top: 3px;
  flex-shrink: 0;
}
.finding-item span {
  font-size: 0.88rem;
  line-height: 1.6;
  color: var(--text-body);
}

/* 4. Fitter 建议区块 - 深鼠尾草绿 */
.advice-block {
  border-left-color: #6E8565;
  background: linear-gradient(135deg, #e5e9e2 0%, #dce2da 100%);
}
.advice-block .block-title i {
  color: #465543;
}
.advice-block:hover {
  background: linear-gradient(135deg, #dce2da 0%, #d3dbcf 100%);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.15);
}
.advice-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 15px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  border-left: 3px solid #6E8565;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.advice-item:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.18);
  border-left-width: 4px;
}
.advice-item i {
  color: #465543;
  font-size: 0.85rem;
  margin-top: 3px;
  flex-shrink: 0;
}
.advice-item span {
  font-size: 0.88rem;
  line-height: 1.6;
  color: var(--text-body);
  font-weight: 500;
}

/* 页脚 */
.report-footer { margin-top: 40px; text-align: center; }
.footer-btns { display: flex; gap: 15px; margin-bottom: 20px; }
.footer-btns .btn { flex: 1; height: 50px; font-size: 1rem; }
.footer-note { font-size: 0.75rem; color: var(--text-muted); opacity: 0.6; }

.close-btn { position: absolute; top: 25px; right: 25px; background: none; border: none; font-size: 1.4rem; color: #DDD; cursor: pointer; transition: 0.3s; }
.close-btn:hover { color: var(--text-main); }

/* 动画效果 */
.modal-pop-enter-active { animation: modal-in 0.5s var(--ease-out-expo); }
.modal-pop-leave-active { animation: modal-in 0.3s reverse ease-in; }
@keyframes modal-in {
  0% { opacity: 0; transform: scale(0.9) translateY(30px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

@media (max-width: 600px) {
  .result-modal-card { padding: 30px 20px; }
  .completeness-box { flex-direction: column; text-align: center; }
  .metrics-grid { grid-template-columns: 1fr; }
}
</style>
