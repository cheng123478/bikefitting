<template>
  <div class="hub-page">
    <div class="container">

      <!-- Header -->
      <header class="page-header fade-up">
        <div class="header-left">
          <h2 class="title serif-title">静态调教中心</h2>
          <p class="subtitle">{{ PROJECT_CONFIG.MASCOT }} 引擎已就绪，请完善本次调教的偏好与身体反馈。</p>
        </div>
        <div class="header-actions">
          <button class="btn-text back-btn" @click="handleBack">
            <i class="fas fa-chevron-left"></i> 返回实验室
          </button>
        </div>
      </header>

      <!-- 核心区域 -->
      <div class="view-grid fade-up delay-1">

        <!-- 左侧：车辆概览/选择 -->
        <div class="view-card glass-card static-view-card">
          <div class="card-icon"><i class="fas fa-bicycle"></i></div>
          <div class="card-text">
            <h3>选择调教单车</h3>
            <div class="bike-selection-list">
              <div
                v-for="bike in allBikes"
                :key="bike.id"
                class="bike-selection-item"
                :class="{ active: selectedBikeId == bike.id }"
                @click="selectBike(bike)"
              >
                <div class="bike-status-dot" :class="{ 'has-report': bike.hasStaticReport }"></div>
                <div class="bike-main-info">
                  <span class="bike-name">{{ bike.brand }} {{ bike.model }}</span>
                  <span class="bike-remark">{{ bike.remark || '无备注' }}</span>
                </div>
                <i class="fas fa-check-circle active-check" :class="{ 'is-visible': selectedBikeId == bike.id }"></i>
              </div>

              <div v-if="allBikes.length === 0" class="empty-bikes">
                <p>车库中暂无现役车辆</p>
                <button class="btn-text" @click="router.push('/lab/garage/edit')">去添加单车</button>
              </div>
            </div>

          </div>
        </div>

        <!-- 右侧：痛感诊断 (多选) -->
        <div class="view-card glass-card pain-assessment-card">
          <div class="card-icon"><i class="fas fa-heartbeat"></i></div>
          <div class="card-text form-section">
            <h3>近期骑行痛点 <span class="subtitle-tip">(可选填)</span></h3>
            <p class="desc-text">如果您在近期骑行中感到不适，请勾选对应部位。引擎会自动引入医疗级修正参数以缓解您的痛苦。</p>

            <PainSelector v-model="selectedPains" />
          </div>
        </div>
      </div>

      <!-- 底部操作区 -->
      <div class="action-section fade-up delay-2">
        <div class="glass-card action-panel">

          <div class="task-info">
            <span class="label">ENGINE STATUS</span>
            <div class="launch-label">
              准备启动: <strong class="highlight">{{ PROJECT_CONFIG.MASCOT }} 核心计算引擎</strong>
            </div>
            <p class="desc-text" v-if="selectedPains.length > 0">已搭载 <strong>{{ selectedPains.length }}</strong> 项医疗修正参数。</p>
            <p class="desc-text" v-else>使用标准基准模式计算。</p>
          </div>

          <div class="btn-group">
            <button class="btn-action submit-action" @click="startEngine" :disabled="isCalculating">
              <div class="btn-icon">
                 <i class="fas fa-spinner fa-spin" v-if="isCalculating"></i>
                 <i class="fas fa-magic" v-else></i>
              </div>
              <div class="btn-content">
                <span class="main">{{ isCalculating ? '计算中...' : '启动分析引擎' }}</span>
                <span class="sub">生成静态诊断报告</span>
              </div>
            </button>
          </div>

        </div>
      </div>

      <!-- 结果弹窗 (可选) -->
       <Teleport to="body">
          <StaticFittingResultModal
              v-model:visible="showResultModal"
              :result="calculationResult"
              @to-report="navigateToReport"
          />
        </Teleport>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useLabStore } from '@/stores/lab.js';
