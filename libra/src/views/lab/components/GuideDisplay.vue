<template>
  <div class="guide-display glass-card">
    <transition name="fade-slide" mode="out-in">
      <!-- 这里的 :key 确保了数据切换时触发过渡动画 -->
      <div :key="currentData.title" class="guide-content">
        <div class="image-placeholder">
          <!-- 实际项目中替换为真实 SVG 路径 -->
          <img :src="currentData.image" :alt="currentData.title" class="guide-illustration" />
          <div class="focus-ring"></div>
        </div>
        <div class="text-wrapper">
          <h4 class="guide-title">{{ currentData.title }}</h4>
          <p class="guide-desc">{{ currentData.desc }}</p>
          <ul class="guide-tips">
            <li v-for="tip in currentData.tips" :key="tip">{{ tip }}</li>
          </ul>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps(['guideKey']);

// 完整的指引数据字典
const guides = {
  // 步骤1 默认指引
  step1_default: {
    title: '个人及躯干基准',
    desc: '这些数据决定了您的整体车架尺码区间。请根据指引依次完成测量。',
    image: '/image/fitting/01.png',
    tips: []
  },
  height: {
    title: '身高 / Height',
    desc: '人体垂直高度基准，决定车架的整体尺寸范围。',
    image: '/image/fitting/height.png',
    tips: ['脱鞋并靠墙站立', '保持双脚并拢']
  },
  spanHeight: {
    title: '跨高 / Inseam',
    desc: '决定座垫高度（Saddle Height）的最核心数据。',
    image: '/image/fitting/inseam.png',
    tips: ['双脚分开15cm', '用书本用力顶住裆部', '测量书本上沿至地面距离']
  },
  breastHeight: {
    title: '胸高 / Sternal Notch',
    desc: '用于计算躯干长度，决定车把的前伸量（Reach）。',
    image: '/image/fitting/chest height.png',
    tips: ['平视前方', '定位锁骨中间的V形凹陷点']
  },
  shoulder: {
    title: '肩宽 / Shoulder Width',
    desc: '决定弯把或平把的宽度建议。',
    image: '/image/fitting/shoulder.png',
    tips: ['触摸肩膀两侧外缘最突出的骨点', '测量两点间的水平直线距离']
  },
  ischial: {
    title: '坐骨间距 / Ischial',
    desc: '决定座垫宽度的解剖学数据，避免压迫。',
    image: '/image/fitting/shoulder.png',
    tips: ['坐在纸板或记忆海绵上', '测量两个压痕中心点的距离']
  },
  arms: {
    title: '手臂长度 / Arm Length',
    desc: '影响上半身在车上的延展性和舒适度。',
    image: '/image/fitting/smallarm.png',
    tips: ['测量肩峰至肘部', '测量肘部至手掌虎口']
  },
  // 步骤2 默认指引
  step2_default: {
    title: '肢体杠杆',
    desc: '精确到毫米的肢体数据是计算 Stack & Reach 的核心。',
    image: '/image/fitting/02.png',
    tips: []
  },
  thigh: {
    title: '大腿长 / Femur',
    desc: '决定座垫的前后偏移量（Setback）。',
    image: '/image/fitting/bigleg.png',
    tips: ['从髋关节大转子量至膝关节外侧间隙']
  },
  calf: {
    title: '小腿长 / Tibia',
    desc: '辅助修正座高，优化踩踏画圆的流畅性。',
    image: '/image/fitting/smallleg.png',
    tips: ['从膝关节间隙量至内踝骨点']
  },
  // 步骤3 默认指引
  step3_default: {
    title: '柔韧性评估',
    desc: '这决定了您能耐受多大的落差（Drop）。',
    image: '/image/fitting/03.png',
    tips: []
  },
  thoracic_rot: {
    title: '胸椎旋转 / Rotation',
    desc: '评估上半身的转向能力。',
    image: '/image/fitting/height.png',
    tips: ['坐姿挺直，缓慢旋转至极限', '保持臀部不要离开座位']
  },
  thoracic_ext: {
    title: '胸椎伸展 / Extension',
    desc: '决定了您能耐受低趴姿态的时间。',
    image: '/image/fitting/height.png',
    tips: ['双手抱头，胸部向上向后舒展']
  },
  arm_raise: {
    title: '举臂角度 / Shoulder Flexion',
    desc: '预防肩颈代偿性酸痛。',
    image: '/image/fitting/bigarm.png',
    tips: ['手臂伸直上举，观察与垂直线的夹角']
  },
  pain_level: {
    title: '痛感评估 / Pain Assessment',
    desc: '主观感受是评估神经张力与灵活性的重要指标。',
    image: '/image/fitting/height.png',
    tips: ['0级：完全无感，轻松完成动作', '1-2级：微弱拉伸感或紧绷', '3级：明显不适，但可维持', '4-5级：由于刺痛或剧痛无法继续']
  },
  // 步骤4 默认指引
  step4_default: {
    title: '核心稳定性',
    desc: '最后的评估项，帮助我们优化您的操控组件。',
    image: '/image/fitting/04.png',
    tips: []
  },
  squat: {
    title: '深蹲稳定性 / Squat Stability',
    desc: '评估下肢动态稳定性和本体感觉。',
    image: '/image/fitting/04.png',
    tips: ['1分：极度摇晃，无法维持身体平衡', '3分：中等稳定，有轻微晃动', '5分：完美控制，核心稳固如山']
  },
  plank: {
    title: '平板支撑 / Plank',
    desc: '评估核心肌群的耐力与稳定性。',
    image: '/image/fitting/04.png',
    tips: ['< 20秒 = 1分', '20-40秒 = 2分', '40-60秒 = 3分', '60-90秒 = 4分', '> 90秒 = 5分']
  },
  stability: {
    title: '稳定性评分 / Stability',
    desc: '核心控制能力直接决定踩踏效率及长途舒适度。',
    image: '/image/fitting/height.png',
    tips: ['1分：极度摇晃，无法维持身体平衡', '3分：中等稳定，有轻微晃动', '5分：完美控制，核心稳固如山']
  }
};

