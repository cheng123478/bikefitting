<template>
  <div class="showroom-page">
    <div class="container">
      <header class="showroom-intro" v-scroll-reveal>
        <div class="intro-left">
          <h1 class="showroom-title serif-title">单车展厅</h1>
          <p class="page-subtitle">Aries 顶级单车几何数据库 & 选号中心</p>
        </div>

        <div class="data-status-wrapper">
          <div class="engine-status">
            <span class="status-dot"></span>
            <span class="status-text">Data: Real API</span>
          </div>
        </div>
      </header>

      <div class="showroom-layout">
        <aside class="sidebar-filters" v-scroll-reveal.delay="100">
          <div class="filter-group">
            <h3 class="filter-title">类型 / Category</h3>
            <div class="category-list">
              <button
                class="category-btn"
                :class="{ active: activeCategories.length === 0 }"
                @click="clearCategories"
              >
                <i class="fas fa-th-large"></i> 全部车型
              </button>
              <button
                v-for="cat in categories"
                :key="cat.id"
                class="category-btn"
                :class="{ active: activeCategories.includes(cat.id) }"
                @click="toggleCategory(cat.id)"
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
                :class="{ active: activeBrands.length === 0 }"
                @click="clearBrands"
              >
                全部
              </button>
              <button
                v-for="brand in brands"
                :key="brand"
                class="brand-chip"
                :class="{ active: activeBrands.includes(brand) }"
                @click="toggleBrand(brand)"
              >
                {{ brand }}
              </button>
            </div>
          </div>

          <div class="filter-group">
            <h3 class="filter-title">价格区间 / Price Range</h3>
            <div class="price-input-group">
              <div class="price-input-wrapper">
                <label class="price-label">最低价格</label>
                <input
                  type="number"
                  v-model="minPrice"
                  class="price-input"
                  :class="{ disabled: priceFilterApplied }"
                  :disabled="priceFilterApplied"
                  placeholder="最低价"
                />
              </div>
              <span class="price-separator">-</span>
              <div class="price-input-wrapper">
                <label class="price-label">最高价格</label>
                <input
                  type="number"
                  v-model="maxPrice"
                  class="price-input"
                  :class="{ disabled: priceFilterApplied }"
                  :disabled="priceFilterApplied"
                  placeholder="最高价"
                />
              </div>
            </div>
            <div class="price-btn-group">
              <button class="price-apply-btn" :class="{ applied: priceFilterApplied }" @click="handlePriceChange">
                {{ priceFilterApplied ? '✓ 已应用' : '应用筛选' }}
              </button>
              <button class="price-reset-btn" @click="resetPriceFilter" v-if="priceFilterApplied || minPrice || maxPrice">
                重置
              </button>
            </div>
          </div>
        </aside>

        <main class="bike-main">
          <div class="grid-header">
            <p class="results-count">为您找到 {{ total }} 款单车</p>
            <div class="search-box">
              <i class="fas fa-search"></i>
              <input type="text" v-model="searchQuery" @input="delaySearch" placeholder="搜索型号或关键词...">
            </div>
          </div>

          <TransitionGroup name="grid-fade" tag="div" class="bike-grid">
            <div
              v-for="(bike, index) in bikes"
              :key="bike.id"
              class="bike-card glass-card"
              v-scroll-reveal="{ delay: index * 18 }"
            >
              <div class="bike-img-wrapper" @click="goToDetail(bike.id)">
                <img :src="bike.img" :alt="bike.model" class="bike-img">
                <div class="card-overlay">
                  <span class="view-detail-text">查看详情</span>
                </div>
              </div>
              <div class="bike-info">
                <div class="info-top">
                  <div class="brand-container">
                    <span class="brand-cn">{{ bike.brandCn }}</span>
                    <span class="brand-en">{{ bike.brand }}</span>
                  </div>
                  <span class="price-val">¥{{ bike.price }}</span>
                </div>
                <h4 class="bike-model">{{ bike.model }}</h4>
                <p class="bike-desc">{{ bike.desc }}</p>
              </div>
              <!-- 收藏按钮 -->
              <button 
                class="favorite-btn" 
                :class="{ 'is-favorited': bike.isFavorited }"
                @click.stop="handleFavorite(bike)"
                :title="bike.isFavorited ? '取消收藏' : '收藏'"
              >
                <i class="fas fa-star"></i>
              </button>
            </div>
          </TransitionGroup>

          <div v-if="loadingMore" class="loading-more">
            <i class="fas fa-spinner fa-spin"></i>
            <p>正在加载更多单车...</p>
          </div>

          <div v-if="bikes.length === 0 && !loading" class="empty-state">
            <i class="fas fa-bicycle"></i>
            <p>没有找到符合条件的单车，尝试调整筛选条件。</p>
          </div>

          <!-- 分页组件 -->
          <div v-if="total > size && !loading" class="pagination-wrapper">
            <nav class="pagination" role="navigation" aria-label="分页">
              <button
                class="page-btn"
                :class="{ disabled: page === 1 }"
                @click="prevPage"
                :disabled="page === 1"
              >
                <i class="fas fa-chevron-left"></i> 上一页
              </button>

              <div class="page-numbers">
                <button
                  v-for="num in visiblePages"
                  :key="num"
                  class="page-num"
                  :class="{ active: page === num }"
                  @click="goToPage(num)"
                >
                  {{ num }}
                </button>
              </div>

              <button
                class="page-btn"
                :class="{ disabled: page >= totalPages }"
                @click="nextPage"
                :disabled="page >= totalPages"
              >
                下一页 <i class="fas fa-chevron-right"></i>
              </button>

              <span class="page-info">
                第 {{ page }} / {{ totalPages }} 页
              </span>
            </nav>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getShowroomBikes } from '@/api/bikeShowroom';
