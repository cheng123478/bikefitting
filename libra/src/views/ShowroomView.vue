<template>
  <div class="showroom-page">
    <div class="container">
      <!-- 1. Header: Left-aligned to match Lab style -->
      <header class="showroom-intro" v-scroll-reveal>
        <div class="intro-left">
          <h1 class="showroom-title serif-title">单车展厅</h1>
          <p class="page-subtitle">{{ PROJECT_CONFIG.MASCOT }} 顶级单车几何数据库 & 选号中心</p>
        </div>

        <div class="data-status-wrapper">
          <div class="engine-status" title="数据已加载自 mock 服务">
            <span class="status-dot"></span>
            <span class="status-text">Data: Mocked</span>
          </div>
        </div>
      </header>

      <div class="showroom-layout">
      <!-- 2. Sidebar Filters -->
      <aside class="sidebar-filters" v-scroll-reveal.delay="100">
        <div class="filter-group">
          <h3 class="filter-title">类型 / Category</h3>
          <div class="category-list">
            <button
                class="category-btn"
                :class="{ active: activeCategory === 'all' }"
                @click="activeCategory = 'all'"
            >
              <i class="fas fa-th-large"></i> 全部车型
            </button>
            <button
                v-for="cat in categories"
                :key="cat.id"
                class="category-btn"
                :class="{ active: activeCategory === cat.id }"
                @click="activeCategory = cat.id"
            >
              <i :class="cat.icon"></i> {{ cat.name }}
            </button>
          </div>
        </div>

        <div class="filter-group">
          <h3 class="filter-title">品牌 / Brand</h3>
          <div class="brand-grid">
            <button
                class="brand-chip"
                :class="{ active: activeBrand === 'all' }"
                @click="activeBrand = 'all'"
            >
              All
            </button>
            <button
                v-for="brand in brands"
                :key="brand"
                class="brand-chip"
                :class="{ active: activeBrand === brand }"
                @click="activeBrand = brand"
            >
              {{ brand }}
            </button>
          </div>
        </div>
      </aside>

      <!-- 3. Main Content: Bike Grid -->
      <main class="bike-main">
        <div class="grid-header">
          <p class="results-count">为您找到 {{ filteredBikes.length }} 款单车</p>
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input type="text" v-model="searchQuery" placeholder="搜索型号或关键词...">
          </div>
        </div>

        <TransitionGroup name="grid-fade" tag="div" class="bike-grid">
          <div
              v-for="(bike, index) in filteredBikes"
              :key="bike.id"
              class="bike-card glass-card"
              v-scroll-reveal="{ delay: index * 50 }"
          >
            <div class="bike-img-wrapper">
              <img :src="bike.img" :alt="bike.model" class="bike-img">
              <div class="card-overlay">
                <button class="btn btn-sm btn-white" @click="openPreview(bike)">查看细节</button>
              </div>
              <span class="bike-year">{{ bike.year }}</span>
            </div>
            <div class="bike-info">
              <div class="info-top">
                <span class="brand-tag">{{ bike.brand }}</span>
                <span class="price-val">{{ bike.price }}</span>
              </div>
              <h4 class="bike-model">{{ bike.model }}</h4>
              <p class="bike-desc">{{ bike.desc }}</p>
              <div class="bike-tags">
                <span v-for="tag in bike.tags" :key="tag" class="small-tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </TransitionGroup>

        <!-- Empty State -->
        <div v-if="filteredBikes.length === 0" class="empty-state">
          <i class="fas fa-bicycle"></i>
          <p>没有找到符合条件的单车，尝试调整筛选条件。</p>
        </div>
      </main>
    </div>
  </div>

  <!-- 图片放大预览 -->
  <Teleport to="body">
    <Transition name="preview-fade">
      <div v-if="previewVisible" class="image-preview-overlay" @click="closePreview">
        <div class="preview-content" @click.stop>
          <button class="preview-close" @click="closePreview"><i class="fas fa-times"></i></button>
          <img :src="previewImage" :alt="previewTitle" class="preview-img">
          <div class="preview-info">
            <span class="preview-brand">{{ previewBrand }}</span>
            <h3 class="preview-title">{{ previewTitle }}</h3>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { showroomService } from '@/api/mockService';
import { PROJECT_CONFIG } from '@/config/constants';

const brands = ref([]);
const categories = ref([]);
const bikes = ref([]);
const loading = ref(true);

const activeCategory = ref('all');
const activeBrand = ref('all');
const searchQuery = ref('');

const filteredBikes = computed(() => {
  return bikes.value.filter(bike => {
    const matchesCategory = activeCategory.value === 'all' || bike.category === activeCategory.value;
    const matchesBrand = activeBrand.value === 'all' || bike.brand === activeBrand.value;
    const matchesSearch = bike.model.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                          bike.brand.toLowerCase().includes(searchQuery.value.toLowerCase());
    return matchesCategory && matchesBrand && matchesSearch;
  });
});

const fetchData = async () => {
  try {
    const data = await showroomService.getShowroomData();
    brands.value = data.brands;
    categories.value = data.categories;
    bikes.value = data.bikes;
  } catch (err) {
    console.error('Failed to fetch showroom data', err);
  } finally {
    loading.value = false;
  }
};

const previewVisible = ref(false);
const previewImage = ref('');
const previewTitle = ref('');
const previewBrand = ref('');

const openPreview = (bike) => {
  previewImage.value = bike.img;
  previewTitle.value = bike.model;
  previewBrand.value = bike.brand;
  previewVisible.value = true;
  document.body.style.overflow = 'hidden';
};

