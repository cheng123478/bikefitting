<template>
  <div class="profile-page">
    <div class="container profile-container">

      <!-- 1. 顶部个人概览卡片 -->
      <div class="glass-card profile-header">
        <div class="header-left">
          <div class="avatar-wrapper-header">
            <img :src="userStore.userInfo?.avatar || defaultAvatar" class="avatar-lg">
          </div>
          <div class="user-identity">
            <h2 class="username">{{ userStore.userInfo?.name || '骑行客' }}</h2>
            <div class="user-meta">
              <i v-if="userStore.userInfo?.gender == 1" class="fas fa-mars gender-icon male"></i>
              <i v-if="userStore.userInfo?.gender == 2" class="fas fa-venus gender-icon female"></i>
              <span class="role-badge">{{ userStore.userInfo?.memberType === 'PRO' ? 'PRO 会员' : '普通会员' }}</span>
            </div>
            <p class="introduction">
              {{ userStore.userInfo?.introduction || '这家伙很懒，什么也没写' }}
            </p>
          </div>
        </div>

        <div class="header-right stats-box">
          <div class="stat-item">
            <span class="val">{{ staticFittingCount }}</span><span class="lbl">静态报告</span>
          </div>
          <div class="divider-v"></div>
          <div class="stat-item">
            <span class="val">{{ dynamicFittingCount }}</span><span class="lbl">动态报告</span>
          </div>
          <div class="divider-v"></div>
          <div class="stat-item">
            <span class="val">{{ activeBikeCount }}</span><span class="lbl">我的车辆</span>
          </div>
        </div>
      </div>

      <!-- 2. 主体内容区 -->
      <div class="main-layout">
        <!-- 左侧导航 -->
        <aside class="sidebar glass-card">
          <ul class="menu-list">
            <li
                v-for="item in menuItems"
                :key="item.id"
                class="menu-item"
                :class="{ active: currentTab === item.id }"
                @click="currentTab = item.id"
            >
              <i :class="item.icon"></i>
              <span>{{ item.name }}</span>
            </li>
          </ul>
        </aside>

        <!-- 右侧内容面板 -->
        <div class="content-panel glass-card">
          <BasicInfoTab v-if="currentTab === 'basic'" @switch-tab="currentTab = $event" />
          <SecurityTab v-if="currentTab === 'security'" />
          <MembershipTab v-if="currentTab === 'membership'" />
          <FavoritesTab v-if="currentTab === 'favorites'" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useUserStore } from '@/stores/user.js';
import { getStaticFittingCount } from '@/api/staticFitting.js';
import { getDynamicFittingCount } from '@/api/dynamicFitting.js';
import { getActiveBikeCount } from '@/api/userBike.js';

// Components
import BasicInfoTab from './components/BasicInfoTab.vue';
import SecurityTab from './components/SecurityTab.vue';
import MembershipTab from './components/MembershipTab.vue';
import FavoritesTab from './components/FavoritesTab.vue';
//import MembershipTab from './components/MembershipDonationTab.vue';

const route = useRoute();
const userStore = useUserStore();
const currentTab = ref(route.query.tab || 'basic');
const defaultAvatar = '/src/assets/image/default-avatar.png';

const staticFittingCount = ref(0);
const dynamicFittingCount = ref(0);
const activeBikeCount = ref(0);

const menuItems = [
  { id: 'basic', name: '个人资料', icon: 'far fa-id-card' },
  { id: 'security', name: '账号安全', icon: 'fas fa-shield-alt' },
  { id: 'membership', name: '会员特权', icon: 'fas fa-crown' },
  { id: 'favorites', name: '我的收藏', icon: 'fas fa-star' }
];

// 辅助函数：将后端返回的 Long（Jackson 会序列化为字符串）转为数字
const toNum = (val) => {
  const n = Number(val);
  return isNaN(n) ? 0 : n;
};

onMounted(async () => {
  try {
    const [staticRes, dynamicRes, bikeRes] = await Promise.all([
      getStaticFittingCount(),
      getDynamicFittingCount(),
      getActiveBikeCount()
    ]);
    staticFittingCount.value = toNum(staticRes);
    dynamicFittingCount.value = toNum(dynamicRes);
    activeBikeCount.value = toNum(bikeRes);
  } catch (e) {
    console.error('加载个人中心统计数据失败:', e);
  }
});
</script>

<style scoped>
.profile-page {
  padding-top: calc(var(--header-height) + 20px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.profile-container { max-width: 1000px; }

/* Header */
.profile-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 40px; margin-bottom: 30px; border: var(--border-glass);
}

.header-left { display: flex; align-items: center; gap: 30px; }

.avatar-wrapper-header { position: relative; width: 100px; height: 100px; }
.avatar-lg { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; border: 4px solid var(--bg-page); box-shadow: var(--shadow-sm); }

.edit-avatar-badge {
  position: absolute; bottom: 0; right: 0; width: 28px; height: 28px;
  background: var(--primary-color); color: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.8rem; border: 2px solid #fff;
}

.user-identity .username {
  font-family: var(--font-serif); font-size: 2rem; color: var(--text-main);
  margin-bottom: 8px; line-height: 1.2;
}

.user-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }

.gender-icon { font-size: 0.9rem; }
.gender-icon.male { color: #6495ED; }
.gender-icon.female { color: #FF69B4; }

.role-badge { background: var(--primary-color); color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; }
.id-badge { background: rgba(0,0,0,0.05); color: var(--text-muted); padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; font-family: monospace; }

.user-identity .introduction { color: var(--text-muted); font-size: 0.9rem; max-width: 400px; }

.stats-box { display: flex; align-items: center; gap: 30px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-item .val { font-size: 1.6rem; font-weight: 600; color: var(--text-main); font-family: var(--font-serif); }
.stat-item .lbl { font-size: 0.8rem; color: var(--text-muted); margin-top: 2px; }
.divider-v { width: 1px; height: 30px; background: rgba(0,0,0,0.1); }

/* Main Layout */
.main-layout { display: grid; grid-template-columns: 240px 1fr; gap: 30px; }

.sidebar { padding: 20px 0; height: fit-content; border: var(--border-glass); }

.menu-item {
  padding: 15px 25px; cursor: pointer; display: flex; align-items: center; gap: 12px;
  color: var(--text-muted); transition: 0.3s; border-left: 3px solid transparent;
}
.menu-item:hover { background: var(--bg-page); color: var(--primary-color); }
.menu-item.active { background: rgba(92, 110, 88, 0.08); color: var(--primary-color); border-left-color: var(--primary-color); font-weight: 600; }
.menu-item i { width: 20px; text-align: center; }

/* Content Panel */
.content-panel { padding: 40px; border: var(--border-glass); min-height: 600px; }

@media (max-width: 768px) {
  .profile-header { flex-direction: column; text-align: center; gap: 30px; padding: 30px 20px; }
  .header-left { flex-direction: column; }
  .main-layout { grid-template-columns: 1fr; }
  .sidebar { overflow-x: auto; padding: 10px; white-space: nowrap; }
  .menu-list { display: flex; }
  .menu-item { border-left: none; border-bottom: 3px solid transparent; padding: 10px 15px; }
  .menu-item.active { border-left-color: transparent; border-bottom-color: var(--primary-color); background: transparent; }
}

</style>
