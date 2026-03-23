<template>
  <div class="team-detail-page">
    <AppHeader @open-auth="showModal = true" />

    <div v-if="team" class="container detail-container">

      <!-- 1. 顶部 Hero 区域 -->
      <div class="team-hero glass-card">
        <div class="hero-banner" :style="{ backgroundImage: `url(${team.cover})` }">
          <div class="overlay"></div>
          <div class="team-badges">
            <span class="level-badge" :class="team.level.toLowerCase()">{{ getLevelName(team.level) }}</span>
            <span class="city-badge"><i class="fas fa-map-marker-alt"></i> {{ team.city }}</span>
          </div>
        </div>

        <div class="hero-body">

          <!-- 左侧：车队信息 -->
          <div class="info-left">
            <div class="logo-wrapper">
              <img :src="team.logo" class="team-logo">
            </div>

            <div class="text-content">
              <div class="title-row">
                <h1 class="team-name">{{ team.name }}</h1>
                <!-- 团长设置 -->
                <button
                    v-if="isLeader"
                    class="btn btn-sm setting-btn"
                    @click="handleTeamSetting"
                    title="修改车队资料"
                >
                  <i class="fas fa-cog"></i> 设置
                </button>
              </div>

              <p class="slogan">{{ team.slogan }}</p>

              <div class="stats-row">
                <div class="stat"><span class="val">{{ team.members.length }}</span><span class="lbl">成员</span></div>
                <div class="divider"></div>
                <div class="stat"><span class="val">{{ team.weeklyKm }}</span><span class="lbl">周里程</span></div>
                <div class="divider"></div>
                <div class="stat"><span class="val">{{ team.totalClimb }}</span><span class="lbl">总爬升</span></div>
              </div>

              <!-- 按钮区域 -->
              <div class="action-area">
                <button v-if="!team.isJoined" class="btn btn-primary join-btn" @click="handleJoin">申请加入</button>
                <button v-else-if="!isLeader" class="btn btn-outline quit-btn" @click="handleQuit">
                  <i class="fas fa-sign-out-alt"></i> 退出车队
                </button>
                <span v-else class="leader-tag"><i class="fas fa-user-shield"></i> 您是团长</span>
              </div>
            </div>
          </div>

          <!-- 右侧：公告管理 -->
          <div class="info-right">
            <!-- 状态 A: 编辑模式 -->
            <div v-if="isEditingAnnouncement" class="notice-card editing">
              <div class="notice-header"><i class="fas fa-pen"></i> 编辑公告</div>
              <textarea
                  v-model="tempAnnouncement"
                  rows="4"
                  class="anno-input"
                  placeholder="发布一条新公告..."
              ></textarea>
              <div class="edit-actions">
                <button class="btn btn-sm btn-text" @click="cancelEditAnnouncement">取消</button>
                <button class="btn btn-sm btn-primary" @click="saveAnnouncement">保存</button>
              </div>
            </div>

            <!-- 状态 B: 展示模式 -->
            <div v-else-if="team.announcement" class="notice-card">
              <div class="notice-header">
                <span><i class="fas fa-bullhorn"></i> 最新公告</span>
                <div v-if="canManage" class="notice-ops">
                  <i class="fas fa-pen op-icon" @click="startEditAnnouncement" title="编辑"></i>
                  <i class="fas fa-trash op-icon" @click="deleteAnnouncement" title="删除"></i>
                </div>
              </div>
              <p class="notice-text">{{ team.announcement }}</p>
              <div class="notice-footer"><span class="date">{{ team.announcementDate }}</span></div>
            </div>

            <!-- 状态 C: 空状态 -->
            <div v-else-if="canManage" class="notice-card empty" @click="startEditAnnouncement">
              <i class="fas fa-plus-circle"></i>
              <span>发布第一条公告</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 内容 Tabs -->
      <div class="content-tabs">
        <div v-for="tab in tabs" :key="tab.id" class="tab-item" :class="{ active: currentTab === tab.id }" @click="currentTab = tab.id">
          {{ tab.name }}
          <span v-if="tab.id === 'members' && canManage && team.joinRequests.length > 0" class="badge-dot"></span>
        </div>
      </div>

      <!-- 3. Tab 内容区 -->
      <div class="tab-content-wrapper">

        <!-- Tab 1: 活动 -->
        <div v-if="currentTab === 'activity'" class="content-section fade-in">
          <div v-if="canManage" class="admin-toolbar">
            <button class="btn btn-primary btn-sm" @click="createActivity">
              <i class="fas fa-plus"></i> 发布车队活动
            </button>
          </div>

          <div class="grid-2 activity-grid">
            <div v-for="act in team.activities" :key="act.id" class="glass-card mini-activity-card" @click="viewActivity(act.id)">
              <div class="act-left">
                <div class="act-date">
                  <span class="d">{{ act.date.day }}</span>
                  <span class="m">{{ act.date.month }}</span>
                </div>
                <div class="act-info">
                  <h4>{{ act.title }}</h4>
                  <p><i class="fas fa-map-pin"></i> {{ act.location }}</p>
                </div>
              </div>
              <div class="act-right">
                <div class="act-status" :class="act.status" v-if="!canManage">{{ act.statusText }}</div>
                <div v-else class="act-manage-btns" @click.stop>
                  <button class="icon-action edit" title="编辑" @click="editActivity(act.id)"><i class="fas fa-edit"></i></button>
                  <button class="icon-action delete" title="删除" @click="deleteActivity(act.id)"><i class="fas fa-trash"></i></button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 2: 简介 -->
        <div v-if="currentTab === 'intro'" class="content-section fade-in">
          <div class="glass-card section-card">
            <h3 class="section-title">关于我们</h3>
            <p class="text-body">{{ team.desc }}</p>
          </div>
        </div>

        <!-- Tab 3: 成员 -->
        <div v-if="currentTab === 'members'" class="content-section fade-in">
          <div class="invite-bar">
            <h3 class="section-title" style="margin:0">成员名单</h3>
            <button v-if="canManage" class="btn btn-outline btn-sm" @click="inviteMember">
              <i class="fas fa-user-plus"></i> 邀请加入
            </button>
          </div>

          <!-- 审核区 -->
          <div v-if="canManage && team.joinRequests.length > 0" class="audit-section glass-card">
            <div class="audit-header"><span><i class="fas fa-bell"></i> 待审核申请</span></div>
            <div class="audit-list">
              <div v-for="req in team.joinRequests" :key="req.id" class="audit-item">
                <div class="req-user">
                  <img :src="req.avatar" class="req-avatar">
                  <div class="req-info"><span class="name">{{ req.name }}</span><span class="note">"{{ req.note }}"</span></div>
                </div>
                <div class="req-actions">
                  <button class="btn btn-sm btn-text text-danger" @click="rejectJoin(req.id)">拒绝</button>
                  <button class="btn btn-sm btn-primary" @click="approveJoin(req.id)">批准</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 成员列表 -->
          <div class="glass-card section-card">
            <div class="members-grid">
              <div v-for="member in team.members" :key="member.id" class="member-item-card">
                <div class="m-avatar-area">
                  <img :src="member.avatar" class="m-avatar">
                  <div v-if="member.role === 'leader'" class="role-pill leader">团长</div>
                  <div v-else-if="member.role === 'admin'" class="role-pill admin">管理员</div>
                </div>
                <div class="m-info-area">
                  <span class="m-name">{{ member.name }}</span>
                  <span v-if="member.specialty" class="specialty-tag">{{ member.specialty }}</span>
                  <span v-else class="specialty-placeholder">-</span>
                </div>

                <!-- 成员管理遮罩 -->
                <div v-if="canManage && canOperateMember(member)" class="manage-overlay">
                  <div class="overlay-btns">
                    <button v-if="isLeader" class="manage-btn primary-ghost" @click.stop="toggleAdmin(member)">
                      <i :class="member.role === 'admin' ? 'fas fa-arrow-down' : 'fas fa-arrow-up'"></i>
                      {{ member.role === 'admin' ? '降级' : '提权' }}
                    </button>
                    <button class="manage-btn danger-ghost" @click.stop="removeMember(member)">
                      <i class="fas fa-sign-out-alt"></i> 移出
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <AuthModal :isOpen="showModal" @close="showModal = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
import AuthModal from '@/components/AuthModal.vue';

