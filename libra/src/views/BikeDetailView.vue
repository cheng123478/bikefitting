<template>
  <div class="bike-detail-page">
    <div class="container">
      <!-- 头部导航 -->
      <header class="detail-header" v-scroll-reveal>
        <button class="back-btn" @click="handleBack()">
          <i class="fas fa-arrow-left"></i> {{ fromRecommend ? '返回推荐列表' : '返回展厅' }}
        </button>
        <div class="header-status">
          <span class="status-dot"></span>
          <span class="status-text">Data: Real API</span>
        </div>
      </header>

      <div class="detail-content">
        <!-- 左侧：单车图片展示 -->
        <aside class="image-section" v-scroll-reveal>
          <div class="main-image-wrapper glass-card">
            <img :src="bikeDetail.img" :alt="bikeDetail.model" class="main-image">
          </div>
          <div class="image-info">
            <span class="brand-tag">{{ bikeDetail.brand }}</span>
            <span v-if="getBikeTypeName(bikeDetail.bikeType)" class="type-tag">{{ getBikeTypeName(bikeDetail.bikeType) }}</span>
            <h1 class="bike-title serif-title">{{ bikeDetail.model }}</h1>
            <div class="bike-desc">
              <span v-for="(sentence, index) in splitByPunctuation(bikeDetail.desc)" :key="index">
                {{ sentence }}<br><br>
              </span>
            </div>
          </div>
        </aside>

        <!-- 右侧：详细信息 -->
        <main class="info-section">
          <!-- 价格信息 -->
          <div class="price-card glass-card" v-scroll-reveal.delay="100">
            <div class="price-label">官网建议零售价</div>
            <div class="price-value">¥{{ bikeDetail.price }}</div>
            <div class="price-note">价格仅供参考，实际价格请咨询经销商</div>
          </div>

          <!-- 尺寸选择按钮 -->
          <div class="size-select-card glass-card" v-scroll-reveal.delay="150">
            <h3 class="card-title">
              <i class="fas fa-shoe-prints"></i> 选择尺寸 (Select Size)
            </h3>
            <div class="size-buttons">
              <button
                v-for="size in allSizes"
                :key="size"
                class="size-btn"
                :class="{ active: selectedSize === size }"
                @click="selectSize(size)"
              >
                {{ size }}
              </button>
            </div>
          </div>

          <!-- 车架几何数据 -->
          <div class="geometry-card glass-card" v-scroll-reveal.delay="200">
            <h3 class="card-title">
              <i class="fas fa-ruler"></i> 车架几何 (Frame Geometry)
            </h3>
            <div class="geometry-grid">
              <div class="geo-item">
                <span class="geo-label">堆高 (Stack)</span>
                <span class="geo-value">{{ currentGeometry.stack || '-' }}<span class="geo-unit">mm</span></span>
              </div>
              <div class="geo-item">
                <span class="geo-label">前伸量 (Reach)</span>
                <span class="geo-value">{{ currentGeometry.reach || '-' }}<span class="geo-unit">mm</span></span>
              </div>
              <div class="geo-item">
                <span class="geo-label">立管角度 (STA)</span>
                <span class="geo-value">{{ currentGeometry.seatTubeAngle || '-' }}<span class="geo-unit">°</span></span>
              </div>
              <div class="geo-item">
                <span class="geo-label">头管角度 (HTA)</span>
                <span class="geo-value">{{ currentGeometry.headTubeAngle || '-' }}<span class="geo-unit">°</span></span>
              </div>
              <div class="geo-item">
                <span class="geo-label">五通下沉 (BB Drop)</span>
                <span class="geo-value">{{ currentGeometry.bbDrop || '-' }}<span class="geo-unit">mm</span></span>
              </div>
              <div class="geo-item">
                <span class="geo-label">轴距 (Wheelbase)</span>
                <span class="geo-value">{{ currentGeometry.wheelBase || '-' }}<span class="geo-unit">mm</span></span>
              </div>
              <div class="geo-item">
                <span class="geo-label">跨高 (Standover)</span>
                <span class="geo-value">{{ currentGeometry.standOverHeight || '-' }}<span class="geo-unit">mm</span></span>
              </div>
      
            </div>
          </div>

          
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { getBikeDetail, getBikeByModel } from '@/api/bikeShowroom';

const route = useRoute();

// 车型类型映射
const bikeTypeMap = {
  '1': '公路车',
  '2': '山地车',
  '3': 'Gravel',
  '4': '城市车'
};

// 获取车型类型中文名称
const getBikeTypeName = (typeId) => {
  return bikeTypeMap[typeId] || typeId || '';
};

// 按"标点符号+空格"分割句子（句号、感叹号、问号后面跟空格时换行）
const splitByPunctuation = (text) => {
  if (!text) return [];
  return text.split(/(?<=[。！？.!?——] )/).filter(s => s.trim());
};

// 主单车数据
const bikeDetail = ref({
  id: '',
  brand: '',
  model: '',
  bikeType: '',
  desc: '',
  price: '',
  img: '',
  stack: '',
  reach: '',
  seatTubeAngle: '',
  headTubeAngle: '',
  bbDrop: '',
  wheelBase: '',
  standOverHeight: '',
  size: '',
  sizes: []
});

// 同一车型的所有尺寸数据
const bikeVariants = ref([]);

// 当前选中的尺寸
const selectedSize = ref('');

// 是否来自推荐列表
const fromRecommend = ref(false);

