<template>
  <div class="lab-dashboard-page">
    <div class="container">

      <!-- 1. 实验室抬头：极致留白与引擎呼吸灯 -->
      <header class="lab-intro" v-scroll-reveal>
        <div class="intro-left">
          <h1 class="lab-title">实验室控制台</h1>
          <p class="page-subtitle">{{ PROJECT_CONFIG.MASCOT }} 视觉引擎与生物力学分析中枢</p>
        </div>

        <!-- 极简的引擎状态指示器 -->
        <div class="engine-status-wrapper">
          <div class="engine-status" :title="`${PROJECT_CONFIG.MASCOT} 引擎运作正常，随时待命`">
            <span class="status-dot"></span>
            <span class="status-text">{{ PROJECT_CONFIG.MASCOT }} Engine: Online</span>
          </div>
        </div>
      </header>

      <div class="lab-workspace">

        <!-- 2. 左侧：核心资产 (Assets) -->
        <aside class="assets-column" v-scroll-reveal.delay="100">
          <h3 class="column-title">
            <span class="en-prefix">PROFILES</span>
            <span class="cn-text">骑士档案</span>
          </h3>

          <!-- A. 身体图谱卡片 -->
          <div class="asset-card body-card glass-card" @click="goTo('/lab/body/history')">
            <div class="card-icon"><i class="fas fa-fingerprint"></i></div>
            <div class="card-content">
              <h4>身体图谱</h4>
              <template v-if="dashboardData.hasBodyProfile">
                <p class="text-muted" style="font-size: 0.8rem">柔韧性评分: <strong class="score-val">{{ dashboardData.flexibilityScore || '--' }}</strong></p>
                <p class="text-muted" style="font-size: 0.8rem">稳定性评分: <strong class="score-val">{{ dashboardData.stabilityScore || '--' }}</strong></p>
              </template>
              <template v-else>
                <p class="pending-text" style="font-size: 0.8rem">尚未录入数据</p>
              </template>
            </div>
            <div class="action-arrow"><i class="fas fa-chevron-right"></i></div>
          </div>

          <!-- B. 我的车库入口 (单车数量归位到这里) -->
          <div class="asset-card garage-card glass-card" @click="goTo('/lab/garage')">
            <div class="card-icon"><i class="fas fa-bicycle"></i></div>
            <div class="card-content">
              <h4>我的车库</h4>
              <!-- 数量统计移到了卡片内部 -->
              <p class="text-muted" style="font-size: 0.8rem" v-if="dashboardData.bikeCount > 0">
                管理 <strong style="color: var(--primary-color);">{{ dashboardData.bikeCount }}</strong> 辆在役战车
              </p>
              <p class="pending-text" style="font-size: 0.8rem" v-else>车库空空如也</p>
            </div>
            <div class="action-arrow"><i class="fas fa-chevron-right"></i></div>
          </div>

          <!-- C. 数据档案室 -->
          <div class="asset-card archive-card glass-card" @click="goTo('/lab/archives')">
            <div class="card-icon"><i class="fas fa-folder-open"></i></div>
            <div class="card-content">
              <h4>数据档案室</h4>
              <p class="text-muted" style="font-size: 0.8rem">集中管理静态/动态/实时历史数据</p>
            </div>
            <div class="action-arrow"><i class="fas fa-chevron-right"></i></div>
          </div>

          <div class="quick-link" @click="goTo('/showroom')">
            <span>浏览单车展厅</span>
            <i class="fas fa-external-link-alt"></i>
          </div>
        </aside>

        <!-- 3. 右侧：调教引擎 (Engines) -->
        <main class="engines-column" v-scroll-reveal.delay="200">
          <h3 class="column-title">
            <span class="en-prefix">DIAGNOSTICS</span>
            <span class="cn-text">生物力学诊断</span>
          </h3>

          <div class="engines-grid">

            <!-- 引擎 A: 静态几何诊断 -->
            <div class="engine-card static-engine glass-card">
              <div class="engine-visual">
                <i class="fas fa-ruler-combined"></i>
                <div class="radar-scan"></div>
              </div>

              <div class="engine-info">
                <div class="info-top-row">
                  <span class="tag">BASELINE / 基准</span>
                  <button class="icon-link-btn"
                          @click.stop="goTo('/lab/archives/static')"
                          title="查看历史记录">
                    <i class="fas fa-history"></i> 进化史
                  </button>
                </div>

                <h3 class="engine-title">静态几何诊断</h3>

                <div class="engine-body">
                    <div class="status-content">
                      <p class="desc-text">基于当前身体图谱生成调教建议。{{ PROJECT_CONFIG.MASCOT }} 引擎已就绪，请启动静态计算。</p>
                      <div class="action-footer">
                        <button class="btn-primary-sm" @click="goToStatic">
                          生成调教方案 <i class="fas fa-magic"></i>
                        </button>
                      </div>
                    </div>
                </div>
              </div>
            </div>

            <!-- 引擎 B: 动态视觉分析 -->
            <div class="engine-card dynamic-engine glass-card" >
              <div class="engine-visual">
                <i class="fas fa-video"></i>
              </div>
              <div class="engine-info">
                <div class="info-top-row">
                  <span class="tag">VISION / 视觉引擎</span>
                  <button class="icon-link-btn"
                          @click.stop="goTo('/lab/archives/dynamic')"
                          title="查看历史记录">
                    <i class="fas fa-history"></i> 进化史
                  </button>
                </div>
                <h3 class="engine-title">动态视觉分析</h3>
                <div class="engine-body">
                  <p class="desc-text">利用机器视觉捕捉实时踩踏轨迹，识别关节运动死点与膝盖轨迹偏移。</p>
                  <div class="action-footer">
                    <button class="btn-primary-sm" @click="goToDynamic">
                      视觉分析 <i class="fas fa-chevron-right"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 引擎 C: 实时监护 -->
            <div class="engine-card live-engine glass-card" @click="goTo('/lab/realtime')">
              <div class="engine-visual">
                <i class="fas fa-heartbeat"></i>
              </div>
              <div class="engine-info">
                <div class="info-top-row">
                  <span class="tag">REAL-TIME / 监护</span>
                </div>
                <h3 class="engine-title">姿态实时监护</h3>
                <div class="engine-body">
                  <p class="desc-text">手机传感器联动，在长距离骑行过程中监控并预警核心疲劳与代偿姿态。</p>
                </div>
              </div>
            </div>

          </div>
        </main>

      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useLabStore } from '@/stores/lab.js';
