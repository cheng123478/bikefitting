<template>
  <div class="message-center-page">
    <div class="container">
      <header class="page-header">
        <button class="back-btn" @click="router.back()">
          <i class="fas fa-arrow-left"></i> 返回
        </button>
        <div class="header-actions">
          <button class="mark-all-btn" @click="markAllRead">
            <i class="fas fa-check-double"></i> 全部已读
          </button>
        </div>
      </header>

      <div class="page-title-area">
        <h1 class="page-title">消息中心</h1>
        <p class="page-subtitle">车队动态、系统通知、互动消息</p>
      </div>

      <!-- 分类标签 -->
      <div class="category-tabs">
        <button v-for="cat in categories" :key="cat.key"
          class="cat-tab" :class="{ active: activeCategory === cat.key }"
          @click="activeCategory = cat.key">
          <i :class="cat.icon"></i>
          <span>{{ cat.label }}</span>
          <span v-if="cat.count > 0" class="cat-badge">{{ cat.count }}</span>
        </button>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" v-if="filteredMessages.length > 0">
        <div v-for="msg in filteredMessages" :key="msg.id"
          class="message-card" :class="{ unread: !msg.read }"
          @click="handleMessageClick(msg)">
          <div class="msg-icon" :class="'icon-' + msg.type">
            <i :class="getTypeIcon(msg.type)"></i>
          </div>
          <div class="msg-content">
            <div class="msg-header">
              <span class="msg-title">{{ msg.title }}</span>
              <span v-if="!msg.read" class="unread-dot"></span>
            </div>
            <p class="msg-body">{{ msg.content }}</p>
            <div class="msg-meta">
              <span class="msg-team" v-if="msg.teamName">
                <i class="fas fa-flag"></i> {{ msg.teamName }}
              </span>
              <span class="msg-time"><i class="far fa-clock"></i> {{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>
          <div class="msg-actions">
            <button v-if="!msg.read" class="read-btn" title="标为已读" @click.stop="markRead(msg)">
              <i class="fas fa-check"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <i class="far fa-bell-slash"></i>
        <p>暂无消息</p>
        <span>新的车队动态和通知会显示在这里</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from '@/utils/toast'
import {
  getNotifications,
  getUnreadCount,
  markAsRead as markAsReadApi,
  markAllAsRead as markAllAsReadApi
} from '@/api/notification'

const router = useRouter()

const categories = [
  { key: 'all', label: '全部', icon: 'fas fa-inbox', count: 0 },
  { key: 'team', label: '车队动态', icon: 'fas fa-users', count: 0 },
  { key: 'interact', label: '互动消息', icon: 'fas fa-heart', count: 0 },
  { key: 'system', label: '系统通知', icon: 'fas fa-bell', count: 0 }
]
const activeCategory = ref('all')
const messages = ref([])
const loading = ref(false)

// 切换分类时重新加载
watch(activeCategory, () => {
  fetchNotifications()
})

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
})

/** 加载通知列表 */
const fetchNotifications = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (activeCategory.value !== 'all') {
      params.category = activeCategory.value
    }
    const res = await getNotifications(params)
    messages.value = res?.records || []
  } catch {
    messages.value = []
  } finally {
    loading.value = false
  }
}

/** 加载各分类未读数 */
const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    const data = res || {}
    categories[0].count = data.total || 0
    categories[1].count = data.team || 0
    categories[2].count = data.interact || 0
    categories[3].count = data.system || 0
  } catch {
    // 未读数加载失败不阻断页面
  }
}

const filteredMessages = computed(() => messages.value)

const getTypeIcon = (type) => {
  const map = {
    'new-activity': 'fas fa-flag-checkered',
    'edit-activity': 'fas fa-edit',
    'delete-activity': 'fas fa-times-circle',
    'member-join': 'fas fa-user-plus',
    'member-leave': 'fas fa-user-minus',
    'captain-leave': 'fas fa-exchange-alt',
    'member-kicked': 'fas fa-user-times',
    'member-promote': 'fas fa-arrow-up',
    'member-demote': 'fas fa-arrow-down',
    'new-notice': 'fas fa-bullhorn',
    'edit-notice': 'fas fa-edit',
    'join-apply': 'fas fa-file-signature',
    'join-approved': 'fas fa-check-circle',
    'join-rejected': 'fas fa-times-circle',
    'team-disbanded': 'fas fa-flag',
    'like': 'fas fa-heart',
    'comment': 'fas fa-comment',
    'fitting-complete': 'fas fa-microscope'
  }
  return map[type] || 'fas fa-bell'
}

