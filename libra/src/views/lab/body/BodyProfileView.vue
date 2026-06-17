-<template>
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
                    <input type="date" v-model="formData.birthday" class="zen-field date-input" min="1920-01-01" :max="todayDate" />
                  </div>
                  <CustomInput label="体重 (Weight) *" v-model="formData.weightKg" unit="kg" placeholder="例如：65" :warning="fieldWarnings.weightKg" @focus="setGuide('weight')" />
                  <CustomInput label="身高 (Height) *" v-model="formData.height" unit="mm" placeholder="例如：1750" :warning="fieldWarnings.height" @focus="setGuide('height')" />
                  <CustomInput label="跨高 (Inseam) *" v-model="formData.spanHeight" unit="mm" placeholder="例如：820" :warning="fieldWarnings.spanHeight" @focus="setGuide('spanHeight')" />
                  <div class="grid-2">
                    <CustomInput label="腰围 (Waist)" v-model="formData.waistCircumference" unit="mm" placeholder="例如：780" :warning="fieldWarnings.waistCircumference" @focus="setGuide('waist')" />
                    <CustomInput label="胸高 (Sternal Notch)" v-model="formData.breastHeight" unit="mm" placeholder="例如：1450" :warning="fieldWarnings.breastHeight" @focus="setGuide('breastHeight')" />
                  </div>
                  <div class="grid-2">
                    <CustomInput label="肩宽" v-model="formData.shoulderWidth" unit="mm" placeholder="例如：420" :warning="fieldWarnings.shoulderWidth" @focus="setGuide('shoulder')" />
                    <CustomInput label="坐骨间距" v-model="formData.ischialDistance" unit="mm" placeholder="例如：120" :warning="fieldWarnings.ischialDistance" @focus="setGuide('ischial')" />
                  </div>
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
                  <div class="grid-2">
                    <CustomInput label="大臂长" v-model="formData.longArmLength" unit="mm" placeholder="例如：330" :warning="fieldWarnings.longArmLength" @focus="setGuide('bigarms')" />
                    <CustomInput label="小臂长" v-model="formData.shortArmLength" unit="mm" placeholder="例如：280" :warning="fieldWarnings.shortArmLength" @focus="setGuide('smallarms')" />
                  </div>
                  <div class="grid-2">
                    <CustomInput label="大腿长 (股骨)" v-model="formData.thighLength" unit="mm" placeholder="例如：450" :warning="fieldWarnings.thighLength" @focus="setGuide('thigh')" />
                    <CustomInput label="小腿长 (胫骨)" v-model="formData.calfLength" unit="mm" placeholder="例如：420" :warning="fieldWarnings.calfLength" @focus="setGuide('calf')" />
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
                  <div class="zen-form-item" @click="setGuide('thoracic_rot')" :class="{'has-warning': stepValidationAttempted[3] && !hasInputValue(formData.thoracicRotationAngle)}">
                    <label class="input-label">坐姿胸椎旋转 *</label>
                    <div class="angle-selector">
                      <div class="angle-option" :class="{active: formData.thoracicRotationAngle === 85}" @click="formData.thoracicRotationAngle = 85">
                        <span class="angle-circle"></span>
                        <span class="angle-text">可以轻松转到侧面甚至略微超过</span>
                      </div>
                      <div class="angle-option" :class="{active: formData.thoracicRotationAngle === 70}" @click="formData.thoracicRotationAngle = 70">
                        <span class="angle-circle"></span>
                        <span class="angle-text">可以明显转动，但还看不到身体正侧面</span>
                      </div>
                      <div class="angle-option" :class="{active: formData.thoracicRotationAngle === 50}" @click="formData.thoracicRotationAngle = 50">
                        <span class="angle-circle"></span>
                        <span class="angle-text">只能稍微转一点点，幅度比较小</span>
                      </div>
                      <div class="angle-option" :class="{active: formData.thoracicRotationAngle === 30}" @click="formData.thoracicRotationAngle = 30">
                        <span class="angle-circle"></span>
                        <span class="angle-text">几乎转不动，感觉很僵硬</span>
                      </div>
                    </div>
                    <p v-if="stepValidationAttempted[3] && !hasInputValue(formData.thoracicRotationAngle)" class="field-warning">请选择坐姿胸椎旋转角度</p>
                  </div>
                  <div class="zen-form-item" @click="setGuide('arm_raise')" :class="{'has-warning': stepValidationAttempted[3] && !hasInputValue(formData.armRaiseAngle)}">
                    <label class="input-label">举臂角度 *</label>
                    <div class="angle-selector">
                      <div class="angle-option" :class="{active: formData.armRaiseAngle === 175}" @click="formData.armRaiseAngle = 175">
                        <span class="angle-circle"></span>
                        <span class="angle-text">手臂可轻松举过头顶，贴近耳朵</span>
                      </div>
                      <div class="angle-option" :class="{active: formData.armRaiseAngle === 155}" @click="formData.armRaiseAngle = 155">
                        <span class="angle-circle"></span>
                        <span class="angle-text">手臂可举过头顶，但无法贴近耳朵 / 有明显紧张</span>
                      </div>
                      <div class="angle-option" :class="{active: formData.armRaiseAngle === 120}" @click="formData.armRaiseAngle = 120">
                        <span class="angle-circle"></span>
                        <span class="angle-text">手臂最高只能到肩膀附近（接近T字）</span>
                      </div>
                      <div class="angle-option" :class="{active: formData.armRaiseAngle === 80}" @click="formData.armRaiseAngle = 80">
                        <span class="angle-circle"></span>
                        <span class="angle-text">手臂无法抬到肩膀高度</span>
                      </div>
                    </div>
                    <p v-if="stepValidationAttempted[3] && !hasInputValue(formData.armRaiseAngle)" class="field-warning">请选择举臂角度</p>
                  </div>
                  <div class="zen-form-item" @click="setGuide('pain_level')" :class="{'has-warning': stepValidationAttempted[3] && !hasInputValue(formData.thoracicPainLevel)}">
                    <label class="input-label">痛感等级 *</label>
                    <div class="pain-selector">
                      <div class="pain-option" :class="{active: formData.thoracicPainLevel === 0}" @click="formData.thoracicPainLevel = 0">
                        <span class="pain-num">0</span>
                        <span class="pain-label">无痛</span>
                      </div>
                      <div class="pain-option" :class="{active: formData.thoracicPainLevel === 1}" @click="formData.thoracicPainLevel = 1">
                        <span class="pain-num">1</span>
                        <span class="pain-label">轻微</span>
                      </div>
                      <div class="pain-option" :class="{active: formData.thoracicPainLevel === 2}" @click="formData.thoracicPainLevel = 2">
                        <span class="pain-num">2</span>
                        <span class="pain-label">轻度</span>
                      </div>
                      <div class="pain-option" :class="{active: formData.thoracicPainLevel === 3}" @click="formData.thoracicPainLevel = 3">
                        <span class="pain-num">3</span>
                        <span class="pain-label">中度</span>
                      </div>
                      <div class="pain-option" :class="{active: formData.thoracicPainLevel === 4}" @click="formData.thoracicPainLevel = 4">
                        <span class="pain-num">4</span>
                        <span class="pain-label">重度</span>
                      </div>
                      <div class="pain-option" :class="{active: formData.thoracicPainLevel === 5}" @click="formData.thoracicPainLevel = 5">
                        <span class="pain-num">5</span>
                        <span class="pain-label">剧烈</span>
                      </div>
                    </div>
                    <p v-if="stepValidationAttempted[3] && !hasInputValue(formData.thoracicPainLevel)" class="field-warning">请选择痛感等级</p>
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
                  <div class="zen-form-item" @click="setGuide('squat')" :class="{'has-warning': stepValidationAttempted[4] && !hasInputValue(formData.squatStabilityScore)}">
                    <label class="input-label">深蹲稳定性评分 (1-5) *</label>
                    <div class="squat-selector">
                      <div class="squat-option" :class="{active: formData.squatStabilityScore === 1}" @click="formData.squatStabilityScore = 1">
                        <span class="squat-num">1</span>
                        <span class="squat-label">无法完成</span>
                      </div>
                      <div class="squat-option" :class="{active: formData.squatStabilityScore === 2}" @click="formData.squatStabilityScore = 2">
                        <span class="squat-num">2</span>
                        <span class="squat-label">晃动明显</span>
                      </div>
                      <div class="squat-option" :class="{active: formData.squatStabilityScore === 3}" @click="formData.squatStabilityScore = 3">
                        <span class="squat-num">3</span>
                        <span class="squat-label">轻微晃动</span>
                      </div>
                      <div class="squat-option" :class="{active: formData.squatStabilityScore === 4}" @click="formData.squatStabilityScore = 4">
                        <span class="squat-num">4</span>
                        <span class="squat-label">整体稳定</span>
                      </div>
                      <div class="squat-option" :class="{active: formData.squatStabilityScore === 5}" @click="formData.squatStabilityScore = 5">
                        <span class="squat-num">5</span>
                        <span class="squat-label">动作流畅</span>
                      </div>
                    </div>
                    <p v-if="stepValidationAttempted[4] && !hasInputValue(formData.squatStabilityScore)" class="field-warning">请选择深蹲稳定性评分</p>
                  </div>
                  <div class="zen-form-item" @click="setGuide('plank')" :class="{'has-warning': stepValidationAttempted[4] && !hasInputValue(formData.plankScore)}">
                    <label class="input-label">平板支撑评分 (1-5) *</label>
                    <div class="plank-selector">
                      <div class="plank-option" :class="{active: formData.plankScore === 1}" @click="formData.plankScore = 1">
                        <span class="plank-num">1</span>
                        <span class="plank-label">&lt; 20s</span>
                      </div>
                      <div class="plank-option" :class="{active: formData.plankScore === 2}" @click="formData.plankScore = 2">
                        <span class="plank-num">2</span>
                        <span class="plank-label">20-40s</span>
                      </div>
                      <div class="plank-option" :class="{active: formData.plankScore === 3}" @click="formData.plankScore = 3">
                        <span class="plank-num">3</span>
                        <span class="plank-label">40-60s</span>
                      </div>
                      <div class="plank-option" :class="{active: formData.plankScore === 4}" @click="formData.plankScore = 4">
                        <span class="plank-num">4</span>
                        <span class="plank-label">60-90s</span>
                      </div>
                      <div class="plank-option" :class="{active: formData.plankScore === 5}" @click="formData.plankScore = 5">
                        <span class="plank-num">5</span>
                        <span class="plank-label">&gt; 90s</span>
                      </div>
                    </div>
                    <p v-if="stepValidationAttempted[4] && !hasInputValue(formData.plankScore)" class="field-warning">请选择平板支撑评分</p>
                  </div>
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
            <button v-if="hasRecommendState && currentStep < 4" type="button" class="btn btn-outline" @click="goBackToRecommend">
              <i class="fas fa-arrow-left"></i>
              <span>返回智能推荐</span>
            </button>
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
import { ref, reactive, onMounted, nextTick, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CustomInput from '@/views/lab/components/CustomInput.vue';
import GuideDisplay from '@/views/lab/components/GuideDisplay.vue';
import ResultModal from '@/views/lab/components/ResultModal.vue';
import { useUserStore } from '@/stores/user.js';
import { showToast } from '@/utils/toast.js';
import { saveUserBody, getLatestUserBody } from '@/api/userBody.js';

const userStore = useUserStore();
const route = useRoute();
const router = useRouter();
const currentStep = ref(Number(route.query.step) || 1);
const activeGuide = ref('body_base');
const loading = ref(false);
const initLoading = ref(true); // 初始加载状态
const showResult = ref(false);
const profileResult = ref(null);
const stepValidationAttempted = reactive({
  1: false,
  2: false,
  3: false,
  4: false
});

// 是否有智能推荐的返回状态
const hasRecommendState = computed(() => {
  return sessionStorage.getItem('showroomRecommendModal') !== null;
});

const userHasGender = ref(false);
const userHasBirthday = ref(false);

// 今天的日期，用于限制出生日期选择器
const todayDate = computed(() => new Date().toISOString().split('T')[0]);

const formData = reactive({
  userId: null,
  birthday: null,
  gender: 0,
  height: null,
  spanHeight: null,
  weightKg: null,
  waistCircumference: null,
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
  thoracicPainLevel: null,
  squatStabilityScore: null,
  plankScore: null
});

const hasInputValue = (value) => value !== null && value !== undefined && value !== '';
const shouldShowRequiredWarning = (field) => stepValidationAttempted[1] && !hasInputValue(formData[field]);
const toNumber = (value) => Number(value);

const numericSafetyRules = {
  weightKg: { label: '体重', min: 30, max: 250, unit: 'kg', required: true },
  height: { label: '身高', min: 1200, max: 2200, unit: 'mm', required: true },
  spanHeight: { label: '跨高', min: 600, max: 1100, unit: 'mm', required: true },
  waistCircumference: { label: '腰围', min: 400, max: 1800, unit: 'mm' },
  breastHeight: { label: '胸高', min: 900, max: 1800, unit: 'mm' },
  shoulderWidth: { label: '肩宽', min: 200, max: 600, unit: 'mm' },
  ischialDistance: { label: '坐骨间距', min: 50, max: 200, unit: 'mm' },
  longArmLength: { label: '大臂长', min: 180, max: 500, unit: 'mm' },
  shortArmLength: { label: '小臂长', min: 160, max: 450, unit: 'mm' },
  thighLength: { label: '大腿长', min: 250, max: 650, unit: 'mm' },
  calfLength: { label: '小腿长', min: 250, max: 650, unit: 'mm' }
};

const numberWarning = (field) => {
  const rule = numericSafetyRules[field];
  const value = formData[field];
  if (!rule) return '';
  if (rule.required && shouldShowRequiredWarning(field)) {
    return `${rule.label}为必填项，请填写${rule.unit}数值。`;
  }
  if (!hasInputValue(value)) return '';

  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return `${rule.label}必须为有效数字，请重新输入。`;
  }
  if (numericValue < rule.min || numericValue > rule.max) {
    return `${rule.label}超出正常录入范围，请确认单位为 ${rule.unit}，正常范围 ${rule.min}-${rule.max}${rule.unit}。`;
  }
  return '';
};

