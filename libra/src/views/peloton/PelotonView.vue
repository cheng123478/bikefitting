<template>
  <div class="peloton-page">
    <div class="container">
      <!-- 顶部 Header + 统计概览条：左右布局 -->
      <div class="hero-row" v-scroll-reveal>
        <header class="peloton-intro">
          <span class="section-tag">PELOTON</span>
          <h1 class="peloton-title serif-title">破风社</h1>
          <p class="page-subtitle">集结骑行伙伴，组队破风前行</p>
        </header>

        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-number">{{ totalTeams }}</span>
            <span class="stat-label">支活跃车队</span>
          </div>
          <span class="stat-sep">/</span>
          <div class="stat-item">
            <span class="stat-number">{{ totalMembers }}</span>
            <span class="stat-label">位骑友已加入</span>
          </div>
          <span class="stat-sep">/</span>
          <div class="stat-item">
            <span class="stat-number">{{ totalCities }}</span>
            <span class="stat-label">座城市覆盖</span>
          </div>
        </div>
      </div>

      <!-- 分类 Tab + 搜索 + 创建车队 -->
      <div class="toolbar" v-scroll-reveal.delay="100">
        <div class="capsule-nav">
          <button
            v-for="cat in categories"
            :key="cat.key"
            class="capsule-btn"
            :class="{ active: activeCategory === cat.key }"
            @click="switchCategory(cat.key)"
          >
            <i :class="cat.icon"></i> {{ cat.label }}
          </button>
        </div>
        <div class="toolbar-right">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input
              type="text"
              v-model="searchQuery"
              @input="onSearch"
              placeholder="搜索车队名称、城市或标签..."
            />
          </div>
          <button class="create-team-btn" @click="handleCreateTeam" title="创建车队">
            <i class="fas fa-plus"></i>
          </button>
        </div>
      </div>

      <!-- 车队卡片网格 -->
      <main class="peloton-main">
        <TransitionGroup name="grid-fade" tag="div" class="team-grid">
          <div
            v-for="(team, index) in teams"
            :key="team.id"
            class="team-card"
            v-scroll-reveal="{ delay: index * 30 }"
            @click="goToDetail(team.id)"
          >
            <div class="card-cover">
              <img :src="team.coverImage" :alt="team.name" class="cover-img" />
              <div class="card-overlay">
                <span class="view-detail-text">查看详情</span>
              </div>
            </div>
            <div class="card-body">
              <div class="card-header-row">
                <img :src="team.avatar" :alt="team.name" class="team-avatar" />
                <div class="team-name-group">
                  <h3 class="team-name">{{ team.name }}</h3>
                  <span class="team-location">
                    <i class="fas fa-map-marker-alt"></i> {{ team.location }}
                  </span>
                </div>
                <button
                  v-if="isCreator(team) || !isMemberOf(team)"
                  class="card-join-btn"
                  :class="{ 'is-edit': isCreator(team) }"
                  @click.stop="isCreator(team) ? router.push(`/peloton/create?id=${team.id}`) : openJoinDialog(team)"
                  :title="isCreator(team) ? '编辑车队' : '申请加入'"
                >
                  <i :class="isCreator(team) ? 'fas fa-pen' : 'fas fa-plus'"></i>
                </button>
              </div>
              <p class="team-desc">{{ team.description }}</p>
              <div class="card-footer">
                <div class="footer-stats">
                  <span class="member-count">
                    <i class="fas fa-user-friends"></i> {{ team.memberCount }} 人
                  </span>
                </div>
                <div class="footer-tags">
                  <span
                    v-for="tag in team.tags"
                    :key="tag"
                    class="team-tag"
                  >{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
        </TransitionGroup>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i>
          <p>正在加载车队...</p>
        </div>

        <!-- 空状态 -->
        <div v-if="teams.length === 0 && !loading" class="empty-state">
          <i class="fas fa-users"></i>
          <p v-if="activeCategory === 'mine'">还没有车队哦，快去探索一下吧！</p>
          <p v-else>暂无符合条件的车队，换个关键词试试吧。</p>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1 && !loading" class="pagination-wrapper">
          <nav class="pagination">
            <button
              class="page-btn"
              :class="{ disabled: page === 1 }"
              @click="goToPage(page - 1)"
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
              @click="goToPage(page + 1)"
              :disabled="page >= totalPages"
            >
              下一页 <i class="fas fa-chevron-right"></i>
            </button>

            <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
          </nav>
        </div>
      </main>
    </div>

    <!-- 申请加入确认弹窗 -->
    <Teleport to="body">
      <Transition name="join-dialog-fade">
        <div v-if="showJoinDialog" class="join-dialog-overlay" @click.self="cancelJoin">
          <div class="join-dialog-box">
            <div class="join-dialog-icon">
              <i class="fas fa-paper-plane"></i>
            </div>
            <h3 class="join-dialog-title">申请加入「{{ joiningTeam?.name }}」</h3>
            <p class="join-dialog-subtitle">请填写申请理由，车队管理员将审核您的申请</p>
            <div class="join-reason-wrap">
              <textarea v-model="joinReason" class="join-reason-input" placeholder="例如：我是一名骑行爱好者，希望能和车队一起训练和参加活动..." rows="4" maxlength="200"></textarea>
              <span class="join-char-count">{{ joinReason.length }}/200</span>
            </div>
            <div class="join-dialog-actions">
              <button class="join-btn-cancel" @click="cancelJoin">取消</button>
              <button class="join-btn-confirm" :disabled="!joinReason.trim()" @click="submitJoin">
                <i class="fas fa-paper-plane"></i> 提交申请
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 已申请提示弹窗 -->
    <Teleport to="body">
      <Transition name="join-dialog-fade">
        <div v-if="showAlreadyApplied" class="join-dialog-overlay" @click.self="showAlreadyApplied = false">
          <div class="join-dialog-box">
            <div class="join-dialog-icon" style="color: var(--accent, #f59e0b)">
              <i class="fas fa-hourglass-half"></i>
            </div>
            <h3 class="join-dialog-title">申请已提交</h3>
            <p class="join-dialog-subtitle">您已向该车队发送过加入申请，请耐心等待车队管理员审核。</p>
            <div class="join-dialog-actions">
              <button class="join-btn-confirm" @click="showAlreadyApplied = false">我知道了</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { getTeamList, getMyTeams, joinTeam, getMyPendingApplications } from '@/api/team';
import { showToast } from '@/utils/toast';

const router = useRouter();
const userStore = useUserStore();

const isCreator = (team) => {
  return userStore.isLoggedIn && userStore.userInfo && String(team.captainId) === String(userStore.userInfo.id);
};

// 用户已加入的车队ID集合（用于隐藏申请加入按钮）
const myTeamIds = ref(new Set());
const isMemberOf = (team) => myTeamIds.value.has(team.id);

// 加载用户已加入的车队
const loadMyTeamIds = async () => {
  if (!userStore.isLoggedIn) { myTeamIds.value = new Set(); return; }
  try {
    const data = await getMyTeams();
    myTeamIds.value = new Set((data || []).map(t => t.id));
  } catch { myTeamIds.value = new Set(); }
};

// 分类
const categories = [
  { key: 'all', label: '全部车队', icon: 'fas fa-th-large' },
  { key: 'hot', label: '热门车队', icon: 'fas fa-fire' },
  { key: 'mine', label: '我的车队', icon: 'fas fa-star' }
];

const activeCategory = ref('all');
const searchQuery = ref('');
const teams = ref([]);
const loading = ref(false);
const page = ref(1);
const size = 9;
const total = ref(0);

let searchTimer = null;

// 统计数据（模拟）
const totalTeams = ref(2186);
const totalMembers = ref(56840);
const totalCities = ref(32);

// 加入车队弹窗
const showJoinDialog = ref(false);
const joiningTeam = ref(null);
const joinReason = ref('');

// 总页数
const totalPages = computed(() => Math.ceil(total.value / size));

// 可见页码
const visiblePages = computed(() => {
  const t = totalPages.value;
  const cur = page.value;
  const pages = [];
  if (t <= 7) {
    for (let i = 1; i <= t; i++) pages.push(i);
  } else {
    if (cur <= 4) {
      for (let i = 1; i <= 7; i++) pages.push(i);
    } else if (cur >= t - 3) {
      for (let i = t - 6; i <= t; i++) pages.push(i);
    } else {
      for (let i = cur - 3; i <= cur + 3; i++) pages.push(i);
    }
  }
  return pages;
});

// 获取数据
const fetchData = async () => {
  loading.value = true;
  try {
    if (activeCategory.value === 'mine') {
      // 我的车队：调用专用接口
      const data = await getMyTeams();
      teams.value = data || [];
      total.value = teams.value.length;
    } else {
      // 其他分类：走分页查询
      const data = await getTeamList({
        page: page.value,
        size,
        keyword: searchQuery.value.trim(),
        category: activeCategory.value === 'all' ? undefined : activeCategory.value
      });
      teams.value = data.records || [];
      total.value = data.total || 0;
    }
  } catch (err) {
    console.error('获取车队列表失败', err);
    teams.value = [];
  } finally {
    loading.value = false;
  }
};

// 切换分类
const switchCategory = (key) => {
  if (key === 'mine') {
    if (!userStore.isLoggedIn) {
      showToast('请先登录后查看我的车队', 'warning');
      userStore.openAuthModal();
      return;
    }
  }
  activeCategory.value = key;
  page.value = 1;
  fetchData();
};

// 创建车队
const handleCreateTeam = () => {
  if (!userStore.isLoggedIn) {
    showToast('请先登录后再创建车队', 'warning');
    userStore.openAuthModal();
    return;
  }
  router.push('/peloton/create');
};

// 搜索防抖
const onSearch = () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    page.value = 1;
    fetchData();
  }, 400);
};

