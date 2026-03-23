<template>
  <div class="share-page">
     <main class="container share-content">
      <!-- 顶部工具栏 -->
      <div class="toolbar-row">
        <div class="text-group">
          <h1 class="page-title">骑行圈</h1>
          <p class="page-subtitle">记录骑行瞬间，分享 Fitting 数据</p>
        </div>
        <div class="tabs-group">
          <div class="category-tabs">
            <div
                v-for="tab in tabs"
                :key="tab.id"
                class="tab-pill"
                :class="{ active: currentTab === tab.id }"
                @click="switchTab(tab.id)"
            >
              {{ tab.name }}
            </div>
          </div>
        </div>
      </div>

      <!-- 列表 -->
      <TransitionGroup name="list-fade" tag="div" class="share-grid">
        <div
            v-for="post in filteredPosts"
            :key="post.id"
            class="share-card glass-card"
            @click="viewDetail(post)"
        >
          <!-- 封面 -->
          <div class="media-wrapper">
            <img :src="post.cover" loading="lazy" class="media-img">
            <div v-if="post.type === 'video'" class="video-badge"><i class="fas fa-play"></i> {{ post.duration }}</div>
            <div v-if="post.isEvent" class="event-badge" :class="post.isTeamPrivate ? 'private' : 'public'">
              {{ post.isTeamPrivate ? '内部活动' : '公开活动' }}
            </div>
          </div>

          <!-- 内容 -->
          <div class="card-body">
            <div class="category-tag-row">
              <span class="cat-dot" :class="post.category"></span>
              <span class="cat-name">{{ getCategoryName(post.category) }}</span>
              <span v-if="post.isEvent" class="mini-tag">骑士团活动</span>
            </div>

            <h3 class="post-title">{{ post.title }}</h3>
            <p class="post-desc">{{ post.desc }}</p>

            <div v-if="post.category === 'fitting' && post.data" class="fit-params-box">
              <div class="param-item">
                <span class="lbl">身高</span>
                <span class="val">{{ post.data.height || '-' }}</span>
              </div>
              <div class="divider-v"></div>
              <div class="param-item">
                <span class="lbl">座高</span>
                <span class="val">{{ post.data.saddleHeight || '-' }}</span>
              </div>
              <div class="divider-v"></div>
              <div class="param-item">
                <span class="lbl">落差</span>
                <span class="val">{{ post.data.drop || '-' }}</span>
              </div>
            </div>

            <div class="card-footer">
              <div class="footer-left">
                <div v-if="currentTab === 'mine'" class="btn-group">
                  <button class="btn btn-sm btn-outline" @click.stop="handleEdit(post)">
                    <i class="fas fa-edit"></i> 编辑
                  </button>
                  <button class="btn btn-sm btn-outline text-danger" @click.stop="handleDelete(post)">
                    <i class="fas fa-trash-alt"></i> 删除
                  </button>
                </div>

                <div v-else class="user-info">
                  <img :src="post.userAvatar" class="avatar">
                  <span class="username">{{ post.userName }}</span>
                </div>
              </div>

              <div class="interaction">
                <span class="like-btn" :class="{ liked: post.isLiked }" @click.stop="toggleLike(post)">
                  <i :class="post.isLiked ? 'fas fa-heart' : 'far fa-heart'"></i> {{ post.likes }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </TransitionGroup>

      <!-- FAB 按钮 -->
      <button class="fab-btn" title="发布动态" @click="goToCreate">
        <i class="fas fa-plus"></i>
      </button>

    </main>

    <AuthModal :isOpen="showModal" @close="showModal = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
import AuthModal from '@/components/AuthModal.vue';
import { shareService } from '@/api/mockService';

const router = useRouter();
const currentTab = ref('all');
const showModal = ref(false);
const isLoading = ref(false);
const myTeamId = 'team_001';

const tabs = [
  { id: 'all', name: '探索发现', icon: 'fa-compass' },
  { id: 'mine', name: '我的发布', icon: 'fa-user' },
  { id: 'fitting', name: 'Fitting 改装', icon: 'fa-tools' },
  { id: 'riding', name: '骑行生活', icon: 'fa-bicycle' },
  { id: 'event', name: '活动现场', icon: 'fa-flag-checkered' }
];

const allPosts = ref([]);

const filteredPosts = computed(() => {
  return allPosts.value.filter(post => {
    if (post.isEvent && post.isTeamPrivate && post.teamId !== myTeamId) return false;
    if (currentTab.value === 'all') return true;
    if (currentTab.value === 'mine') return post.isMine;
    if (currentTab.value === 'fitting') return post.category === 'fitting';
    if (currentTab.value === 'riding') return post.category === 'riding';
    if (currentTab.value === 'event') return post.isEvent;
    return true;
  });
});

const fetchPosts = async () => {
  isLoading.value = true;
  try {
    allPosts.value = await shareService.getAllPosts();
  } catch (err) {
    console.error('Failed to fetch posts', err);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchPosts();
});

const switchTab = (tabId) => { 
  currentTab.value = tabId; 
  isLoading.value = true; 
  setTimeout(() => isLoading.value = false, 100); 
};
const toggleLike = (post) => { 
  post.isLiked = !post.isLiked; 
  post.likes += post.isLiked ? 1 : -1; 
};
const viewDetail = (post) => { 
  window.open(router.resolve({ name: 'logbook-detail', params: { id: post.id } }).href, '_blank'); 
};
const goToCreate = () => router.push({ name: 'logbook-add' });
const handleEdit = (post) => router.push({ name: 'logbook-edit', params: { id: post.id } });
const handleDelete = (post) => { 
  if(confirm('确定删除?')) allPosts.value = allPosts.value.filter(p => p.id !== post.id); 
};
const getCategoryName = (cat) => ({ fitting: 'Fitting', riding: '骑行生活' }[cat] || cat);
</script>

<style scoped>
.share-page { padding-top: var(--header-height); min-height: 100vh; }
.share-content { padding-top: 40px; padding-bottom: 80px; }

/* Toolbar */
.toolbar-row {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: var(--space-xl); padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,0.03);
}
.page-title {
  font-family: var(--font-serif); font-size: 2.2rem; margin-bottom: 5px; color: var(--text-main); line-height: 1.1;
}
.page-subtitle { font-size: 1rem; color: var(--text-muted); margin: 0; font-family: var(--font-serif); font-style: italic; }