import { PROJECT_CONFIG } from '@/config/constants';
import { showToast } from '@/utils/toast.js';

const route = useRoute();
const router = useRouter();
const labStore = useLabStore();

const dashboardData = computed(() => labStore.dashboardData);

const goTo = (path) => router.push(path);

const goToDynamic = () => {
  const d = dashboardData.value;
  if (!d.hasBodyProfile) {
    showToast('请先完成身体图谱录入', 'warning');
    router.push('/lab/body');
    return;
  }
  router.push({
    path: '/lab/dynamic'
  });
};

const goToStatic = async () => {
  const d = dashboardData.value;
  if (!d.hasBodyProfile) {
    showToast('请先完成身体图谱录入', 'warning');
    router.push('/lab/body');
    return;
  }
  router.push('/lab/static');
};

onMounted(async () => {
  try {
    // 实验室首页作为入口，强制刷新数据
    const data = await labStore.fetchDashboard(true);
  } catch (err) {
    console.error("加载实验室数据失败", err);
  }
});
</script>

<style scoped>
.lab-dashboard-page {
  padding: 120px 0 100px;
  min-height: 100vh;
  background: transparent;
}

/* 1. Header 与呼吸灯样式 */
.lab-intro {
  display: flex;
  justify-content: space-between;
  align-items: center; /* 改为居中对齐，适应右侧的小部件 */
  margin-bottom: 50px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.lab-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  color: var(--text-main);
  font-weight: 700;
  margin: 0 0 5px 0;
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 1rem;
  margin: 0;
}

/* 呼吸灯指示器核心样式 */
.engine-status {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 8px 18px;
  background: rgba(92, 110, 88, 0.05); /* 淡淡的鼠尾草绿底色 */
  border: 1px solid rgba(92, 110, 88, 0.15);
  border-radius: 30px;
  cursor: default;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #52c41a; /* 荧光绿 */
  box-shadow: 0 0 8px #52c41a;
  /* 呼吸灯动画 */
  animation: breathe 3s infinite ease-in-out;
}

.status-text {
  font-family: 'Lato', monospace;
  font-size: 0.75rem;
  letter-spacing: 2px;
  color: var(--primary-color);
  font-weight: 700;
  text-transform: uppercase;
}

