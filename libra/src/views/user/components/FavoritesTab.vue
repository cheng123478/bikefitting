<template>
  <div class="favorites-tab">
    <div class="tab-header">
      <h2 class="tab-title">
        <i class="fas fa-star"></i> 我的收藏
      </h2>
      <p class="tab-desc">您收藏的单车车型和尺码</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <i class="fas fa-circle-notch fa-spin"></i>
      <p>正在加载收藏列表...</p>
    </div>

    <!-- 收藏列表 -->
    <div v-else-if="favorites.length > 0" class="favorites-grid">
      <div 
        v-for="favorite in favorites" 
        :key="favorite.id" 
        class="favorite-card glass-card"
      >
        <div class="favorite-image" @click="goToDetail(favorite.bikeModelId)">
          <img :src="favorite.bikeModelImg || defaultBikeImg" :alt="favorite.bikeModelName">
          <div class="image-overlay">
            <span class="view-text">查看详情</span>
          </div>
        </div>
        
        <div class="favorite-info">
          <div class="info-header">
            <div class="brand-info">
              <span class="brand-cn">{{ favorite.bikeModelBrandCn }}</span>
              <span class="brand-en">{{ favorite.bikeModelBrand }}</span>
            </div>
            <button 
              class="remove-fav-btn" 
              @click="handleRemoveFavorite(favorite.id)"
              title="取消收藏"
            >
              <i class="fas fa-trash-alt"></i>
            </button>
          </div>
          
          <h3 class="model-name">{{ favorite.bikeModelName }}</h3>
          
          <div class="favorite-details">
            <div v-if="favorite.size" class="detail-item">
              <i class="fas fa-ruler-horizontal"></i>
              <span>尺码: <strong>{{ favorite.size }}</strong></span>
            </div>
            <div class="detail-item">
              <i class="fas fa-bicycle"></i>
              <span>{{ getBikeTypeName(favorite.bikeModelType) }}</span>
            </div>
          </div>
          
          <div class="price-row">
            <span class="price">¥{{ favorite.bikeModelPrice }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">
        <i class="far fa-star"></i>
      </div>
      <h3>暂无收藏</h3>
      <p>您还没有收藏任何单车，去展厅看看吧！</p>
      <button class="go-showroom-btn" @click="goToShowroom">
        <i class="fas fa-arrow-right"></i> 前往单车展厅
      </button>
    </div>

    <!-- 分页 -->
    <div v-if="total > size && favorites.length > 0" class="pagination-wrapper">
      <nav class="pagination">
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
      </nav>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getFavoriteList, removeFavorite } from '@/api/favorite';
import { showToast, showConfirm } from '@/utils/toast';

const router = useRouter();

const favorites = ref([]);
const loading = ref(true);
const page = ref(1);
const size = 10;
const total = ref(0);

const defaultBikeImg = '/image/default-bike-card.png';

// 车型类型映射
const bikeTypeMap = {
  '1': '公路车',
  '2': '山地车',
  '3': 'Gravel',
  '4': '城市车'
};

const getBikeTypeName = (typeId) => {
  return bikeTypeMap[typeId] || typeId || '单车';
};

// 总页数
const totalPages = computed(() => {
  return Math.ceil(total.value / size);
});

// 可见页码
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

// 加载收藏列表
const loadFavorites = async () => {
  try {
    loading.value = true;
    const data = await getFavoriteList({
      page: page.value,
      size: size
    });
    
    favorites.value = data.records || data.list || [];
    total.value = data.total || 0;
  } catch (err) {
    console.error('加载收藏列表失败', err);
    showToast('加载失败，请重试', 'error');
  } finally {
    loading.value = false;
  }
};

// 取消收藏
const handleRemoveFavorite = async (favoriteId) => {
  const confirmed = await showConfirm('确定要取消收藏吗？', '取消收藏');
  
  if (confirmed) {
    try {
      await removeFavorite(favoriteId);
      showToast('已取消收藏', 'success');
      
      // 如果当前页只有一条数据且不是第一页，回到上一页
      if (favorites.value.length === 1 && page.value > 1) {
        page.value--;
      }
      
      await loadFavorites();
    } catch (err) {
      console.error('取消收藏失败', err);
      showToast('操作失败，请重试', 'error');
    }
  }
};

// 跳转到详情页
const goToDetail = (bikeModelId) => {
  router.push(`/showroom/${bikeModelId}`);
};

// 前往展厅
const goToShowroom = () => {
  router.push('/showroom');
};

// 分页操作
const prevPage = () => {
  if (page.value > 1) {
    page.value--;
    loadFavorites();
  }
};

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++;
    loadFavorites();
  }
};

const goToPage = (num) => {
  if (num !== page.value && num >= 1 && num <= totalPages.value) {
    page.value = num;
    loadFavorites();
  }
};

onMounted(() => {
  loadFavorites();
});
</script>

<style scoped>
.favorites-tab {
  width: 100%;
}

.tab-header {
  margin-bottom: 30px;
}

.tab-title {
  font-size: 1.5rem;
  color: var(--text-main);
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.tab-title i {
  color: #ffd700;
}

.tab-desc {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin: 0;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted);
}

.loading-state i {
  font-size: 2rem;
  margin-bottom: 15px;
  color: var(--primary-color);
}

/* 收藏网格 - 一行两个 */
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 25px;
  margin-bottom: 30px;
}

.favorite-card {
  overflow: hidden;
  transition: all 0.3s;
  border: 1px solid rgba(92, 110, 88, 0.12);
}

.favorite-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
  border-color: rgba(92, 110, 88, 0.25);
}

.favorite-image {
  position: relative;
  height: 200px;
  background: #f5f5f5;
  overflow: hidden;
  cursor: pointer;
}

.favorite-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.favorite-card:hover .favorite-image img {
  transform: scale(1.08);
}

.image-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  backdrop-filter: blur(2px);
}

.favorite-image:hover .image-overlay {
  opacity: 1;
}

.view-text {
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 30px;
  backdrop-filter: blur(4px);
}

.favorite-info {
  padding: 20px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.brand-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.brand-cn {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--primary-color);
  font-family: var(--font-serif);
}

.brand-en {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--primary-color);
  text-transform: uppercase;
  opacity: 0.8;
}

.remove-fav-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-fav-btn:hover {
  background: #ffebee;
  color: #f44336;
  transform: scale(1.1);
}

.model-name {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 12px 0;
}

.favorite-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 15px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.detail-item i {
  font-size: 0.8rem;
  color: var(--primary-color);
}

.price-row {
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.price {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--primary-color);
  font-family: 'Lato', monospace;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 25px;
  border-radius: 50%;
  background: rgba(255, 215, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon i {
  font-size: 3rem;
  color: #ffd700;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 1.3rem;
  color: var(--text-main);
  margin: 0 0 10px 0;
}

.empty-state p {
  font-size: 0.95rem;
  color: var(--text-muted);
  margin: 0 0 25px 0;
}

.go-showroom-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.go-showroom-btn:hover {
  background: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 30px 0 10px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
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

/* 响应式 */
@media (max-width: 768px) {
  .favorites-grid {
    grid-template-columns: 1fr;
  }
  
  .page-numbers {
    display: none;
  }
}
</style>
