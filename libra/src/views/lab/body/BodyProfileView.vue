<template>
  <div class="body-profile-page">
    <div class="container lab-container">

      <!-- 头部：标题与进度 -->
      <div class="toolbar-row" v-scroll-reveal>
        <div class="text-group">
          <h1 class="page-title">身体图谱</h1>
          <p class="page-subtitle">FITTING LAB / 构建您的数字解剖模型</p>
        </div>
        
        <div class="tabs-group">
          <div class="step-indicator">
            <div v-for="i in 4" :key="i" class="step-dot" :class="{ active: currentStep >= i, current: currentStep === i }">
              <span class="step-num">{{ i }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 核心内容区 -->
      <div class="lab-content glass-card" v-scroll-reveal>
        <div v-if="initLoading" class="lab-loading">
          <i class="fas fa-spinner fa-spin"></i> 正在调取实验室档案...
        </div>

        <form v-else>

          <!-- PHASE 1: 基础比例 (DNA) -->
          <div v-if="currentStep === 1" :key="'step1'" class="step-content fade-in">
            <div class="split-layout">
              <div class="form-area">
                <h3 class="step-title">01 个人及躯干基准</h3>
                <p class="step-desc">这些数据决定了您的整体车架尺码区间。</p>
                <div class="input-grid">
                  <div v-if="!userHasGender" class="zen-form-item">
                    <label class="input-label">性别 *</label>
                    <div class="gender-selector">
                      <button type="button" :class="{active: formData.gender === 1}" @click="formData.gender = 1">男性</button>
                      <button type="button" :class="{active: formData.gender === 2}" @click="formData.gender = 2">女性</button>
                    </div>
                  </div>
                  <div v-if="!userHasBirthday" class="zen-form-item">
                    <label class="input-label">出生日期 *</label>
                    <input type="date" v-model="formData.birthday" class="zen-field date-input" />
                  </div>
                  <CustomInput label="身高 (Height) *" v-model="formData.height" unit="mm" placeholder="例如：1750" @focus="setGuide('height')" />
                  <CustomInput label="跨高 (Inseam) *" v-model="formData.spanHeight" unit="mm" placeholder="例如：820" @focus="setGuide('spanHeight')" />
                  <CustomInput label="胸高 (Sternal Notch)" v-model="formData.breastHeight" unit="mm" placeholder="例如：1450" @focus="setGuide('breastHeight')" />
                </div>
              </div>
              <div class="guide-area">
                <GuideDisplay :guideKey="activeGuide" />
              </div>
            </div>
          </div>

          <!-- PHASE 2: 肢体细节 (LEVERS) -->
          <div v-if="currentStep === 2" :key="'step2'" class="step-content fade-in">
            <div class="split-layout">
              <div class="form-area">
                <h3 class="step-title">02 肢体杠杆</h3>
                <p class="step-desc">精确到毫米的肢体数据是计算 Stack & Reach 的核心。</p>
                <div class="input-grid">
                  <CustomInput label="肩宽" v-model="formData.shoulderWidth" unit="mm" placeholder="例如：420" @focus="setGuide('shoulder')" />
                  <CustomInput label="坐骨间距" v-model="formData.ischialDistance" unit="mm" placeholder="例如：120" @focus="setGuide('ischial')" />
                  <div class="grid-2">
                    <CustomInput label="大臂长" v-model="formData.longArmLength" unit="mm" placeholder="例如：330" @focus="setGuide('arms')" />
                    <CustomInput label="小臂长" v-model="formData.shortArmLength" unit="mm" placeholder="例如：280" @focus="setGuide('arms')" />
                  </div>
                  <div class="grid-2">
                    <CustomInput label="大腿长 (股骨)" v-model="formData.thighLength" unit="mm" placeholder="例如：450" @focus="setGuide('thigh')" />
                    <CustomInput label="小腿长 (胫骨)" v-model="formData.calfLength" unit="mm" placeholder="例如：420" @focus="setGuide('calf')" />
                  </div>
                </div>
              </div>
              <div class="guide-area">
                <GuideDisplay :guideKey="activeGuide" />
              </div>
            </div>
          </div>

          <!-- PHASE 3: 柔韧性测试 (FLEX) -->
          <div v-if="currentStep === 3" :key="'step3'" class="step-content fade-in">
            <div class="split-layout">
              <div class="form-area">
                <h3 class="step-title">03 柔韧性评估</h3>
                <p class="step-desc">这决定了您能耐受多大的落差（Drop）。</p>
                <div class="input-grid">
                  <CustomInput label="胸椎旋转角度" v-model="formData.thoracicRotationAngle" unit="°" placeholder="例如：45" @focus="setGuide('thoracic_rot')" />
                  <CustomInput label="胸椎伸展角度" v-model="formData.thoracicExtensionAngle" unit="°" placeholder="例如：20" @focus="setGuide('thoracic_ext')" />
                  <CustomInput label="举臂角度" v-model="formData.armRaiseAngle" unit="°" placeholder="例如：165" @focus="setGuide('arm_raise')" />
                  <div class="zen-form-item" @click="setGuide('pain_level')">
                    <div class="label-with-value">
                      <label class="input-label">痛感等级 (0-5)</label>
                      <span class="current-value-badge" :class="{ 'is-null': formData.thoracicPainLevel === null }">{{ formData.thoracicPainLevel ?? '-' }}</span>
                    </div>
                    <input type="range" min="0" max="5" 
                      :value="formData.thoracicPainLevel ?? 0" 
                      @input="formData.thoracicPainLevel = Number($event.target.value)"
                      class="zen-range" :class="{ 'is-null': formData.thoracicPainLevel === null }">
                    <div class="range-labels"><span>无痛</span><span>剧烈</span></div>
                  </div>
                </div>
              </div>
              <div class="guide-area">
                <GuideDisplay :guideKey="activeGuide" />
              </div>
            </div>
          </div>

          <!-- PHASE 4: 稳定性评估 (STABILITY) -->
          <div v-if="currentStep === 4" :key="'step4'" class="step-content fade-in">
            <div class="split-layout">
              <div class="form-area">
                <h3 class="step-title">04 核心稳定性</h3>
                <p class="step-desc">最后的评估项，帮助我们优化您的操控组件。</p>
                <div class="input-grid">
                  <div class="zen-form-item" @click="setGuide('squat')">
                    <label class="input-label">深蹲稳定性评分 (1-5)</label>
                    <div class="score-selector">
                      <button v-for="i in 5" :key="i" type="button" :class="{active: formData.squatStabilityScore === i}" @click="formData.squatStabilityScore = i">{{ i }}</button>
                    </div>
                  </div>
                  <CustomInput label="最高深蹲深度" v-model="formData.squatDepth" unit="cm" placeholder="例如：-15" @focus="setGuide('squat')" />
                </div>
              </div>
              <div class="guide-area">
                <GuideDisplay :guideKey="activeGuide" />
              </div>
            </div>
          </div>

          <!-- 导航按钮 -->
          <div class="lab-actions">
            <button v-if="currentStep > 1" type="button" class="btn btn-outline" @click="currentStep--">上一步</button>
            <button v-if="currentStep < 4" type="button" class="btn btn-primary" @click="handleNext">继续</button>
            <button v-else type="button" class="btn btn-primary btn-glow" :disabled="loading" @click.stop="submitProfile">
              {{ loading ? '解析中...' : '生成身体图谱' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 弹窗放在 form 之外防止事件冒泡导致闪退 -->
    <ResultModal v-show="showResult" :visible="showResult" :profile="profileResult" @close="showResult = false" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import CustomInput from '@/components/lab/CustomInput.vue';
import GuideDisplay from '@/components/lab/GuideDisplay.vue';
import ResultModal from '@/components/lab/ResultModal.vue';
import { useUserStore } from '@/stores/user.js';
import { showToast } from '@/utils/toast.js';
import { saveUserBody, getLatestUserBody } from '@/api/userBody.js';

const userStore = useUserStore();
const route = useRoute();
const currentStep = ref(Number(route.query.step) || 1);
const activeGuide = ref('height');
const loading = ref(false);
const initLoading = ref(true); // 初始加载状态
const showResult = ref(false);
const profileResult = ref(null);

const userHasGender = ref(false);
const userHasBirthday = ref(false);

const formData = reactive({
  userId: null,
  birthday: null,
  gender: 0,
  height: null,
  spanHeight: null,
  breastHeight: null,
  shoulderWidth: null,
  ischialDistance: null,
  footLength: null,
  longArmLength: null,
  shortArmLength: null,
  thighLength: null,
  calfLength: null,
  armRaiseAngle: null,
  thoracicRotationAngle: null,
  thoracicExtensionAngle: null,
  thoracicPainLevel: null,
  squatDepth: null,
  squatStabilityScore: null
});

/**
 * 初始化逻辑：加载用户信息及最新的历史档案
 */
onMounted(async () => {
  const user = userStore.userInfo;
  if (!user) {
    showToast('用户信息未识别，请重新登录', 'error');
    initLoading.value = false;
    return;
  }

  formData.userId = user.id;
  if (user.gender) {
    formData.gender = user.gender;
    userHasGender.value = true;
  }
  if (user.birthday) {
    formData.birthday = user.birthday;
    userHasBirthday.value = true;
  }

  try {
    // 调用接口获取最近一次记录，实现回显
    const res = await getLatestUserBody(user.id);
    const latestData = res.data || res;

    if (latestData && latestData.id) {
      Object.keys(formData).forEach(key => {
        if (latestData[key] !== undefined && latestData[key] !== null && key !== 'userId') {
          formData[key] = latestData[key];
        }
      });
      showToast('已为您加载最新的身体档案', 'success');
    }
  } catch (error) {
    console.log("首次录入，无历史档案");
  } finally {
    initLoading.value = false;
  }
});

const setGuide = (key) => {
  activeGuide.value = key;
};

/**
 * 分步验证逻辑
 */
const validateStep = (step) => {
  const data = formData;
  if (step === 1) {
    if (!userHasGender.value && !data.gender) {
      showToast('请选择性别', 'warning');
      return false;
    }
    if (!userHasBirthday.value && !data.birthday) {
      showToast('请填写出生日期', 'warning');
      return false;
    }
    // 出生日期校验
    if (!userHasBirthday.value && data.birthday) {
      const birthDate = new Date(data.birthday);
      const today = new Date();
      if (birthDate > today) {
        showToast('出生日期不能晚于今天', 'warning');
        return false;
      }
      const age = today.getFullYear() - birthDate.getFullYear();
      if (age < 5 || age > 100) {
        showToast('出生日期异常：请确认年份是否正确 (5-100岁)', 'warning');
        return false;
      }
    }
    if (!data.height || !data.spanHeight) {
      showToast('基础数据(身高/跨高)不可为空', 'warning');
      return false;
    }
    if (data.height < 500) {
      showToast('数值异常：请确认单位为毫米(mm)', 'warning');
      return false;
    }
    if (Number(data.height) <= Number(data.spanHeight)) {
      showToast('逻辑错误：身高必须大于跨高', 'warning');
      return false;
    }
  }
  return true;
};

const handleNext = () => {
  if (validateStep(currentStep.value)) {
    currentStep.value++;
    // 切换步骤时根据当前步骤设置默认指引
    const stepGuides = { 2: 'shoulder', 3: 'thoracic_rot', 4: 'squat' };
    if (stepGuides[currentStep.value]) activeGuide.value = stepGuides[currentStep.value];
  }
};

/**
 * 核心提交 logic
 */
const submitProfile = async () => {
  if (loading.value) return;
  loading.value = true;
  
  try {
    const res = await saveUserBody(formData);
    // 确保数据先赋值
    profileResult.value = res.data || res;
    
    // 如果是第一次填性别和生日，则静默更新本地用户信息以免下次再弹
    if (!userHasGender.value || !userHasBirthday.value) {
      const updatedUser = { ...userStore.userInfo };
      if (!userHasGender.value) updatedUser.gender = formData.gender;
      if (!userHasBirthday.value) updatedUser.birthday = formData.birthday;
      userStore.setUserInfo(updatedUser);
    }
    
    // 增加延迟至 500ms，确保用户手指/鼠标完全离开提交按钮且浏览器事件流完全消化
    setTimeout(() => {
      showResult.value = true;
      showToast('身体图谱构建成功', 'success');
      loading.value = false; // 延迟结束后再恢复按钮状态，防止在此期间二次操作
    }, 500);
    
  } catch (error) {
    console.error('Fitting Error:', error);
    showToast(error.message || '系统繁忙，请稍后再试', 'error');
    loading.value = false;
  }
};
</script>

<style scoped>
.body-profile-page {
  padding-top: var(--header-height);
  min-height: 100vh;
}

.lab-container {
  padding-top: 40px;
  padding-bottom: 80px;
}

/* Toolbar */
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.step-indicator { display: flex; gap: 12px; }
.step-dot {
  width: 36px; height: 36px; border-radius: 50%; border: 1px solid #E0E0E0;
  display: flex; align-items: center; justify-content: center; color: var(--text-muted);
  transition: all 0.5s var(--ease-out-expo); font-size: 0.9rem; background: white; font-weight: 600;
}
.step-dot.active { background: var(--primary-light); color: white; border-color: var(--primary-light); }
.step-dot.current { background: var(--primary-color); color: white; border-color: var(--primary-color); box-shadow: 0 4px 15px rgba(92, 110, 88, 0.2); transform: scale(1.1); }

.lab-content { padding: 50px 60px; min-height: 620px; display: flex; flex-direction: column; position: relative; }
.input-grid { display: grid; gap: 25px; margin-top: 30px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }

.step-title { font-family: var(--font-serif); font-size: 2rem; color: var(--text-main); margin-bottom: 8px; }
.step-desc { color: var(--text-muted); font-size: 1rem; margin-bottom: 20px; opacity: 0.8; }

.gender-selector { display: flex; gap: 15px; margin-top: 10px; }
.gender-selector button { flex: 1; padding: 14px; border: 1px solid #EDEDED; border-radius: var(--radius-sm); background: #F9F9F9; cursor: pointer; transition: 0.3s; font-weight: 600; }
.gender-selector button.active { background: var(--primary-color); color: white; border-color: var(--primary-color); box-shadow: 0 4px 12px rgba(92, 110, 88, 0.2); }

.score-selector { display: flex; gap: 10px; margin-top: 10px; }
.score-selector button { width: 48px; height: 48px; border-radius: var(--radius-sm); border: 1px solid #EDEDED; background: #F9F9F9; cursor: pointer; transition: 0.3s; font-weight: 700; }
.score-selector button.active { background: var(--primary-color); color: white; border-color: var(--primary-color); }

.lab-actions { margin-top: auto; padding-top: 50px; display: flex; gap: 20px; justify-content: flex-end; border-top: 1px solid rgba(0,0,0,0.03); }
.current-value-badge { background: var(--primary-color); color: white; padding: 2px 8px; border-radius: 10px; font-size: 0.8rem; font-weight: 700; }
.current-value-badge.is-null { background: #b0b0b0; }
.label-with-value { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.zen-range { width: 100%; accent-color: var(--primary-color); cursor: pointer; height: 6px; }
.zen-range.is-null { accent-color: #b0b0b0; }
.range-labels { display: flex; justify-content: space-between; font-size: 0.8rem; color: var(--text-muted); margin-top: 10px; }

.date-input {
  width: 100%; border: 1px solid #E0E0E0; border-radius: var(--radius-sm);
  padding: 12px 15px; background: #FAFAFA; transition: all 0.3s var(--ease-out-expo);
  box-sizing: border-box;
}
.date-input:focus {
  border-color: var(--primary-color); background: #FFF;
  box-shadow: 0 0 0 3px rgba(92, 110, 88, 0.1);
}

@media (max-width: 1024px) {
  .split-layout { flex-direction: column; gap: 40px; }
  .lab-content { padding: 30px; }
}
</style>