const currentData = computed(() => guides[props.guideKey] || guides.height);
</script>

<style scoped>
.guide-display { 
  padding: 40px; 
  background: white; 
  border: 1px solid rgba(0,0,0,0.03); 
  height: 100%; 
  display: flex; 
  flex-direction: column; 
  box-shadow: var(--shadow-sm);
}

.image-placeholder {
  flex: 1; 
  min-height: 240px; 
  background: #FDFDFD; 
  border-radius: var(--radius-sm); 
  margin-bottom: 30px;
  display: flex; 
  align-items: center; 
  justify-content: center; 
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(0,0,0,0.02);
}

.guide-illustration { 
  max-height: 220px; 
  opacity: 0.9; 
  mix-blend-mode: multiply; 
  transition: transform 0.6s var(--ease-out-expo);
}

.image-placeholder:hover .guide-illustration {
  transform: scale(1.05);
}

.focus-ring {
  position: absolute;
  width: 100%; height: 100%;
  border: 2px solid var(--primary-color);
  border-radius: var(--radius-sm);
  opacity: 0.1;
  pointer-events: none;
}

.text-wrapper { text-align: left; }
.guide-title { 
  font-size: 1.4rem; 
  color: var(--text-main); 
  margin-bottom: 15px; 
  font-weight: 700; 
  font-family: var(--font-serif);
}
.guide-desc { 
  font-size: 1rem; 
  color: var(--text-body); 
  margin-bottom: 20px; 
  line-height: 1.6; 
}
.guide-tips { padding-left: 0; }
.guide-tips li { 
  font-size: 0.85rem; 
  color: var(--text-muted); 
  margin-bottom: 10px; 
  display: flex;
  align-items: center;
  gap: 10px;
}
.guide-tips li::before {
  content: '→';
  color: var(--accent-gold);
  font-weight: bold;
}

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.5s var(--ease-out-expo); }
.fade-slide-enter-from { opacity: 0; transform: translateX(30px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-30px); }
</style>