const fieldWarnings = computed(() => {
  const height = hasInputValue(formData.height) ? toNumber(formData.height) : null;
  const spanHeight = hasInputValue(formData.spanHeight) ? toNumber(formData.spanHeight) : null;

  const warnings = {
    height: '',
    spanHeight: '',
    weightKg: '',
    waistCircumference: '',
    breastHeight: '',
    shoulderWidth: '',
    ischialDistance: '',
    longArmLength: '',
    shortArmLength: '',
    thighLength: '',
    calfLength: ''
  };

  Object.keys(warnings).forEach(field => {
    warnings[field] = numberWarning(field);
  });

  if (!warnings.spanHeight && height !== null && spanHeight !== null && height <= spanHeight) {
    warnings.spanHeight = '跨高通常应小于身高，请重新核对两项数据。';
  }

  const breastHeight = hasInputValue(formData.breastHeight) ? toNumber(formData.breastHeight) : null;
  if (!warnings.breastHeight && breastHeight !== null && spanHeight !== null && breastHeight <= spanHeight) {
    warnings.breastHeight = '胸高应大于跨高，否则无法计算躯干长度，请重新核对。';
  }

  const longArmLength = hasInputValue(formData.longArmLength) ? toNumber(formData.longArmLength) : null;
  const shortArmLength = hasInputValue(formData.shortArmLength) ? toNumber(formData.shortArmLength) : null;
  if (!warnings.longArmLength && !warnings.shortArmLength && longArmLength !== null && shortArmLength !== null) {
    const armRatio = longArmLength / shortArmLength;
    if (armRatio < 0.7 || armRatio > 1.5) {
      warnings.shortArmLength = '大臂与小臂比例偏离常规，请确认测量点是否准确。';
    }
  }

  const thighLength = hasInputValue(formData.thighLength) ? toNumber(formData.thighLength) : null;
  const calfLength = hasInputValue(formData.calfLength) ? toNumber(formData.calfLength) : null;
  if (!warnings.thighLength && !warnings.calfLength && thighLength !== null && calfLength !== null) {
    const legRatio = thighLength / calfLength;
    if (legRatio < 0.7 || legRatio > 1.5) {
      warnings.calfLength = '大腿与小腿比例偏离常规，请确认测量点是否准确。';
    }
  }

  return warnings;
});

