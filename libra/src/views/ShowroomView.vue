<template>
  <div class="showroom-page">
    <!-- 智能推荐弹窗 -->
    <div v-if="showRecommendModal" class="simple-modal">
      <div class="simple-modal-overlay" @click="closeRecommendModal"></div>
      <div class="simple-modal-content recommend-modal">
        <div class="simple-modal-header">
          <h3>智能推荐</h3>
          <button @click="closeRecommendModal">×</button>
        </div>
        
        <!-- 步骤指示器 -->
        <div class="recommend-steps">
          <div 
            v-for="(step, index) in recommendStepsList" 
            :key="index"
            class="step-item"
            :class="{ active: recommendStep === getStepNumber(index), done: recommendStep > getStepNumber(index) }"
          >
            <span class="step-number">{{ index + 1 }}</span>
            <span class="step-text">{{ step }}</span>
          </div>
        </div>
        
        <div class="simple-modal-body">
          <!-- 步骤0：填写身体信息（仅当需要时显示） -->
          <div v-if="recommendStep === 0">
            <p class="body-info-intro">为了给您更精准的单车推荐，请先填写身高信息：</p>
            <div class="body-info-form">
              <div class="body-input-wrapper">
                <label>身高 (mm)</label>
                <input
                  type="number"
                  v-model="userHeight"
                  class="body-input"
                  placeholder="请输入身高"
                  min="1000"
                  max="2500"
                />
              </div>
            </div>
            <div class="body-info-more">
              <p>如想得到更精确的尺寸推荐，</p>
              <button class="simple-btn-outline" @click="goToBodyProfile">填写更多信息</button>
            </div>
          </div>
          
          <!-- 步骤1：选择场景 -->
          <div v-if="recommendStep === 1">
            <p>请选择您的骑行场景：</p>
            <div class="simple-scene-grid">
              <button
                v-for="scene in ridingScenes"
                :key="scene.id"
                :class="{ active: selectedScenes.includes(scene.id) }"
                @click="toggleScene(scene.id)"
              >
                {{ scene.name }}
              </button>
            </div>
          </div>
          
          <!-- 步骤2：填写价格区间 -->
          <div v-if="recommendStep === 2">
            <p>您选择的场景：<span class="selected-scenes">{{ getSelectedSceneNames() }}</span></p>
            <p class="price-label">请输入您的理想价格区间（可不填）：</p>
            <div class="recommend-price-input">
              <div class="price-input-wrapper">
                <label>最低价格</label>
                <input
                  type="number"
                  v-model="recommendMinPrice"
                  class="price-input"
                  placeholder="最低价"
                />
              </div>
              <span class="price-separator">-</span>
              <div class="price-input-wrapper">
                <label>最高价格</label>
                <input
                  type="number"
                  v-model="recommendMaxPrice"
                  class="price-input"
                  placeholder="最高价"
                />
              </div>
            </div>
          </div>
          
          <!-- 步骤3：显示推荐结果 -->
          <div v-if="recommendStep === 3">
            <div v-if="recommendLoading" class="loading-state">
              <i class="fas fa-spinner fa-spin"></i>
              <p>正在为您推荐最佳单车...</p>
            </div>
            <div v-else-if="recommendResults.length > 0">
              <p>为您推荐以下 {{ recommendResults.length }} 款单车：</p>
              <div class="recommend-results">
                <div 
                  v-for="(bike, index) in recommendResults" 
                  :key="bike.id"
                  class="recommend-item"
                  @click="goToDetail(bike.id, true)"
                >
                  <div class="result-rank">{{ index + 1 }}</div>
                  <img :src="bike.imageUrl || bike.img" :alt="bike.model" class="result-img">
                  <div class="result-info">
                    <div class="result-brand">{{ bike.brandCn }} {{ bike.brand }}</div>
                    <div class="result-model">{{ bike.model }} ({{ bike.size }})</div>
                    <div class="result-price">¥{{ bike.price }}</div>
                  </div>
                  <div class="result-score">
                    <div class="score-label">推荐分</div>
                    <div class="score-value">{{ bike.totalScore.toFixed(1) }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <i class="fas fa-bicycle"></i>
              <p>暂无符合条件的推荐单车</p>
            </div>
          </div>
        </div>
        
        <div class="simple-modal-footer">
          <button 
            v-if="recommendStep === 0"
            class="simple-btn-cancel" 
            @click="closeRecommendModal"
          >
            取消
          </button>
          <button 
            v-if="recommendStep === 1"
            class="simple-btn-cancel" 
            @click="closeRecommendModal"
          >
            取消
          </button>
          <button 
            v-if="recommendStep === 2"
            class="simple-btn-cancel" 
            @click="goBackStep"
          >
            上一步
          </button>
          <button 
            v-if="recommendStep === 3"
            class="simple-btn-cancel" 
            @click="closeRecommendModal"
          >
            关闭
          </button>
          
          <button 
            v-if="recommendStep === 0"
            class="simple-btn-confirm" 
            :disabled="!isBodyInfoValid" 
            @click="goToSceneStep"
          >
            下一步
          </button>
          <button 
            v-if="recommendStep === 1"
            class="simple-btn-confirm" 
            :disabled="selectedScenes.length === 0" 
            @click="goToPriceStep"
          >
            下一步
          </button>
          <button 
            v-if="recommendStep === 2"
            class="simple-btn-confirm" 
            @click="confirmRecommend"
          >
            {{ recommendLoading ? '推荐中...' : '确认推荐' }}
          </button>
        </div>
      </div>
    </div>

    <div class="container">
      <header class="showroom-intro" v-scroll-reveal>
        <div class="intro-left">
          <h1 class="showroom-title serif-title">单车展厅</h1>
          <p class="page-subtitle">Aries 顶级单车几何数据库 & 选号中心</p>
        </div>

        <div class="header-right">
          <div class="engine-status">
            <span class="status-dot"></span>
            <span class="status-text">Data: Real API</span>
          </div>
          <div class="header-actions">
            <button 
              class="smart-recommend-btn" 
              @click="handleSmartRecommend"
            >
              <i class="fas fa-robot"></i>
              <span>智能推荐</span>
            </button>
            <button 
              class="smart-recommend-btn" 
              :class="{ active: showFavoritesOnly }"
              @click="toggleFavoritesView"
            >
              <i class="fas fa-heart"></i>
              <span>我的收藏</span>
            </button>
          </div>
        </div>
      </header>

      <div class="showroom-layout" :class="{ 'full-width': showFavoritesOnly }">
        <aside v-if="!showFavoritesOnly" class="sidebar-filters" v-scroll-reveal.delay="100">
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
            <div v-if="!showFavoritesOnly" class="search-box">
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
              <!-- 收藏按钮 - 绝对定位在右下角 -->
              <button 
                class="favorite-btn" 
                :class="{ favorited: isFavorite(bike.id) }"
                @click.stop="handleFavorite(bike.id)"
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
import { useUserStore } from '@/stores/user';
import { getShowroomBikes, recommendBikes, toggleFavorite } from '@/api/bikeShowroom';
import { saveUserBody, getLatestUserBody } from '@/api/userBody';
import { showToast } from '@/utils/toast';
import { getFavoriteList } from '@/api/favorite';

const router = useRouter();
const userStore = useUserStore();

const brands = ref([]);
const categories = ref([]);
const bikes = ref([]);
const loading = ref(true);
const loadingMore = ref(false);
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

// 智能推荐弹窗相关
const showRecommendModal = ref(false);
const recommendStep = ref(1); // 1: 选择场景, 2: 填写价格, 3: 显示结果, 0: 填写身高跨高(如果需要)
const selectedScenes = ref([]);
const recommendMinPrice = ref(null);
const recommendMaxPrice = ref(null);
const recommendResults = ref([]);
const recommendLoading = ref(false);
const userHeight = ref('');
const userInseam = ref('');
const userBodyInfo = ref(null); // 存储从后端获取的用户身体信息
const needsBodyInfoStep = ref(true); // 是否需要身体信息步骤
const ridingScenes = [
  { id: 1, name: '日常通勤代步', icon: 'fas fa-building' },
  { id: 2, name: '运动健身骑行', icon: 'fas fa-heartbeat' },
  { id: 3, name: '山地越野骑行', icon: 'fas fa-tree' },
  { id: 4, name: '长途户外旅行', icon: 'fas fa-map-pin' },
  { id: 5, name: '速度竞技竞速', icon: 'fas fa-trophy' }
];

// 收藏相关
const favoriteBikeIds = ref([]);
const showFavoritesOnly = ref(false);

let searchTimer = null;

// 步骤列表（动态计算，根据是否需要身体信息）
const recommendStepsList = computed(() => {
  if (needsBodyInfoStep.value) {
    return ['身体信息', '选择场景', '价格区间', '推荐结果'];
  }
  return ['选择场景', '价格区间', '推荐结果'];
});

// 获取步骤编号（考虑是否有身体信息步骤）
const getStepNumber = (index) => {
  if (needsBodyInfoStep.value) {
    return index; // 0, 1, 2, 3
  }
  return index + 1; // 1, 2, 3
};

// 身体信息是否有效（单位：毫米）- 现在只检查身高
const isBodyInfoValid = computed(() => {
  const height = parseFloat(userHeight.value);
  return !isNaN(height) && height >= 1000 && height <= 2500;
});

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
    bikes.value = data.bikes || [];

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

// 智能推荐按钮点击处理
const handleSmartRecommend = async () => {
  // 如果用户未登录，打开登录模态框
  if (!userStore.isLoggedIn) {
    userStore.openAuthModal();
    return;
  }
  
  showRecommendModal.value = true;
  selectedScenes.value = [];
  recommendMinPrice.value = null;
  recommendMaxPrice.value = null;
  recommendResults.value = [];
  userHeight.value = '';
  userInseam.value = '';
  userBodyInfo.value = null;
  
  // 判断是否需要填写身高跨高
  let needsBodyInfo = true;
  
  try {
    // 从后端获取用户最新的身体信息
    const response = await getLatestUserBody();
    userBodyInfo.value = response;
    
    // 检查是否有身高和跨高数据（后端返回的字段名是 spanHeight）
    if (response && response.height && response.spanHeight) {
      // 有完整的身高和跨高信息，不需要填写
      needsBodyInfo = false;
    } else if (response && response.height) {
      // 有身高但没有跨高，需要显示填写弹窗，并预填身高
      needsBodyInfo = true;
      userHeight.value = response.height.toString();
    }
  } catch (error) {
    console.error('获取用户身体信息失败', error);
    // 获取失败时，需要用户填写
    needsBodyInfo = true;
  }
  
  // 设置是否需要身体信息步骤
  needsBodyInfoStep.value = needsBodyInfo;
  
  if (needsBodyInfo) {
    // 需要填写身高跨高，从步骤0开始
    recommendStep.value = 0;
  } else {
    // 不需要填写，直接从选择场景开始
    recommendStep.value = 1;
  }
};

// 关闭推荐弹窗
const closeRecommendModal = () => {
  showRecommendModal.value = false;
  recommendStep.value = 1;
  selectedScenes.value = [];
  recommendMinPrice.value = null;
  recommendMaxPrice.value = null;
  recommendResults.value = [];
};

// 切换场景选择
const toggleScene = (sceneId) => {
  const index = selectedScenes.value.indexOf(sceneId);
  if (index === -1) {
    selectedScenes.value.push(sceneId);
  } else {
    selectedScenes.value.splice(index, 1);
  }
};

// 下一步（从场景选择到价格填写）
const goToPriceStep = () => {
  if (selectedScenes.value.length === 0) {
    showToast('请至少选择一个骑行场景', 'warning');
    return;
  }
  recommendStep.value = 2;
};

// 返回上一步
const goBackStep = () => {
  if (recommendStep.value > 0) {
    recommendStep.value--;
  }
};

// 从身体信息步骤到场景选择步骤
const goToSceneStep = async () => {
  if (!isBodyInfoValid.value) {
    showToast('请填写有效的身高信息', 'warning');
    return;
  }

  // 如果用户已登录，保存身高信息到用户资料
  if (userStore.isLoggedIn) {
    try {
      // 调用API保存用户身体信息
      await saveUserBody({
        userId: userStore.userInfo.id,
        height: parseFloat(userHeight.value)
      });
      // 更新本地用户信息
      userStore.userInfo.height = userHeight.value;
    } catch (error) {
      console.error('保存身体信息失败', error);
      // 即使保存失败也继续，因为本地已经记录了
    }
  }

  recommendStep.value = 1;
};

// 跳转到身体图谱页面
const goToBodyProfile = () => {
  // 保存当前弹窗状态，用于返回
  sessionStorage.setItem('showroomRecommendModal', JSON.stringify({
    showRecommendModal: true,
    recommendStep: recommendStep.value,
    selectedScenes: selectedScenes.value,
    recommendMinPrice: recommendMinPrice.value,
    recommendMaxPrice: recommendMaxPrice.value,
    userHeight: userHeight.value
  }));
  router.push('/lab/body');
};

// 确认推荐（发送请求到后端）
const confirmRecommend = async () => {
  recommendLoading.value = true;
  try {
    // 准备请求参数
    const requestData = {
      scenes: selectedScenes.value,
      minPrice: recommendMinPrice.value || undefined,
      maxPrice: recommendMaxPrice.value || undefined
    };
    
    // 添加身高信息（优先使用用户填写的，其次使用从后端获取的）
    if (userHeight.value) {
      requestData.height = parseInt(userHeight.value);
    } else if (userBodyInfo.value && userBodyInfo.value.height) {
      requestData.height = userBodyInfo.value.height;
    }
    
    // 添加跨高信息（如果用户填写了）
    if (userInseam.value) {
      requestData.inseam = parseInt(userInseam.value);
    } else if (userBodyInfo.value && userBodyInfo.value.spanHeight) {
      requestData.inseam = userBodyInfo.value.spanHeight;
    }
    
    console.log('推荐请求参数:', requestData);
    
    const response = await recommendBikes(requestData);
    recommendResults.value = response; // 响应拦截器已直接返回 data
    recommendStep.value = 3;
  } catch (error) {
    console.error('推荐请求失败', error);
    showToast('推荐请求失败，请稍后重试', 'error');
  } finally {
    recommendLoading.value = false;
  }
};

// 获取选中场景的名称
const getSelectedSceneNames = () => {
  return ridingScenes
    .filter(scene => selectedScenes.value.includes(scene.id))
    .map(scene => scene.name)
    .join('、');
};

// 跳转到详情页
const goToDetail = (id, fromRecommend = false) => {
  // 保存当前滚动位置
  sessionStorage.setItem('showroomScrollTop', window.scrollY.toString());
  // 保存筛选条件
  const filterState = {
    activeCategories: activeCategories.value,
    activeBrands: activeBrands.value,
    searchQuery: searchQuery.value,
    minPrice: minPrice.value,
    maxPrice: maxPrice.value,
    priceFilterApplied: priceFilterApplied.value,
    page: page.value,
    fromRecommend: fromRecommend
  };
  sessionStorage.setItem('showroomFilterState', JSON.stringify(filterState));
  
  // 如果从推荐列表跳转，保存推荐结果
  if (fromRecommend && recommendResults.value.length > 0) {
    sessionStorage.setItem('recommendResults', JSON.stringify(recommendResults.value));
    
    // 查找点击的车型数据，获取推荐尺寸
    const clickedBike = recommendResults.value.find(bike => bike.id === id);
    if (clickedBike && clickedBike.recommendedSize) {
      // 保存推荐尺寸信息到sessionStorage
      sessionStorage.setItem('recommendSizeData', JSON.stringify({
        recommendedSize: clickedBike.recommendedSize
      }));
    }
  }
  
  router.push(`/showroom/${id}`);
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
  // 恢复筛选条件
  const savedFilter = sessionStorage.getItem('showroomFilterState');
  if (savedFilter) {
    try {
      const filterState = JSON.parse(savedFilter);
      activeCategories.value = filterState.activeCategories || [];
      activeBrands.value = filterState.activeBrands || [];
      searchQuery.value = filterState.searchQuery || '';
      minPrice.value = filterState.minPrice;
      maxPrice.value = filterState.maxPrice;
      priceFilterApplied.value = filterState.priceFilterApplied || false;
      page.value = filterState.page || 1;
      // 清除已恢复的筛选条件
      sessionStorage.removeItem('showroomFilterState');
    } catch (e) {
      console.error('恢复筛选条件失败', e);
    }
  }

  // 检查是否从身体图谱页面返回，如果是则恢复推荐弹窗状态
  const recommendModalState = sessionStorage.getItem('showroomRecommendModal');
  if (recommendModalState) {
    try {
      const state = JSON.parse(recommendModalState);
      showRecommendModal.value = state.showRecommendModal;
      recommendStep.value = state.recommendStep;
      selectedScenes.value = state.selectedScenes || [];
      recommendMinPrice.value = state.recommendMinPrice;
      recommendMaxPrice.value = state.recommendMaxPrice;
      userHeight.value = state.userHeight || '';
      sessionStorage.removeItem('showroomRecommendModal');
    } catch (e) {
      console.error('恢复推荐弹窗状态失败', e);
    }
  }

  // 检查是否从推荐详情页返回，如果是则自动打开推荐弹窗
  const isFromRecommend = sessionStorage.getItem('recommendBack');
  if (isFromRecommend === 'true') {
    sessionStorage.removeItem('recommendBack');
    
    // 恢复推荐结果
    const savedResults = sessionStorage.getItem('recommendResults');
    if (savedResults) {
      try {
        recommendResults.value = JSON.parse(savedResults);
        sessionStorage.removeItem('recommendResults');
      } catch (e) {
        console.error('恢复推荐结果失败', e);
      }
    }
    
    showRecommendModal.value = true;
    recommendStep.value = 3; // 直接显示推荐结果
  }

  fetchData().then(() => {
    // 数据加载完成后恢复滚动位置
    setTimeout(restoreScrollPosition, 100);
  });

  // 加载用户收藏列表
  loadUserFavorites();

  // 监听页面切换时的滚动事件
  window.addEventListener('beforeunload', saveScrollPosition);
});