// 翻页
const goToPage = (num) => {
  if (num < 1 || num > totalPages.value || num === page.value) return;
  page.value = num;
  fetchData();
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// 跳转详情
const goToDetail = (id) => {
  sessionStorage.setItem('pelotonScrollTop', window.scrollY.toString());
  router.push(`/peloton/${id}`);
};

// 申请加入弹窗
const showAlreadyApplied = ref(false)
const pendingTeamIds = ref(new Set())

const loadPendingIds = async () => {
  if (!userStore.isLoggedIn) { pendingTeamIds.value = new Set(); return }
  try {
    const ids = await getMyPendingApplications()
    pendingTeamIds.value = new Set(ids || [])
  } catch { pendingTeamIds.value = new Set() }
}

const openJoinDialog = async (team) => {
  if (!userStore.isLoggedIn) { showToast('请先登录', 'warning'); userStore.openAuthModal(); return }
  // 如果还没加载过待审核列表，即时加载
  if (pendingTeamIds.value.size === 0 && userStore.isLoggedIn) {
    await loadPendingIds()
  }
  if (pendingTeamIds.value.has(team.id)) {
    showAlreadyApplied.value = true
    return
  }
  joiningTeam.value = team;
  joinReason.value = '';
  showJoinDialog.value = true;
};

const submitJoin = async () => {
  if (!joinReason.value.trim()) return;
  try {
    await joinTeam(joiningTeam.value.id, joinReason.value.trim());
    pendingTeamIds.value.add(joiningTeam.value.id)
    showToast('申请已提交，等待管理员审核', 'success');
  } catch (err) {
    showToast(err.message || '提交申请失败', 'error');
    return;
  }
  showJoinDialog.value = false;
  joinReason.value = '';
  joiningTeam.value = null;
};

const cancelJoin = () => {
  showJoinDialog.value = false;
  joiningTeam.value = null;
  joinReason.value = '';
};

onMounted(() => {
  fetchData();
  loadMyTeamIds();
  loadPendingIds();
});

onUnmounted(() => {
  clearTimeout(searchTimer);
});
</script>

<style scoped>
.peloton-page {
  padding: 140px 0 120px;
  min-height: 100vh;
}

/* --- 顶部行：Header + 统计 左右布局 --- */
.hero-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 56px;
  gap: 56px;
}

