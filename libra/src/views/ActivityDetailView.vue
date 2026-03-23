<template>
  <div class="activity-detail-page">
    <div class="container detail-container">
      <div class="content-wrapper">

        <!-- 左侧：活动核心信息 -->
        <div class="main-column">

          <!-- A. 封面与标题卡片 -->
          <div class="glass-card hero-card">
            <div class="hero-image-wrapper">
              <img :src="activity.cover" alt="Cover" class="hero-img" :class="{ grayscale: activity.isEnded }">
              <div class="status-badge" :class="activity.statusClass">{{ activity.statusText }}</div>
            </div>

            <div class="hero-body">
              <div class="organizer-row">
                <img :src="activity.organizer.avatar" class="org-avatar">
                <span class="org-name">发起方：{{ activity.organizer.name }}</span>
              </div>
              <h1 class="activity-title">{{ activity.title }}</h1>

              <!-- 核心参数 Grid -->
              <div class="info-grid">
                <div class="info-item">
                  <i class="far fa-clock"></i>
                  <div>
                    <span class="label">集合时间</span>
                    <div class="val">{{ activity.startTime }}</div>
                  </div>
                </div>
                <div class="info-item">
                  <i class="fas fa-map-marker-alt"></i>
                  <div>
                    <span class="label">集合地点</span>
                    <div class="val">{{ activity.location }}</div>
                  </div>
                </div>
                <div class="info-item">
                  <i class="fas fa-mountain"></i>
                  <div>
                    <span class="label">强度/爬升</span>
                    <div class="val">{{ activity.difficulty }} / {{ activity.elevation }}m</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- B. 活动详情与行程 -->
          <div class="glass-card section-card">
            <h3 class="section-title">活动介绍</h3>
            <div class="rich-text">
              <p>{{ activity.desc }}</p>
            </div>

            <div class="divider"></div>

            <h3 class="section-title">行程安排</h3>
            <div class="timeline">
              <div v-for="(node, index) in activity.itinerary" :key="index" class="timeline-item">
                <div class="time-point">{{ node.time }}</div>
                <div class="timeline-dot"></div>
                <div class="node-content">
                  <h4>{{ node.title }}</h4>
                  <p>{{ node.detail }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- C. 关联的分享圈 -->
          <div class="glass-card section-card">
            <div class="share-header-row">
              <h3 class="section-title">骑友精彩分享 ({{ activity.linkedShares.length }})</h3>
              <button class="btn btn-sm btn-outline" @click="createShare">
                <i class="fas fa-camera"></i> 发布分享
              </button>
            </div>

            <!-- 分享内容 Grid -->
            <div v-if="activity.linkedShares.length > 0" class="linked-shares-grid">
              <div
                  v-for="share in activity.linkedShares"
                  :key="share.id"
                  class="mini-share-card"
                  @click="viewShareDetail(share.id)"
              >
                <img :src="share.cover" class="share-cover">
                <div class="share-overlay">
                  <span class="user"><img :src="share.userAvatar"> {{ share.userName }}</span>
                </div>
              </div>
            </div>
            <div v-else class="empty-shares">
              <p>还没有人分享，快来抢占沙发！</p>
            </div>
          </div>
        </div>

        <!-- 右侧：侧边栏 (报名信息) -->
        <aside class="side-column">
          <div class="sticky-wrapper">
            <!-- 1. 报名信息卡片 -->
            <div class="glass-card signup-card">
              <div class="price-row">
                <span class="label">报名费用</span>
                <span class="price">{{ activity.price === 0 ? '免费' : `¥${activity.price}` }}</span>
              </div>

              <div class="progress-box">
                <div class="progress-text">
                  <span>已报名 {{ activity.joinedCount }}</span>
                  <span>限额 {{ activity.maxCount }}</span>
                </div>
                <div class="progress-bar"><div class="fill" :style="{ width: (activity.joinedCount/activity.maxCount)*100 + '%' }"></div></div>
              </div>

              <div class="avatar-list">
                <img v-for="(u, i) in activity.participants.slice(0, 5)" :key="i" :src="u" class="p-avatar">
                <span class="more" v-if="activity.joinedCount > 5">+{{ activity.joinedCount - 5 }}</span>
              </div>

              <button
                  class="btn btn-primary btn-block action-btn"
                  :disabled="activity.isEnded || activity.joined"
                  @click="handleJoin">
                {{ getJoinBtnText }}
              </button>
              <p class="status-hint" v-if="activity.joined">您已报名，请准时参加</p>
            </div>

            <!-- 2. 地图与路书卡片 -->
            <div class="glass-card map-card">
              <h4 class="card-title-sm">路线预览</h4>
              <div class="map-placeholder">
                <img src="https://images.unsplash.com/photo-1569336415962-a4bd9f69cd83?q=80&w=400&fit=crop" class="map-img">
                <div class="map-overlay">
                  <i class="fas fa-route"></i>
                  <span>点击查看详情</span>
                </div>
              </div>

              <div class="map-stats">
                <span><i class="fas fa-arrows-alt-h"></i> 45km</span>
                <span><i class="fas fa-arrow-up"></i> 320m</span>
              </div>

              <button class="btn btn-sm btn-outline btn-block" style="margin-top:15px; width: 100%">
                <i class="fas fa-download"></i> 下载 GPX 路书
              </button>
            </div>
          </div>
        </aside>

      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue';
import {useRoute, useRouter} from 'vue-router';

const route = useRoute();
const router = useRouter();

const activity = ref({
  id: 101,
  title: '周三夜骑·滨江休闲拉练',
  cover: 'https://images.unsplash.com/photo-1558507312-320092288344?q=80&w=1000&fit=crop',
  status: 'open',
  statusText: '报名中',
  statusClass: 'open',
  isEnded: false,
  joined: false,
  organizer: {name: '破风猎人车队', avatar: 'https://images.unsplash.com/photo-1599566150163-29194dcaad36?q=80&w=100'},
  startTime: '2026-10-23 19:30',
  location: '杭州市滨江区射潮广场',
  difficulty: 'L2 休闲',
  elevation: '50',
  price: 0,
  joinedCount: 24,
  maxCount: 50,
  desc: '本周三我们将进行例行的滨江夜骑活动。全程平路，适合新手进阶和老手排酸。请大家务必佩戴头盔，检查车灯电量。',
  itinerary: [
    {time: '19:00', title: '集合签到', detail: '射潮广场集合，分发号码牌。'},
    {time: '19:30', title: '出发', detail: '准时出发，均速控制在 25km/h 左右。'},
    {time: '21:30', title: '解散', detail: '返回起点，活动结束。'}
  ],
  participants: ['https://i.pravatar.cc/100?img=1', 'https://i.pravatar.cc/100?img=2', 'https://i.pravatar.cc/100?img=3', 'https://i.pravatar.cc/100?img=4', 'https://i.pravatar.cc/100?img=5'],
  linkedShares: [
    {
      id: 201,
      cover: 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?q=80&w=300',
      userName: 'Alex',
      userAvatar: 'https://i.pravatar.cc/100?img=1'
    },
    {
      id: 202,
      cover: 'https://images.unsplash.com/photo-1517932301327-7ac9b0200786?q=80&w=300',
      userName: 'Sarah',
      userAvatar: 'https://i.pravatar.cc/100?img=5'
    }
  ]
});

const getJoinBtnText = computed(() => {
  if (activity.value.isEnded) return '活动已结束';
  if (activity.value.joined) return '已报名 (查看凭证)';
  if (activity.value.joinedCount >= activity.value.maxCount) return '名额已满';
  return '立即报名';
});

const goBack = () => router.back();
const handleJoin = () => {
  if (!activity.value.joined && confirm('确认报名参加该活动吗？')) {
    activity.value.joined = true;
    activity.value.joinedCount++;
  }
};
const createShare = () => {
  router.push({name: 'share-circle-add', query: {activityId: activity.value.id, activityTitle: activity.value.title}});
};
const viewShareDetail = (id) => {
  window.open(router.resolve({name: 'share-circle-detail', params: {id}}).href, '_blank');
};
onMounted(() => {
  console.log("Loading activity ID:", route.params.id);
});
</script>

<style scoped>
.activity-detail-page {
  padding-top: calc(var(--header-height) + 20px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.detail-container {
  max-width: 1100px;
}

/* 头部 */
.detail-header {
  margin-bottom: 20px;
}

.back-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  transition: 0.3s;
  padding: 0;
}

.back-btn:hover {
  color: var(--primary-color);
  transform: translateX(-5px);
}

/* 布局 */
.content-wrapper {
  display: grid;
  grid-template-columns: 2.5fr 1fr;
  gap: 30px;
}

/* 左侧 */
.hero-card {
  padding: 0;
  overflow: hidden;
  margin-bottom: 30px;
  border: var(--border-glass);
}

.hero-image-wrapper {
  position: relative;
  height: 340px;
  background: #eee;
}

.hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-img.grayscale {
  filter: grayscale(90%);
}

.status-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  padding: 6px 16px;
  border-radius: var(--radius-pill);
  font-weight: 600;
  font-size: 0.9rem;
  backdrop-filter: blur(8px);
}

.status-badge.open {
  background: rgba(255, 255, 255, 0.95);
  color: var(--primary-color);
}

.status-badge.ended {
  background: rgba(43, 42, 41, 0.85);
  color: #ccc;
}

.hero-body {
  padding: 30px;
}

.organizer-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.org-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.activity-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  margin-bottom: 25px;
  line-height: 1.2;
  color: var(--text-main);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-item i {
  font-size: 1.2rem;
  color: var(--primary-color);
  margin-top: 3px;
}

.info-item .label {
  font-size: 0.8rem;
  color: var(--text-muted);
  display: block;
  margin-bottom: 2px;
}

.info-item .val {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-main);
}

