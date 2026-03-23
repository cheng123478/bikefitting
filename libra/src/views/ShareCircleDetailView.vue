<template>
  <div class="share-detail-page">
    <div class="container detail-container">
      <div class="content-wrapper">

        <!-- 左侧/上方：主体内容卡片 -->
        <article class="main-card glass-card">

          <!-- 作者信息栏 -->
          <div class="author-bar">
            <img :src="post.userAvatar" class="avatar" alt="Author">
            <div class="author-info">
              <span class="username">{{ post.userName }}</span>
              <span class="post-time">{{ post.publishTime }} · {{ post.location }}</span>
            </div>
            <button class="btn btn-sm btn-outline follow-btn">+ 关注</button>
          </div>

          <!-- 媒体区域 -->
          <div class="media-container">
            <div v-if="post.type === 'video'" class="video-wrapper">
              <video controls :poster="post.cover" class="media-content">
                <source :src="post.mediaUrl" type="video/mp4">
                您的浏览器不支持视频播放。
              </video>
            </div>
            <div v-else class="image-wrapper">
              <img :src="post.mediaUrl" class="media-content" alt="Post Image">
            </div>
          </div>

          <!-- 文本内容 -->
          <div class="article-body">
            <!-- 分类标签 -->
            <div class="tags-header">
              <span class="cat-tag" :class="post.category">{{ getCategoryName(post.category) }}</span>
              <span v-if="post.isEvent" class="event-tag">活动</span>
            </div>

            <!-- 关联活动/骑士团卡片 -->
            <div v-if="post.activityId" class="related-activity-card" @click="goToActivity(post.activityId)">
              <div class="link-icon">
                <i class="fas fa-flag-checkered"></i>
              </div>
              <div class="link-content">
                <div class="link-label">来自于骑士团活动</div>
                <div class="link-title">
                  <span class="team-name">[{{ post.teamName }}]</span>
                  {{ post.activityTitle }}
                </div>
              </div>
              <div class="link-arrow">
                <i class="fas fa-chevron-right"></i>
              </div>
            </div>

            <h1 class="article-title">{{ post.title }}</h1>

            <!-- 正文 -->
            <div class="article-text">
              <p v-for="(para, index) in post.content.split('\n')" :key="index">
                {{ para }}
              </p>
            </div>

            <!-- Fitting 专属数据看板 -->
            <div v-if="post.category === 'fitting' && post.fitData" class="fit-spec-container">
              <div class="spec-header">
                <i class="fas fa-ruler-combined"></i> Fitting 设定参数
              </div>

              <div class="spec-body">
                <!-- 第一组：骑士身体数据 -->
                <div class="spec-group">
                  <span class="group-label">RIDER</span>
                  <div class="spec-items">
                    <div class="spec-item">
                      <span class="label">身高</span>
                      <span class="value">{{ post.fitData.height }} <small>cm</small></span>
                    </div>
                    <div class="spec-item">
                      <span class="label">跨高</span>
                      <span class="value">{{ post.fitData.inseam }} <small>cm</small></span>
                    </div>
                  </div>
                </div>

                <div class="divider-v"></div>

                <!-- 第二组：核心设定 (高亮) -->
                <div class="spec-group highlight-group">
                  <span class="group-label">KEY STATS</span>
                  <div class="spec-items">
                    <div class="spec-item highlight">
                      <span class="label">座垫高度</span>
                      <span class="value">{{ post.fitData.saddleHeight }} <small>mm</small></span>
                    </div>
                    <div class="spec-item highlight">
                      <span class="label">座把落差</span>
                      <span class="value">{{ post.fitData.drop }} <small>mm</small></span>
                    </div>
                  </div>
                </div>

                <div class="divider-v"></div>

                <!-- 第三组：车架几何 -->
                <div class="spec-group">
                  <span class="group-label">FRAME</span>
                  <div class="spec-items">
                    <div class="spec-item">
                      <span class="label">Stack</span>
                      <span class="value">{{ post.fitData.stack }}</span>
                    </div>
                    <div class="spec-item">
                      <span class="label">Reach</span>
                      <span class="value">{{ post.fitData.reach }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="action-bar">
            <div class="action-left">
              <button class="action-btn like-btn" :class="{ active: post.isLiked }" @click="toggleLike">
                <i :class="post.isLiked ? 'fas fa-heart' : 'far fa-heart'"></i>
                <span>{{ post.likes }}</span>
              </button>
              <button class="action-btn">
                <i class="far fa-comment"></i>
                <span>{{ comments.length }}</span>
              </button>
              <button class="action-btn">
                <i class="far fa-share-square"></i>
              </button>
            </div>
            <div class="action-right">
              <span class="views-count">2,341 次浏览</span>
            </div>
          </div>
        </article>

        <!-- 右侧/下方：评论区 -->
        <aside class="comment-section glass-card">
          <h3 class="section-title-sm">评论 ({{ comments.length }})</h3>

          <!-- 输入框 -->
          <div class="comment-input-box">
            <img src="https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=100" class="avatar-sm">
            <div class="input-area">
              <textarea v-model="newComment" placeholder="写下你的想法..." rows="2"></textarea>
              <button class="btn btn-primary btn-sm send-btn" :disabled="!newComment.trim()" @click="submitComment">
                发送
              </button>
            </div>
          </div>

          <!-- 评论列表 -->
          <div class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <img :src="comment.avatar" class="avatar-sm">
              <div class="comment-content">
                <div class="comment-header">
                  <span class="c-name">{{ comment.name }}</span>
                  <span class="c-time">{{ comment.time }}</span>
                </div>
                <p class="c-text">{{ comment.text }}</p>
                <div class="c-actions">
                  <span class="reply-link">回复</span>
                  <span class="like-link"><i class="far fa-heart"></i> {{ comment.likes }}</span>
                </div>
              </div>
            </div>
          </div>
        </aside>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const post = ref({
  id: 1, type: 'video', category: 'fitting', isEvent: false,
  title: 'Fitting 调整日记：解决膝盖外扩问题',
  content: `最近骑行总感觉右膝盖有异响...`,
  mediaUrl: 'https://media.w3.org/2010/05/sintel/trailer.mp4',
  cover: 'https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=1000&fit=crop',
  userAvatar: 'https://images.unsplash.com/photo-1527980965255-d3b416303d12?q=80&w=100',
  userName: 'BikeGeek_Pro',
  publishTime: '2小时前', location: '上海 · Specialized Fitting Lab',
  likes: 128, isLiked: false,
  fitData: { height: 178, inseam: 83, stack: 540, reach: 385, saddleHeight: 735, drop: 95, stem: 110 },
  activityId: 101, activityTitle: '2026 千岛湖环湖耐力挑战赛', teamName: '破风猎人车队',
});

const comments = ref([
  { id: 101, name: '骑行小透明', avatar: 'https://images.unsplash.com/photo-1599566150163-29194dcaad36?q=80&w=100', time: '1小时前', text: '非常有用的分享！', likes: 5 },
  { id: 102, name: 'PowerWatts', avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=100', time: '30分钟前', text: '请问需要提前多久预约？', likes: 2 }
]);

const newComment = ref('');

const goBack = () => router.back();
const toggleLike = () => { post.value.isLiked = !post.value.isLiked; post.value.likes += post.value.isLiked ? 1 : -1; };
const submitComment = () => {
  if (!newComment.value.trim()) return;
  comments.value.unshift({ id: Date.now(), name: '我', avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=100', time: '刚刚', text: newComment.value, likes: 0 });
  newComment.value = '';
};
const getCategoryName = (cat) => ({ fitting: 'Fitting 分享', riding: '骑行生活', event: '赛事活动' }[cat] || cat);
const goToActivity = (id) => { window.open(router.resolve({ name: 'activity-detail', params: { id } }).href, '_blank'); };
</script>

<style scoped>
.share-detail-page {
  padding-top: calc(var(--header-height) + 20px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.detail-container { max-width: 1100px; }

/* 头部 */
.detail-header { margin-bottom: 20px; }
.back-btn {
  background: transparent; border: none; font-size: 1rem; color: var(--text-muted);
  cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s; padding: 0;
}
.back-btn:hover { color: var(--primary-color); transform: translateX(-5px); }

/* 布局 */
.content-wrapper { display: grid; grid-template-columns: 2.5fr 1fr; gap: var(--space-lg); }

/* 主卡片 */
.main-card { padding: 0; overflow: hidden; border: var(--border-glass); }

.author-bar {
  display: flex; align-items: center; padding: 20px 30px;
  border-bottom: 1px solid rgba(0,0,0,0.03);
}
.avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; margin-right: 15px; }
.author-info { flex: 1; display: flex; flex-direction: column; }
.username { font-weight: 600; font-size: 1rem; color: var(--text-main); }
.post-time { font-size: 0.8rem; color: var(--text-muted); }
.follow-btn { font-size: 0.8rem; padding: 4px 16px; border-radius: var(--radius-pill); }

/* 媒体区域 */
.media-container { width: 100%; background: #000; display: flex; justify-content: center; align-items: center; }
.media-content { width: 100%; max-height: 600px; object-fit: contain; display: block; }
.video-wrapper { width: 100%; }

/* 内容区域 */
.article-body { padding: 30px; }

.tags-header { display: flex; gap: 10px; margin-bottom: 15px; }
.cat-tag { background: var(--bg-page); color: var(--primary-color); padding: 4px 12px; border-radius: 4px; font-size: 0.8rem; font-weight: 600; }
.event-tag { background: var(--secondary-color); color: #fff; padding: 4px 12px; border-radius: 4px; font-size: 0.8rem; }

.article-title {
  font-family: var(--font-serif);
  font-size: 1.8rem; margin-bottom: 20px; line-height: 1.3; color: var(--text-main); font-weight: 600;
}
.article-text { font-size: 1.05rem; line-height: 1.8; color: var(--text-body); }
.article-text p { margin-bottom: 1.2em; }

/* 关联活动卡片 */
.related-activity-card {
  display: flex; align-items: center;
  background: rgba(92, 110, 88, 0.04);
  border: 1px solid rgba(92, 110, 88, 0.1);
  border-radius: var(--radius-sm);
  padding: 12px 16px; margin-bottom: 25px;
  cursor: pointer; transition: 0.3s;
}
.related-activity-card:hover { background: rgba(92, 110, 88, 0.08); transform: translateX(5px); }
.link-icon {
  width: 36px; height: 36px; background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--primary-color); margin-right: 12px; box-shadow: var(--shadow-xs);
}
.link-content { flex: 1; display: flex; flex-direction: column; }
.link-label { font-size: 0.75rem; color: var(--text-muted); }
.link-title { font-size: 0.95rem; font-weight: 600; color: var(--text-main); font-family: var(--font-serif); }
.team-name { color: var(--primary-color); margin-right: 4px; }
.link-arrow { color: var(--text-muted); font-size: 0.9rem; margin-left: 10px; }

/* Fitting 数据面板 */
.fit-spec-container {
  margin-top: 30px; background: #F9FAF8;
  border: 1px solid rgba(92, 110, 88, 0.1);
  border-radius: var(--radius-md); overflow: hidden;
}
.spec-header {
  background: rgba(92, 110, 88, 0.08); padding: 10px 20px;
  font-size: 0.9rem; font-weight: 600; color: var(--primary-color);
  border-bottom: 1px solid rgba(92, 110, 88, 0.1); display: flex; align-items: center; gap: 8px;
}
.spec-body { padding: 20px; display: flex; justify-content: space-between; align-items: center; }
.spec-group { flex: 1; display: flex; flex-direction: column; align-items: center; }
.group-label { font-size: 0.7rem; letter-spacing: 1px; color: var(--text-muted); margin-bottom: 12px; font-weight: 700; opacity: 0.6; }
.spec-items { display: flex; gap: 25px; }
.spec-item { display: flex; flex-direction: column; align-items: center; }
.spec-item .label { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 4px; }
.spec-item .value { font-family: var(--font-serif); font-size: 1.4rem; font-weight: 600; color: var(--text-main); line-height: 1; }
.spec-item .value small { font-size: 0.7rem; font-family: var(--font-sans); color: var(--text-muted); font-weight: 400; }
.highlight-group .spec-item .value { color: var(--primary-color); font-size: 1.6rem; }
.divider-v { width: 1px; height: 40px; background: rgba(0,0,0,0.06); }

/* 操作栏 */
.action-bar { padding: 15px 30px; border-top: 1px solid rgba(0,0,0,0.05); display: flex; justify-content: space-between; align-items: center; }
.action-left { display: flex; gap: 25px; }
.action-btn { background: transparent; border: none; font-size: 1.4rem; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s; padding: 0; }
.action-btn span { font-size: 1rem; }
.action-btn:hover { color: var(--primary-color); }
.like-btn.active { color: #C18C7E; } /* Danger color */
.views-count { font-size: 0.85rem; color: var(--text-muted); }

/* 评论区 */
.comment-section { padding: 25px; position: sticky; top: 100px; height: fit-content; }
.section-title-sm { font-size: 1.2rem; margin-bottom: 20px; font-family: var(--font-serif); color: var(--text-main); }

.comment-input-box { display: flex; gap: 10px; margin-bottom: 30px; }
.avatar-sm { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.input-area { flex: 1; display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.input-area textarea {
  width: 100%; border: 1px solid rgba(0,0,0,0.1); background: #FAFAFA;
  border-radius: var(--radius-sm); padding: 10px; font-family: var(--font-sans);
  resize: none; outline: none; transition: 0.3s; font-size: 0.9rem;
}
.input-area textarea:focus { border-color: var(--primary-color); background: #fff; }

.comment-list { display: flex; flex-direction: column; gap: 20px; }
.comment-item { display: flex; gap: 12px; }
.comment-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.c-name { font-size: 0.9rem; font-weight: 600; color: var(--text-main); }
.c-time { font-size: 0.75rem; color: var(--text-muted); }
.c-text { font-size: 0.9rem; color: var(--text-body); line-height: 1.5; margin-bottom: 6px; }
.c-actions { font-size: 0.8rem; color: var(--text-muted); display: flex; gap: 15px; }
.reply-link:hover, .like-link:hover { color: var(--primary-color); cursor: pointer; }

@media (max-width: 900px) {
  .content-wrapper { grid-template-columns: 1fr; }
  .comment-section { position: static; margin-top: 20px; }
  .spec-body { flex-direction: column; gap: 20px; align-items: stretch; }
  .divider-v { width: 100%; height: 1px; margin: 10px 0; }
  .spec-items { justify-content: space-around; width: 100%; }
}
</style>