/* --- 顶部 Header --- */
.peloton-intro {
  text-align: left;
  flex-shrink: 0;
}

.section-tag {
  display: block;
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 5px;
  color: var(--primary-color);
  margin-bottom: 16px;
}

.peloton-title {
  font-family: var(--font-serif);
  font-size: 2.8rem;
  color: var(--text-main);
  font-weight: 700;
  margin: 0 0 12px 0;
  line-height: 1.15;
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 1.1rem;
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
  line-height: 1.5;
}

/* --- 统计概览条 --- */
.stats-bar {
  display: flex;
  align-items: center;
  gap: 32px;
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.stat-number {
  font-size: 2rem;
  font-weight: 800;
  color: var(--primary-color);
  font-family: var(--font-serif);
  line-height: 1;
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: 0.82rem;
  color: var(--text-muted);
  font-weight: 500;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.stat-sep {
  font-size: 1.4rem;
  font-weight: 200;
  color: rgba(0, 0, 0, 0.10);
  user-select: none;
  line-height: 1;
  transform: translateY(-1px);
}

/* --- 分类 Tab + 搜索 --- */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 48px;
  gap: 20px;
  flex-wrap: wrap;
}

.capsule-nav {
  display: flex;
  gap: 6px;
  padding: 6px;
  background: rgba(0, 0, 0, 0.035);
  border-radius: 30px;
  width: fit-content;
}

.capsule-btn {
  padding: 10px 22px;
  border-radius: 24px;
  border: none;
  background: transparent;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.capsule-btn:hover {
  color: var(--text-main);
}

.capsule-btn.active {
  background: #fff;
  color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

.capsule-btn i {
  margin-right: 6px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  position: relative;
  width: 300px;
}

.search-box i {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
  font-size: 0.85rem;
}

.search-box input {
  width: 100%;
  padding: 11px 16px 11px 40px;
  border-radius: 30px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  font-size: 0.9rem;
  transition: all 0.3s;
}

.search-box input:focus {
  border-color: var(--primary-color);
  outline: none;
  box-shadow: 0 0 0 4px rgba(92, 110, 88, 0.1);
}

/* --- 卡片网格 --- */
.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 40px;
}

.team-card {
  background: var(--bg-surface);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.45s cubic-bezier(0.165, 0.84, 0.44, 1);
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: var(--shadow-sm);
  position: relative;
}

.team-card:hover {
  transform: translateY(-6px);
  border-color: rgba(92, 110, 88, 0.2);
  box-shadow: var(--shadow-lg);
}

/* 封面 */
.card-cover {
  position: relative;
  height: 170px;
  background: #f0f2ef;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: 0.7s transform cubic-bezier(0.165, 0.84, 0.44, 1);
}

.team-card:hover .cover-img {
  transform: scale(1.06);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.35s;
  backdrop-filter: blur(2px);
}

.team-card:hover .card-overlay {
  opacity: 1;
}

.view-detail-text {
  font-size: 0.9rem;
  font-weight: 600;
  color: #fff;
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 30px;
  backdrop-filter: blur(4px);
}

/* 卡片主体 */
.card-body {
  padding: 26px;
  background: #fff;
}

.card-header-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.team-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  margin-top: -40px;
  background: #fff;
}

.team-name-group {
  flex: 1;
  min-width: 0;
}

.team-name {
  font-size: 1.08rem;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-location {
  font-size: 0.78rem;
  color: var(--text-muted);
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.team-location i {
  font-size: 0.7rem;
}

/* 加入按钮（卡片右上角 + 号） */
.card-join-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1.5px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  color: var(--text-muted);
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
  flex-shrink: 0;
  padding: 0;
  line-height: 1;
}

.card-join-btn:hover {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
  transform: rotate(90deg) scale(1.1);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.35);
}
.card-join-btn.is-edit:hover {
  transform: scale(1.1);
}

.team-card:hover .card-join-btn {
  border-color: rgba(92, 110, 88, 0.2);
  color: var(--primary-color);
}
.team-card:hover .card-join-btn.is-edit {
  color: var(--primary-color);
}

/* 简介 */
.team-desc {
  font-size: 0.9rem;
  color: var(--text-muted);
  line-height: 1.6;
  margin: 0 0 22px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 底部信息栏 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.footer-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-count {
  font-size: 0.82rem;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 5px;
}

.member-count i {
  color: var(--primary-color);
  font-size: 0.75rem;
}

.footer-tags {
  display: flex;
  gap: 6px;
}

.team-tag {
  font-size: 0.7rem;
  padding: 4px 10px;
  border-radius: 4px;
  background: rgba(92, 110, 88, 0.06);
  color: var(--primary-color);
  white-space: nowrap;
}

/* --- 创建车队按钮 --- */
.create-team-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1.5px solid rgba(92, 110, 88, 0.25);
  background: transparent;
  color: var(--primary-color);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.create-team-btn:hover {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
  transform: rotate(90deg) scale(1.1);
  box-shadow: 0 4px 14px rgba(92, 110, 88, 0.3);
}

.create-team-btn i {
  font-size: 0.82rem;
  line-height: 1;
}

/* --- 加载状态 --- */
.loading-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
}

.loading-state i {
  font-size: 1.5rem;
  margin-bottom: 12px;
  color: var(--primary-color);
}

.loading-state p {
  font-size: 0.9rem;
}

/* --- 空状态 --- */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 100px 0;
  color: var(--text-muted);
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 16px;
  opacity: 0.3;
}