const getSafetyWarning = (field) => fieldWarnings.value[field] || '';

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

// 返回智能推荐页面
const goBackToRecommend = async () => {
  // 如果用户填写了基础身体档案，先保存数据
  if (formData.weightKg || formData.height || formData.spanHeight) {
    try {
      await saveUserBody(formData);
      userStore.setUserInfo({
        ...userStore.userInfo,
        weightKg: formData.weightKg,
        height: formData.height,
        spanHeight: formData.spanHeight
      });
    } catch (error) {
      console.error('保存身体信息失败', error);
      // 保存失败也继续返回，不阻塞用户操作
    }
  }
  // 返回单车展厅页面（状态恢复由 ShowroomView 的 onMounted 处理）
  router.push('/showroom');
};

// 步骤默认指引映射
const stepDefaultGuides = {
  1: 'body_base',
  2: 'step2_default',
  3: 'step3_default',
  4: 'step4_default'
};

// 监听步骤变化，自动更新指引
watch(currentStep, (newStep) => {
  if (stepDefaultGuides[newStep]) {
    activeGuide.value = stepDefaultGuides[newStep];
  }
});

// 监听路由参数变化，响应外部跳转
watch(() => route.query.step, (newStep) => {
  if (newStep) {
    currentStep.value = Number(newStep);
  }
});