import { staticFitting } from "@/api/staticFitting.js";
import { showToast } from '@/utils/toast';
import { PROJECT_CONFIG } from '@/config/constants';
import StaticFittingResultModal from "@/views/lab/components/StaticFittingResultModal.vue";
import PainSelector from "@/views/lab/components/PainSelector.vue";

const route = useRoute();
const router = useRouter();
const labStore = useLabStore();

// 状态
const selectedBikeId = ref(route.query.bikeId || null);
const selectedBike = ref(null);
const allBikes = ref([]);
const selectedPains = ref([]);
const isCalculating = ref(false);

const showResultModal = ref(false);
const calculationResult = ref({});

const fetchBikeInfo = async () => {
    try {
        // 使用 store 获取数据，如果已加载则直接返回
        const data = await labStore.fetchDashboard();
        const bikes = data.bikes || [];
        allBikes.value = bikes;

        if (selectedBikeId.value) {
            selectedBike.value = bikes.find(b => b.id === Number(selectedBikeId.value)) || null;
        } else if (bikes.length > 0) {
            selectedBike.value = bikes[0];
            selectedBikeId.value = selectedBike.value.id;
        }
    } catch(err) {
        console.error("加载车辆信息失败", err);
    }
}

const selectBike = (bike) => {
  selectedBikeId.value = bike.id;
  selectedBike.value = bike;
};

onMounted(() => {
   fetchBikeInfo();
});

const handleBack = () => {
  router.push('/lab');
};

const startEngine = async () => {
  if (isCalculating.value) return;

  isCalculating.value = true;
  try {
    showToast(`${PROJECT_CONFIG.MASCOT} 引擎正在计算...`, 'loading');
    const bId = selectedBike.value ? selectedBike.value.id : null;
    const res = await staticFitting(bId, selectedPains.value);
    calculationResult.value = res.data || res;

    // 显示弹窗 (或者可直接跳转报告页面)
    showResultModal.value = true;
  } catch(e) {
    showToast('计算失败，请稍后重试', 'error');
  } finally {
    isCalculating.value = false;
  }
};

const navigateToReport = () => {
    showResultModal.value = false;
    router.push({
        path: '/lab/report/static',
        query: { bikeId: selectedBikeId.value }
    });
};

</script>

<style scoped>
.hub-page {
  padding: 140px 0 80px;
  min-height: 100vh;
  background: transparent;
}

@media (max-width: 768px) {
  .hub-page {
    padding: 100px 0 40px;
  }
}

/* Header */
.page-header {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 40px; padding-bottom: 20px; border-bottom: 1px solid rgba(0,0,0,0.05);
}
.title { font-family: var(--font-serif); font-size: 2.2rem; color: var(--text-main); margin: 0; }
.subtitle { color: var(--text-muted); font-size: 1rem; margin-top: 5px; }
.header-actions { display: flex; align-items: center; }
.back-btn { color: var(--text-muted); font-size: 0.9rem; background: transparent; border: none; cursor: pointer; transition: 0.3s; }
.back-btn:hover { color: var(--primary-color); transform: translateX(-5px); }

/* 布局体系 (参考 DynamicFittingHub) */
.view-grid {
  display: grid;
  grid-template-columns: 1fr 2fr; /* 左车，右问卷 */
  gap: 25px;
  margin-bottom: 40px;
}

.view-card {
  position: relative;
  padding: 30px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  border: var(--border-glass);
  display: flex;
  gap: 25px;
  text-align: left;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
}

.static-view-card {
    align-items: flex-start;
}

.pain-assessment-card {
    align-items: flex-start;
}

