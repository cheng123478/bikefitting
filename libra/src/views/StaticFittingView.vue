<template>
  <div class="static-fit-page">

    <div class="container fit-card-container">

      <!-- 头部 -->
      <div class="fit-header">
        <router-link to="/fitting" class="back-link">
          <i class="fas fa-chevron-left"></i> <span>返回控制台</span>
        </router-link>
        <h2 class="section-title" style="margin:0; font-size: 2rem;">静态分析</h2>
      </div>

      <form @submit.prevent="submitForm">

        <!-- ================= PHASE 1: 身体数据 ================= -->
        <Transition name="fade" mode="out-in">
          <div v-if="currentPhase === 1" key="phase1" class="fit-phase">
            <h3 class="phase-heading">1. 身体几何数据录入</h3>

            <div class="split-layout">
              <!-- 左侧：输入表单 -->
              <div class="form-column">
                <div v-for="field in ['height', 'shoulder', 'arm_upper', 'arm_lower', 'sit_bone', 'inseam']" :key="field" class="input-wrapper">
                  <label class="input-label">{{ getLabel(field) }}</label>
                  <div class="input-group suffix-group">
                    <input
                        type="number"
                        v-model="formData[field]"
                        class="zen-input"
                        placeholder="请输入测量值"
                        @focus="updateGuide(field)"
                    >
                    <span class="unit-suffix">{{ getUnit(field) }}</span>
                  </div>
                </div>
              </div>

              <!-- 右侧：动态指引 -->
              <div class="guide-column">
                <div class="visual-guide glass-card">
                  <transition name="fade" mode="out-in">
                    <img :key="currentGuide.image" :src="currentGuide.image" alt="示意图" class="guide-img">
                  </transition>
                </div>
                <div class="instruction-box">
                  <h4>{{ currentGuide.title }}</h4>
                  <p>{{ currentGuide.content }}</p>
                </div>
              </div>
            </div>

            <div class="action-bar">
              <button type="button" class="btn btn-primary btn-block" @click="nextPhase(2)">
                下一步 (机能测试) <i class="fas fa-arrow-right"></i>
              </button>
            </div>
          </div>

          <!-- ================= PHASE 2: 机能测试 ================= -->
          <div v-else-if="currentPhase === 2" key="phase2" class="fit-phase">

            <h3 class="phase-heading">2. 身体机能评估</h3>

            <!-- Tab -->
            <div class="func-tabs-wrapper">
              <div class="func-tab-item" :class="{ active: funcTab === 'flexibility' }" @click="funcTab = 'flexibility'">
                <i class="fas fa-walking"></i> <span>柔韧性检测</span>
              </div>
              <div class="func-tab-item" :class="{ active: funcTab === 'core' }" @click="funcTab = 'core'">
                <i class="fas fa-dumbbell"></i> <span>核心力量测试</span>
              </div>
            </div>

            <!-- 内容：柔韧性 -->
            <div v-if="funcTab === 'flexibility'" class="func-content">
              <div class="media-placeholder glass-card">
                <div class="inner-circle"><i class="fas fa-play"></i></div>
                <span class="placeholder-text">演示视频 / 交互图示</span>
              </div>

              <div class="instruction-box">
                <p><strong>测量工具：</strong> 手机测角仪或量角器</p>
                <p><strong>操作指引：</strong> 坐姿挺直，缓慢旋转胸椎至极限，记录肩线与中轴线的夹角。</p>
              </div>

              <div class="grid-2" style="margin-top: 20px;">
                <div class="input-wrapper">
                  <label class="input-label">胸椎旋转角度</label>
                  <div class="input-group suffix-group">
                    <input type="number" v-model="formData.spine_rotation" class="zen-input">
                    <span class="unit-suffix">°</span>
                  </div>
                </div>
                <div class="input-wrapper">
                  <label class="input-label">体前屈距离</label>
                  <div class="input-group suffix-group">
                    <input type="number" v-model="formData.sit_and_reach" class="zen-input" placeholder="负数表示未及脚尖">
                    <span class="unit-suffix">cm</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 内容：核心力量 -->
            <div v-if="funcTab === 'core'" class="func-content">
              <div class="media-placeholder glass-card">
                <i class="fas fa-dumbbell fa-2x" style="color: var(--text-muted); margin-bottom: 10px;"></i>
                <span class="placeholder-text">Plank 动作标准演示</span>
              </div>

              <div class="input-wrapper single-field-center">
                <label class="input-label">维持时长</label>
                <div class="input-group suffix-group">
                  <input type="number" v-model="formData.plank_time" class="zen-input">
                  <span class="unit-suffix">秒</span>
                </div>
              </div>
            </div>

            <div class="action-bar dual-btns">
              <button type="button" class="btn btn-outline" @click="currentPhase = 1">上一步</button>
              <button type="button" class="btn btn-primary" @click="nextPhase(3)">
                下一步 (选择车辆) <i class="fas fa-arrow-right"></i>
              </button>
            </div>
          </div>

          <!-- ================= PHASE 3: 选择车辆 ================= -->
          <div v-else key="phase3" class="fit-phase">
            <h3 class="phase-heading">3. 选择待调试车辆</h3>
            <p class="phase-desc">Fitting 方案将基于所选车辆的几何（Stack/Reach）生成。</p>

            <div class="bike-selector-grid">
              <div
                  v-for="bike in userBikes"
                  :key="bike.id"
                  class="bike-select-card glass-card"
                  :class="{ active: selectedBikeId === bike.id }"
                  @click="selectedBikeId = bike.id"
              >
                <img :src="bike.image" class="bike-thumb">
                <div class="bike-info">
                  <div class="b-name">{{ bike.brand }} {{ bike.model }}</div>
                  <div class="b-spec">{{ bike.size }}码 · {{ bike.type }}</div>
                </div>
                <div class="check-mark" v-if="selectedBikeId === bike.id">
                  <i class="fas fa-check-circle"></i>
                </div>
              </div>

              <div class="bike-select-card add-new glass-card" @click="openBikeModal">
                <i class="fas fa-plus"></i>
                <span>添加新车辆</span>
              </div>
            </div>

            <div class="action-bar dual-btns">
              <button type="button" class="btn btn-outline" @click="currentPhase = 2">上一步</button>
              <button type="submit" class="btn btn-primary">
                生成分析报告 <i class="fas fa-check"></i>
              </button>
            </div>
          </div>

        </Transition>
      </form>
    </div>

    <BikeModal
        :is-open="showBikeModal"
        @close="showBikeModal = false"
        @submit="handleBikeSave"
    />
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import BikeModal from '@/components/BikeModal.vue';