const closePreview = () => {
  previewVisible.value = false;
  document.body.style.overflow = '';
};

onMounted(fetchData);
</script>

<style scoped>
.showroom-page {
  padding: 120px 0 100px;
  min-height: 100vh;
}

.showroom-intro {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 50px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.showroom-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  color: var(--text-main);
  font-weight: 700;
  margin: 0 0 5px 0;
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 1rem;
  margin: 0;
}

.engine-status {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 8px 18px;
  background: transparent;
  border: 1px solid rgba(92, 110, 88, 0.15);
  border-radius: 30px;
  cursor: default;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--success-color, #52c41a);
  box-shadow: 0 0 8px var(--success-color, #52c41a);
  animation: breathe 3s infinite ease-in-out;
}

.status-text {
  font-family: 'Lato', monospace;
  font-size: 0.75rem;
  letter-spacing: 1px;
  color: var(--primary-color);
  font-weight: 700;
  text-transform: uppercase;
}

@keyframes breathe {
  0% { opacity: 0.4; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
  100% { opacity: 0.4; transform: scale(0.8); }
}

.showroom-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 40px;
  align-items: start;
  padding-bottom: 100px;
}

/* 2. Sidebar */
.sidebar-filters {
  position: sticky;
  top: 120px;
}

.filter-group {
  margin-bottom: 35px;
}

.filter-title {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--text-muted);
  margin-bottom: 15px;
  font-weight: 700;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  padding-bottom: 8px;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-btn {
  background: none;
  border: none;
  text-align: left;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 0.95rem;
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-btn i {
  width: 20px;
  color: var(--text-muted);
  transition: 0.3s;
}

.category-btn:hover {
  background: rgba(92, 110, 88, 0.05);
  color: var(--primary-color);
}

.category-btn.active {
  background: var(--primary-color);
  color: white;
  box-shadow: var(--shadow-sm);
}

.category-btn.active i {
  color: white;
}

.brand-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.brand-chip {
  background: white;
  border: 1px solid rgba(0,0,0,0.05);
  padding: 8px;
  font-size: 0.8rem;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.3s;
  text-align: center;
}

.brand-chip:hover {
  border-color: var(--primary-light);
  color: var(--primary-color);
}

.brand-chip.active {
  background: var(--text-main);
  color: white;
  border-color: var(--text-main);
}

/* 3. Main Grid */
.grid-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.results-count {
  font-size: 0.9rem;
  color: var(--text-muted);
  font-style: italic;
}

.search-box {
  position: relative;
  width: 280px;
}

.search-box i {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}

.search-box input {
  width: 100%;
  padding: 10px 15px 10px 40px;
  border-radius: 30px;
  border: 1px solid rgba(0,0,0,0.1);
  background: white;
  font-size: 0.9rem;
  transition: 0.3s;
}

.search-box input:focus {
  border-color: var(--primary-color);
  outline: none;
  box-shadow: 0 0 0 4px rgba(92, 110, 88, 0.1);
}

.bike-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
}

.bike-card {
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
  border: 1px solid transparent;
}

.bike-card:hover {
  transform: translateY(-10px);
  border-color: rgba(92, 110, 88, 0.2);
  box-shadow: var(--shadow-lg);
}

.bike-img-wrapper {
  position: relative;
  height: 200px;
  background: #f5f5f5;
  overflow: hidden;
}

.bike-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: 0.6s transform cubic-bezier(0.165, 0.84, 0.44, 1);
}

.bike-card:hover .bike-img {
  transform: scale(1.1);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: 0.3s;
  backdrop-filter: blur(2px);
}

.bike-card:hover .card-overlay {
  opacity: 1;
}

.bike-year {
  position: absolute;
  top: 15px;
  right: 15px;
  background: rgba(255,255,255,0.9);
  padding: 4px 10px;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 4px;
  color: var(--text-main);
}

.bike-info {
  padding: 25px;
}

.info-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.brand-tag {
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--primary-color);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.price-val {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-main);
}

.bike-model {
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-main);
}

.bike-desc {
  font-size: 0.9rem;
  color: var(--text-muted);
  line-height: 1.5;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 2.7em;
}

.bike-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.small-tag {
  font-size: 0.65rem;
  background: rgba(0,0,0,0.04);
  padding: 4px 10px;
  border-radius: 4px;
  color: var(--text-muted);
  font-weight: 500;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 100px 0;
  color: var(--text-muted);
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 20px;
  opacity: 0.3;
}

/* Animations */
.grid-fade-enter-active,
.grid-fade-leave-active {
  transition: all 0.5s ease;
}

.grid-fade-enter-from,
.grid-fade-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (max-width: 900px) {
  .showroom-layout {
    grid-template-columns: 1fr;
  }
  .sidebar-filters {
    position: static;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .brand-grid {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  }
}

/* 图片放大预览 */
.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
}

.preview-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-img {
  max-width: 100%;
  max-height: 75vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.preview-close {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: 0.3s;
}

.preview-close:hover {
  background: rgba(255, 255, 255, 0.15);
}

.preview-info {
  margin-top: 20px;
  text-align: center;
}

.preview-brand {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--primary-color);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.preview-title {
  color: white;
  font-size: 1.4rem;
  font-weight: 700;
  margin-top: 6px;
}

.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: all 0.3s ease;
}

.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}

.preview-fade-enter-from .preview-content,
.preview-fade-leave-to .preview-content {
  transform: scale(0.92);
}
</style>