@keyframes breathe {
  0% { opacity: 0.4; transform: scale(0.8); box-shadow: 0 0 4px #52c41a; }
  50% { opacity: 1; transform: scale(1.2); box-shadow: 0 0 12px #52c41a; }
  100% { opacity: 0.4; transform: scale(0.8); box-shadow: 0 0 4px #52c41a; }
}

/* 布局 */
.lab-workspace {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 40px;
}

.column-title {
  font-size: 0.8rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.15em;
  margin-bottom: 20px;
  font-weight: 700;
}

/* 资产列表 */
.assets-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.asset-card {
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
  transition: 0.3s;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(92, 110, 88, 0.15);
}

.asset-card:hover {
  transform: translateX(5px);
  background: #FFF;
  border-color: var(--primary-light);
  box-shadow: var(--shadow-sm);
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.card-content h4 {
  font-size: 1.1rem;
  color: var(--text-main);
  margin-bottom: 4px;
}
.card-content p {
  margin: 0;
}
.pending-text {
  color: var(--warning-color);
  font-style: italic;
}

.score-val {
  color: var(--primary-color);
  font-family: var(--font-serif);
  font-size: 1.2rem;
}

.action-arrow {
  margin-left: auto;
  color: #DDD;
}

/* 车辆快速切换器 */
.bike-quick-picker { margin-top: 10px; }
.picker-label { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 12px; font-weight: 600; }
.bike-mini-list { display: flex; flex-direction: column; gap: 8px; }
.bike-chip {
  padding: 12px 16px; border-radius: 8px; background: rgba(255, 255, 255, 0.4);
  cursor: pointer; transition: 0.3s; display: flex; align-items: center; gap: 10px; font-size: 0.9rem;
  border: 1px solid transparent;
}
.bike-chip.active {
  background: #FFF; border-color: var(--primary-color); color: var(--primary-color);
  font-weight: 600; box-shadow: var(--shadow-sm);
}
.verified-icon { color: var(--success-color); font-size: 0.8rem; }

/* 引擎网格 */
.engines-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 25px; }

/* --- 引擎卡片基础容器 --- */
.engine-card {
  padding: 35px 30px; /* 统一内边距，使其显得更宽敞 */
  display: flex;
  gap: 25px; /* 与静态引擎的 gap: 30px 接近 */
  transition: 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
  background: white;
  border-radius: 12px;
  border: 1px solid rgba(92, 110, 88, 0.12);
}
.engine-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 35px rgba(92, 110, 88, 0.08); /* 稍微柔和的绿色阴影 */
  border-color: rgba(92, 110, 88, 0.22);
}
.static-engine {
  grid-column: 1 / -1; display: flex; gap: 30px; padding: 30px !important; cursor: default;
  background: linear-gradient(135deg, rgba(255,255,255,0.8) 0%, rgba(248,249,247,0.8) 100%);
  border: 1px solid rgba(92, 110, 88, 0.15);
}

.engine-info { flex: 1; display: flex; flex-direction: column; }
/* --- 专为实验室分栏设计的精美标题 --- */
.column-title {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
}

.column-title .en-prefix {
  font-size: 0.7rem;
  color: var(--accent-gold); /* 哑光金 */
  letter-spacing: 2px;
  font-weight: 800;
  padding-right: 10px;
  margin-right: 10px;
  border-right: 1px solid rgba(92, 110, 88, 0.2); /* 淡淡的鼠尾草绿竖线分割 */
}

.column-title .cn-text {
  font-size: 1.05rem;
  color: var(--text-main);
  font-weight: 700;
  font-family: var(--font-serif); /* 继承衬线体，增加人文感 */
  letter-spacing: 1px;
}
/* 静态引擎顶部行布局 */
.info-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px; /* 统一间距 */
}
/* 统一副标题 Tag 样式 */
.tag {
  font-size: 0.65rem;
  font-weight: 800;
  color: var(--text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.icon-link-btn {
  background: none; border: none; color: var(--text-muted); font-size: 0.8rem; cursor: pointer;
  display: flex; align-items: center; gap: 5px; padding: 4px 8px; border-radius: 4px; transition: 0.3s;
}
.icon-link-btn:hover { background: rgba(0,0,0,0.05); color: var(--primary-color); }

/* 统一大标题样式 */
.engine-title {
  font-family: var(--font-serif);
  font-size: 1.6rem; /* 统一字号，之前可能是 1.4rem */
  margin-bottom: 15px;
  color: var(--text-main);
  font-weight: 600;
}

/* 统一描述正文样式 */
.desc-text {
  font-size: 0.9rem;
  color: var(--text-muted);
  line-height: 1.6;
  margin: 0;
}

.action-footer { margin-top: auto; display: flex; gap: 15px; }
.btn-primary-sm, .btn-gold-sm {
  padding: 10px 24px; border-radius: 8px; border: none; font-size: 0.9rem; font-weight: 700;
  cursor: pointer; transition: 0.3s; display: flex; align-items: center; gap: 8px;
}
.btn-primary-sm { background: var(--primary-color); color: white; }
.btn-primary-sm:hover { background: var(--primary-dark); transform: translateY(-2px); box-shadow: 0 5px 15px rgba(92, 110, 88, 0.3); }
.btn-gold-sm { background: var(--accent-gold); color: white; }
.btn-gold-sm:hover { background: #b8972f; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(212, 175, 55, 0.3); }
.empty-state { color: var(--text-muted); font-style: italic; font-size: 0.9rem; padding: 20px 0; }

/* --- 统一视觉图标区 --- */
.engine-visual {
  width: 70px; /* 从之前的 60px 调大，向静态的 80px 靠拢 */
  height: 70px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(92, 110, 88, 0.08) 0%, rgba(92, 110, 88, 0.03) 100%);
  border: 1.5px solid rgba(92, 110, 88, 0.12);
  box-shadow: 
    inset 0 1px 1px rgba(255, 255, 255, 0.8),
    0 4px 12px rgba(92, 110, 88, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: var(--primary-color);
  position: relative;
  flex-shrink: 0; /* 防止被右侧长文本挤压 */
  transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.engine-visual i {
  background: linear-gradient(135deg, var(--primary-color) 20%, var(--primary-light) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 3px rgba(92, 110, 88, 0.2));
  transition: all 0.3s ease;
}

.engine-card:hover .engine-visual {
  transform: scale(1.05);
  border-color: rgba(92, 110, 88, 0.25);
  background: linear-gradient(135deg, rgba(92, 110, 88, 0.14) 0%, rgba(92, 110, 88, 0.06) 100%);
  box-shadow: 
    inset 0 1px 1px rgba(255, 255, 255, 0.9),
    0 8px 20px rgba(92, 110, 88, 0.15);
}

.static-engine .engine-visual { 
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 6px 20px rgba(92, 110, 88, 0.35); 
}

.static-engine .engine-visual i {
  background: none;
  -webkit-text-fill-color: white;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
}

/* 扫描动画 */
.radar-scan {
  position: absolute; width: 100%; height: 100%; border-radius: 50%; border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #FFF; animation: spin 2s infinite linear;
}
@keyframes spin { to { transform: rotate(360deg); } }

.quick-link {
  margin-top: 10px; padding: 15px; border: 1px dashed #CCC; text-align: center; color: var(--text-muted);
  font-size: 0.85rem; cursor: pointer; transition: 0.3s; display: flex; align-items: center; justify-content: center; gap: 8px; border-radius: 8px;
}
.quick-link:hover { border-color: var(--primary-color); color: var(--primary-color); background: rgba(255, 255, 255, 0.5); }

/* --- 4. 响应式适配 --- */
@media (max-width: 1024px) {
  .lab-workspace { grid-template-columns: 1fr; gap: 30px; }
  .engines-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .lab-dashboard-page { padding: 100px 0 60px; }
  
  /* Header 适配 */
  .lab-intro { 
    flex-direction: column; 
    align-items: flex-start; 
    gap: 15px; 
    margin-bottom: 35px;
  }
  .lab-title { font-size: 1.8rem; }
  .page-subtitle { font-size: 0.9rem; }
  .engine-status { padding: 6px 14px; }

  /* 资产卡片适配 */
  .asset-card { padding: 18px; }
  .card-icon { width: 38px; height: 38px; font-size: 1rem; }
  .card-content h4 { font-size: 1rem; }
  .score-val { font-size: 1.1rem; }

  /* 引擎卡片适配 */
  .engine-card { 
    flex-direction: column; 
    padding: 25px 20px !important; 
    gap: 15px; 
  }
  
  .static-engine { 
    background: #fff; /* 手机端改回纯白背景减少视觉负担 */
    border-color: rgba(0,0,0,0.05);
  }

  .engine-visual { width: 56px; height: 56px; font-size: 1.2rem; }
  .engine-title { font-size: 1.3rem; margin-bottom: 10px; }
  .desc-text { font-size: 0.85rem; }

  .action-footer { 
    margin-top: 10px; 
    flex-direction: column; 
  }
  
  .btn-primary-sm, .btn-gold-sm { 
    width: 100%; 
    justify-content: center; 
    padding: 12px;
  }

  /* 标题组件适配 */
  .column-title .cn-text { font-size: 0.95rem; }
  .column-title .en-prefix { font-size: 0.6rem; margin-right: 8px; padding-right: 8px; }
}

@media (max-width: 480px) {
  .lab-title { font-size: 1.6rem; }
  .engine-status-wrapper { width: 100%; }
  .engine-status { width: 100%; justify-content: center; }
}
</style>
