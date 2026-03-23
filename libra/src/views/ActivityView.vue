<template>
  <div class="activity-page">

    <main class="container activity-container">

      <!-- 工具栏 -->
      <div class="toolbar-row">
        <!-- 左侧：标题区 -->
        <div class="text-group">
          <h1 class="page-title">活动中心</h1>
          <p class="page-subtitle">探索未知旅程，或召集你的骑士军团</p>
        </div>

        <!-- 右侧：Tabs -->
        <div class="tabs-group">
          <div class="category-tabs">
            <div
                v-for="tab in tabs"
                :key="tab.id"
                class="tab-pill"
                :class="{ active: currentTab === tab.id }"
                @click="currentTab = tab.id"
            >
              {{ tab.name }}
            </div>
          </div>
        </div>
      </div>

      <!-- 列表内容 -->
      <TransitionGroup name="list-fade" tag="div" class="activity-grid">

        <div
            v-for="item in filteredList"
            :key="item.id"
            class="activity-card glass-card"
            @click="viewDetail(item)"
        >
          <!-- 封面 -->
          <div class="media-wrapper">
            <img :src="item.cover" loading="lazy" class="cover-img" :class="{ grayscale: item.isEnded }">
            <div class="status-badge" :class="item.statusClass">{{ item.statusText }}</div>
            <div v-if="currentTab === 'discover' && item.isHot" class="hot-badge">
              <i class="fas fa-fire"></i> HOT
            </div>
          </div>

          <!-- 内容主体 -->
          <div class="card-body">
            <div class="flex-row">
              <div class="date-box">
                <span class="d-day">{{ item.date.day }}</span>
                <span class="d-month">{{ item.date.month }}</span>
              </div>
              <div class="info-box">
                <h3 class="card-title">{{ item.title }}</h3>
                <div class="meta-row">
                  <span class="meta-item"><i class="fas fa-map-marker-alt"></i> {{ item.location }}</span>
                </div>
              </div>
            </div>

            <div class="desc-row"><p>{{ item.desc }}</p></div>

            <div class="card-footer">
              <div class="participants">
                <template v-if="currentTab === 'created'">
                  <span class="count-text"><i class="fas fa-user-check"></i> {{ item.count }}人 已报名</span>
                </template>
                <template v-else>
                  <div class="avatars">
                    <img v-for="(avatar, i) in item.participants.slice(0,3)" :key="i" :src="avatar" class="mini-avatar">
                  </div>
                  <span class="count-text" v-if="item.count > 0">{{ item.count }}人已报名</span>
                </template>
              </div>

              <div class="btn-group">
                <template v-if="item.isCreator">
                  <button class="btn btn-sm btn-outline" @click.stop="handleEdit(item)"><i class="fas fa-edit"></i>编辑</button>
                  <button class="btn btn-sm btn-outline text-danger" @click.stop="handleDelete(item)"><i class="fas fa-trash"></i>删除</button>
                </template>
                <template v-else>
                  <button class="btn btn-sm" :class="getActionBtnClass(item)" @click.stop="handleAction(item)">
                    {{ getActionBtnText(item) }}
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>

      </TransitionGroup>

      <!-- 空状态 -->
      <div v-if="filteredList.length === 0" class="empty-state">
        <div class="icon-box" style="margin-bottom:20px; opacity:0.5;"><i class="far fa-calendar-plus"></i></div>
        <p v-if="currentTab === 'created'">你还没有发起过活动，点击右下角创建一个吧！</p>
        <p v-else>暂无相关活动</p>
      </div>

    </main>

    <!-- 悬浮创建按钮 -->
    <button class="fab-btn" @click="goToCreate" title="发起活动">
      <i class="fas fa-plus"></i>
    </button>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { activityService } from '@/api/mockService';

const router = useRouter();
const currentTab = ref('discover');

const tabs = [
  {id: 'discover', name: '热门推荐'},
  {id: 'my_upcoming', name: '我的行程'},
  {id: 'created', name: '我发起的'},
  {id: 'history', name: '往期足迹'}
];

const allActivities = ref([]);

const processedActivities = computed(() => {
  return allActivities.value.map(item => {
    const newItem = { ...item };
    if (newItem.status === 'ended') {
      newItem.statusText = '已结束';
      newItem.statusClass = 'ended';
      newItem.isEnded = true;
    } else if (newItem.status === 'joined') {
      newItem.statusText = '已报名';
      newItem.statusClass = 'joined';
    } else if (newItem.status === 'reviewing') {
      newItem.statusText = '审核中';
      newItem.statusClass = 'reviewing';
    } else {
      newItem.statusText = '报名中';
      newItem.statusClass = 'open';
    }
    return newItem;
  });
});

const filteredList = computed(() => {
  return processedActivities.value.filter(item => {
    if (currentTab.value === 'created') return item.isCreator;
    if (currentTab.value === 'my_upcoming') return item.status === 'joined' && !item.isEnded;
    if (currentTab.value === 'discover') return item.status === 'open' && !item.isEnded && !item.isCreator;
    if (currentTab.value === 'history') return item.isEnded;
    return false;
  });
});