const route = useRoute();
const router = useRouter();
const showModal = ref(false);
const currentTab = ref('activity');

const tabs = [
  {id: 'activity', name: '活动'},
  {id: 'members', name: '成员'},
  {id: 'intro', name: '简介'}
];

const team = ref(null);
const isEditingAnnouncement = ref(false);
const tempAnnouncement = ref('');

const isLeader = computed(() => team.value?.role === 'leader');
const isAdmin = computed(() => team.value?.role === 'admin');
const canManage = computed(() => isLeader.value || isAdmin.value);

const getLevelName = (level) => ({'Pro': '竞技精英', 'Amateur': '进阶训练', 'Casual': '休闲骑游'}[level] || level);

const canOperateMember = (member) => {
  if (member.id === 'u1') return false;
  if (isLeader.value) return true;
  if (isAdmin.value && member.role === 'member') return true;
  return false;
};

onMounted(() => {
  const id = route.params.id;
  setTimeout(() => {
    team.value = {
      id: id,
      name: '破风猎人 (Aero Hunters)',
      slogan: '速度是唯一的信仰。',
      city: '上海',
      level: 'Pro',
      logo: 'https://images.unsplash.com/photo-1576082218084-25cbaf818c64?q=80&w=200&fit=crop',
      cover: 'https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=1200&fit=crop',
      desc: '成立于2024年，汇聚了上海地区热爱竞技骑行的伙伴。我们每周三夜骑，周末长途拉练。',
      announcement: '本周六千岛湖拉练，请在群里接龙报名！',
      announcementDate: '2026-10-24',
      weeklyKm: 12500, totalClimb: 45000,
      isJoined: true,
      role: 'leader',
      members: [
        {id: 'u1', name: '我 (Captain)', avatar: 'https://i.pravatar.cc/100?img=11', role: 'leader', specialty: '全能'},
        {id: 'u2', name: '副队_B', avatar: 'https://i.pravatar.cc/100?img=12', role: 'admin', specialty: '爬坡'},
        {id: 'u3', name: 'Rider_C', avatar: 'https://i.pravatar.cc/100?img=13', role: 'member', specialty: '冲刺'},
        {id: 'u4', name: 'Rider_D', avatar: 'https://i.pravatar.cc/100?img=14', role: 'member', specialty: '破风'},
      ],
      joinRequests: [
        {id: 'r1', name: '新车手_X', avatar: 'https://i.pravatar.cc/100?img=20', note: '想加入'}
      ],
      activities: [
        {
          id: 101,
          title: '千岛湖环湖耐力赛',
          date: {day: '02', month: 'NOV'},
          location: '千岛湖',
          status: 'open',
          statusText: '报名中'
        },
        {
          id: 104,
          title: '周三夜骑拉练',
          date: {day: '25', month: 'OCT'},
          location: '滨江公园',
          status: 'ended',
          statusText: '已结束'
        }
      ]
    };
  }, 300);
});