.empty-state p {
  font-size: 0.95rem;
}

/* --- 分页 --- */
.pagination-wrapper {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  padding: 64px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  margin-top: 36px;
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
  background: #fff;
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
  background: #fff;
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
  color: #fff;
}

.page-info {
  margin-left: 15px;
  padding: 10px 15px;
  font-size: 0.85rem;
  color: var(--text-muted);
  background: rgba(0, 0, 0, 0.03);
  border-radius: 6px;
}

/* ================================================================
   申请加入确认弹窗
   ================================================================ */
.join-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.join-dialog-box {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 420px;
  padding: 40px 36px 32px;
  text-align: center;
  box-shadow:
    0 24px 64px rgba(0, 0, 0, 0.18),
    0 0 0 1px rgba(0, 0, 0, 0.04);
}

.join-dialog-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(92, 110, 88, 0.12) 0%, rgba(92, 110, 88, 0.04) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.join-dialog-icon i {
  font-size: 1.5rem;
  color: var(--primary-color);
}

.join-dialog-title {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 6px 0;
}

.join-dialog-subtitle {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 0 0 24px 0;
}

.join-reason-wrap {
  position: relative;
  margin-bottom: 24px;
}

.join-reason-input {
  width: 100%;
  padding: 14px 16px 32px;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  font-size: 0.9rem;
  color: var(--text-main);
  background: rgba(0, 0, 0, 0.01);
  transition: all 0.25s ease;
  resize: none;
  line-height: 1.65;
  font-family: inherit;
  box-sizing: border-box;
}