.card-icon {
  width: 50px; height: 50px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.card-text h3 { margin: 0 0 15px 0; font-size: 1.2rem; color: var(--text-main); font-weight: 600;}
.card-text .subtitle-tip { font-size: 0.8rem; color: var(--text-muted); font-weight: normal; margin-left:8px;}

/* 车辆信息面板 */
.bike-desc {
    margin-top: 15px;
    font-size: 0.85rem;
    line-height: 1.5;
    color: var(--text-muted);
    margin-bottom: 0;
}

/* 车辆选择列表 */
.bike-selection-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 10px 0 20px;
  max-height: 240px;
  overflow-y: auto;
  padding-right: 5px;
}

.bike-selection-list::-webkit-scrollbar {
  width: 4px;
}
.bike-selection-list::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.05);
  border-radius: 4px;
}

.bike-selection-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  padding-right: 45px; /* 为绝对定位的打勾留出空间 */
  background: rgba(255,255,255,0.5);
  border: 1px solid rgba(92, 110, 88, 0.12);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.bike-selection-item:hover {
  background: #fff;
  border-color: var(--primary-color);
  transform: translateX(5px);
}

.bike-selection-item.active {
  background: #fff;
  border-color: var(--primary-color);
  box-shadow: 0 4px 15px rgba(92, 110, 88, 0.1);
}

.bike-status-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #ccc;
}
.bike-status-dot.has-report {
  background: #52c41a;
  box-shadow: 0 0 8px rgba(82, 196, 26, 0.4);
}

.bike-main-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.bike-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.2;
}
.bike-remark {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 4px;
  /* 限制单行 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.active-check {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%) scale(0.5);
  color: var(--primary-color);
  font-size: 1.1rem;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  pointer-events: none;
}

.active-check.is-visible {
  opacity: 1;
  transform: translateY(-50%) scale(1);
}

.selected-bike-detail {
  border-top: 1px solid rgba(0,0,0,0.05);
  padding-top: 15px;
}

.empty-bikes {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
}
.empty-bikes .btn-text {
  color: var(--primary-color);
  margin-top: 5px;
  font-weight: 600;
}

/* 痛楚多选 */
.form-section .desc-text {
    font-size: 0.9rem;
    color: var(--text-muted);
    margin-bottom: 20px;
    line-height: 1.5;
}

/* 2. 底部操作面板 */
.action-panel {
  padding: 30px 40px;
  border-radius: 20px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 40px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
}

.task-info { flex: 1; }
.task-info .label { font-size: 0.7rem; color: var(--text-muted); letter-spacing: 1px; }
.selected-text { font-size: 1.4rem; color: var(--text-main); margin: 5px 0; font-family: var(--font-serif); }
.highlight { color: var(--primary-color); }
.desc-text { font-size: 0.9rem; color: var(--text-muted); margin:0;}

.btn-group { display: flex; gap: 15px; }

.btn-action {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; border-radius: 12px;
  border: 1px solid transparent; cursor: pointer; transition: 0.3s;
  min-width: 160px;
}

.btn-icon { font-size: 1.2rem; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.btn-content { display: flex; flex-direction: column; text-align: left; }
.btn-content .main { font-weight: 700; font-size: 0.95rem; line-height: 1.2; }
.btn-content .sub { font-size: 0.7rem; opacity: 0.8; font-weight: 400; margin-top: 2px; }

/* 提交按钮 */
.submit-action { background: var(--accent-gold); color: #000; box-shadow: 0 4px 15px rgba(212, 175, 55, 0.3); }
.submit-action .btn-icon { background: rgba(0,0,0,0.1); }
.submit-action:hover { background: #fff; transform: translateY(-2px); border-color: var(--accent-gold); }
.submit-action:disabled { background: #eee; color: #aaa; cursor: not-allowed; box-shadow: none; border-color: transparent; }
.submit-action:disabled:hover { transform: none; }

@media (max-width: 1024px) {
  .view-grid { grid-template-columns: 1fr; gap: 15px; }
  .action-panel { flex-direction: column; text-align: center; gap: 20px; padding: 20px; }
  .btn-group { width: 100%; flex-direction: column; }
  .btn-action { justify-content: center; }
}
</style>
