<template>
  <div class="my-garage-page">
    <div class="container lab-container">

      <!-- 1. 页面头部：包含 Tab 切换 -->
      <div class="toolbar-row" v-scroll-reveal>
        <div class="text-group">
          <h1 class="page-title">我的车库</h1>
          <p class="page-subtitle">EQUIPMENT REGISTRY / 你的单车资产库</p>
        </div>

        <div class="tabs-group">
          <div class="header-actions">
            <!-- 状态切换 Tab -->
            <div class="custom-tabs">
              <div
                  class="tab-item"
                  :class="{ active: currentTab === 'actived' }"
                  @click="handleTabChange('actived')"
              >
                在役单车 <span class="count-dot">{{ activeCount }}</span>
              </div>
              <div
                  class="tab-item"
                  :class="{ active: currentTab === 'retired' }"
                  @click="handleTabChange('retired')"
              >
                荣誉档案 <span class="count-dot">{{ archivedCount }}</span>
              </div>
            </div>

            <button class="btn btn-primary" @click="handleAddNew">
              <i class="fas fa-plus"></i> 添加新成员
            </button>
          </div>
        </div>
      </div>

      <!-- 2. 单车画廊网格 -->
      <div v-if="loading" class="lab-loading">
        <i class="fas fa-circle-notch fa-spin"></i> 正在打开车库大门...
      </div>

      <div v-else-if="bikeList.length > 0" class="bike-grid">
        <!-- 名片组件：如果是在荣誉档案，名片会通过 CSS 变灰（在子组件内控制或在此处传参） -->
        <BikeCard
            v-for="(bike, index) in bikeList"
            :key="bike.id"
            :bike="bike"
            :is-retired="currentTab === 'retired'"
            v-scroll-reveal="index"
            @open-showcase="openShowcase"
            @delete="handleDeleteBike"
        />

        <!-- 快速添加入口（仅在“在役”标签显示） -->
        <div v-if="currentTab === 'actived'" class="add-card-placeholder glass-card" @click="handleAddNewManual">
          <div class="add-icon"><i class="fas fa-plus"></i></div>
          <span>扩充我的单车库</span>
        </div>
      </div>

      <!-- 3. 空状态引导 -->
      <div v-else class="empty-garage glass-card" v-scroll-reveal>
        <div class="empty-visual">
          <i class="fas" :class="currentTab === 'actived' ? 'fa-bicycle' : 'fa-archive'"></i>
          <div class="echo-ring"></div>
        </div>
        <h3>{{ currentTab === 'actived' ? '车库尚未录入资产' : '暂无荣誉记录' }}</h3>
        <p v-if="currentTab === 'actived'">从“单车展厅”同步全球几何数据，或手动开启调教档案。</p>
        <p v-else>这里记录您曾经并肩作战的战友。</p>
        <div class="empty-actions" v-if="currentTab === 'actived'">
          <button class="btn btn-primary" @click="$router.push('/showroom')">前往单车展厅</button>
          <button class="btn btn-outline" @click="handleAddNewManual">手动录入</button>
        </div>
      </div>

      <!-- 4. 底部说明 -->
      <footer class="garage-footer" v-scroll-reveal>
        <p><i class="fas fa-info-circle"></i> 点击任意车辆名片，进入“数字化身”沉浸式调教展台。</p>
      </footer>
    </div>

    <!-- 5. 沉浸式单车展台 -->
    <Transition name="showcase-zoom">
      <BikeShowcase
          v-if="selectedBike"
          :bike="selectedBike"
          @close="closeShowcase"
          @refresh="refreshData"
      />
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user.js';
import { getMyGarage, getRetiredGarage, deleteUserBike } from '@/api/userBike.js';
import BikeCard from '@/views/lab/components/BikeCard.vue';
import BikeShowcase from '@/views/lab/components/BikeShowcase.vue';
import { showToast, showConfirm } from '@/utils/toast.js';

const router = useRouter();
const userStore = useUserStore();

// --- 状态变量 ---
const currentTab = ref('actived'); // actived 或 retired
const bikeList = ref([]);
const activeCount = ref(0);
const archivedCount = ref(0);
const selectedBike = ref(null);
const loading = ref(false);

/**
 * 核心加载逻辑：根据 Tab 状态决定调用哪个接口
 */
const loadBikeList = async () => {
  loading.value = true;
  try {
    const fetchFunc = currentTab.value === 'actived' ? getMyGarage : getRetiredGarage;
    const res = await fetchFunc();
    const data = res.data || res || [];
    bikeList.value = data;

    // 更新对应状态的数量计数
    if (currentTab.value === 'actived') {
      activeCount.value = data.length;
    } else {
      archivedCount.value = data.length;
    }
  } catch (error) {
    console.error("数据获取失败:", error);
    showToast("无法连接至车库，请检查网络", "error");
  } finally {
    loading.value = false;
  }
};

/**
 * 初始化时同时获取两侧的数量（可选，为了让 Tab 上的数字准确）
 */
