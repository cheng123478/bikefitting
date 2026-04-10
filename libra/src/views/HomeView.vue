<template>
  <div class="home-page">
    <!-- 1. Hero Banner -->
    <section class="hero-section">
      <div class="carousel-container">
        <div
            v-for="(slide, index) in slides"
            :key="index"
            class="carousel-slide"
            :class="{ active: currentSlide === index }"
        >
          <!-- 背景适配新样式结构 -->
          <div class="hero-bg-wrapper">
            <div class="hero-bg-img" :style="{ backgroundImage: `url(${slide.bg})` }"></div>
          </div>
          <div class="hero-overlay"></div>

          <div class="container hero-content">
            <!-- Logo / Icon -->
            <div v-if="index === 0" class="hero-logo-circle fade-up">
              <img src="@/assets/image/logo.png" alt="Logo" class="logo-img">
            </div>
            <div v-else class="hero-icon-header fade-up">
              <i :class="slide.icon"></i>
            </div>

            <!-- 标题与描述 -->
            <h1 class="hero-title fade-up delay-1" v-html="DOMPurify.sanitize(slide.slogan)"></h1>
            <p class="hero-desc fade-up delay-2">{{ slide.desc }}</p>

            <!-- 按钮组 -->
            <div v-if="slide.btnText" class="hero-btns fade-up delay-3">
              <router-link v-if="!slide.link.startsWith('#')" :to="slide.link" class="btn btn-primary">
                {{ slide.btnText }} <i :class="slide.btnIcon"></i>
              </router-link>
              <a v-else :href="slide.link" class="btn btn-primary">
                {{ slide.btnText }} <i :class="slide.btnIcon"></i>
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- 轮播控制 (样式升级) -->
      <button class="carousel-control prev" @click="handleManualChange(prevSlide)">
        <i class="fas fa-chevron-left"></i>
      </button>
      <button class="carousel-control next" @click="handleManualChange(nextSlide)">
        <i class="fas fa-chevron-right"></i>
      </button>

      <!-- 指示点 -->
      <div class="carousel-dots">
        <span
            v-for="(dot, index) in slides"
            :key="index"
            class="dot"
            :class="{ active: currentSlide === index }"
            @click="handleManualChange(() => setSlide(index))"
        ></span>
      </div>

      <div class="arrow-container-abs">
        <a href="#fitting" class="white-arrow"><i class="fas fa-chevron-down"></i></a>
      </div>
    </section>

    <!-- 2. Fitting 服务 -->
    <section id="fitting" class="section-common">
      <div class="container content-center">
        <div class="section-header text-center">
          <h2 class="section-title">专业 Fitting 系统</h2>
          <p class="section-subtitle">从静态设定到 AI 实战监测，全链路闭环</p>
        </div>

        <!-- 使用 Bento Grid 和 Glass Card -->
        <div class="bento-grid three-cols">
          <div class="glass-card" v-scroll-reveal="1">
            <div class="card-icon"><i class="fas fa-ruler-combined"></i></div>
            <h3 class="card-title">静态 Fitting</h3>
            <p class="card-desc">基于身体数据的快速几何设定，适合入门调试。</p>
            <router-link to="/fitting/static" class="btn btn-outline full-width-mobile">立即测量 &rarr;</router-link>
          </div>
          <div class="glass-card" v-scroll-reveal="2">
            <div class="card-icon"><i class="fas fa-video"></i></div>
            <h3 class="card-title">动态 Fitting</h3>
            <p class="card-desc">AI 视频捕捉踩踏画圆，生成包含动作建议的报告。</p>
            <router-link to="/fitting/dynamic" class="btn btn-outline full-width-mobile">上传视频 &rarr;</router-link>
          </div>
          <div class="glass-card" v-scroll-reveal="3">
            <div class="badge-new">NEW</div>
            <div class="card-icon"><i class="fas fa-mobile-alt"></i></div>
            <h3 class="card-title">RideFlow™ 实时伴骑</h3>
            <p class="card-desc">手机即仪表盘。骑行中实时捕捉姿态，语音纠错预警。</p>
            <a href="#" class="btn btn-outline full-width-mobile">开启监测 &rarr;</a>
          </div>
        </div>

        <div class="arrow-container-flow"><a href="#share" class="dark-arrow"><i class="fas fa-chevron-down"></i></a></div>
      </div>
    </section>

    <!-- 3. 分享圈 -->
    <section id="share" class="section-common bg-alt">
      <div class="container content-center">
        <div class="section-header text-center">
          <h2 class="section-title">骑行分享圈</h2>
          <p class="section-subtitle">晒出数据，记录每一公里</p>
        </div>

        <!-- 图片网格 -->
        <div class="gallery-grid-fixed">
          <div
              v-for="(item, index) in galleryImages"
              :key="index"
              class="gallery-item"
              @click="goToDetail(item.id)"
              v-scroll-reveal="index"
          >
            <img :src="item.src" loading="lazy" alt="Gallery Image" />
            <div class="gallery-overlay"><i class="fas fa-heart"></i> {{ item.likes }}</div>
          </div>
        </div>

        <div class="section-footer-btn" style="margin-top: var(--space-lg);">
          <a href="/sharecircle" class="btn btn-outline">浏览更多 <i class="fas fa-arrow-right"></i></a>
        </div>

        <div class="arrow-container-flow">
          <a href="#activities" class="dark-arrow"><i class="fas fa-chevron-down"></i></a>
        </div>
      </div>
    </section>

    <!-- 4. 活动 -->
    <section id="activities" class="section-common">
      <div class="container content-center">
        <div class="section-header text-center">
          <h2 class="section-title">热门赛事与活动</h2>
          <p class="section-subtitle">拒绝平庸，直面挑战</p>
        </div>

        <!-- 活动列表 -->
        <div class="events-grid-compact">
          <div
              v-for="(event, index) in eventsList"
              :key="index"
              class="event-row"
              v-scroll-reveal="index"
          >
            <div class="event-date">
              <span class="day">{{ event.day }}</span>
              <span class="month">{{ event.month }}</span>
            </div>
            <div class="event-details">
              <h4>{{ event.title }}</h4>
              <p>{{ event.location }}</p>
            </div>
            <a href="#" class="btn-icon"><i class="fas fa-chevron-right"></i></a>
          </div>
        </div>

        <div class="section-footer-btn" style="margin-top: var(--space-lg);">
          <a href="#" class="btn btn-outline">查看活动日历 <i class="fas fa-calendar-alt"></i></a>
        </div>

        <div class="arrow-container-flow">
          <a href="#knights" class="dark-arrow"><i class="fas fa-chevron-down"></i></a>
        </div>
      </div>
    </section>

    <!-- 5. 骑士团 -->
    <section id="knights" class="section-common bg-alt">
      <div class="container content-center">
        <div class="section-header text-center">
          <h2 class="section-title">活跃骑士团推荐</h2>
          <p class="section-subtitle">加入本地精英车队，共享轮转破风的乐趣</p>
        </div>

        <!-- 车队网格 -->
        <div class="bento-grid four-cols">
          <div
              v-for="(team, index) in teamList"
              :key="index"
              class="glass-card team-card"
              v-scroll-reveal="index"
          >
            <div class="team-avatar-wrapper">
              <img :src="team.avatar" :alt="team.name" class="team-avatar">
            </div>
            <h4 class="team-name">{{ team.name }}</h4>
            <p class="team-meta"><i class="fas fa-map-marker-alt"></i> {{ team.city }} · {{ team.members }}成员</p>
            <span class="active-badge"><i class="fas fa-fire"></i> {{ team.achievement }}</span>
            <a href="#" class="btn btn-sm btn-outline team-btn">申请加入</a>
          </div>
        </div>

        <div class="section-footer-btn" style="margin-top: var(--space-lg);">
          <a href="#" class="btn btn-outline">探索更多车队 <i class="fas fa-search"></i></a>
        </div>

      </div>
    </section>
    <!-- 6. 回到顶部按钮 -->
    <Transition name="fade">
      <button
          v-show="showBackToTop"
          class="back-to-top-btn"
          @click="scrollToTop"
          title="回到顶部"
      >
        <i class="fas fa-arrow-up"></i>
      </button>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { homeService } from '@/api/mockService';
