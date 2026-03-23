<template>
  <main class="survey-section bg-zen">
    <div class="paper-texture"></div>

    <div class="container survey-container">

      <!-- 进度指示 -->
      <div class="survey-controls">
        <div class="nav-left">
          <button @click="prevStep" class="nav-back-btn" :class="{ hidden: currentStep === 1 }">
            <i class="fas fa-chevron-left"></i>
            <span>BACK</span>
          </button>
        </div>

        <div class="zen-progress">
          <div class="step-unit" :class="{ active: currentStep >= 1 }"><span class="dot"></span><span class="line"></span></div>
          <div class="step-unit" :class="{ active: currentStep >= 2 }"><span class="dot"></span><span class="line"></span></div>
          <div class="step-unit" :class="{ active: currentStep >= 3 }"><span class="dot"></span></div>
        </div>

        <div class="nav-right">
          <span class="step-count">{{ currentStep }} / 3</span>
        </div>
      </div>

      <form @submit.prevent="submitForm" class="survey-form">
        <!-- 步骤 1 -->
        <div v-show="currentStep === 1" class="survey-step active">
          <h2 class="question-title serif-title">您的 Fitting 目标是?</h2>
          <p class="question-subtitle">选择一个方向，我们将为您匹配最佳的几何逻辑。</p>

          <div class="options-group" :class="{ 'shake-active': showError }">
            <label class="zen-option" v-for="opt in goalOptions" :key="opt.value">
              <input type="radio" v-model="formData.goal" :value="opt.value" @change="clearError">
              <div class="option-content">
                <span class="radio-dot"></span>
                <span class="text">{{ opt.label }}</span>
              </div>
            </label>
          </div>
        </div>

        <!-- 步骤 2 -->
        <div v-show="currentStep === 2" class="survey-step active">
          <h2 class="question-title serif-title">常出现的骑行场景?</h2>
          <p class="question-subtitle">场景决定了身体与器材互动的频率。</p>

          <div class="options-group" :class="{ 'shake-active': showError }">
            <label class="zen-option" v-for="opt in sceneOptions" :key="opt.value">
              <input type="radio" v-model="formData.scene" :value="opt.value" @change="clearError">
              <div class="option-content"><span class="radio-dot"></span><span class="text">{{ opt.label }}</span></div>
            </label>
          </div>
        </div>

        <!-- 步骤 3 -->
        <div v-show="currentStep === 3" class="survey-step active">
          <h2 class="question-title serif-title">目前的车辆状态?</h2>
          <p class="question-subtitle">我们将根据购车计划为您提供静态推荐或动态调整。</p>

          <div class="options-group" :class="{ 'shake-active': showError }">
            <label class="zen-option" v-for="opt in statusOptions" :key="opt.value">
              <input type="radio" v-model="formData.bike_status" :value="opt.value" @change="clearError">
              <div class="option-content"><span class="radio-dot"></span><span class="text">{{ opt.label }}</span></div>
            </label>
          </div>
        </div>

        <!-- 统一的反馈与操作区 -->
        <div class="action-area">
          <transition name="zen-fade">
            <p v-if="showError" class="error-hint">请选择一个选项，以继续这段旅程</p>
          </transition>

          <button v-if="currentStep < totalSteps" type="button" class="btn btn-zen-next" @click="nextStep">
            继续探索 &rarr;
          </button>
          <button v-else type="submit" class="btn btn-zen-primary">完成并开启旅程</button>
        </div>
      </form>
    </div>
  </main>
</template>

<script setup>
import { ref, reactive } from 'vue';

const currentStep = ref(1);
const totalSteps = 3;
const showError = ref(false);

const formData = reactive({ goal: '', scene: '', bike_status: '' });

const goalOptions = [
  { label: '预防损伤 / 保障健康', value: 'injury_prevention' },
  { label: '提升舒适性 / 长途耐力', value: 'comfort' },
  { label: '优化运动表现 / 力量输出', value: 'performance' },
  { label: '专业竞赛需求 / 极致气动', value: 'pro_racing' }
];
const sceneOptions = [
  { label: '日常通勤 / 城市穿梭', value: 'commuting' },
  { label: '健身锻炼 / 周末骑游', value: 'fitness' },
  { label: '长途旅行 / 纵横山野', value: 'touring' },
  { label: '竞技比赛 / 封闭绕圈', value: 'competition' }
];
const statusOptions = [
  { label: '已有爱车，需优化设定', value: 'owned' },
  { label: '计划一周内购入', value: 'buy_week' },
  { label: '本月内有购车意向', value: 'buy_month' },
  { label: '暂不考虑买车', value: 'not_consider' }
];

const clearError = () => { showError.value = false; };

const nextStep = () => {
  const isStep1Empty = currentStep.value === 1 && !formData.goal;
  const isStep2Empty = currentStep.value === 2 && !formData.scene;
  if (isStep1Empty || isStep2Empty) { triggerWarning(); return; }
  showError.value = false;
  if (currentStep.value < totalSteps) currentStep.value++;
};

