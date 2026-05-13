<template>
  <div class="home-page">
    <!-- 1. Hero Dynamic Banner (Now Floating Boxed Layout) -->
    <section class="hero-section">
      <div class="container hero-container-box" v-scroll-reveal>
        <div class="carousel-wrapper">
          <div
              v-for="(slide, index) in slides"
              :key="index"
              class="carousel-slide"
              :class="{ active: currentSlide === index }"
          >
            <div class="hero-bg-wrapper">
              <div class="hero-bg-img" :style="{ backgroundImage: `url(${slide.bg})` }"></div>
            </div>
            <div class="hero-overlay" :class="slide.overlayClass"></div>

            <div class="hero-content">
              <div v-if="index === 0" class="hero-logo-circle fade-up">
                <img src="@/assets/image/logo.png" alt="Logo" class="logo-img">
              </div>
              <div v-else class="hero-icon-header fade-up">
                <i :class="slide.icon"></i>
              </div>

              <h1 class="hero-title fade-up delay-1" v-html="DOMPurify.sanitize(slide.slogan)"></h1>
              <p class="hero-desc fade-up delay-2">{{ slide.desc }}</p>

              <div v-if="slide.btnText" class="hero-btns fade-up delay-3">
                <router-link v-if="!slide.link.startsWith('#')" :to="slide.link" class="btn btn-primary btn-glow">
                  {{ slide.btnText }} <i :class="slide.btnIcon"></i>
                </router-link>
                <a v-else :href="slide.link" class="btn btn-primary">
                  {{ slide.btnText }} <i :class="slide.btnIcon"></i>
                </a>
              </div>
              
              <div v-if="slide.isPreview" class="preview-tag fade-up delay-3">
                <span class="pulse-dot"></span> 1.5 EVOLUTION PREVIEW
              </div>
            </div>
          </div>

          <!-- Controls moved inside -->
          <button class="carousel-control prev" @click="handleManualChange(prevSlide)">
            <i class="fas fa-chevron-left"></i>
          </button>
          <button class="carousel-control next" @click="handleManualChange(nextSlide)">
            <i class="fas fa-chevron-right"></i>
          </button>

          <div class="carousel-dots">
            <span
                v-for="(dot, index) in slides"
                :key="index"
                class="dot"
                :class="{ active: currentSlide === index }"
                @click="handleManualChange(() => setSlide(index))"
            ></span>
          </div>
        </div>
      </div>

      <div class="scroll-indicator">
        <a href="#showroom" class="white-arrow"><i class="fas fa-chevron-down"></i></a>
      </div>
    </section>

    <!-- 2. 单车展厅 (Showroom Preview) - Now featured first -->
    <section id="showroom" class="section-common">
      <div class="container" v-scroll-reveal>
        <div class="section-island bg-white-island">
          <div class="section-header-row">
            <div class="header-left">
              <span class="section-tag">COLLECTION</span>
              <h2 class="section-title serif-title">单车展厅</h2>
              <p class="section-subtitle">从公路竞赛到林道穿梭，发现您的下一台战车</p>
            </div>
            <router-link to="/showroom" class="btn-text-link">查看完整库 <i class="fas fa-arrow-right"></i></router-link>
          </div>

          <div class="showroom-horizontal-grid">
            <div 
              v-for="(bike, index) in featuredBikes" 
              :key="bike.id" 
              class="featured-bike-card"
              @click="router.push('/showroom')"
            >
              <div class="bike-visual">
                <img :src="bike.img" :alt="bike.model">
                <div class="bike-specs-bubble">
                  <span>{{ bike.category }}</span>
                </div>
              </div>
              <div class="bike-meta">
                <span class="brand">{{ bike.brand }}</span>
                <h4 class="model">{{ bike.model }}</h4>
                <p class="price">{{ bike.price }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 3. Aries 控制台 (The Console / Laboratory Hub) -->
    <section id="lab" class="section-common">
      <div class="container" v-scroll-reveal>
        <div class="section-island bg-lab-island">
          <div class="section-header-row">
            <div class="header-left">
              <span class="section-tag glow">{{ PROJECT_CONFIG.MASCOT }} CONSOLE</span>
              <h2 class="section-title serif-title">实验室控制台</h2>
              <p class="section-subtitle">生物力学与单车几何的数字化映射中心</p>
            </div>
          </div>

          <!-- Asymmetric Console Layout -->
          <div class="console-layout">
            <!-- Main Action: Static Diagnosis -->
            <div class="console-main glass-panel featured-panel" @click="router.push('/lab/static')">
              <div class="panel-header">
                <span class="panel-tag">ENGINE A: STATIC</span>
                <i class="fas fa-expand-alt"></i>
              </div>
              <div class="panel-body">
                <h3 class="panel-title">静态诊断引擎</h3>
                <p class="panel-desc">高精度人体几何建模，为您生成基于 Baseline 的个性化调教建议。</p>
                
                <!-- Decorative HUD elements -->
                <div class="hud-display">
                  <div class="hud-circle"></div>
                </div>
              </div>
              <div class="panel-footer">
                <button class="btn btn-primary btn-sm">启动诊断 &rarr;</button>
              </div>
            </div>

            <!-- Secondary Actions Column -->
            <div class="console-side">
              <div class="glass-panel profile-panel" @click="router.push('/lab/body/history')">
                <div class="panel-icon"><i class="fas fa-fingerprint"></i></div>
                <div class="panel-info">
                  <h4 class="panel-title-sm">身体档案</h4>
                  <p class="panel-desc-sm">管理生理参数与柔韧性评分</p>
                </div>
              </div>

              <div class="glass-panel garage-panel" @click="router.push('/lab/garage')">
                <div class="panel-icon"><i class="fas fa-bicycle"></i></div>
                <div class="panel-info">
                  <h4 class="panel-title-sm">我的车库</h4>
                  <p class="panel-desc-sm">维护名下战车的硬件设定参数</p>
                </div>
              </div>
            </div>

            <!-- Full Width Bar: Bike Recommendation -->
            <div class="console-bar glass-panel" @click="router.push('/showroom')">
              <div class="bar-content">
                <i class="fas fa-magic spark-effect"></i>
                <div class="bar-text">
                  <span class="bar-title">单车推荐</span>
                  <span class="bar-subtitle">基于您的生物力学模型，在展厅中智能检索最适配的品牌尺寸</span>
                </div>
              </div>
              <div class="bar-action">
                <span>EXPLORE</span>
                <i class="fas fa-chevron-right"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Back to Top -->
    <Transition name="fade">
      <button v-show="showBackToTop" class="back-to-top-btn" @click="scrollToTop">
        <i class="fas fa-arrow-up"></i>
      </button>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { showroomService } from '@/api/mockService';
import DOMPurify from 'dompurify';
import { PROJECT_CONFIG } from '@/config/constants';

const router = useRouter();
const showBackToTop = ref(false);

const slides = ref([
  {
    bg: 'https://images.unsplash.com/photo-1541625602330-2277a4c46182?q=80&w=2070&auto=format&fit=crop',
    slogan: `Fitting Is Science<br><span class="hero-highlight">${PROJECT_CONFIG.MASCOT} 1.0 Performance</span>`,
    desc: `从 Baseline 诊断到展厅探索，由 ${PROJECT_CONFIG.MASCOT} 定义您的骑行几何。`,
    link: '#lab',
    btnText: '进入中枢',
    btnIcon: 'fas fa-rocket',
    overlayClass: 'overlay-soft'
  },
  {
    bg: '/image/index/fit.jpg',
    icon: 'fas fa-ruler-combined',
    slogan: '精密静态设定<br><span class="hero-highlight">Baseline Diagnosis</span>',
    desc: '基于骑行科学的身体建模，快速锁定您的理想座垫与 Reach 基准。',
    btnText: '启动引擎',
    btnIcon: 'fas fa-magic',
    link: '/lab/static',
    overlayClass: 'overlay-medium'
  },
  {
    bg: 'https://images.unsplash.com/photo-1511994298241-608e28f14fde?q=80&w=2070&auto=format&fit=crop',
    icon: 'fas fa-video',
    slogan: `动态视觉追踪<br><span class="hero-highlight">Vision Intelligence</span>`,
    desc: `实时关节角度捕捉与踩踏轨迹分析，进阶版 ${PROJECT_CONFIG.MASCOT} 1.5 即将开启公测。`,
    btnText: '了解未来',
    btnIcon: 'fas fa-atom',
    link: '#lab',
    isPreview: true,
    overlayClass: 'overlay-dark'
  }
]);

const featuredBikes = ref([]);

// Banner Logic
const currentSlide = ref(0);
let timer = null;
const slideInterval = 7000;

const nextSlide = () => { currentSlide.value = (currentSlide.value + 1) % slides.value.length; };
const prevSlide = () => { currentSlide.value = (currentSlide.value - 1 + slides.value.length) % slides.value.length; };
const setSlide = (index) => { currentSlide.value = index; };

const startTimer = () => { if (timer) clearInterval(timer); timer = setInterval(nextSlide, slideInterval); };
const pauseTimer = () => { if (timer) clearInterval(timer); };
const handleManualChange = (action) => { pauseTimer(); action(); startTimer(); };

const handleScroll = () => { showBackToTop.value = window.scrollY > window.innerHeight * 0.8; };
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }); };