import { favoriteBikeModel, unfavoriteBikeModel, checkFavoriteStatus } from '@/api/favorite';
import { showToast } from '@/utils/toast';

const router = useRouter();

const brands = ref([]);
const categories = ref([]);
const bikes = ref([]);
const loading = ref(true);
const total = ref(0);

const activeCategories = ref([]);
const activeBrands = ref([]);
const searchQuery = ref('');

const page = ref(1);
const size = 18;

// 价格区间筛选
const minPrice = ref(null);
const maxPrice = ref(null);
const priceFilterApplied = ref(false);

let searchTimer = null;

// 总页数
const totalPages = computed(() => {
  return Math.ceil(total.value / size);
});

// 可见页码（最多显示7个页码）
const visiblePages = computed(() => {
  const total = totalPages.value;
  const current = page.value;
  const pages = [];

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 7; i++) pages.push(i);
    } else if (current >= total - 3) {
      for (let i = total - 6; i <= total; i++) pages.push(i);
    } else {
      for (let i = current - 3; i <= current + 3; i++) pages.push(i);
    }
  }

  return pages;
});

// 滚动到内容区域顶部
const scrollToContentTop = () => {
  // 滚动到页面顶部，考虑到导航栏高度
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
};

// 上一页
const prevPage = () => {
  if (page.value > 1) {
    page.value--;
    fetchData().then(() => {
      scrollToContentTop();
    });
  }
};

// 下一页
const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++;
    fetchData().then(() => {
      scrollToContentTop();
    });
  }
};

// 跳转到指定页
const goToPage = (num) => {
  if (num !== page.value && num >= 1 && num <= totalPages.value) {
    page.value = num;
    fetchData().then(() => {
      scrollToContentTop();
    });
  }
};

// 获取数据
const fetchData = async () => {
  try {
    loading.value = true;

    const bikeTypesStr = activeCategories.value.length > 0 ? activeCategories.value.join(',') : '';
    const brandsStr = activeBrands.value.length > 0 ? activeBrands.value.join(',') : '';

    const data = await getShowroomBikes({
      keyword: searchQuery.value.trim(),
      bikeTypes: bikeTypesStr,
      brands: brandsStr,
      minPrice: minPrice.value !== null && minPrice.value !== undefined ? minPrice.value : undefined,
      maxPrice: maxPrice.value !== null && maxPrice.value !== undefined ? maxPrice.value : undefined,
      page: page.value,
      size: size
    });

    total.value = data.total || 0;
    brands.value = data.brands || [];
    categories.value = data.categories || [];
    bikes.value = (data.bikes || []).map(bike => ({
      ...bike,
      isFavorited: false
    }));

    // 加载收藏状态
    await loadFavoriteStatus();

  } catch (err) {
    console.error('获取数据失败', err);
  } finally {
    loading.value = false;
  }
};

