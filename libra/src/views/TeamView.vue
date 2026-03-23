<template>
  <div class="team-page">

    <main class="container team-content">

      <!-- 顶部工具栏 -->
      <div class="toolbar-row">
        <div class="text-group">
          <h1 class="page-title">骑士团</h1>
          <p class="page-subtitle">无兄弟，不骑行。加入精英车队，共享轮转破风。</p>
        </div>
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
      <TransitionGroup name="fade" tag="div" class="team-grid">

        <div
            v-for="team in filteredTeams"
            :key="team.id"
            class="team-card glass-card"
            @click="viewDetail(team)"
        >
          <!-- 头部：Logo与背景 -->
          <div class="team-header-bg">
            <div class="team-level-badge" :class="team.level.toLowerCase()">
              {{ getLevelName(team.level) }}
            </div>
            <div v-if="team.isLeader" class="leader-badge"><i class="fas fa-crown"></i> 团长</div>
          </div>

          <div class="team-logo-wrapper">
            <img :src="team.logo" class="team-logo">
          </div>

          <!-- 主体信息 -->
          <div class="team-body">
            <h3 class="team-name">{{ team.name }}</h3>
            <p class="team-slogan">{{ team.slogan }}</p>

            <div class="meta-row">
              <span><i class="fas fa-map-marker-alt"></i> {{ team.city }}</span>
              <span><i class="fas fa-users"></i> {{ team.members }}人</span>
            </div>

            <!-- 数据统计 -->
            <div class="team-stats">
              <div class="stat-item">
                <span class="label">本周里程</span>
                <span class="val">{{ team.weeklyKm }} <small>km</small></span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="label">总爬升</span>
                <span class="val">{{ team.totalClimb }} <small>m</small></span>
              </div>
            </div>

            <!-- 按钮操作区域 -->
            <div class="team-actions">
              <!-- A: 团长 -->
              <div v-if="team.isLeader" class="status-text primary">
                <i class="fas fa-user-shield"></i> 我管理的
              </div>
              <!-- B: 成员 -->
              <div v-else-if="team.isJoined" class="status-text muted">
                <i class="fas fa-check-circle"></i> 已加入
              </div>
              <!-- C: 路人 -->
              <button v-else class="btn btn-sm btn-primary btn-block" @click.stop="handleJoin(team)">
                申请加入
              </button>
            </div>
          </div>
        </div>

      </TransitionGroup>

      <!-- 空状态 -->
      <div v-if="filteredTeams.length === 0" class="empty-state">
        <div class="icon-box" style="margin-bottom:20px; opacity:0.5;"><i class="fas fa-shield-alt"></i></div>
        <p>{{ emptyStateText }}</p>
      </div>

    </main>

    <!-- FAB -->
    <button class="fab-btn" title="创建骑士团" @click="goToCreate">
      <i class="fas fa-plus"></i>
    </button>
    <AuthModal :isOpen="showModal" @close="showModal = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
import AppFooter from '@/components/AppFooter.vue';
import AuthModal from '@/components/AuthModal.vue';
import { teamService } from '@/api/mockService';

const router = useRouter();
const showModal = ref(false);
const currentTab = ref('discover');

const tabs = [
  { id: 'discover', name: '推荐车队' },
  { id: 'joined', name: '我加入的' },
  { id: 'created', name: '我创建的' }
];

const allTeams = ref([]);

const filteredTeams = computed(() => {
  return allTeams.value.filter(team => {
    if (currentTab.value === 'discover') return !team.isJoined && !team.isLeader;
    if (currentTab.value === 'joined') return team.isJoined && !team.isLeader;
    if (currentTab.value === 'created') return team.isLeader;
    return true;
  });
});

const emptyStateText = computed(() => {
  if (currentTab.value === 'joined') return '你还没有加入任何车队';
  if (currentTab.value === 'created') return '你还没有创建过车队，点击右下角创建一个吧';
  return '暂无推荐车队';
});

const getLevelName = (level) => ({ 'Pro': '⚡️ 竞技精英', 'Amateur': '🔥 进阶训练', 'Casual': '☕️ 休闲骑游' }[level] || level);
const goToCreate = () => router.push({ name: 'peloton-add' });
const viewDetail = (team) => window.open(router.resolve({ name: 'peloton-detail', params: { id: team.id } }).href, '_blank');
const handleJoin = (team) => alert(`申请加入 ${team.name} 已提交，等待团长审核。`);

const fetchTeams = async () => {
  try {
    allTeams.value = await teamService.getAllTeams();
  } catch (err) {
    console.error('Failed to fetch teams', err);
  }
};

onMounted(() => {
  fetchTeams();
});
</script>

<style scoped>
.team-page { padding-top: var(--header-height); min-height: 100vh; }
.team-content { padding-top: 40px; padding-bottom: 80px; }