const fetchData = async () => {
  try {
    const data = await showroomService.getShowroomData();
    featuredBikes.value = data.bikes.slice(0, 4);
    startTimer();
  } catch (err) {
    console.error('Fetch error', err);
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
/* --- 1. Hero Sections (Boxed Overhaul) --- */
.hero-section {
  padding-top: calc(var(--header-height) + 40px);
  padding-bottom: 60px;
  min-height: auto;
}

.hero-container-box {
  height: 75vh;
  min-height: 500px;
}

.carousel-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.1);
}

.carousel-slide {
  position: absolute; inset: 0; opacity: 0; transition: opacity 1s var(--ease-out-expo);
  display: flex; align-items: center; justify-content: center; z-index: 1;
}
.carousel-slide.active { opacity: 1; z-index: 2; }
.hero-bg-wrapper { position: absolute; inset: 0; z-index: 1; pointer-events: none; }
.hero-bg-img { width: 100%; height: 100%; background-size: cover; background-position: center; transition: 7s transform ease-out; }
.carousel-slide.active .hero-bg-img { transform: scale(1.08); }

.overlay-soft { background: linear-gradient(to bottom, rgba(43, 42, 41, 0.1), rgba(43, 42, 41, 0.4)); }
.overlay-medium { background: linear-gradient(to bottom, rgba(43, 42, 41, 0.3), rgba(43, 42, 41, 0.6)); }
.overlay-dark { background: radial-gradient(circle at center, rgba(0,0,0,0.2), rgba(0,0,0,0.6)); }