/* 详情与行程 */
.section-card {
  padding: 30px;
  margin-bottom: 30px;
}

.section-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  margin-bottom: 20px;
  color: var(--text-main);
}

.rich-text {
  font-size: 1rem;
  line-height: 1.8;
  color: var(--text-body);
}

.divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.05);
  margin: 30px 0;
}

.timeline {
  position: relative;
  padding-left: 10px;
}

.timeline-item {
  display: flex;
  gap: 20px;
  margin-bottom: 25px;
  position: relative;
}

.timeline-item::before {
  content: '';
  position: absolute;
  top: 25px;
  left: 65px;
  bottom: -25px;
  width: 1px;
  background: rgba(0, 0, 0, 0.1);
  z-index: 0;
}

.timeline-item:last-child::before {
  display: none;
}

.time-point {
  font-family: var(--font-sans);
  font-weight: 600;
  color: var(--primary-color);
  width: 50px;
  text-align: right;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--bg-surface);
  border: 3px solid var(--primary-color);
  z-index: 1;
  margin-top: 5px;
  flex-shrink: 0;
}

.node-content h4 {
  font-size: 1.1rem;
  margin-bottom: 5px;
  color: var(--text-main);
}

.node-content p {
  font-size: 0.9rem;
  color: var(--text-muted);
}