onUnmounted(() => {
  clearTimeout(searchTimer);
  window.removeEventListener('beforeunload', saveScrollPosition);
});

// 加载用户收藏的单车ID列表
const loadUserFavorites = async () => {
  try {
    // 使用与个人中心相同的接口获取收藏列表
    const data = await getFavoriteList({
      page: 1,
      size: 100
    });
    
    const records = data.records || data.list || [];
    favoriteBikeIds.value = records.map(item => item.bikeModelId);
  } catch (error) {
    console.error('加载收藏列表失败', error);
    favoriteBikeIds.value = [];
  }
};

// 切换收藏视图
const toggleFavoritesView = async () => {
  // 检查登录状态
  if (!userStore.isLoggedIn) {
    showToast('请先登录后查看我的收藏', 'warning');
    return;
  }
  
  showFavoritesOnly.value = !showFavoritesOnly.value;
  if (showFavoritesOnly.value) {
    // 如果切换到收藏视图，直接从后端获取收藏的车辆详情
    await fetchFavoriteBikes();
  } else {
    // 切换回普通视图，刷新全部数据
    await fetchData();
  }
};

// 获取收藏的车辆详情
const fetchFavoriteBikes = async () => {
  try {
    loading.value = true;
    
    // 使用与个人中心相同的接口获取收藏列表
    const data = await getFavoriteList({
      page: 1,
      size: 100 // 获取足够的收藏数量
    });
    
    console.log('收藏列表数据:', data);
    
    const records = data.records || data.list || [];
    
    if (records.length === 0) {
      // 没有收藏的单车
      console.log('没有收藏的单车');
      bikes.value = [];
      total.value = 0;
      favoriteBikeIds.value = [];
      return;
    }
    
    // 将收藏列表转换为展厅格式
    bikes.value = records.map(item => ({
      id: item.bikeModelId,
      brand: item.bikeModelBrand,
      brandCn: item.bikeModelBrandCn,
      model: item.bikeModelName,
      img: item.bikeModelImg,
      desc: item.bikeModelDesc || '',
      bikeType: item.bikeModelType,
      price: item.bikeModelPrice
    }));
    
    total.value = data.total || bikes.value.length;
    
    // 更新本地收藏ID列表
    favoriteBikeIds.value = bikes.value.map(bike => bike.id);
    
    console.log('设置的bikes:', bikes.value);
    console.log('收藏ID列表:', favoriteBikeIds.value);
  } catch (err) {
    console.error('获取收藏车辆失败', err);
    bikes.value = [];
    total.value = 0;
    favoriteBikeIds.value = [];
  } finally {
    loading.value = false;
  }
};