.hero-content { position: relative; z-index: 5; text-align: center; color: white; padding-bottom: 20px; max-width: 800px; }
.hero-title { font-size: clamp(2rem, 5vw, 3.2rem); line-height: 1.1; margin-bottom: 15px; }
.hero-desc { font-size: 1.1rem; opacity: 0.9; margin-bottom: 25px; font-weight: 300; max-width: 600px; margin-inline: auto; line-height: 1.5; }

.preview-tag {
  display: inline-flex; align-items: center; gap: 8px; font-size: 0.7rem; font-weight: 800;
  letter-spacing: 2px; color: var(--primary-light); background: rgba(0,0,0,0.3);
  padding: 5px 14px; border-radius: 20px; border: 1px solid rgba(143, 160, 139, 0.3); margin-top: 10px;
}

/* 轮播控制 - 移入内部并微调 */
.carousel-control {
  position: absolute; top: 50%; width: 44px; height: 44px; border-radius: 50%;
  background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2);
  color: white; cursor: pointer; z-index: 20; backdrop-filter: blur(8px); transition: 0.3s; transform: translateY(-50%);
}
.carousel-control:hover { background: rgba(255,255,255,0.25); transform: translateY(-50%) scale(1.1); }
.prev { left: 30px; } .next { right: 30px; }