/* Tabs */
.category-tabs { display: flex; background: rgba(0,0,0,0.04); padding: 4px; border-radius: var(--radius-pill); }
.tab-pill {
  padding: 8px 24px; font-size: 0.9rem; color: var(--text-muted); cursor: pointer;
  border-radius: var(--radius-pill); transition: 0.3s; font-weight: 500; white-space: nowrap;
}
.tab-pill:hover { color: var(--primary-color); }
.tab-pill.active { background: #fff; color: var(--primary-color); box-shadow: var(--shadow-xs); font-weight: 600; }

/* Grid Layout (Responsive) - 核心修改点 */
.share-grid {
  display: grid;
  /* 默认 4 列 */
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md);
}

/* Card */
.share-card {
  padding: 0; overflow: hidden; cursor: pointer; display: flex; flex-direction: column;
  border: var(--border-glass);
}
.media-wrapper { position: relative; height: 180px; overflow: hidden; background: #eee; }
.media-img { width: 100%; height: 100%; object-fit: cover; transition: 0.5s var(--ease-out-expo); }
.share-card:hover .media-img { transform: scale(1.05); }

/* Badges */
.video-badge { position: absolute; top: 10px; right: 10px; background: rgba(0,0,0,0.6); color: #fff; padding: 2px 10px; border-radius: 20px; font-size: 0.75rem; }
.event-badge { position: absolute; bottom: 10px; left: 10px; padding: 4px 10px; border-radius: 4px; font-size: 0.7rem; font-weight: 600; backdrop-filter: blur(4px); }
.event-badge.public { background: rgba(255,255,255,0.9); color: var(--primary-color); }
.event-badge.private { background: rgba(43, 42, 41, 0.85); color: #fff; }

.card-body { padding: 16px; text-align: left; }
.category-tag-row { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 8px; display: flex; align-items: center; gap: 6px; font-weight: 500; }
.cat-dot { width: 6px; height: 6px; border-radius: 50%; }
.cat-dot.fitting { background: var(--primary-color); }
.cat-dot.riding { background: #E6A23C; }
.mini-tag { font-size: 0.6rem; background: #C18C7E; color: white; padding: 1px 4px; border-radius: 2px; margin-left: 4px; }

.post-title {
  font-size: 1.05rem; font-weight: 600; margin-bottom: 6px; color: var(--text-main);
  line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.post-desc {
  font-size: 0.85rem; color: var(--text-muted); margin-bottom: 12px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

/* Fitting 参数盒 */
.fit-params-box {
  display: flex; align-items: center; background: rgba(92, 110, 88, 0.05);
  border-radius: var(--radius-sm); padding: 8px 12px; margin-bottom: 15px; width: fit-content;
}
.param-item { display: flex; align-items: baseline; gap: 4px; }
.param-item .lbl { font-size: 0.7rem; color: var(--text-muted); }
.param-item .val { font-size: 0.9rem; font-weight: 600; color: var(--text-main); font-family: var(--font-serif); }
.divider-v { width: 1px; height: 10px; background: rgba(0,0,0,0.1); margin: 0 12px; }

/* Footer */
.card-footer {
  display: flex; justify-content: space-between; align-items: center; margin-top: auto;
  padding-top: 10px; border-top: 1px solid rgba(0,0,0,0.03); height: 40px;
}
.footer-left { display: flex; align-items: center; flex: 1; }
.user-info { display: flex; align-items: center; gap: 8px; }
.avatar { width: 24px; height: 24px; border-radius: 50%; object-fit: cover; }
.username { font-size: 0.8rem; color: var(--text-muted); }
.interaction { font-size: 0.85rem; color: var(--text-muted); }
.like-btn { cursor: pointer; transition: 0.3s; display: flex; align-items: center; gap: 4px; }
.like-btn:hover, .like-btn.liked { color: #C18C7E; }

/* 按钮组 */
.btn-group { display: flex; gap: 8px; }
.btn-sm { padding: 4px 10px; font-size: 0.75rem; }
.text-danger { color: #C18C7E !important; border-color: #C18C7E !important; }
.text-danger:hover { background: #C18C7E !important; color: white !important; }

/* FAB */
.fab-btn {
  position: fixed; bottom: 40px; right: 30px; width: 60px; height: 60px;
  background: var(--primary-color); color: #fff; border: none; border-radius: 50%;
  box-shadow: var(--shadow-lg); font-size: 1.5rem; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: 0.3s var(--ease-out-expo); z-index: 100;
}
.fab-btn:hover { transform: scale(1.1) rotate(90deg); background: var(--primary-dark); }

/* --- 响应式断点 (实现平滑列数递减) --- */

/* 1. 中型屏幕 (平板横屏/小笔记本) -> 3列 */
@media (max-width: 1100px) {
  .share-grid { grid-template-columns: repeat(3, 1fr); }
  .toolbar-row { flex-direction: column; align-items: flex-start; gap: 15px; }
  .tabs-group { width: 100%; overflow-x: auto; padding-bottom: 5px; }
  .category-tabs { width: max-content; }
}

/* 2. 平板竖屏 -> 2列 */
@media (max-width: 850px) {
  .share-grid { grid-template-columns: repeat(2, 1fr); }
}

/* 3. 手机屏幕 -> 1列 */
@media (max-width: 550px) {
  .share-grid { grid-template-columns: 1fr; }
  .fab-btn { bottom: 30px; right: 20px; width: 50px; height: 50px; font-size: 1.2rem; }
}
</style>