const router = useRouter();
const currentPhase = ref(1);
const funcTab = ref('flexibility');
const showBikeModal = ref(false);

const formData = reactive({
  height: '', shoulder: '', arm_upper: '', arm_lower: '',
  sit_bone: '', inseam: '',
  spine_rotation: '', sit_and_reach: '', plank_time: ''
});

const defaultGuide = {
  title: '测量准备',
  content: '请穿着贴身衣物，在自然的呼吸节奏中开始测量。每一个数据的准确，都是为了骑行中更好的平衡。',
  image: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=500&fit=crop'
};
const currentGuide = ref(defaultGuide);
const guideData = {
  'height': { title: '身高 (Height)', content: '双脚并拢靠墙，平视前方。测量从头顶到地面的垂直轨迹。', image: '/image/fitting/height.png' },
  'shoulder': { title: '肩宽 (Shoulder)', content: '寻找肩峰两点，测量其间的水平距离。', image: '/image/fitting/shoulder.png' }
  // ... 其他数据保持不变
};

const userBikes = ref([
  { id: 1, brand: 'Specialized', model: 'Tarmac SL7', size: '54', type: '公路车', image: 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?q=80&w=200' },
  { id: 2, brand: 'Brompton', model: 'P Line', size: 'M', type: '折叠车', image: 'https://images.unsplash.com/photo-1485965120184-e224f7a1adb1?q=80&w=200' }
]);
const selectedBikeId = ref(null);

const getLabel = (field) => ({
  height: '身高', shoulder: '肩宽', arm_upper: '大臂长',
  arm_lower: '小臂长', sit_bone: '坐骨间距', inseam: '跨高'
}[field]);
const getUnit = (field) => (field === 'sit_bone' ? 'mm' : 'cm');

const updateGuide = (field) => { if (guideData[field]) currentGuide.value = guideData[field]; };

const nextPhase = (step) => {
  if (step === 2 && !formData.height) return alert('请先完善身体数据');
  currentPhase.value = step;
};

const openBikeModal = () => { showBikeModal.value = true; };
const handleBikeSave = (newBike) => {
  newBike.id = Date.now();
  if(!newBike.image) newBike.image = 'https://via.placeholder.com/200x150?text=No+Image';
  userBikes.value.push(newBike);
  selectedBikeId.value = newBike.id;
  showBikeModal.value = false;
};

const submitForm = () => {
  if (!selectedBikeId.value) return alert("请选择一辆需要 Fitting 的车辆");
  router.push('/fitting');
};
</script>

<style scoped>
.static-fit-page { padding: 120px 0 80px; min-height: 100vh; }

.fit-card-container {
  max-width: 960px;
  background: var(--bg-surface);
  border-radius: var(--radius-md);
  padding: 50px 60px;
  box-shadow: var(--shadow-lg);
  border: var(--border-glass);
}

.fit-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 40px; padding-bottom: 20px; border-bottom: 1px solid rgba(0,0,0,0.05);
}
.back-link { color: var(--text-muted); font-size: 0.9rem; display: flex; align-items: center; gap: 8px; transition: 0.3s; }
.back-link:hover { color: var(--primary-color); transform: translateX(-3px); }