/** 格式化时间显示 */
const formatTime = (createdAt) => {
  if (!createdAt) return ''
  const date = new Date(createdAt)
  const now = new Date()
  const diffMs = now - date
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return diffMin + '分钟前'
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return diffHour + '小时前'
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 30) return diffDay + '天前'
  return date.toLocaleDateString('zh-CN')
}

/** 标记单条已读 */
const markRead = async (msg) => {
  msg.read = true
  await fetchUnreadCount()
  try {
    await markAsReadApi(msg.id)
  } catch {
    // 乐观更新，失败不回滚
  }
}

/** 全部标记已读 */
const markAllRead = async () => {
  try {
    await markAllAsReadApi()
    messages.value.forEach(m => m.read = true)
    categories.forEach(cat => cat.count = 0)
    showToast('已全部标为已读', 'success')
  } catch {
    showToast('操作失败，请重试', 'error')
  }
}

const handleMessageClick = (msg) => {
  if (!msg.read) markRead(msg)
  if (msg.link) router.push(msg.link)
}
</script>

<style scoped>
.message-center-page {
  padding: 100px 0 80px;
  min-height: 100vh;
  background: linear-gradient(180deg, rgba(92,110,88,0.03) 0%, transparent 40%);
}

.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 28px;
}
.back-btn {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 18px; background: #fff;
  border: 1px solid rgba(0,0,0,0.06); border-radius: 10px;
  font-size: 0.88rem; font-weight: 600; color: var(--text-body);
  cursor: pointer; transition: all 0.25s ease;
}
.back-btn:hover { border-color: var(--primary-color); color: var(--primary-color) }
.mark-all-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; background: rgba(92,110,88,0.06);
  border: none; border-radius: 8px;
  font-size: 0.82rem; font-weight: 600; color: var(--primary-color);
  cursor: pointer; transition: all 0.25s ease;
}
.mark-all-btn:hover { background: rgba(92,110,88,0.12) }

.page-title-area { margin-bottom: 32px }
.page-title {
  font-family: var(--font-serif); font-size: 1.8rem; font-weight: 800;
  color: var(--text-main); margin: 0 0 6px 0;
}
.page-subtitle { font-size: 0.88rem; color: var(--text-muted); margin: 0 }

/* 分类标签 */
.category-tabs {
  display: flex; gap: 10px; margin-bottom: 28px;
  flex-wrap: wrap;
}
.cat-tab {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 18px; border-radius: 20px;
  border: 1px solid rgba(0,0,0,0.06); background: #fff;
  font-size: 0.85rem; font-weight: 600; color: var(--text-muted);
  cursor: pointer; transition: all 0.25s ease;
}
.cat-tab:hover { border-color: var(--primary-color); color: var(--primary-color) }
.cat-tab.active {
  background: var(--primary-color); color: #fff;
  border-color: var(--primary-color);
}
.cat-badge {
  background: rgba(231,76,60,0.9); color: #fff;
  font-size: 0.65rem; font-weight: 700;
  padding: 1px 6px; border-radius: 10px; min-width: 16px; text-align: center;
}
.cat-tab.active .cat-badge { background: rgba(255,255,255,0.3) }

/* 消息列表 */
.message-list { display: flex; flex-direction: column; gap: 10px }
.message-card {
  display: flex; align-items: flex-start; gap: 16px;
  padding: 18px 20px; border-radius: 14px;
  background: #fff; border: 1px solid rgba(0,0,0,0.04);
  cursor: pointer; transition: all 0.3s ease;
}
.message-card:hover {
  border-color: rgba(92,110,88,0.12);
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  transform: translateY(-2px);
}
.message-card.unread { background: rgba(92,110,88,0.02); border-left: 3px solid var(--primary-color) }