const initCounts = async () => {
  const [activeRes, retiredRes] = await Promise.allSettled([getMyGarage(), getRetiredGarage()]);
  if (activeRes.status === 'fulfilled') {
    activeCount.value = (activeRes.value.data || activeRes.value).length;
  }
  if (retiredRes.status === 'fulfilled') {
    archivedCount.value = (retiredRes.value.data || retiredRes.value).length;
  }
};

const handleTabChange = (tab) => {
  currentTab.value = tab;
  loadBikeList();
};

const openShowcase = (bike) => {
  selectedBike.value = bike;
  document.body.style.overflow = 'hidden';
};

const closeShowcase = () => {
  selectedBike.value = null;
  document.body.style.overflow = '';
};

const refreshData = () => {
  loadBikeList();
  initCounts(); // 发生退役或恢复时更新 Tab 上的计数
};

onUnmounted(() => {
  document.body.style.overflow = '';
});

const handleAddNew = () => router.push('/showroom');
const handleAddNewManual = () => router.push('/lab/garage/edit');

/**
 * 处理删除单车
 */
const handleDeleteBike = async (bike) => {
  const bikeName = bike.brandModel || `${bike.brand || ''} ${bike.model || ''}`.trim() || '此单车';
  const isConfirmed = await showConfirm(
    `您确定要删除${bikeName}吗？此操作不可恢复。`,
    '删除确认'
  );
  
  if (isConfirmed) {
    try {
      await deleteUserBike(bike.id);
      showToast('删除成功', 'success');
      refreshData();
    } catch (error) {
      console.error('删除失败:', error);
      showToast('删除失败', 'error');
    }
  }
};

onMounted(() => {
  loadBikeList();
  initCounts();
});
</script>

<style scoped>
.my-garage-page { padding-top: var(--header-height); min-height: 100vh; background: transparent; }
.lab-container { padding-top: 40px; padding-bottom: 40px; }

/* 头部样式 (Toolbar) */
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.page-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  margin-bottom: 5px;
  color: var(--text-main);
  line-height: 1.1;
}

.page-subtitle {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 30px;
}

/* 自定义 Tab 样式 - 优雅禅意风格 */
.custom-tabs {
  display: flex;
  gap: 30px;
  padding-right: 30px;
  border-right: 1px solid rgba(0,0,0,0.05);
}

.tab-item {
  cursor: pointer;
  font-family: var(--font-serif);
  font-size: 1.1rem;
  color: var(--text-muted);
  padding-bottom: 5px;
  position: relative;
  transition: 0.3s;
}

.tab-item .count-dot {
  font-size: 0.75rem;
  font-family: var(--font-sans);
  background: rgba(0,0,0,0.05);
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 4px;
  vertical-align: middle;
}

.tab-item.active {
  color: var(--primary-color);
  font-weight: 700;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -11px; /* 对齐 toolbar-row 的 border-bottom */
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--primary-color);
}

.tab-item.active .count-dot {
  background: var(--primary-light);
  color: white;
}

/* 网格布局 */
.bike-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

/* 快速添加占位符 */
.add-card-placeholder {
  height: 100%; min-height: 240px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  border: 2px dashed rgba(92, 110, 88, 0.25); background: transparent; cursor: pointer;
  transition: all 0.4s var(--ease-out-expo); color: var(--text-muted);
}
.add-card-placeholder:hover {
  border-color: var(--primary-color); color: var(--primary-color);
  background: white; transform: translateY(-5px);
}
.add-icon {
  width: 60px; height: 60px; border-radius: 50%; border: 1px solid #EEE;
  display: flex; align-items: center; justify-content: center; margin-bottom: 15px; font-size: 1.2rem;
}

/* 空状态与动画 */
.empty-garage {
  text-align: center; padding: 100px 40px;
  display: flex; flex-direction: column; align-items: center; gap: 20px;
}
.empty-visual { position: relative; margin-bottom: 20px; }
.empty-visual i { font-size: 5rem; color: var(--primary-color); opacity: 0.15; }
.echo-ring {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 100px; height: 100px; border: 1px solid var(--primary-color);
  border-radius: 50%; opacity: 0.1; animation: pulse 3s infinite;
}
@keyframes pulse { 0% { transform: translate(-50%, -50%) scale(1); opacity: 0.1; } 100% { transform: translate(-50%, -50%) scale(1.8); opacity: 0; } }

.empty-actions { display: flex; gap: 20px; margin-top: 10px; }

.garage-footer { margin-top: 60px; padding-top: 30px; border-top: 1px solid rgba(0,0,0,0.05); text-align: center; }
.garage-footer p { font-size: 0.85rem; color: var(--text-muted); font-style: italic; }

.showcase-zoom-enter-active, .showcase-zoom-leave-active {
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.showcase-zoom-enter-from, .showcase-zoom-leave-to {
  opacity: 0; transform: scale(1.05);
}

.lab-loading { height: 40vh; display: flex; align-items: center; justify-content: center; gap: 15px; color: var(--primary-color); font-weight: 600; }

@media (max-width: 768px) {
  .toolbar-row { flex-direction: column; align-items: flex-start; gap: 30px; }
  .header-actions { width: 100%; flex-direction: column-reverse; align-items: flex-start; gap: 20px; }
  .custom-tabs { border: none; padding: 0; }
  .bike-grid { grid-template-columns: 1fr; }
}
</style>