// 公告
const startEditAnnouncement = () => {
  tempAnnouncement.value = team.value.announcement;
  isEditingAnnouncement.value = true;
};
const cancelEditAnnouncement = () => isEditingAnnouncement.value = false;
const saveAnnouncement = () => {
  team.value.announcement = tempAnnouncement.value;
  isEditingAnnouncement.value = false;
};
const deleteAnnouncement = () => {
  if (confirm('确定清空公告吗？')) team.value.announcement = '';
};

// 活动
const createActivity = () => router.push({
  name: 'activity-add',
  query: {teamId: team.value.id, teamName: team.value.name}
});
const editActivity = (actId) => router.push({name: 'activity-edit', params: {id: actId}});
const deleteActivity = (actId) => {
  if (confirm('确定删除?')) team.value.activities = team.value.activities.filter(a => a.id !== actId);
};

// 成员
const handleTeamSetting = () => router.push({name: 'team-edit', params: {id: team.value.id}});
const inviteMember = () => alert('邀请链接已复制');
const approveJoin = (reqId) => {
  const req = team.value.joinRequests.find(r => r.id === reqId);
  if (req) {
    team.value.members.push({...req, role: 'member', id: 'new_' + reqId});
    team.value.joinRequests = team.value.joinRequests.filter(r => r.id !== reqId);
  }
};
const rejectJoin = (reqId) => team.value.joinRequests = team.value.joinRequests.filter(r => r.id !== reqId);
const removeMember = (member) => {
  if (confirm(`移出 ${member.name}?`)) team.value.members = team.value.members.filter(m => m.id !== member.id);
};
const toggleAdmin = (member) => {
  member.role = member.role === 'admin' ? 'member' : 'admin';
};

// 通用
const handleJoin = () => alert('申请已提交');
const handleQuit = () => {
  if (confirm('确定退出?')) team.value.isJoined = false;
};
const viewActivity = (id) => window.open(router.resolve({name: 'activity-detail', params: {id}}).href, '_blank');
</script>

<style scoped>
.team-detail-page {
  padding-top: calc(var(--header-height) + 20px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.detail-container {
  max-width: 1000px;
}

/* Hero */
.team-hero {
  padding: 0;
  overflow: hidden;
  margin-bottom: var(--space-lg);
  position: relative;
  /* glass-card 提供背景与边框 */
}

.hero-banner {
  height: 240px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent, rgba(0, 0, 0, 0.6));
}

.team-badges {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  gap: 10px;
}

.level-badge, .city-badge {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 0.8rem;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  backdrop-filter: blur(4px);
}

.city-badge {
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-main);
}