// 判断单车是否已收藏
const isFavorite = (bikeId) => {
  return favoriteBikeIds.value.includes(bikeId);
};

// 处理收藏/取消收藏
const handleFavorite = async (bikeId) => {
  // 检查登录状态
  if (!userStore.isLoggedIn) {
    showToast('请先登录后再收藏', 'warning');
    return;
  }
  
  try {
    const isFavorited = await toggleFavorite(bikeId);
    if (isFavorited) {
      favoriteBikeIds.value.push(bikeId);
      showToast('收藏成功', 'success');
    } else {
      const index = favoriteBikeIds.value.indexOf(bikeId);
      if (index > -1) {
        favoriteBikeIds.value.splice(index, 1);
      }
      showToast('已取消收藏', 'success');
    }
    
    // 如果当前在收藏视图，更新收藏列表
    if (showFavoritesOnly.value) {
      await fetchFavoriteBikes();
    }
  } catch (error) {
    console.error('操作收藏失败', error);
    showToast('操作失败，请稍后重试', 'error');
  }
};
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

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
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

.showroom-layout.full-width {
  grid-template-columns: 1fr;
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

.smart-recommend-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: linear-gradient(135deg, #5c6e58 0%, #4a5c48 100%);
  border: none;
  border-radius: 20px;
  color: white;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(92, 110, 88, 0.3);
}

.smart-recommend-btn:hover {
  background: linear-gradient(135deg, #6b7f67 0%, #5a6e56 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.4);
}

.smart-recommend-btn:active {
  transform: translateY(0);
}

.smart-recommend-btn i {
  font-size: 0.9rem;
}

.smart-recommend-btn.active {
  background: linear-gradient(135deg, #e6a817 0%, #d4941a 100%);
  box-shadow: 0 4px 12px rgba(230, 168, 23, 0.4);
}

.smart-recommend-btn.active:hover {
  background: linear-gradient(135deg, #f0b422 0%, #e6a817 100%);
  box-shadow: 0 4px 15px rgba(230, 168, 23, 0.5);
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
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
  border: 0.5px solid rgba(0, 0, 0, 0.03);
  border-radius: 8px;
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

.favorite-btn {
  position: absolute;
  right: 15px;
  bottom: 15px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.4rem;
  color: #ccc;
  transition: all 0.3s ease;
  padding: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.favorite-btn:hover {
  color: #ffc107;
  transform: scale(1.15);
}

.favorite-btn.favorited {
  color: #ffc107;
}

.favorite-btn.favorited:hover {
  color: #e6b000;
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

/* 简单弹窗样式 */
.simple-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.simple-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
}

.simple-modal-content {
  position: relative;
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 450px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.simple-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.simple-modal-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #333;
}

.simple-modal-header button {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0 8px;
}

.simple-modal-header button:hover {
  color: #666;
}

.simple-modal-body {
  padding: 20px;
}

.simple-modal-body p {
  margin: 0 0 16px 0;
  color: #666;
  font-size: 0.95rem;
}

.simple-scene-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.simple-scene-grid button {
  padding: 12px 16px;
  background: #f5f5f5;
  border: 2px solid transparent;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
}

.simple-scene-grid button:hover {
  background: #eee;
}

.simple-scene-grid button.active {
  background: rgba(92, 110, 88, 0.1);
  border-color: #5c6e58;
  color: #5c6e58;
}

.simple-modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #eee;
  justify-content: flex-end;
}

.simple-btn-cancel {
  padding: 10px 20px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  color: #666;
  cursor: pointer;
}

.simple-btn-cancel:hover {
  background: #eee;
}

.simple-btn-confirm {
  padding: 10px 24px;
  background: #5c6e58;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  color: white;
  cursor: pointer;
}

.simple-btn-confirm:hover:not(:disabled) {
  background: #4a5c48;
}

.simple-btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 智能推荐弹窗样式 */
.recommend-modal {
  max-width: 550px;
}

.recommend-steps {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 0.85rem;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 600;
}

.step-item.active {
  color: #5c6e58;
}

.step-item.active .step-number {
  background: #5c6e58;
  color: white;
}

.step-item.done .step-number {
  background: #5c6e58;
  color: white;
}

.selected-scenes {
  color: #5c6e58;
  font-weight: 600;
}

/* 身体信息填写样式 */
.body-info-intro {
  color: #666;
  margin-bottom: 20px;
  font-size: 0.95rem;
}

.body-info-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 15px;
}

.body-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.body-input-wrapper label {
  font-size: 0.85rem;
  color: #333;
  font-weight: 500;
}

.body-input {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.body-input:focus {
  outline: none;
  border-color: #5c6e58;
}

.body-input::placeholder {
  color: #aaa;
}



.body-info-more {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px dashed #eee;
  text-align: center;
}

.body-info-more p {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 12px;
}

.simple-btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  border: 1px solid #5c6e58;
  border-radius: 8px;
  background: transparent;
  color: #5c6e58;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.simple-btn-outline:hover {
  background: #5c6e58;
  color: white;
}

.recommend-price-input {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recommend-price-input .price-input-wrapper {
  flex: 1;
}

.recommend-price-input .price-input-wrapper label {
  display: block;
  font-size: 0.75rem;
  color: #999;
  margin-bottom: 5px;
}

.recommend-price-input .price-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}

.recommend-price-input .price-input:focus {
  outline: none;
  border-color: #5c6e58;
}

.recommend-price-input .price-separator {
  font-size: 1rem;
  color: #999;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 0;
  gap: 12px;
}

.loading-state i {
  font-size: 1.5rem;
  color: #5c6e58;
}

.loading-state p {
  color: #666;
  font-size: 0.9rem;
}

.recommend-results {
  max-height: 400px;
  overflow-y: auto;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.recommend-item:hover {
  background: #fafafa;
}

.recommend-item:last-child {
  border-bottom: none;
}

.result-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #5c6e58;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
}

.result-img {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-brand {
  font-size: 0.75rem;
  color: #5c6e58;
  font-weight: 600;
  margin-bottom: 4px;
}

.result-model {
  font-size: 0.9rem;
  color: #333;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-price {
  font-size: 0.85rem;
  color: #666;
}

.result-score {
  text-align: center;
  flex-shrink: 0;
}

.score-label {
  font-size: 0.65rem;
  color: #999;
  margin-bottom: 2px;
}

.score-value {
  font-size: 1.1rem;
  font-weight: 700;
  color: #5c6e58;
}

.recommend-modal .empty-state {
  text-align: center;
  padding: 30px 0;
  color: #999;
}

.recommend-modal .empty-state i {
  font-size: 2.5rem;
  margin-bottom: 10px;
  color: #ddd;
}
</style>