/**
 * 分步验证逻辑
 */
const validateStep = (step) => {
  const data = formData;
  if (step === 1) {
    stepValidationAttempted[1] = true;
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
    const step1Warning = [
      'weightKg',
      'height',
      'spanHeight',
      'waistCircumference',
      'breastHeight',
      'shoulderWidth',
      'ischialDistance'
    ].map(getSafetyWarning).find(Boolean);
    if (step1Warning) {
      showToast(step1Warning, 'warning');
      return false;
    }
  }
  if (step === 2) {
    stepValidationAttempted[2] = true;
    const step2Warning = [
      'longArmLength',
      'shortArmLength',
      'thighLength',
      'calfLength'
    ].map(getSafetyWarning).find(Boolean);
    if (step2Warning) {
      showToast(step2Warning, 'warning');
      return false;
    }
  }
  if (step === 3) {
    stepValidationAttempted[3] = true;
    if (!hasInputValue(formData.thoracicRotationAngle)) {
      showToast('请选择坐姿胸椎旋转角度', 'warning');
      return false;
    }
    if (!hasInputValue(formData.armRaiseAngle)) {
      showToast('请选择举臂角度', 'warning');
      return false;
    }
    if (!hasInputValue(formData.thoracicPainLevel)) {
      showToast('请选择痛感等级', 'warning');
      return false;
    }
  }
  if (step === 4) {
    stepValidationAttempted[4] = true;
    if (!hasInputValue(formData.squatStabilityScore)) {
      showToast('请选择深蹲稳定性评分', 'warning');
      return false;
    }
    if (!hasInputValue(formData.plankScore)) {
      showToast('请选择平板支撑评分', 'warning');
      return false;
    }
  }
  return true;
};

