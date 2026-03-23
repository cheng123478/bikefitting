<template>
  <div class="entrance-page">
    <div class="container entrance-container">
      
      <!-- 1. Header Section -->
      <header class="entrance-header fade-up">
        <router-link to="/lab" class="back-link">
          <i class="fas fa-chevron-left"></i> 返回实验室
        </router-link>
        <h1 class="title">动态视觉实验室 <span class="badge">Dynamic</span></h1>
        <p class="subtitle">通过计算机视觉捕获您的骑行灵魂，优化效率并预防运动损伤。</p>
      </header>

      <!-- 2. Bike Selection Section -->
      <section class="bike-selection fade-up delay-1">
        <h3 class="section-label">选择分析车辆</h3>
        <div class="bike-grid">
          <div 
            v-for="bike in userBikes" 
            :key="bike.id" 
            class="bike-card glass-card"
            :class="{ active: selectedBikeId === bike.id }"
            @click="selectedBikeId = bike.id"
          >
            <div class="bike-image">
              <img :src="bike.image" :alt="bike.model">
            </div>
            <div class="bike-info">
              <div class="b-brand">{{ bike.brand }}</div>
              <div class="b-model">{{ bike.model }}</div>
              <div class="b-meta">{{ bike.size }}码 · {{ bike.type }}</div>
            </div>
            <div class="active-check" v-if="selectedBikeId === bike.id">
              <i class="fas fa-check-circle"></i>
            </div>
          </div>
          
          <div class="bike-card add-new glass-card" @click="openBikeModal">
            <i class="fas fa-plus"></i>
            <span>添加新车</span>
          </div>
        </div>
      </section>

      <!-- 3. Guide & Info Section -->
      <section class="info-grid fade-up delay-2">
        <div class="glass-card guide-card">
          <h3 class="card-label">如何开始？</h3>
          <ul class="guide-steps">
            <li>
              <div class="step-num">01</div>
              <div class="step-content">
                <strong>选择车辆</strong>
                <p>选择您即将进行调教的自行车，确保数据准确归档。</p>
              </div>
            </li>
            <li>
              <div class="step-num">02</div>
              <div class="step-content">
                <strong>多视角采集</strong>
                <p>我们将引导您完成侧面、正面和背面的动作捕获。</p>
              </div>
            </li>
            <li>
              <div class="step-num">03</div>
              <div class="step-content">
                <strong>实时反馈</strong>
                <p>Aries 引擎将实时计算关节角度，并给出初步建议。</p>
              </div>
            </li>
          </ul>
        </div>

        <div class="glass-card hardware-card">
          <h3 class="card-label">准备工作</h3>
          <div class="tips">
            <div class="tip-item">
              <i class="fas fa-video"></i>
              <span><strong>机位：</strong> 垂直于骑行者，高度与中轴齐平。</span>
            </div>
            <div class="tip-item">
              <i class="fas fa-lightbulb"></i>
              <span><strong>光照：</strong> 确保背景不背光，光线均匀照亮关节。</span>
            </div>
            <div class="tip-item">
              <i class="fas fa-tshirt"></i>
              <span><strong>着装：</strong> 穿着紧身骑行服，以便 AI 高效识别关节。</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 4. Action Footer -->
      <footer class="action-footer fade-up delay-3">
        <button 
          class="btn-enter" 
          :disabled="!selectedBikeId"
          @click="enterLab"
        >
          <span>进入采集中心</span>
          <i class="fas fa-arrow-right"></i>
        </button>
      </footer>

    </div>

    <!-- Bike Modal -->
    <BikeModal
        :is-open="showBikeModal"
        @close="showBikeModal = false"
        @submit="handleBikeSave"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BikeModal from '@/components/BikeModal.vue';
import { showToast } from '@/utils/toast';

const router = useRouter();
const selectedBikeId = ref(null);
const showBikeModal = ref(false);

// 模拟数据，实际应从 API 获取
const userBikes = ref([
  { id: 1, brand: 'Specialized', model: 'Tarmac SL7', size: '54', type: '公路车', image: 'https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?q=80&w=200' },
  { id: 2, brand: 'Brompton', model: 'P Line', size: 'M', type: '折叠车', image: 'https://images.unsplash.com/photo-1485965120184-e224f7a1adb1?q=80&w=200' }
]);

onMounted(() => {
  if (userBikes.value.length > 0) {
    selectedBikeId.value = userBikes.value[0].id;
  }
});

const openBikeModal = () => { showBikeModal.value = true; };
const handleBikeSave = (newBike) => {
  newBike.id = Date.now();
  userBikes.value.push(newBike);
  selectedBikeId.value = newBike.id;
  showBikeModal.value = false;
  showToast('车辆添加成功', 'success');
};