import DOMPurify from 'dompurify';

const router = useRouter();
const showBackToTop = ref(false);

const slides = ref([
  {
    bg: 'https://images.unsplash.com/photo-1511994298241-608e28f14fde?q=80&w=2070&auto=format&fit=crop',
    slogan: 'Fit Your Ride<br><span class="hero-highlight">Perfect Your Pride</span>',
    desc: '数据驱动表现 · 姿态成就荣耀',
    link: '#',
    btnText: null
  },
  {
    bg: '/image/index/fit.jpg',
    icon: 'fas fa-project-diagram',
    slogan: 'AI 精准设定<br><span class="hero-highlight">释放无限潜能</span>',
    desc: '利用计算机视觉技术捕捉骨骼关键点，在线生成 Fitting 报告。',
    btnText: '上传视频分析',
    btnIcon: 'fas fa-cloud-upload-alt',
    link: '/survey'
  },
  {
    bg: 'https://images.unsplash.com/photo-1474962558142-9ca83af74bb7?q=80&w=2070&auto=format&fit=crop',
    icon: 'fas fa-camera-retro',
    slogan: '记录热爱<br><span class="hero-highlight">见证蜕变</span>',
    desc: '晒出你的 Fitting 改装数据，分享沿途最美的风景。',
    btnText: '进入分享圈',
    btnIcon: 'fas fa-share-alt',
    link: '/sharecircle'
  },
  {
    bg: '/image/index/activities.jpg',
    icon: 'fas fa-flag-checkered',
    slogan: '拒绝平庸<br><span class="hero-highlight">直面挑战</span>',
    desc: '每一次出发，都是为了超越昨天的自己。',
    btnText: '查看活动日历',
    btnIcon: 'fas fa-calendar-alt',
    link: '#activities'
  },
  {
    bg: '/image/index/team.jpg',
    icon: 'fas fa-users-cog',
    slogan: '破风前行<br><span class="hero-highlight">岂止于快</span>',
    desc: '无兄弟，不骑行。加入本地精英骑士团，与队友轮转交替。',
    btnText: '寻找我的车队',
    btnIcon: 'fas fa-user-plus',
    link: '#knights'
  }
]);
const galleryImages = ref([]);
const eventsList = ref([]);
const teamList = ref([]);