const handleNext = () => {
  if (validateStep(currentStep.value)) {
    currentStep.value++;
    // 指引更新由 watch 自动处理
  }
};

/**
 * 核心提交 logic
 */
const submitProfile = async () => {
  if (loading.value) return;
  for (const step of [1, 2, 3, 4]) {
    if (!validateStep(step)) return;
  }
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

/* Angle Selector */
.angle-selector { display: flex; flex-direction: column; gap: 12px; margin-top: 10px; }
.angle-selector--grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.angle-option {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px 16px;
  border: 1px solid #EDEDED;
  border-radius: var(--radius-sm);
  background: #F9F9F9;
  cursor: pointer;
  transition: all 0.3s ease;
}
.angle-option:hover { border-color: #D0D0D0; }
.angle-option.active {
  border-color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
}
.angle-circle {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #B0B0B0;
  transition: all 0.3s ease;
  flex-shrink: 0;
  margin-top: 2px;
}
.angle-option.active .angle-circle {
  background: var(--primary-color);
}
.angle-text {
  font-size: 0.9rem;
  line-height: 1.4;
  color: var(--text-main);
}

/* Pain Selector */
.pain-selector { display: flex; gap: 10px; margin-top: 10px; }
.pain-option {
  flex: 1;
  display: flex; flex-direction: column; align-items: center;
  padding: 12px 8px;
  border: 1px solid #EDEDED;
  border-radius: var(--radius-sm);
  background: #F9F9F9;
  cursor: pointer;
  transition: all 0.3s ease;
}
.pain-option:hover { border-color: #D0D0D0; }
.pain-option.active {
  border-color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.15);
}
.pain-num {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text-main);
}
.pain-option.active .pain-num {
  color: var(--primary-color);
}
.pain-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 4px;
}
.pain-option.active .pain-label {
  color: var(--primary-color);
}