const enterLab = () => {
  if (!selectedBikeId.value) return;
  router.push({
    path: '/lab/dynamic',
    query: { bikeId: selectedBikeId.value }
  });
};
</script>

<style scoped>
.entrance-page { padding: 120px 0 80px; min-height: 100vh; background: var(--bg-page); }
.entrance-container { max-width: 1000px; }

/* Header */
.entrance-header { margin-bottom: 40px; }
.back-link { color: var(--text-muted); font-size: 0.9rem; text-decoration: none; display: flex; align-items: center; gap: 8px; margin-bottom: 15px; }
.title { font-family: var(--font-serif); font-size: 2.4rem; color: var(--text-main); margin: 0; display: flex; align-items: center; gap: 15px; }
.badge { font-size: 0.7rem; background: var(--primary-color); color: white; padding: 4px 10px; border-radius: 5px; text-transform: uppercase; letter-spacing: 1px; }
.subtitle { color: var(--text-muted); font-size: 1.1rem; margin-top: 10px; }

/* Info Grid */
.info-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 30px; margin-bottom: 50px; }
.card-label { font-size: 1rem; color: var(--text-main); font-weight: 700; margin-bottom: 20px; border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 10px; }

.guide-steps { list-style: none; padding: 0; }
.guide-steps li { display: flex; gap: 20px; margin-bottom: 20px; }
.step-num { font-family: var(--font-serif); font-size: 1.5rem; font-weight: 800; color: var(--primary-light); opacity: 0.5; }
.step-content strong { display: block; font-size: 1rem; color: var(--text-main); margin-bottom: 4px; }
.step-content p { font-size: 0.85rem; color: var(--text-muted); line-height: 1.5; margin: 0; }

.tips { display: flex; flex-direction: column; gap: 20px; }
.tip-item { display: flex; gap: 15px; align-items: flex-start; }
.tip-item i { font-size: 1.2rem; color: var(--primary-color); width: 24px; text-align: center; margin-top: 3px; }
.tip-item span { font-size: 0.85rem; color: var(--text-body); line-height: 1.5; }

/* Bike Selection */
.section-label { font-size: 1.1rem; color: var(--text-main); font-weight: 700; margin-bottom: 20px; border-left: 4px solid var(--accent-gold); padding-left: 15px; }
.bike-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px; }

.bike-card { 
  display: flex; align-items: center; gap: 15px; padding: 20px; cursor: pointer; border: 2px solid transparent; 
  transition: all 0.3s var(--ease-out-expo); position: relative;
}
.bike-card:hover { transform: translateY(-5px); border-color: var(--primary-light); }
.bike-card.active { border-color: var(--primary-color); background: #fff; box-shadow: var(--shadow-md); }

.bike-image { width: 60px; height: 50px; background: #f8f8f8; border-radius: 8px; overflow: hidden; }
.bike-image img { width: 100%; height: 100%; object-fit: contain; }
.bike-info .b-brand { font-size: 0.75rem; color: var(--text-muted); font-weight: 700; text-transform: uppercase; }
.bike-info .b-model { font-size: 1.05rem; font-weight: 700; color: var(--text-main); }
.bike-info .b-meta { font-size: 0.8rem; color: var(--text-muted); margin-top: 2px; }

.active-check { position: absolute; top: 10px; right: 10px; color: var(--primary-color); font-size: 1.2rem; }

.bike-card.add-new { flex-direction: column; justify-content: center; border-style: dashed; border-color: #ddd; color: var(--text-muted); }
.bike-card.add-new i { font-size: 1.5rem; margin-bottom: 8px; }
.bike-card.add-new:hover { border-color: var(--primary-color); color: var(--primary-color); }

/* Action Footer */
.action-footer { margin-top: 60px; text-align: center; }
.btn-enter { 
  display: inline-flex; align-items: center; gap: 20px; padding: 18px 45px;
  background: var(--primary-color); color: white; border: none; border-radius: 50px;
  font-size: 1.2rem; font-weight: 700; cursor: pointer; transition: all 0.4s var(--ease-out-expo);
  box-shadow: 0 10px 30px rgba(92, 110, 88, 0.3);
}
.btn-enter:hover:not(:disabled) { transform: translateY(-5px); box-shadow: 0 15px 40px rgba(92, 110, 88, 0.4); background: var(--primary-dark); }
.btn-enter:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 768px) {
  .info-grid { grid-template-columns: 1fr; }
  .bike-grid { grid-template-columns: 1fr; }
}
</style>