.hero-body {
  padding: 0 40px 30px;
  display: flex;
  gap: 40px;
  position: relative;
}

.info-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.logo-wrapper {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid var(--bg-surface);
  background: #fff;
  overflow: hidden;
  margin-top: -50px;
  box-shadow: var(--shadow-md);
  margin-bottom: 15px;
}

.team-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.team-name {
  font-family: var(--font-serif);
  font-size: 2rem;
  margin: 0;
  color: var(--text-main);
  line-height: 1.2;
}

.setting-btn {
  background: transparent;
  border: 1px solid var(--text-muted);
  color: var(--text-muted);
  margin-left: 10px;
}

.setting-btn:hover {
  background-color: var(--text-main);
  border-color: var(--text-main);
  color: #fff !important;
}

.slogan {
  color: var(--text-muted);
  font-size: 1rem;
  font-style: italic;
  margin-bottom: 20px;
  font-family: var(--font-serif);
}

.stats-row {
  display: flex;
  gap: 30px;
  align-items: center;
  margin-bottom: 25px;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat .val {
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--primary-color);
  font-family: var(--font-serif);
  line-height: 1;
}

.stat .lbl {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.divider {
  width: 1px;
  height: 20px;
  background: rgba(0, 0, 0, 0.1);
}

.action-area {
  margin-top: 10px;
  display: flex;
  align-items: center;
}

.join-btn {
  padding: 8px 30px;
}

.quit-btn {
  border: 1px solid #C18C7E;
  color: #C18C7E;
  background: transparent;
  padding: 8px 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: 0.3s;
}

.quit-btn:hover {
  background-color: #C18C7E;
  color: #fff;
}

.leader-tag {
  color: var(--primary-color);
  font-size: 0.9rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(92, 110, 88, 0.1);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
}

/* 公告栏 */
.info-right {
  width: 320px;
  margin-top: 30px;
  flex-shrink: 0;
}

.notice-card {
  background: var(--bg-page);
  border: 1px solid rgba(0, 0, 0, 0.05);
  padding: 20px;
  border-radius: var(--radius-md);
  position: relative;
  min-height: 120px;
  display: flex;
  flex-direction: column;
}

.notice-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: var(--secondary-color);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--secondary-color);
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 10px;
}

.notice-ops {
  display: flex;
  gap: 8px;
}

.op-icon {
  cursor: pointer;
  color: var(--text-muted);
  transition: 0.2s;
}

.op-icon:hover {
  color: var(--text-main);
}

.notice-text {
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--text-body);
  margin-bottom: 10px;
  flex: 1;
}

.notice-footer {
  text-align: right;
  margin-top: auto;
}