/* Squat Selector */
.squat-selector { display: flex; gap: 10px; margin-top: 10px; }
.squat-option {
  flex: 1;
  display: flex; flex-direction: column; align-items: center;
  padding: 12px 8px;
  border: 1px solid #EDEDED;
  border-radius: var(--radius-sm);
  background: #F9F9F9;
  cursor: pointer;
  transition: all 0.3s ease;
}
.squat-option:hover { border-color: #D0D0D0; }
.squat-option.active {
  border-color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.15);
}
.squat-num {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text-main);
}
.squat-option.active .squat-num {
  color: var(--primary-color);
}
.squat-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 4px;
}
.squat-option.active .squat-label {
  color: var(--primary-color);
}

/* Plank Selector */
.plank-selector { display: flex; gap: 10px; margin-top: 10px; }
.plank-option {
  flex: 1;
  display: flex; flex-direction: column; align-items: center;
  padding: 12px 8px;
  border: 1px solid #EDEDED;
  border-radius: var(--radius-sm);
  background: #F9F9F9;
  cursor: pointer;
  transition: all 0.3s ease;
}
.plank-option:hover { border-color: #D0D0D0; }
.plank-option.active {
  border-color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.15);
}
.plank-num {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text-main);
}
.plank-option.active .plank-num {
  color: var(--primary-color);
}
.plank-label {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 4px;
}
.plank-option.active .plank-label {
  color: var(--primary-color);
}

.score-hint {
  margin-top: 8px;
  font-size: 0.8rem;
  color: var(--text-muted);
  text-align: center;
}

.date-input {
  width: 100%; border: 1px solid #E0E0E0; border-radius: var(--radius-sm);
  padding: 12px 15px; background: #FAFAFA; transition: all 0.3s var(--ease-out-expo);
  box-sizing: border-box;
}
.date-input:focus {
  border-color: var(--primary-color); background: #FFF;
  box-shadow: 0 0 0 3px rgba(92, 110, 88, 0.1);
}

/* 必填标记 */
.required-mark {
  color: #CF1322;
  font-weight: 700;
  margin-left: 2px;
}

/* 字段警告样式 */
.zen-form-item.has-warning .angle-selector,
.zen-form-item.has-warning .pain-selector,
.zen-form-item.has-warning .squat-selector,
.zen-form-item.has-warning .plank-selector {
  border: 1px solid #FF3B30;
  border-radius: var(--radius-sm);
  padding: 4px;
}

.field-warning {
  color: #CF1322;
  font-size: 0.8rem;
  margin-top: 8px;
  margin-bottom: 0;
  font-weight: 500;
}

@media (max-width: 1024px) {
  .split-layout { flex-direction: column; gap: 40px; }
  .lab-content { padding: 30px; }
}
</style>