const fetchActivities = async () => {
  try {
    allActivities.value = await activityService.getAllActivities();
  } catch (err) {
    console.error('Failed to fetch activities', err);
  }
};

onMounted(() => {
  fetchActivities();
});

const viewDetail = (item) => {
  const url = router.resolve({name: 'rally-detail', params: {id: item.id}}).href;
  window.open(url, '_blank');
};
const goToCreate = () => router.push({name: 'rally-add'});
const handleEdit = (item) => {
  router.push({name: 'rally-edit', params: {id: item.id}});
};
const handleDelete = (item) => {
  if (confirm(`确定删除?`)) allActivities.value = allActivities.value.filter(i => i.id !== item.id);
};
const handleAction = (item) => {
  if (item.status === 'joined') alert('签到码'); else if (item.status === 'open') alert('已申请'); else viewDetail(item);
};
const getActionBtnText = (item) => {
  if (item.isEnded) return '回顾';
  if (item.status === 'joined') return '详情';
  return '报名';
};
const getActionBtnClass = (item) => {
  if (item.isEnded || item.status === 'joined') return 'btn-outline';
  return 'btn-primary';
};
</script>

<style scoped>
.activity-page {
  padding-top: var(--header-height);
  min-height: 100vh;
}

.activity-container {
  padding-top: 40px;
  padding-bottom: 80px;
}

/* Toolbar */
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

/* Tabs */
.category-tabs {
  display: flex;
  background: rgba(0, 0, 0, 0.04);
  padding: 4px;
  border-radius: var(--radius-pill);
}

.tab-pill {
  padding: 8px 20px;
  font-size: 0.9rem;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: var(--radius-pill);
  transition: 0.3s;
  font-weight: 500;
  white-space: nowrap;
}

.tab-pill:hover {
  color: var(--primary-color);
}

.tab-pill.active {
  background: #fff;
  color: var(--primary-color);
  box-shadow: var(--shadow-xs);
  font-weight: 600;
}

/* Grid (Responsive) */
.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md);
  margin: 0 auto;
}

/* Card */
.activity-card {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  /* glass-card 已提供背景和圆角，此处微调边框 */
  border: var(--border-glass);
}

.activity-card:hover {
  border-color: rgba(92, 110, 88, 0.2);
}

.media-wrapper {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: #eee;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: 0.6s var(--ease-out-expo);
}

.activity-card:hover .cover-img {
  transform: scale(1.05);
}

.cover-img.grayscale {
  filter: grayscale(90%);
}

.status-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  backdrop-filter: blur(8px);
}

.status-badge.open {
  background: rgba(255, 255, 255, 0.9);
  color: var(--primary-color);
}

.status-badge.joined {
  background: var(--primary-color);
  color: #fff;
}

.status-badge.ended {
  background: rgba(43, 42, 41, 0.85);
  color: #ccc;
}

.hot-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #C18C7E;
  color: #fff;
  padding: 3px 6px;
  border-radius: 4px;
  font-size: 0.65rem;
  font-weight: 700;
}

.card-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.flex-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.date-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: rgba(92, 110, 88, 0.08);
  border-radius: 8px;
  color: var(--primary-color);
  flex-shrink: 0;
}

.d-day {
  font-family: var(--font-serif);
  font-size: 1.4rem;
  line-height: 1;
  font-weight: 600;
}

.d-month {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.info-box {
  flex: 1;
  overflow: hidden;
}

.card-title {
  font-family: var(--font-serif);
  font-size: 1.1rem;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-main);
  font-weight: 600;
}

.meta-row {
  display: flex;
  gap: 10px;
  font-size: 0.8rem;
  color: var(--text-muted);
}

.meta-item i {
  margin-right: 4px;
  color: var(--primary-color);
  opacity: 0.8;
}

.desc-row {
  margin-bottom: 15px;
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.6;
  height: 3em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.participants {
  display: flex;
  align-items: center;
  gap: 6px;
}

.avatars {
  display: flex;
}

.mini-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid #fff;
  margin-left: -8px;
  object-fit: cover;
}

.mini-avatar:first-child {
  margin-left: 0;
}

.count-text {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.btn-group {
  display: flex;
  gap: 8px;
}

/* FAB */
.fab-btn {
  position: fixed;
  bottom: 40px;
  right: 30px;
  width: 60px;
  height: 60px;
  background: var(--primary-color);
  color: #fff;
  border: none;
  border-radius: 50%;
  box-shadow: var(--shadow-lg);
  font-size: 1.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.3s var(--ease-out-expo);
  z-index: 100;
}

.fab-btn:hover {
  transform: scale(1.1) rotate(90deg);
  background: var(--primary-dark);
}

/* 响应式断点 */
@media (max-width: 1200px) {
  .activity-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .toolbar-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .tabs-group {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 5px;
  }

  .category-tabs {
    width: max-content;
  }
}

@media (max-width: 900px) {
  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .activity-grid {
    grid-template-columns: 1fr;
  }

  .fab-btn {
    bottom: 30px;
    right: 20px;
    width: 50px;
    height: 50px;
    font-size: 1.2rem;
  }
}
</style>