/* 关联分享 */
.share-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.linked-shares-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

.mini-share-card {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
}

.share-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: 0.3s;
}

.mini-share-card:hover .share-cover {
  transform: scale(1.05);
}

.share-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 8px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.6), transparent);
  color: #fff;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
}

.share-overlay img {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-right: 5px;
}

.empty-shares {
  color: var(--text-muted);
  font-style: italic;
  font-size: 0.9rem;
}

/* 右侧侧边栏 */
.sticky-wrapper {
  position: sticky;
  top: 100px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.signup-card {
  padding: 25px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 20px;
}

.price-row .price {
  font-size: 2rem;
  font-weight: 600;
  color: #C18C7E;
  font-family: var(--font-serif);
}

.progress-box {
  margin-bottom: 20px;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-bottom: 5px;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
  overflow: hidden;
}

.fill {
  height: 100%;
  background: var(--primary-color);
}

.avatar-list {
  display: flex;
  margin-bottom: 25px;
}

.p-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--bg-surface);
  margin-left: -8px;
}

.p-avatar:first-child {
  margin-left: 0;
}

.more {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eee;
  color: #888;
  font-size: 0.7rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: -8px;
  border: 2px solid var(--bg-surface);
  z-index: 0;
}

.action-btn {
  height: 48px;
  font-size: 1rem;
  margin-bottom: 10px;
  width: 100%;
}

.status-hint {
  font-size: 0.8rem;
  color: #6E8565;
  text-align: center;
}

/* 地图卡片 */
.map-card {
  padding: 20px;
}

.card-title-sm {
  font-size: 1rem;
  margin-bottom: 15px;
  color: var(--text-main);
  font-weight: 600;
}

.map-placeholder {
  position: relative;
  height: 180px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  margin-bottom: 15px;
  cursor: pointer;
}

.map-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(0.9);
  transition: 0.3s;
}

.map-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: 0.3s;
  background: rgba(0, 0, 0, 0.3);
}

.map-placeholder:hover .map-overlay {
  opacity: 1;
}

.map-placeholder:hover .map-img {
  transform: scale(1.05);
}

.map-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 5px;
  font-size: 0.85rem;
  color: var(--text-muted);
  background: var(--bg-page);
  padding: 10px;
  border-radius: var(--radius-sm);
}

@media (max-width: 900px) {
  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .side-column {
    order: -1;
    margin-bottom: 30px;
  }

  .signup-card {
    position: static;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .timeline-item::before {
    left: 18px;
  }

  .time-point {
    display: none;
  }

  .timeline-dot {
    margin-left: 13px;
  }

  .sticky-wrapper {
    position: static;
  }
}
</style>