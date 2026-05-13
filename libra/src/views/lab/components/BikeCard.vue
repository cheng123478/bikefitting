<template>
  <!-- 根据状态动态绑定 is-retired 类 -->
  <div class="bike-card glass-card" :class="{ 'is-retired': bike.status === 2 }">
    <!-- 1. 车型视觉区 -->
    <div class="bike-visual">
      <!-- 仅在役车辆显示快捷编辑 -->
      <div v-if="bike.status === 1" class="quick-edit-btn" @click.stop="handleQuickEdit">
        <i class="fas fa-pen-nib"></i>
      </div>

      <div class="img-container">
        <img :src="displayImageUrl" :alt="bikeName" class="bike-img" />
      </div>

      <div class="bike-type-badge">{{ getBikeTypeName(bike.bikeType) }}</div>

      <!-- 缩小后的 STR 指示器 -->
      <div class="str-indicator" v-if="bike.frameStr">
        <div class="str-bar">
          <div class="str-fill" :style="{ width: calcStrPercent(bike.frameStr) + '%' }"></div>
        </div>
        <div class="str-text-row">
          <span class="str-label">STR {{ bike.frameStr }}</span>
        </div>
      </div>
    </div>

    <!-- 2. 核心参数区 -->
    <div class="bike-content" @click.stop="handleShowcaseClick">
      <h3 class="bike-name">{{ bikeName }}</h3>

      <div class="bike-meta">
        <span>{{ bike.frameSize || '--' }} 尺码</span>
        <span class="dot">·</span>
        <span>{{ getBikeTypeName(bike.bikeType) }}</span>
      </div>

      <!-- 新增：显示备注 -->
      <p v-if="bike.remark" class="bike-remark">{{ bike.remark }}</p>

      <!-- 更加紧凑的指标网格 -->
      <div class="plus-metrics" v-if="bike.effectiveReachPlus">
        <div class="m-item">
          <label>前伸 REACH+</label>
          <span class="m-val">{{ bike.effectiveReachPlus }}</span>
        </div>
        <div class="m-item">
          <label>堆高 STACK+</label>
          <span class="m-val">{{ bike.effectiveStackPlus }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 卡片底部 -->
    <footer class="card-footer">
      <div class="update-time">
        <i class="far fa-clock"></i> {{ formatDate(bike.createTime) }}
      </div>
      <div class="footer-actions">
        <div class="action-hint" @click.stop="goToReport">
          查看诊断 <i class="fas fa-stethoscope"></i>
        </div>
        <div class="action-delete" @click.stop="handleDelete" title="删除单车">
          <i class="fas fa-trash-alt"></i>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';

const router = useRouter();
const props = defineProps({
  bike: { type: Object, required: true }
});

const emit = defineEmits(['open-showcase', 'delete']);
const handleShowcaseClick = () => {
  emit('open-showcase', props.bike);
};

const defaultBikeImg = '/image/default-bike-card.png';

const displayImageUrl = computed(() => {
  if (!props.bike.imageUrl) return defaultBikeImg;
  if (props.bike.imageUrl.startsWith('http') || props.bike.imageUrl.startsWith('data:')) {
    return props.bike.imageUrl;
  }
  return '/api' + props.bike.imageUrl;
});

const bikeName = computed(() => props.bike.brandModel || `${props.bike.brand || ''} ${props.bike.model || ''}`.trim() || '未命名单车');

const formatDate = (date) => date ? dayjs(date).format('YYYY.MM.DD') : '未校准';

const BIKE_TYPES = { 1: '公路车', 2: '山地车', 3: '全地形', 4: 'TT/计时赛', 5: '城市车' };
const getBikeTypeName = (type) => BIKE_TYPES[type] || '单车';

const STR_MIN = 1.35;
const STR_MAX = 1.55;
const calcStrPercent = (str) => {
  if (!str) return 0;
  const percent = ((str - STR_MIN) / (STR_MAX - STR_MIN)) * 100;
  return Math.max(0, Math.min(100, percent));
};

const handleQuickEdit = () => {
  router.push({ path: '/lab/garage/edit', query: { id: props.bike.id } });
};

const goToReport = () => {
  router.push({ path: '/lab/report', query: { bikeId: props.bike.id } });
};

const handleDelete = () => {
  emit('delete', props.bike);
};
</script>

<style scoped>
.bike-card {
  padding: 0;
  transition: all 0.5s var(--ease-out-expo);
  border: 1px solid rgba(0,0,0,0.03);
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.85);
  height: 100%;
  position: relative;
  overflow: hidden;
}