.phase-heading { font-size: 1.6rem; color: var(--primary-color); margin-bottom: 30px; font-weight: 600; font-family: var(--font-serif); }
.phase-desc { color: var(--text-muted); margin-bottom: 30px; margin-top: -20px; }

.suffix-group { position: relative; }
.unit-suffix { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 0.85rem; pointer-events: none; }

/* 指引区域 */
.visual-guide {
  height: 320px; padding: 0; overflow: hidden; border: none;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-page); position: relative;
}
.guide-img {
  width: 100%; height: 100%; object-fit: contain; padding: 20px;
  mix-blend-mode: multiply; opacity: 0.9;
}
.instruction-box { background: rgba(92, 110, 88, 0.05); padding: 25px; border-radius: var(--radius-sm); border-left: 3px solid var(--primary-color); }
.instruction-box h4 { font-size: 1.1rem; margin-bottom: 10px; font-family: var(--font-serif); color: var(--text-main); }
.instruction-box p { font-size: 0.9rem; line-height: 1.6; color: var(--text-muted); }

/* Tab */
.func-tabs-wrapper {
  display: flex; background-color: var(--bg-page); padding: 6px; border-radius: var(--radius-pill);
  margin-bottom: 40px; max-width: 500px; margin-left: auto; margin-right: auto;
}
.func-tab-item {
  flex: 1; text-align: center; padding: 12px 0; border-radius: var(--radius-pill); cursor: pointer;
  color: var(--text-muted); font-size: 1rem; font-weight: 500; transition: all 0.3s ease;
  display: flex; align-items: center; justify-content: center; gap: 10px;
}
.func-tab-item.active { background-color: var(--primary-color); color: var(--white); box-shadow: var(--shadow-sm); font-weight: 600; }

.media-placeholder { height: 220px; display: flex; flex-direction: column; align-items: center; justify-content: center; margin-bottom: 30px; background: #fafafa; }
.placeholder-text { color: var(--text-muted); margin-top: 10px; }
.inner-circle { width: 50px; height: 50px; border-radius: 50%; border: 1px solid var(--text-muted); display: flex; align-items: center; justify-content: center; color: var(--text-muted); }

/* 车辆选择器 */
.bike-selector-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; margin-bottom: 40px; }
.bike-select-card { display: flex; align-items: center; gap: 15px; padding: 15px 20px; cursor: pointer; transition: 0.3s; }
.bike-select-card:hover { border-color: var(--primary-color); transform: translateY(-3px); }
.bike-select-card.active { border: 1.5px solid var(--primary-color); background: rgba(92,110,88,0.05); }

.bike-thumb { width: 60px; height: 45px; object-fit: contain; background: #f5f5f5; border-radius: 4px; }
.bike-info { flex: 1; overflow: hidden; }
.b-name { font-size: 0.95rem; font-weight: 600; color: var(--text-main); margin-bottom: 4px; }
.b-spec { font-size: 0.75rem; color: var(--text-muted); }
.check-mark { color: var(--primary-color); font-size: 1.2rem; }

.bike-select-card.add-new {
  justify-content: center; color: var(--text-muted); border-style: dashed; font-weight: 500;
  flex-direction: column; gap: 8px; min-height: 80px;
}
.bike-select-card.add-new:hover { color: var(--primary-color); border-color: var(--primary-color); }

.action-bar { margin-top: 40px; }
.dual-btns { display: flex; gap: 20px; }
.dual-btns .btn { flex: 1; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .fit-card-container { padding: 30px 20px; }
  .split-layout { flex-direction: column-reverse; gap: 30px; }
  .guide-column { height: auto; }
  .visual-guide { height: 180px; }
}
</style>