const toggleCategory = (catId) => {
  const index = activeCategories.value.indexOf(catId);
  if (index === -1) {
    activeCategories.value.push(catId);
  } else {
    activeCategories.value.splice(index, 1);
  }
  page.value = 1;
  fetchData();
};

const clearCategories = () => {
  activeCategories.value = [];
  page.value = 1;
  fetchData();
};

const toggleBrand = (brand) => {
  const index = activeBrands.value.indexOf(brand);
  if (index === -1) {
    activeBrands.value.push(brand);
  } else {
    activeBrands.value.splice(index, 1);
  }
  page.value = 1;
  fetchData();
};

const clearBrands = () => {
  activeBrands.value = [];
  page.value = 1;
  fetchData();
};

// 价格筛选变化处理
const handlePriceChange = () => {
  priceFilterApplied.value = true;
  page.value = 1;
  fetchData();
};

// 重置价格筛选
const resetPriceFilter = () => {
  minPrice.value = null;
  maxPrice.value = null;
  priceFilterApplied.value = false;
  page.value = 1;
  fetchData();
};

// 搜索防抖
const delaySearch = () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    page.value = 1;
    fetchData();
  }, 400);
};

// 跳转到详情页
const goToDetail = (id) => {
  // 保存当前滚动位置
  sessionStorage.setItem('showroomScrollTop', window.scrollY.toString());
  router.push(`/showroom/${id}`);
};

// 收藏/取消收藏
const handleFavorite = async (bike) => {
  try {
    if (bike.isFavorited) {
      // 取消收藏
      await unfavoriteBikeModel(bike.id);
      bike.isFavorited = false;
      showToast('已取消收藏', 'info');
    } else {
      // 添加收藏
      await favoriteBikeModel(bike.id);
      bike.isFavorited = true;
      showToast('收藏成功！可在个人中心查看', 'success');
    }
  } catch (err) {
    console.error('收藏操作失败', err);
    showToast('操作失败，请重试', 'error');
  }
};

// 加载收藏状态
const loadFavoriteStatus = async () => {
  try {
    for (const bike of bikes.value) {
      const status = await checkFavoriteStatus(bike.id);
      bike.isFavorited = status.data || status || false;
    }
  } catch (err) {
    console.error('加载收藏状态失败', err);
  }
};

// 保存滚动位置
const saveScrollPosition = () => {
  sessionStorage.setItem('showroomScrollTop', window.scrollY.toString());
};

// 恢复滚动位置
const restoreScrollPosition = () => {
  const scrollTop = sessionStorage.getItem('showroomScrollTop');
  if (scrollTop !== null) {
    window.scrollTo(0, parseInt(scrollTop));
    // 清除已恢复的位置
    sessionStorage.removeItem('showroomScrollTop');
  }
};

onMounted(() => {
  fetchData().then(() => {
    // 数据加载完成后恢复滚动位置
    setTimeout(restoreScrollPosition, 100);
  });

  // 监听页面切换时的滚动事件
  window.addEventListener('beforeunload', saveScrollPosition);
});

onUnmounted(() => {
  clearTimeout(searchTimer);
  window.removeEventListener('beforeunload', saveScrollPosition);
});
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
  background: rgba(92, 110, 88, 0.05);
  border: 1px solid rgba(92, 110, 88, 0.15);
  border-radius: 30px;
  cursor: default;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #2f80ed;
  box-shadow: 0 0 8px #2f80ed;
  animation: breathe 3s infinite ease-in-out;
}

.status-text {
  font-family: 'Lato', monospace;
  font-size: 0.75rem;
  letter-spacing: 2px;
  color: #2f80ed;
  font-weight: 700;
  text-transform: uppercase;
}