/* Toolbar */
.toolbar-row {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: var(--space-xl); padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,0.03);
}
.page-title {
  font-family: var(--font-serif); font-size: 2.2rem; margin-bottom: 5px; color: var(--text-main); line-height: 1.1;
}
.page-subtitle { font-size: 1rem; color: var(--text-muted); font-family: var(--font-serif); font-style: italic; margin: 0; }

.category-tabs { display: flex; background: rgba(0,0,0,0.04); padding: 4px; border-radius: var(--radius-pill); }
.tab-pill {
  padding: 8px 24px; font-size: 0.9rem; color: var(--text-muted); cursor: pointer;
  border-radius: var(--radius-pill); transition: 0.3s; font-weight: 500; white-space: nowrap;
}
.tab-pill:hover { color: var(--primary-color); }
.tab-pill.active { background: var(--white); color: var(--primary-color); box-shadow: var(--shadow-xs); font-weight: 600; }

/* Grid (4列 -> 1列) */
.team-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md); margin: 0 auto;
}

/* Card */
.team-card {
  padding: 0; overflow: visible; display: flex; flex-direction: column; align-items: center;
  margin-top: 30px; cursor: pointer;
  border: var(--border-glass); transition: 0.4s var(--ease-out-expo);
}
.team-card:hover { transform: translateY(-5px); border-color: rgba(92, 110, 88, 0.2); }

.team-header-bg {
  width: 100%; height: 90px;
  background: linear-gradient(135deg, rgba(92, 110, 88, 0.1), rgba(181, 164, 149, 0.1));
  border-radius: var(--radius-md) var(--radius-md) 0 0; position: relative;
}

.team-level-badge {
  position: absolute; top: 10px; right: 10px; padding: 3px 10px;
  border-radius: var(--radius-sm); font-size: 0.7rem; font-weight: 600;
}
.team-level-badge.pro { background: var(--text-main); color: #fff; }
.team-level-badge.amateur { background: var(--primary-color); color: #fff; }
.team-level-badge.casual { background: #E6A23C; color: #fff; }

.leader-badge { position: absolute; top: 10px; left: 10px; background: #C18C7E; color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 0.7rem; }

.team-logo-wrapper {
  width: 80px; height: 80px; border-radius: 50%; border: 4px solid var(--bg-surface);
  background: #fff; overflow: hidden; margin-top: -40px;
  box-shadow: var(--shadow-sm); position: relative; z-index: 2;
}
.team-logo { width: 100%; height: 100%; object-fit: cover; }

.team-body { padding: 15px 20px 25px; text-align: center; width: 100%; display: flex; flex-direction: column; flex: 1; }
.team-name { font-family: var(--font-serif); font-size: 1.3rem; color: var(--text-main); margin-bottom: 5px; font-weight: 600; }
.team-slogan { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 15px; font-style: italic; min-height: 1.2em; }
.meta-row { display: flex; justify-content: center; gap: 15px; font-size: 0.8rem; color: var(--text-muted); margin-bottom: 20px; }
.meta-row i { color: var(--primary-color); margin-right: 4px; }

.team-stats {
  display: flex; justify-content: space-around; align-items: center;
  background: rgba(92, 110, 88, 0.05); border-radius: var(--radius-sm);
  padding: 10px; margin-bottom: 20px;
}
.stat-item { display: flex; flex-direction: column; }
.stat-item .label { font-size: 0.7rem; color: var(--text-muted); margin-bottom: 2px; }
.stat-item .val { font-size: 1rem; font-weight: 600; color: var(--primary-color); font-family: var(--font-serif); }
.stat-divider { width: 1px; height: 20px; background: rgba(0,0,0,0.1); }

.team-actions { margin-top: auto; display: flex; justify-content: center; width: 100%; }
.status-text { font-size: 0.9rem; font-weight: 500; padding: 6px 0; }
.status-text.primary { color: var(--primary-color); }
.status-text.muted { color: var(--text-muted); }

.fab-btn {
  position: fixed; bottom: 40px; right: 30px; width: 60px; height: 60px;
  background: var(--primary-color); color: #fff; border: none; border-radius: 50%;
  box-shadow: var(--shadow-lg); font-size: 1.5rem; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: 0.3s var(--ease-out-expo); z-index: 100;
}
.fab-btn:hover { transform: scale(1.1) rotate(90deg); background: var(--text-main); }

/* 响应式 Grid */
@media (max-width: 1200px) {
  .team-grid { grid-template-columns: repeat(3, 1fr); }
  .toolbar-row { flex-direction: column; align-items: flex-start; gap: 15px; }
  .category-tabs { width: max-content; }
  .tabs-group { width: 100%; overflow-x: auto; }
}
@media (max-width: 900px) { .team-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px) { .team-grid { grid-template-columns: 1fr; } }
</style>