.join-reason-input:focus {
  border-color: var(--primary-color);
  background: #fff;
  outline: none;
  box-shadow: 0 0 0 4px rgba(92, 110, 88, 0.08);
}

.join-reason-input::placeholder {
  color: rgba(0, 0, 0, 0.2);
}

.join-char-count {
  position: absolute;
  right: 14px;
  bottom: 12px;
  font-size: 0.7rem;
  color: var(--text-muted);
  opacity: 0.5;
}

.join-dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.join-btn-cancel {
  padding: 12px 28px;
  border-radius: 30px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  color: var(--text-muted);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.join-btn-cancel:hover {
  border-color: rgba(0, 0, 0, 0.2);
  color: var(--text-main);
  background: #f8f8f8;
}

.join-btn-confirm {
  padding: 12px 28px;
  border-radius: 30px;
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(92, 110, 88, 0.3);
  display: flex;
  align-items: center;
  gap: 7px;
}

.join-btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.join-btn-confirm:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(92, 110, 88, 0.4);
}

.join-btn-confirm i {
  font-size: 0.8rem;
}

/* 弹窗过渡动画 */
.join-dialog-fade-enter-active {
  transition: all 0.35s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.join-dialog-fade-leave-active {
  transition: all 0.25s ease-in;
}

.join-dialog-fade-enter-from {
  opacity: 0;
}

.join-dialog-fade-enter-from .join-dialog-box {
  transform: scale(0.9) translateY(30px);
  opacity: 0;
}

.join-dialog-fade-leave-to {
  opacity: 0;
}

.join-dialog-fade-leave-to .join-dialog-box {
  transform: scale(0.95);
  opacity: 0;
}

/* --- 过渡动画 --- */
.grid-fade-enter-active,
.grid-fade-leave-active {
  transition: all 0.5s ease;
}

.grid-fade-enter-from,
.grid-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* --- 响应式 --- */
@media (max-width: 900px) {
  .hero-row {
    gap: 28px;
  }

  .stats-bar {
    gap: 20px;
  }

  .stat-item {
    gap: 6px;
  }

  .stat-number {
    font-size: 1.6rem;
  }

  .stat-label {
    font-size: 0.72rem;
  }

  .stat-sep {
    font-size: 1.1rem;
  }

  .team-grid {
    gap: 28px;
  }
}

@media (max-width: 768px) {
  .peloton-page {
    padding: 110px 0 80px;
  }

  .hero-row {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
  }

  .peloton-intro {
    text-align: center;
  }

  .peloton-title {
    font-size: 2.2rem;
  }

  .stats-bar {
    gap: 18px;
    justify-content: center;
  }

  .stat-number {
    font-size: 1.4rem;
  }

  .stat-label {
    font-size: 0.72rem;
  }

  .stat-sep {
    font-size: 1rem;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .capsule-nav {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .capsule-nav::-webkit-scrollbar {
    display: none;
  }

  .capsule-btn {
    font-size: 0.8rem;
    padding: 8px 16px;
  }

  .toolbar-right {
    width: 100%;
  }

  .search-box {
    width: 100%;
    flex: 1;
  }

  .team-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .page-info {
    display: none;
  }

  .join-dialog-box {
    padding: 32px 24px 28px;
    margin: 0 16px;
  }
}

@media (max-width: 480px) {
  .create-team-btn {
    width: 36px;
    height: 36px;
  }

  .peloton-title {
    font-size: 1.6rem;
  }

  .stats-bar {
    gap: 12px;
  }

  .stat-item {
    gap: 4px;
  }

  .stat-number {
    font-size: 1.2rem;
  }

  .stat-label {
    font-size: 0.65rem;
  }

  .stat-sep {
    font-size: 0.85rem;
  }

  .join-dialog-actions {
    flex-direction: column;
  }

  .join-btn-cancel,
  .join-btn-confirm {
    width: 100%;
    justify-content: center;
  }
}
</style>