.msg-icon {
  width: 42px; height: 42px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.95rem;
}
.icon-new-activity { background: linear-gradient(135deg, rgba(255,140,0,0.12), rgba(255,180,50,0.08)); color: #e67e22 }
.icon-edit-activity { background: linear-gradient(135deg, rgba(255,165,0,0.12), rgba(255,190,80,0.08)); color: #e68a00 }
.icon-delete-activity { background: linear-gradient(135deg, rgba(200,60,60,0.12), rgba(230,100,100,0.08)); color: #c0392b }
.icon-member-join { background: linear-gradient(135deg, rgba(46,204,113,0.12), rgba(80,220,140,0.08)); color: #27ae60 }
.icon-member-leave { background: linear-gradient(135deg, rgba(231,76,60,0.12), rgba(255,120,100,0.08)); color: #e74c3c }
.icon-captain-leave { background: linear-gradient(135deg, rgba(243,156,18,0.12), rgba(255,180,50,0.08)); color: #f39c12 }
.icon-member-kicked { background: linear-gradient(135deg, rgba(192,57,43,0.12), rgba(220,90,80,0.08)); color: #c0392b }
.icon-member-promote { background: linear-gradient(135deg, rgba(255,215,0,0.14), rgba(255,200,50,0.08)); color: #d4a017 }
.icon-member-demote { background: linear-gradient(135deg, rgba(100,100,100,0.1), rgba(140,140,140,0.06)); color: #666 }
.icon-new-notice { background: linear-gradient(135deg, rgba(0,180,255,0.12), rgba(80,200,255,0.08)); color: #1e90ff }
.icon-edit-notice { background: linear-gradient(135deg, rgba(106,90,205,0.12), rgba(140,120,230,0.08)); color: #6a5acd }
.icon-join-apply { background: linear-gradient(135deg, rgba(52,152,219,0.12), rgba(80,180,240,0.08)); color: #3498db }
.icon-join-approved { background: linear-gradient(135deg, rgba(39,174,96,0.12), rgba(60,200,120,0.08)); color: #27ae60 }
.icon-join-rejected { background: linear-gradient(135deg, rgba(231,76,60,0.12), rgba(255,110,100,0.08)); color: #e74c3c }
.icon-team-disbanded { background: linear-gradient(135deg, rgba(142,68,173,0.12), rgba(170,100,200,0.08)); color: #8e44ad }
.icon-like { background: linear-gradient(135deg, rgba(255,69,0,0.12), rgba(255,120,80,0.08)); color: #ff4500 }
.icon-comment { background: linear-gradient(135deg, rgba(0,128,128,0.12), rgba(40,170,170,0.08)); color: #008080 }
.icon-fitting-complete { background: linear-gradient(135deg, rgba(41,128,185,0.12), rgba(60,160,210,0.08)); color: #2980b9 }

.msg-content { flex: 1; min-width: 0 }
.msg-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px }
.msg-title { font-size: 0.92rem; font-weight: 700; color: var(--text-main) }
.unread-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: #e74c3c; flex-shrink: 0;
}
.msg-body {
  font-size: 0.84rem; color: var(--text-muted); line-height: 1.55;
  margin: 0 0 8px 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.msg-meta { display: flex; align-items: center; gap: 14px; font-size: 0.72rem; color: var(--text-muted); opacity: 0.7 }
.msg-meta i { font-size: 0.65rem; margin-right: 4px }

.msg-actions { flex-shrink: 0; padding-top: 4px }
.read-btn {
  width: 28px; height: 28px; border-radius: 50%; border: none;
  background: rgba(92,110,88,0.06); color: var(--primary-color);
  display: flex; align-items: center; justify-content: center;
  font-size: 0.7rem; cursor: pointer; transition: all 0.25s ease;
}
.read-btn:hover { background: var(--primary-color); color: #fff; transform: scale(1.1) }

/* 空状态 */
.empty-state {
  text-align: center; padding: 80px 20px;
  color: var(--text-muted);
}
.empty-state i { font-size: 3rem; opacity: 0.3; margin-bottom: 16px; display: block }
.empty-state p { font-size: 1.1rem; font-weight: 600; margin: 0 0 6px 0 }
.empty-state span { font-size: 0.85rem; opacity: 0.6 }

@media (max-width: 768px) {
  .message-center-page { padding: 90px 0 60px }
  .page-title { font-size: 1.4rem }
  .message-card { padding: 14px 16px; gap: 12px }
  .msg-icon { width: 36px; height: 36px; font-size: 0.82rem }
}
</style>