.notice-footer .date {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.notice-card.empty {
  border: 2px dashed rgba(0, 0, 0, 0.1);
  background: transparent;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  gap: 10px;
}

.notice-card.empty::before {
  display: none;
}

.notice-card.empty:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.notice-card.editing {
  background: #fff;
  border: 1px solid var(--primary-color);
  box-shadow: var(--shadow-md);
  z-index: 5;
}

.notice-card.editing::before {
  background: var(--primary-color);
}

.anno-input {
  width: 100%;
  border: 1px solid #eee;
  background: #fafafa;
  padding: 10px;
  border-radius: var(--radius-sm);
  font-family: var(--font-sans);
  resize: none;
  outline: none;
  margin-bottom: 10px;
  font-size: 0.9rem;
  color: var(--text-main);
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Tabs */
.content-tabs {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 30px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.tab-item {
  padding: 10px 5px;
  font-size: 1.1rem;
  color: var(--text-muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: 0.3s;
  font-family: var(--font-serif);
  position: relative;
}

.tab-item:hover {
  color: var(--primary-color);
}

.tab-item.active {
  color: var(--text-main);
  border-bottom-color: var(--primary-color);
  font-weight: 600;
}

.badge-dot {
  position: absolute;
  top: 8px;
  right: -6px;
  width: 6px;
  height: 6px;
  background: #C18C7E;
  border-radius: 50%;
}

/* 内容区 */
.section-card {
  padding: 30px;
  min-height: 200px;
}

.section-title {
  font-family: var(--font-serif);
  font-size: 1.4rem;
  margin-bottom: 20px;
  border-left: 3px solid var(--primary-color);
  padding-left: 12px;
  line-height: 1;
  color: var(--text-main);
}

.text-body {
  font-size: 1rem;
  line-height: 1.8;
  color: var(--text-body);
}

/* 活动列表 */
.admin-toolbar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.mini-activity-card {
  margin-top: 10px;
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: 0.3s; /* glass-card 提供边框背景 */
}

.mini-activity-card:hover {
  transform: translateY(-3px);
  border-color: rgba(92, 110, 88, 0.3);
}

.act-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.act-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.act-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 8px;
  width: 48px;
  height: 48px;
  color: var(--primary-color);
  flex-shrink: 0;
}

.act-date .d {
  font-size: 1.3rem;
  font-weight: 600;
  line-height: 1;
  font-family: var(--font-serif);
}

.act-date .m {
  font-size: 0.65rem;
  text-transform: uppercase;
}

.act-info h4 {
  font-size: 1rem;
  margin-bottom: 4px;
  color: var(--text-main);
}

.act-info p {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.act-status {
  font-size: 0.75rem;
  font-weight: 500;
}

.act-status.open {
  color: #6E8565;
}

.act-status.ended {
  color: var(--text-muted);
}

.act-manage-btns {
  display: flex;
  gap: 8px;
  opacity: 0.5;
  transition: 0.3s;
}

.mini-activity-card:hover .act-manage-btns {
  opacity: 1;
}

.icon-action {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid transparent;
  background: rgba(0, 0, 0, 0.03);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
  font-size: 0.9rem;
}

.icon-action.edit:hover {
  color: var(--primary-color);
  background: rgba(92, 110, 88, 0.1);
}

.icon-action.delete:hover {
  color: #C18C7E;
  background: rgba(193, 140, 126, 0.1);
}

/* 成员列表 */
.invite-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.audit-section {
  padding: 20px;
  margin-bottom: 30px;
  background: rgba(255, 248, 240, 0.5);
  border: 1px dashed var(--secondary-color);
}

.audit-header {
  font-size: 0.9rem;
  color: var(--secondary-color);
  font-weight: 600;
  margin-bottom: 15px;
}

.audit-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #fff;
  border-radius: 6px;
}

.req-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.req-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.req-info {
  display: flex;
  flex-direction: column;
}

.req-info .name {
  font-size: 0.9rem;
  font-weight: 600;
}

.req-info .note {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.req-actions {
  display: flex;
  gap: 8px;
}

.members-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 20px;
}

.member-item-card {
  background: var(--bg-surface);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: var(--radius-sm);
  padding: 20px 10px;
  text-align: center;
  position: relative;
  overflow: hidden;
  transition: 0.3s;
}

.member-item-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-sm);
  border-color: var(--primary-color);
}

.m-avatar-area {
  position: relative;
  margin-bottom: 12px;
  display: inline-block;
}

.m-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: var(--shadow-sm);
}

.role-pill {
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.6rem;
  padding: 2px 8px;
  border-radius: 10px;
  color: #fff;
  white-space: nowrap;
  box-shadow: var(--shadow-xs);
  z-index: 2;
}

.role-pill.leader {
  background: #C18C7E;
}

.role-pill.admin {
  background: var(--primary-color);
}

.m-name {
  display: block;
  font-size: 0.95rem;
  color: var(--text-main);
  font-weight: 600;
  margin-bottom: 6px;
}

.specialty-tag {
  display: inline-block;
  font-size: 0.7rem;
  color: var(--text-muted);
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 8px;
  border-radius: 4px;
}

.specialty-placeholder {
  font-size: 0.7rem;
  color: #eee;
}

/* 成员管理遮罩 */
.manage-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 10;
}

.member-item-card:hover .manage-overlay {
  opacity: 1;
}

.overlay-btns {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 85%;
}

.manage-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 8px 0;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
  box-shadow: var(--shadow-sm);
}

.manage-btn.primary-ghost {
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
}

.manage-btn.primary-ghost:hover {
  background: var(--primary-color);
  color: #fff;
}

.manage-btn.danger-ghost {
  border: 1px solid #C18C7E;
  color: #C18C7E;
}

.manage-btn.danger-ghost:hover {
  background: #C18C7E;
  color: #fff;
}

@media (max-width: 768px) {
  .hero-body {
    flex-direction: column;
    padding: 0 20px 30px;
  }

  .info-right {
    width: 100%;
    margin-top: 20px;
  }

  .grid-2 {
    grid-template-columns: 1fr;
  }

  .manage-overlay {
    position: static;
    opacity: 1;
    background: transparent;
    backdrop-filter: none;
    margin-top: 15px;
    height: auto;
  }

  .overlay-btns {
    flex-direction: row;
    width: 100%;
  }
}

.fade-in {
  animation: fadeIn 0.5s ease;
}
</style>