/* --- 核心优化：灰度化处理 --- */
.is-retired {
  filter: grayscale(1) sepia(0.2);
  opacity: 0.6;
}
.is-retired:hover {
  filter: grayscale(0.6); /* 悬停时稍微恢复一点色泽 */
  opacity: 1;
}

.bike-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
  background: white;
  border-color: var(--primary-light);
}

/* 视觉区：缩小高度 */
.bike-visual {
  height: 155px; /* 从 190px 缩小至 155px */
  background: #FFF;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 15px 15px 35px; /* 压缩内边距 */
  border-bottom: 1px solid rgba(0,0,0,0.02);
}

.img-container {
  width: 100%;
  height: 100px; /* 锁定图片容器高度 */
  display: flex;
  align-items: center;
  justify-content: center;
}

.bike-img {
  max-width: 85%;
  max-height: 100%;
  object-fit: contain;
  transition: 0.5s var(--ease-out-expo);
  filter: drop-shadow(0 4px 10px rgba(0,0,0,0.05));
}

.bike-type-badge {
  position: absolute; top: 10px; left: 10px;
  font-size: 0.7rem; font-weight: 800; color: white;
  background: var(--text-main); padding: 3px 8px; border-radius: 4px;
  z-index: 2;
}

/* STR 指示器微调 */
.str-indicator {
  position: absolute; bottom: 8px; left: 0; right: 0;
  padding: 0 15px;
}
.str-bar { height: 3px; background: rgba(0,0,0,0.05); border-radius: 2px; margin-bottom: 4px; overflow: hidden; }
.str-fill { height: 100%; background: var(--primary-color); }
.str-text-row { display: flex; justify-content: flex-end; }
.str-label { font-size: 0.65rem; color: var(--text-muted); font-weight: 700; }

/* 内容区压缩 */
.bike-content {
  padding: 15px 20px 20px; /* 压缩内边距 */
  flex: 1;
  cursor: pointer;
}

.bike-name {
  font-size: 1.25rem; /* 从 1.6rem 缩小至 1.25rem */
  color: var(--text-main);
  margin-bottom: 6px;
  font-family: var(--font-serif);
  font-weight: 700;
  line-height: 1.2;
}

.bike-meta {
  display: flex; align-items: center; gap: 8px;
  font-size: 0.85rem; /* 减小字号 */
  color: var(--text-muted);
  margin-bottom: 12px; /* 减小间距以容纳备注 */
}

.bike-remark {
  font-size: 0.8rem;
  color: var(--primary-color);
  opacity: 0.8;
  margin-bottom: 15px;
  font-style: italic;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 指标区域：更加精致 */
.plus-metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.m-item {
  background: #F9FAF9;
  padding: 10px 12px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.m-item label {
  font-size: 0.65rem;
  color: var(--text-muted);
  font-weight: 700;
  letter-spacing: 0.5px;
}
.m-val {
  font-family: var(--font-serif);
  font-weight: 700;
  color: var(--primary-color);
  font-size: 1.35rem; /* 从 1.8rem 缩小至 1.35rem */
  line-height: 1;
}

/* 页脚 */
.card-footer {
  padding: 12px 20px;
  border-top: 1px solid rgba(0,0,0,0.03);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.update-time { font-size: 0.75rem; color: var(--text-muted); }
.footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.action-hint { font-size: 0.8rem; font-weight: 700; color: var(--primary-color); }
.action-hint:hover { cursor: pointer; }
.action-delete {
  font-size: 0.9rem;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.3s;
  padding: 4px;
}
.action-delete:hover {
  color: #ff4444;
  transform: scale(1.1);
}
.quick-edit-btn {
  position: absolute; top: 10px; right: 10px;
  width: 28px; height: 28px; border-radius: 50%;
  background: rgba(255,255,255,0.9); color: var(--text-muted);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transform: scale(0.8); transition: 0.3s; z-index: 10;
  box-shadow: var(--shadow-xs);
  cursor: pointer;
}
.bike-card:hover .quick-edit-btn { opacity: 1; transform: scale(1); }
.quick-edit-btn:hover { background: var(--primary-color); color: white; }
</style>