// 滚动监听与处理
const handleScroll = () => {
  showBackToTop.value = window.scrollY > window.innerHeight * 0.8;
};

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
};

const goToDetail = (id) => {
  console.log(`跳转到详情页 ID: ${id}`);
};

// 轮播图逻辑
const currentSlide = ref(0);
let timer = null;
const slideInterval = 5000;

const nextSlide = () => {
  if (slides.value.length > 0) {
    currentSlide.value = (currentSlide.value + 1) % slides.value.length;
  }
};
const prevSlide = () => {
  if (slides.value.length > 0) {
    currentSlide.value = (currentSlide.value - 1 + slides.value.length) % slides.value.length;
  }
};
const setSlide = (index) => { currentSlide.value = index; };

const startTimer = () => {
  if (timer) clearInterval(timer);
  timer = setInterval(nextSlide, slideInterval);
};

const pauseTimer = () => {
  if (timer) clearInterval(timer);
};

const resumeTimer = () => {
  startTimer();
};

const handleManualChange = (action) => {
  pauseTimer();
  action();
  resumeTimer();
};

const fetchData = async () => {
  try {
    const data = await homeService.getHomeData();
    galleryImages.value = data.galleryImages;
    eventsList.value = data.eventsList;
    teamList.value = data.teamList;

    startTimer();
  } catch (err) {
    console.error('Failed to fetch home data', err);
  }
};