@keyframes breathe {
  0% { opacity: 0.4; transform: scale(0.8); box-shadow: 0 0 4px #2f80ed; }
  50% { opacity: 1; transform: scale(1.2); box-shadow: 0 0 12px #2f80ed; }
  100% { opacity: 0.4; transform: scale(0.8); box-shadow: 0 0 4px #2f80ed; }
}

.showroom-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 40px;
  align-items: start;
  padding-bottom: 100px;
}

.sidebar-filters {
  position: sticky;
  top: 120px;
  max-height: calc(100vh - 150px);
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-filters::-webkit-scrollbar {
  width: 6px;
}

.sidebar-filters::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-filters::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.sidebar-filters::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
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
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

/* 价格筛选样式 */
.price-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.price-input-wrapper {
  flex: 1;
}

.price-label {
  display: block;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-bottom: 5px;
}

.price-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(0,0,0,0.08);
  border-radius: 6px;
  font-size: 0.9rem;
  background: white;
  transition: 0.3s;
}

.price-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(92, 110, 88, 0.1);
}

.price-input::placeholder {
  color: var(--text-muted);
  font-size: 0.8rem;
}

.price-input.disabled {
  background: rgba(0,0,0,0.03);
  color: var(--text-muted);
  cursor: not-allowed;
  border-color: rgba(0,0,0,0.08);
}

.price-input.disabled:focus {
  outline: none;
  border-color: rgba(0,0,0,0.08);
  box-shadow: none;
}

.price-separator {
  font-size: 1rem;
  color: var(--text-muted);
  padding: 0 4px;
}

.price-btn-group {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.price-apply-btn {
  flex: 1;
  padding: 10px 16px;
  background: white;
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 6px;
  color: var(--text-muted);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.price-apply-btn:hover {
  background: rgba(0,0,0,0.03);
  color: var(--text-main);
}

.price-apply-btn.applied {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.price-apply-btn.applied:hover {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
}

.price-reset-btn {
  padding: 10px 16px;
  background: rgba(0,0,0,0.03);
  border: none;
  border-radius: 6px;
  color: var(--text-muted);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.price-reset-btn:hover {
  background: rgba(0,0,0,0.08);
  color: var(--text-main);
}

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
  border: 1px solid rgba(92, 110, 88, 0.12);
  position: relative;
}

.bike-card:hover {
  transform: translateY(-10px);
  border-color: rgba(92, 110, 88, 0.22);
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


.bike-info {
  padding: 25px;
}

.info-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.brand-container {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.brand-cn {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--primary-color);
  font-family: var(--font-serif);
  letter-spacing: 0.5px;
}

.brand-en {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--primary-color);
  text-transform: uppercase;
  letter-spacing: 1px;
  opacity: 0.8;
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

.small-tag {
  font-size: 0.65rem;
  background: rgba(0,0,0,0.04);
  padding: 4px 10px;
  border-radius: 4px;
  color: var(--text-muted);
  font-weight: 500;
}

.loading-more {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.loading-more i {
  font-size: 1.2rem;
  margin-bottom: 10px;
}

.no-more {
  grid-column: 1 / -1;
  text-align: center;
  padding: 30px 0;
  color: #999;
  font-size: 0.85rem;
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

.view-detail-text {
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 30px;
  backdrop-filter: blur(4px);
  cursor: pointer;
}

/* 收藏按钮样式 */
.favorite-btn {
  position: absolute;
  bottom: 15px;
  right: 15px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.favorite-btn i {
  font-size: 1.1rem;
  color: #ccc;
  transition: all 0.3s;
}

.favorite-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.favorite-btn:hover i {
  color: #ffd700;
}

.favorite-btn.is-favorited {
  background: #fff8e1;
}

.favorite-btn.is-favorited i {
  color: #ffd700;
  animation: starPulse 0.5s ease-in-out;
}

@keyframes starPulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

/* 分页组件样式 */
.pagination-wrapper {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  padding: 40px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  margin-top: 20px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.page-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 0.9rem;
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(.disabled) {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
}

.page-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 6px;
}

.page-num {
  width: 40px;
  height: 40px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.3s;
}

.page-num:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.page-num.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.page-info {
  margin-left: 15px;
  padding: 10px 15px;
  font-size: 0.85rem;
  color: var(--text-muted);
  background: rgba(0, 0, 0, 0.03);
  border-radius: 6px;
}

@media (max-width: 600px) {
  .page-info {
    display: none;
  }

  .page-btn span {
    display: none;
  }

  .page-btn {
    padding: 10px;
  }
}
</style>