.carousel-dots { position: absolute; bottom: 30px; left: 50%; transform: translateX(-50%); display: flex; gap: 10px; z-index: 20; }
.dot { width: 6px; height: 6px; background: rgba(255,255,255,0.3); border-radius: 50%; cursor: pointer; transition: 0.4s; }
.dot.active { background: white; width: 20px; border-radius: 4px; }

.scroll-indicator { position: absolute; bottom: 10px; left: 50%; transform: translateX(-50%); z-index: 20; }
.white-arrow { color: var(--text-muted); font-size: 1.4rem; animation: bounce 2.5s infinite; opacity: 0.5; }

/* --- 2. 展示区域 (Island Mode) --- */
.section-common { padding: 40px 0; min-height: auto; }

.section-island {
  padding: 60px 50px;
  border-radius: 24px;
  box-shadow: 0 10px 40px rgba(92, 110, 88, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.6);
  position: relative;
}
.bg-white-island { background: #fff; }
.bg-lab-island { background: #fdfdfb; background-image: radial-gradient(circle at top right, #f3f2ed, #fdfdfb); }

.section-header-row { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 50px; }
.section-tag { display: block; font-size: 0.7rem; font-weight: 900; letter-spacing: 4px; color: var(--primary-color); margin-bottom: 12px; }
.section-tag.glow { text-shadow: 0 0 10px rgba(143, 160, 139, 0.5); }
.section-title { font-size: 2.5rem; margin-bottom: 8px; line-height: 1.1; }
.section-subtitle { font-family: var(--font-serif); font-size: 1rem; color: var(--text-muted); font-style: italic; }

.btn-text-link { font-family: var(--font-serif); font-weight: 700; color: var(--text-main); display: flex; align-items: center; gap: 10px; transition: 0.3s; }
.btn-text-link:hover { color: var(--primary-color); transform: translateX(5px); }

.showroom-horizontal-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }
.featured-bike-card { cursor: pointer; transition: 0.4s; }
.featured-bike-card:hover { transform: translateY(-8px); }

.bike-visual { position: relative; border-radius: var(--radius-md); overflow: hidden; background: #F8F8F8; height: 180px; }
.bike-visual img { width: 100%; height: 100%; object-fit: cover; transition: 0.6s transform var(--ease-out-expo); }
.featured-bike-card:hover .bike-visual img { transform: scale(1.1); }

.bike-specs-bubble { position: absolute; top: 15px; left: 15px; background: rgba(255,255,255,0.9); padding: 4px 10px; border-radius: 4px; font-size: 0.65rem; font-weight: 800; text-transform: uppercase; color: var(--primary-color); }

.bike-meta { padding: 20px 0 0; }
.brand { font-size: 0.65rem; font-weight: 800; color: var(--primary-light); text-transform: uppercase; letter-spacing: 1px; }
.model { font-size: 1rem; color: var(--text-main); margin-top: 4px; font-weight: 700; }
.price { font-size: 0.85rem; color: var(--text-muted); font-weight: 300; margin-top: 4px; }

/* --- 3. Aries Console (Laboratory) --- */
.console-layout { display: grid; grid-template-columns: 2fr 1fr; grid-template-rows: auto auto; gap: 24px; }

.glass-panel { background: rgba(255, 255, 255, 0.6); border: 1px solid rgba(0, 0, 0, 0.05); border-radius: 12px; padding: 30px; transition: 0.4s; cursor: pointer; overflow: hidden; position: relative; box-shadow: var(--shadow-sm); }
.glass-panel:hover { background: rgba(255, 255, 255, 0.95); border-color: var(--primary-light); box-shadow: var(--shadow-md); }

.console-main { grid-row: span 1; display: flex; flex-direction: column; justify-content: space-between; min-height: 320px; }
.panel-header { display: flex; justify-content: space-between; color: var(--text-muted); font-size: 0.7rem; font-weight: 800; letter-spacing: 2px; }
.panel-title { color: var(--text-main); font-size: 1.8rem; margin: 20px 0 10px; }
.panel-desc { color: var(--text-muted); line-height: 1.6; font-size: 0.9rem; max-width: 380px; }

/* HUD Elements */
.hud-display { position: absolute; right: -20px; bottom: 80px; opacity: 0.1; pointer-events: none; }
.hud-circle { width: 140px; height: 140px; border-radius: 50%; border: 1px dashed var(--primary-color); animation: spin 20s infinite linear; }

.console-side { display: flex; flex-direction: column; gap: 24px; }
.console-side .glass-panel { flex: 1; display: flex; align-items: center; gap: 20px; padding: 20px 25px; }

.panel-icon { width: 48px; height: 48px; background: rgba(92, 110, 88, 0.05); color: var(--primary-color); border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; transition: 0.3s; }
.glass-panel:hover .panel-icon { color: white; background: var(--primary-color); }

.panel-title-sm { color: var(--text-main); font-size: 1.05rem; margin-bottom: 2px; font-weight: 700; }
.panel-desc-sm { color: var(--text-muted); font-size: 0.75rem; }

.console-bar { grid-column: span 2; display: flex; justify-content: space-between; align-items: center; padding: 20px 40px; border-left: 4px solid var(--primary-color); background: #fff; }
.bar-content { display: flex; align-items: center; gap: 25px; }
.spark-effect { font-size: 1.4rem; color: var(--accent-gold); }
.bar-text { display: flex; flex-direction: column; }
.bar-title { color: var(--text-main); font-size: 1.05rem; font-weight: 700; margin-bottom: 1px; }
.bar-subtitle { color: var(--text-muted); font-size: 0.8rem; font-family: var(--font-serif); font-style: italic; }
.bar-action { color: var(--primary-dark); font-size: 0.7rem; font-weight: 800; letter-spacing: 2px; display: flex; align-items: center; gap: 10px; }

/* Animations */
@keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }
@keyframes bounce { 0%, 20%, 50%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-10px); } 60% { transform: translateY(-5px); } }

.back-to-top-btn { position: fixed; bottom: 40px; right: 30px; width: 50px; height: 50px; border-radius: 50%; background: var(--primary-color); color: white; border: none; box-shadow: var(--shadow-lg); cursor: pointer; z-index: 1000; }

@media (max-width: 900px) {
  .console-layout { grid-template-columns: 1fr; }
  .console-bar { grid-column: span 1; padding: 20px; }
  .showroom-horizontal-grid { grid-template-columns: 1fr 1fr; }
  .section-island { padding: 40px 20px; }
  .section-title { font-size: 2rem; }
}

/* Animations */
@keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }
@keyframes bounce { 0%, 20%, 50%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-10px); } 60% { transform: translateY(-5px); } }

.back-to-top-btn { position: fixed; bottom: 40px; right: 30px; width: 50px; height: 50px; border-radius: 50%; background: var(--primary-color); color: white; border: none; box-shadow: var(--shadow-lg); cursor: pointer; z-index: 1000; }

@media (max-width: 900px) {
  .console-layout { grid-template-columns: 1fr; }
  .console-bar { grid-column: span 1; }
  .showroom-horizontal-grid { grid-template-columns: 1fr 1fr; }
  .section-title { font-size: 2.2rem; }
}
</style>