const triggerWarning = () => { showError.value = true; };
const prevStep = () => { showError.value = false; if (currentStep.value > 1) currentStep.value--; };
const submitForm = () => {
  if (!formData.bike_status) { triggerWarning(); return; }
  console.log("提交数据:", formData);
};
</script>

<style scoped>
.bg-zen {
  background-color: transparent;
  min-height: 100vh;
  display: flex; flex-direction: column; align-items: center;
  padding-top: 100px; position: relative;
  font-family: var(--font-sans); color: var(--text-main);
}

/* 纹理：body 已经有全局噪点纹理，这里可以移除或保留叠加 */
.paper-texture { display: none; }

.survey-container {
  max-width: 640px; width: 90%; padding: 40px;
  background: var(--bg-surface); border-radius: var(--radius-md);
  border: var(--border-glass);
  box-shadow: var(--shadow-lg);
  z-index: 10; position: relative;
}

.survey-controls {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 60px; position: relative; z-index: 20;
}

.nav-left, .nav-right { width: 80px; }
.nav-back-btn {
  background: none; border: none; color: var(--text-muted); cursor: pointer; transition: 0.3s;
  display: flex; align-items: center; gap: 8px; font-size: 0.8rem; letter-spacing: 1px;
}
.nav-back-btn:hover { color: var(--primary-color); }
.hidden { visibility: hidden; pointer-events: none; }

/* 进度指示 */
.zen-progress { display: flex; align-items: center; }
.step-unit { display: flex; align-items: center; }
.step-unit .dot { width: 6px; height: 6px; border-radius: 50%; background: #E0E0E0; transition: 0.6s; }
.step-unit .line { width: 30px; height: 1px; background: #E0E0E0; margin: 0 4px; transition: 0.6s; }
.step-unit.active .dot { background: var(--primary-color); transform: scale(1.4); }
.step-unit.active .line { background: var(--primary-color); }
.step-count { font-size: 0.8rem; color: var(--text-muted); font-family: var(--font-serif); text-align: right; display: block; }

/* 题目排版 */
.serif-title { font-family: var(--font-serif); font-weight: 600; font-size: 2.2rem; margin: 12px 0; color: var(--text-main); }
.question-subtitle { font-size: 1rem; color: var(--text-muted); margin-bottom: 40px; font-family: var(--font-serif); font-style: italic; }

/* 选项组与提醒动画 */
.options-group { display: flex; flex-direction: column; gap: 16px; margin-bottom: 30px; }

.shake-active { animation: zenShake 0.5s ease-in-out; }
@keyframes zenShake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-4px); }
  40%, 80% { transform: translateX(4px); }
}

.zen-option { cursor: pointer; position: relative; }
.zen-option input { position: absolute; opacity: 0; }
.option-content {
  display: flex; align-items: center; padding: 20px 25px;
  background: var(--bg-page); border: 1px solid transparent; border-radius: var(--radius-sm); transition: 0.3s;
}
.radio-dot { width: 10px; height: 10px; border-radius: 50%; border: 1px solid var(--text-muted); margin-right: 18px; transition: 0.3s; }
.text { font-size: 1rem; color: var(--text-main); font-weight: 500; }

.zen-option:hover .option-content { background: rgba(92, 110, 88, 0.03); }
.zen-option input:checked + .option-content { background: #FFF; border-color: var(--primary-color); box-shadow: var(--shadow-sm); }
.zen-option input:checked + .option-content .radio-dot { background: var(--primary-color); border-color: var(--primary-color); box-shadow: 0 0 0 4px rgba(92, 110, 88, 0.1); }

/* 操作区域反馈 */
.action-area {
  display: flex; flex-direction: column; align-items: flex-end; gap: 12px;
}

.error-hint {
  font-family: var(--font-serif); font-style: italic; font-size: 0.95rem;
  color: var(--secondary-color); margin: 0; letter-spacing: 0.5px;
}

/* 按钮样式 */
.btn-zen-next {
  background: none; border: none; color: var(--primary-color);
  font-family: var(--font-serif); font-size: 1.2rem;
  font-style: italic; cursor: pointer; transition: 0.3s;
}
.btn-zen-next:hover { color: var(--primary-dark); transform: translateX(5px); }

.btn-zen-primary {
  width: 100%; padding: 16px; background: var(--text-main); color: #FFF;
  border: none; border-radius: var(--radius-pill); font-size: 1rem; cursor: pointer; transition: 0.3s;
}
.btn-zen-primary:hover { background: var(--primary-color); }

/* 提示文字过渡 */
.zen-fade-enter-active, .zen-fade-leave-active { transition: opacity 0.4s; }
.zen-fade-enter-from, .zen-fade-leave-to { opacity: 0; }

.survey-step { animation: zenStepIn 0.8s ease; }
@keyframes zenStepIn { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 600px) {
  .bg-zen { padding-top: 40px; }
  .survey-container { padding: 30px 20px; border-radius: 0; border: none; width: 100%; }
  .serif-title { font-size: 1.8rem; }
}
</style>