onMounted(() => {
  fetchData();
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  pauseTimer();
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
/* --- 1. Hero 区域 (覆盖增强) --- */
.carousel-container { width: 100%; height: 100%; position: relative; }
.carousel-slide {
  position: absolute; inset: 0;
  opacity: 0; transition: opacity 1.2s var(--ease-out-expo);
  display: flex; align-items: center; justify-content: center; z-index: 1;
}
.carousel-slide.active { opacity: 1; z-index: 2; }
/* 背景放大动效 */
.carousel-slide.active .hero-bg-img { transform: scale(1.05); }

.hero-icon-header {
  font-size: 3.5rem; color: rgba(255,255,255,0.95);
  margin-bottom: var(--space-md); filter: drop-shadow(0 4px 10px rgba(0,0,0,0.2));
}

.hero-desc {
  font-size: 1.25rem; opacity: 0.95; margin-bottom: var(--space-lg);
  font-weight: 300; max-width: 700px; margin-left: auto; margin-right: auto;
}
:deep(.hero-highlight) {
  font-family: var(--font-serif); font-style: italic;
  display: block; margin-top: 8px; font-size: 1.1em;
}

/* 轮播控件 */
.carousel-control {
  position: absolute; top: 50%; transform: translateY(-50%);
  width: 50px; height: 50px; border-radius: 50%;
  background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.4);
  color: white; font-size: 1.2rem; cursor: pointer; z-index: 20;
  backdrop-filter: blur(4px); transition: all 0.3s;
}
.carousel-control:hover { background: rgba(255,255,255,0.3); transform: translateY(-50%) scale(1.1); }
.prev { left: var(--space-lg); }
.next { right: var(--space-lg); }

.carousel-dots {
  position: absolute; bottom: 100px; left: 50%; transform: translateX(-50%);
  display: flex; gap: 12px; z-index: 20;
}
.dot {
  width: 10px; height: 10px; background: rgba(255,255,255,0.4); border-radius: 50%;
  cursor: pointer; transition: 0.3s;
}
.dot.active { background: white; transform: scale(1.4); }

.arrow-container-abs {
  position: absolute; bottom: 30px; left: 50%; transform: translateX(-50%); z-index: 20;
}
.white-arrow {
  color: white; font-size: 2rem; display: block;
  animation: bounce 2.5s infinite; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
}

/* --- 2. 通用 Section --- */
.section-common {
  min-height: 100vh; padding: var(--space-xxl) 0;
  display: flex; flex-direction: column; justify-content: center;
  position: relative;
}
.content-center { width: 100%; display: flex; flex-direction: column; align-items: center; }
.bg-alt { background-color: var(--bg-page); }

.section-header { margin-bottom: var(--space-xl); max-width: 800px; }
.section-title { font-size: var(--h2-size); margin-bottom: var(--space-xs); color: var(--text-main); }

/* 串联箭头 */
.arrow-container-flow { margin-top: var(--space-xl); width: 100%; text-align: center; }
.dark-arrow {
  color: var(--text-muted); font-size: 1.6rem; display: inline-block;
  transition: 0.3s; animation: bounce 2.5s infinite;
}
.dark-arrow:hover { color: var(--primary-color); transform: translateY(3px); }

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-10px); }
  60% { transform: translateY(-5px); }
}

/* --- 3. Grid 布局 --- */
/* .bento-grid, .glass-card 已在 main.css 定义，这里仅做微调 */
.three-cols { grid-template-columns: repeat(3, 1fr); width: 100%; max-width: var(--max-width); }
.four-cols { grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); width: 100%; max-width: 1100px; }

/* 卡片内部元素适配 */
.badge-new {
  position: absolute; top: 16px; right: -32px;
  background: var(--secondary-color); color: white; width: 120px; text-align: center;
  transform: rotate(45deg); font-size: 0.75rem; font-weight: 600; padding: 6px 0;
  box-shadow: var(--shadow-sm);
}

