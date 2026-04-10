<template>
  <div class="guide-display glass-card">
    <transition name="fade-slide" mode="out-in">
      <!-- 这里的 :key 确保了数据切换时触发过渡动画 -->
      <div :key="currentData.title" class="guide-content">
        <div class="image-placeholder" :class="{ 'large-placeholder': guideKey === 'ischial' }">
          <!-- 实际项目中替换为真实 SVG 路径 -->
          <img :src="currentData.image" :alt="currentData.title" class="guide-illustration" />
          <div class="focus-ring"></div>
        </div>
        <div class="text-wrapper">
          <h4 class="guide-title">{{ currentData.title }}</h4>
          <p class="guide-desc">{{ currentData.desc }}</p>
          <ul class="guide-tips">
            <li v-for="(tip, index) in currentData.tips" :key="index">
              <template v-if="typeof tip === 'object' && tip.label">
                <div class="tip-item">
                  <span class="tip-label">{{ tip.label }}</span>
                  <span class="tip-content">{{ tip.content }}</span>
                </div>
              </template>
              <template v-else>{{ tip }}</template>
            </li>
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
    tips: [
      { label: '刚性测量工具', content: '使用金属或硬质塑料材质的立式身高尺，避免使用软尺或折叠尺' },
      { label: '测量姿势', content: '双臂自然下垂，双足并拢，身体避免前倾或屈膝，保持身体成一条垂直线' }
    ]
  },
  spanHeight: {
    title: '跨高 / Inseam',
    desc: '决定座垫高度（Saddle Height）的最核心数据。',
    image: '/image/fitting/inseam.png',
    tips:[
      { label: '准备工作', content: '受测者赤足，站立于水平地面，双脚分开约 10–15 cm（模拟骑行姿势），身体保持直立稳定。' },
      { label: '测量姿势', content: '将硬质书本置于两腿之间，书脊朝上，缓慢向上顶至裆部，施加适度压力（模拟骑行坐垫承重）。' },
      { label: '测量过程', content: '保持书本上边缘水平，用卷尺测量地面至书本上边缘的垂直距离。' },
      { label: '注意事项', content: '必须施加足够压力，否则数据偏小\n书本必须保持水平，否则误差可达 1–2 cm\n建议重复测量 2–3 次取平均值' }
    ]
  },
  breastHeight: {
    title: '胸高 / Sternal Notch',
    desc: '用于计算躯干长度，决定车把的前伸量（Reach）。',
    image: '/image/fitting/chest height.png',
    tips: [
      { label: '准备工作', content: '受测者自然站立，保持身体直立，双脚与肩同宽。' },
      { label: '测量点定位', content: '定位胸骨上切迹（锁骨之间的凹陷处）。' },
      { label: '测量过程', content: '测量地面至该点的垂直距离。' },
      { label: '注意事项', content: '不可耸肩或含胸\n测量点必须准确，否则误差较大' }
    ]
  },
  shoulder: {
    title: '肩宽 / Shoulder Width',
    desc: '决定弯把或平把的宽度建议。',
    image: '/image/fitting/shoulder.png',
    tips: [
      { label: '准备工作', content: '受测者自然站立，双臂放松下垂。' },
      { label: '测量点定位', content: '定位左右肩峰（肩关节外侧最突出骨点）。' },
      { label: '测量过程', content: '测量两肩峰之间的直线距离。' },
      { label: '注意事项', content: '避免测量到肌肉外缘\n建议由他人辅助测量' }
    ]
  },
  ischial: {
    title: '坐骨间距 / Ischial',
    desc: '决定座垫宽度的解剖学数据，避免压迫。',
    image: '/image/fitting/Ischial distance.png',
    tips: [
      { label: '准备工具', content: '硬纸板 / 瓦楞纸（A3 或更大）\n硬、平的椅子（无软垫）\n笔、直尺（毫米）\n穿薄裤 / 骑行裤（不要厚垫）' },
      { label: '测量步骤', content: '1.纸板放在硬椅上，坐正、腰背挺直、双脚抬离地面或减少支撑，使坐骨受力明显。身体前倾约 30–45（模拟骑行姿势），稍左右晃动（或双手拉住椅子边缘向上拉），让坐骨压实压出清晰凹痕，保持 5-10 秒后站起。\n2.用笔标出两个凹痕的中心点。\n3.直尺量两点距离（毫米） 就是你的坐骨宽度。' },
      { label: '注意事项', content: '必须前倾，否则测得偏小\n压痕不明显时可增加柔软层' }
    ]
  },
  bigarms: {
    title: '大臂长度 / BigArm Length',
    desc: '影响上半身在车上的延展性和舒适度。',
    image: '/image/fitting/bigarm.png',
    tips: [
      { label: '测量步骤', content: '测量点：肩峰到肘关节外侧\n手臂自然下垂，保持放松\n测量两点直线距离' },
      { label: '注意事项', content: '避免肘部弯曲' }
    ]
  },
  smallarms: {
    title: '小臂长度 / SmallArm Length',
    desc: '影响上半身在车上的延展性和舒适度。',
    image: '/image/fitting/smallarm.png',
    tips: [
      { label: '测量步骤', content: '测量点：肘关节到手腕（尺骨茎突）\n手掌自然放松' }
    ]
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
    tips: [
      { label: '测量步骤', content: '测量点：髋关节大转子  膝关节中心\n受测者自然站立' },
      { label: '注意事项', content: '大转子需通过触诊定位（大腿外侧突起骨点）' }
    ]
  },
  calf: {
    title: '小腿长 / Tibia',
    desc: '辅助修正座高，优化踩踏画圆的流畅性。',
    image: '/image/fitting/smallleg.png',
    tips: [
      { label: '测量步骤', content: '测量点：膝关节中心  外踝\n测量直线距离' }
    ]
  },
  // 步骤3 默认指引
  step3_default: {
    title: '柔韧性评估',
    desc: '这决定了您能耐受多大的落差（Drop）。',
    image: '/image/fitting/03.png',
    tips: []
  },
  thoracic_rot: {
    title: '坐姿抱胸旋转 / Seated Rotation',
    desc: '评估胸椎旋转灵活性，影响骑行时的上身姿态调整能力。',
    image: '/image/fitting/height.png',
    tips: ['坐姿挺直，双手交叉抱胸', '保持臀部不离开座位，缓慢旋转至极限', '观察肩膀转向的角度']
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
    image: '/image/fitting/raisearm.png',
    tips: [
      { label: '测量姿势', content: '受测者将手臂沿身体侧方（冠状面）缓慢向外抬起\n手掌可自然朝前或向下，保持手臂伸直\n动作持续至最大活动范围' },
      { label: '注意事项', content: '避免身体向一侧倾斜（常见代偿）\n避免耸肩（肩胛骨代偿上提）\n手臂必须保持伸直，避免肘关节弯曲\n动作需缓慢、可控，避免借助惯性' }
    ]
  },
  pain_level: {
    title: '痛感评估 / Pain Assessment',
    desc: '在完成坐姿抱胸旋转和手臂上举测试时，根据你的真实感受选择最接近的一项：',
    image: '/image/fitting/pain.png',
    tips: [
      { label: '0级', content: '完全没有不适或疼痛，动作轻松完成' },
      { label: '1级', content: '仅有轻微拉伸或牵拉感，感觉自然，无需刻意注意' },
      { label: '2级', content: '有明显的紧绷或牵拉感，开始能感知到受限，但仍可轻松完成动作' },
      { label: '3级', content: '出现明显不适或压迫感，但仍可以坚持完成动作' },
      { label: '4级', content: '出现明显疼痛或刺痛感，可以勉强完成动作，但过程中需要忍耐' },
      { label: '5级', content: '疼痛强烈，无法继续或被迫停止动作' }
    ]
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
    image: '/image/fitting/squat.png',
    tips: [
      { label: '测量姿势', content: '受测者双臂可自然前伸或置于胸前，以维持平衡\n双脚与肩同宽，脚尖自然略向外\n保持躯干直立或轻微前倾，核心收紧\n缓慢下蹲，使髋关节向后下方移动（类似"坐椅子"动作）\n下蹲过程中膝关节沿脚尖方向移动\n全程动作保持连续、平稳、可控' },
      { label: '1级', content: '明显失去控制，晃动大或无法完成标准动作' },
      { label: '2级', content: '能完成，但不稳定，晃动明显或重心不稳' },
      { label: '3级', content: '基本稳定，有轻微晃动或代偿' },
      { label: '4级', content: '整体稳定，仅有轻微偏移' },
      { label: '5级', content: '非常稳定，无晃动，动作流畅' }
    ]
  },
  plank: {
    title: '平板支撑 / Plank',
    desc: '评估核心肌群的耐力与稳定性。',
    image: '/image/fitting/plank.png',
    tips: [
      { label: '准备工作', content: '受测者取俯卧位，准备进入支撑姿势，可用秒表或手机计时' },
      { label: '测量姿势', content: '受测者以前臂和脚尖支撑身体（肘关节位于肩关节正下方）\n前臂平行放置，双手自然放松或握拳\n双腿伸直并拢或略分开，脚尖触地\n身体从头部、肩部、髋部到踝部保持一条直线\n核心收紧，骨盆保持中立位（避免塌腰或翘臀）\n头部保持中立，目视地面\n维持该姿势并开始计时，直至无法保持标准姿势为止' },
      { label: '注意事项', content: '避免腰部下塌（增加腰椎压力）\n避免臀部过高（降低测试有效性）\n避免耸肩或肩部前移（保持肘在肩正下方）\n保持呼吸均匀，不可屏气\n动作需稳定，避免身体晃动\n当姿势明显变形时应立即终止测试' },
      { label: '评分标准', content: '< 20秒 = 1分\n20-40秒 = 2分\n40-60秒 = 3分\n60-90秒 = 4分\n> 90秒 = 5分' }
    ]
  },
  stability: {
    title: '稳定性评分 / Stability',
    desc: '核心控制能力直接决定踩踏效率及长途舒适度。',
    image: '/image/fitting/plank.png',
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
  min-height: 280px;
  background: #FDFDFD;
  border-radius: var(--radius-sm);
  margin-bottom: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(0,0,0,0.02);
  padding: 20px;
}

.image-placeholder.large-placeholder {
  min-height: 520px;
}

.guide-illustration {
  max-height: 100%;
  max-width: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
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
  align-items: flex-start;
  gap: 10px;
}
.guide-tips li::before {
  content: '→';
  color: var(--accent-gold);
  font-weight: bold;
}
.guide-tips li .tip-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.guide-tips li .tip-label {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-main);
}
.guide-tips li .tip-content {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.8;
  white-space: pre-line;
}

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.5s var(--ease-out-expo); }
.fade-slide-enter-from { opacity: 0; transform: translateX(30px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-30px); }
</style>