// 获取所有尺寸列表（去重排序）
const allSizes = computed(() => {
  const sizes = new Set();
  bikeVariants.value.forEach(v => {
    if (v.size) sizes.add(v.size);
  });
  return Array.from(sizes).sort((a, b) => {
    // 尝试按数字排序
    const numA = parseFloat(a);
    const numB = parseFloat(b);
    if (!isNaN(numA) && !isNaN(numB)) return numA - numB;
    return a.localeCompare(b);
  });
});

// 根据选中尺寸获取对应的几何数据
const currentGeometry = computed(() => {
  if (!selectedSize.value) return bikeDetail.value;
  const variant = bikeVariants.value.find(v => v.size === selectedSize.value);
  return variant || bikeDetail.value;
});

// 切换尺寸
const selectSize = (size) => {
  selectedSize.value = size;
};

// 返回处理
const handleBack = () => {
  // 如果来自推荐列表，返回展厅页面后需要保持推荐弹窗打开
  if (fromRecommend.value) {
    sessionStorage.setItem('recommendBack', 'true');
  }
  window.history.back();
};

onMounted(async () => {
  // 读取来源信息
  try {
    const filterState = sessionStorage.getItem('showroomFilterState');
    if (filterState) {
      const state = JSON.parse(filterState);
      fromRecommend.value = state.fromRecommend || false;
    }
  } catch (e) {
    console.error('读取来源信息失败', e);
  }

  const id = route.params.id;
  try {
    // 获取单车详情
    const detailData = await getBikeDetail(id);
    bikeDetail.value = {
      ...bikeDetail.value,
      ...(detailData.data || detailData)
    };

    // 获取同一车型的所有尺寸数据（传递品牌以确保精确匹配）
    if (bikeDetail.value.model) {
      const variantsData = await getBikeByModel(bikeDetail.value.model, bikeDetail.value.brand);
      bikeVariants.value = variantsData.data || variantsData || [];
      
      // 默认选中当前尺寸或第一个尺寸
      if (bikeDetail.value.size) {
        selectedSize.value = bikeDetail.value.size;
      } else if (allSizes.value.length > 0) {
        selectedSize.value = allSizes.value[0];
      }
    }
  } catch (err) {
    console.error('获取单车详情失败', err);
  }
});
</script>

<style scoped>
.bike-detail-page {
  padding: 120px 0 100px;
  min-height: 100vh;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 50px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  background: none;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  color: var(--text-main);
  font-size: 0.95rem;
  cursor: pointer;
  transition: 0.3s;
}

.back-btn:hover {
  background: rgba(92, 110, 88, 0.05);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.header-status {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 8px 18px;
  background: transparent;
  border: 1px solid rgba(92, 110, 88, 0.15);
  border-radius: 30px;
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
  letter-spacing: 1px;
  color: #2f80ed;
  font-weight: 700;
  text-transform: uppercase;
}

@keyframes breathe {
  0% { opacity: 0.4; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
  100% { opacity: 0.4; transform: scale(0.8); }
}

.detail-content {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 50px;
}

.image-section {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.main-image-wrapper {
  overflow: hidden;
  background: #f8f8f8;
}

.main-image {
  width: 100%;
  height: 450px;
  object-fit: cover;
}

.image-info {
  padding: 20px 0;
}

.brand-tag {
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--primary-color);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.type-tag {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-muted);
  background: rgba(0, 0, 0, 0.05);
  padding: 4px 12px;
  border-radius: 12px;
  margin-left: 10px;
  text-transform: capitalize;
}

.bike-title {
  font-family: var(--font-serif);
  font-size: 2.5rem;
  color: var(--text-main);
  font-weight: 700;
  margin: 10px 0 15px 0;
}

.bike-desc {
  font-size: 1rem;
  color: var(--text-muted);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.glass-card {
  background: var(--bg-surface);
  border: var(--border-glass);
  border-radius: var(--radius-md);
  padding: 30px;
}

.card-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title i {
  color: var(--primary-color);
}

.price-card {
  text-align: center;
  background: linear-gradient(135deg, rgba(92, 110, 88, 0.08) 0%, rgba(92, 110, 88, 0.02) 100%);
}

.price-label {
  font-size: 0.8rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.price-value {
  font-size: 2.8rem;
  font-weight: 700;
  color: var(--primary-color);
  font-family: 'Lato', monospace;
}

.price-note {
  font-size: 0.8rem;
  color: #999;
  margin-top: 10px;
}

.geometry-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.geo-item {
  background: #fafafa;
  padding: 18px;
  border-radius: 8px;
}

.geo-label {
  display: block;
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.geo-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--text-main);
  font-family: 'Lato', monospace;
}

.geo-unit {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-muted);
  margin-left: 4px;
}

.size-select-card {
  background: #fafafa;
}

.size-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.size-btn {
  padding: 12px 28px;
  background: white;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.3s;
  min-width: 60px;
  text-align: center;
}

.size-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
}

.size-btn.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
  box-shadow: var(--shadow-sm);
}

.selected-size-hint {
  margin-top: 15px;
  padding: 10px 15px;
  background: rgba(92, 110, 88, 0.08);
  border-radius: 6px;
  font-size: 0.85rem;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: 8px;
}



@media (max-width: 900px) {
  .detail-content {
    grid-template-columns: 1fr;
  }

  .main-image {
    height: 350px;
  }

  .bike-title {
    font-size: 2rem;
  }

  .geometry-grid {
    grid-template-columns: 1fr;
  }
}
</style>