/* --- 4. 画廊 (Fixed Grid 保持原布局，应用新变量) --- */
.gallery-grid-fixed {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-sm);
  width: 100%; max-width: 1000px; margin: 0 auto;
}
.gallery-item {
  height: 220px; border-radius: var(--radius-md); overflow: hidden;
  position: relative; cursor: pointer; background: #ddd; box-shadow: var(--shadow-sm);
}
.gallery-item img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.8s var(--ease-out-expo); filter: saturate(0.9);
}
.gallery-overlay {
  position: absolute; inset: 0; background: rgba(43, 42, 41, 0.5);
  display: flex; align-items: center; justify-content: center;
  color: white; opacity: 0; transition: 0.3s; backdrop-filter: blur(2px);
  font-family: var(--font-serif); font-size: 1.2rem;
}
.gallery-item:hover img { transform: scale(1.1); filter: saturate(1.1); }
.gallery-item:hover .gallery-overlay { opacity: 1; }

/* --- 5. 活动列表 --- */
.events-grid-compact {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-md);
  width: 100%; max-width: 900px; margin: 0 auto;
}
.event-row {
  background: var(--bg-surface); padding: 20px 24px;
  border-radius: var(--radius-md); box-shadow: var(--shadow-sm);
  border: var(--border-glass); display: flex; align-items: center;
  transition: all 0.3s; border-left: 3px solid transparent;
}
.event-row:hover {
  transform: translateX(5px) translateY(-2px);
  border-left-color: var(--primary-color);
  box-shadow: var(--shadow-md);
}
.event-date { margin-right: 20px; text-align: center; color: var(--primary-color); min-width: 50px; }
.event-date .day {
  font-size: 1.8rem; font-weight: 600; line-height: 1; font-family: var(--font-serif);
}
.event-date .month { font-size: 0.75rem; font-weight: 700; letter-spacing: 2px; color: var(--text-muted); text-transform: uppercase; }

.event-details h4 { font-size: 1.1rem; margin-bottom: 4px; color: var(--text-main); }
.event-details p { font-size: 0.9rem; color: var(--text-muted); margin: 0; }
.btn-icon { color: var(--text-muted); margin-left: auto; font-size: 1.1rem; transition: 0.3s; }
.event-row:hover .btn-icon { color: var(--primary-color); transform: translateX(4px); }

/* --- 6. 骑士团 --- */
.team-card {
  display: flex; flex-direction: column; align-items: center; padding: 30px 20px;
}
.team-avatar-wrapper {
  width: 80px; height: 80px; border-radius: 50%;
  margin: 0 auto var(--space-sm); padding: 3px;
  border: 1px solid var(--secondary-color);
}
.team-avatar { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.team-name { font-size: 1.15rem; margin-bottom: 4px; color: var(--text-main); }
.team-meta { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 12px; font-style: italic; font-family: var(--font-serif); }
.active-badge {
  display: inline-block; background: #FFF3E0; color: #E65100;
  border: none; font-size: 0.75rem; font-weight: 600; padding: 4px 12px;
  border-radius: 20px; margin-bottom: 15px;
}
.team-btn { margin-top: auto; }
/* --- 回到顶部按钮 --- */
.back-to-top-btn {
  position: fixed;
  bottom: 40px;
  right: 30px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--primary-color); /* 主色调 */
  color: #fff;
  border: none;
  box-shadow: var(--shadow-lg); /* 统一阴影 */
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  z-index: 900; /* 低于导航栏和模态框，高于内容 */
  transition: all 0.3s var(--ease-out-expo); /* 统一缓动函数 */
  opacity: 0.9;
}

.back-to-top-btn:hover {
  transform: translateY(-5px);
  background: var(--primary-dark, #465543); /* 如果 main.css 没有定义 dark，这里提供后备 */
  opacity: 1;
  box-shadow: 0 8px 20px rgba(92, 110, 88, 0.3);
}

/* 按钮进出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(20px); /* 同样增加位移动效 */
}
/* 响应式 */
@media (max-width: 768px) {
  .three-cols { grid-template-columns: 1fr; }
  .gallery-grid-fixed { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .gallery-item { height: 140px; }
  .events-grid-compact { grid-template-columns: 1fr; }
  .four-cols { grid-template-columns: repeat(2, 1fr); gap: 16px; }
  .full-width-mobile { width: 100%; margin-top: 10px; }
  .back-to-top-btn {
    bottom: 30px;
    right: 20px;
    width: 44px;
    height: 44px;
    font-size: 1rem;
  }
}
</style>