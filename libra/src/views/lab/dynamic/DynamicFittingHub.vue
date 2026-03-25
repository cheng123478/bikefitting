<template>
  <div class="hub-page">
    <div class="container main-content-area">

      <!-- Header -->
      <header class="page-header fade-up">
        <div class="header-left">
          <h1 class="title">动态视觉实验室<span class="beta-tag">BETA</span></h1>
          <p class="subtitle">Aries 引擎已就绪，请选择采集视角以启动或补充分析。</p>
        </div>
        <div class="header-actions">
          <button v-if="capturedCount > 0" class="btn-text text-danger" style="margin-right: 15px;" @click="discardSession">
            <i class="fas fa-trash-alt"></i> 清空暂存
          </button>
          <button class="btn-text back-btn" @click="handleBack">
            <i class="fas fa-chevron-left"></i> 返回实验室
          </button>
        </div>
      </header>

      <!-- 1. 顶部选择区：单车 & 痛点 -->
      <div class="top-selection-row fade-up delay-1">
        <!-- 左侧：车辆选择 -->
        <div class="view-card glass-card bike-selection-card">
          <div class="card-header">
            <div class="card-icon"><i class="fas fa-bicycle"></i></div>
            <h3>选择分析车辆</h3>
          </div>
          <div class="card-body">
            <div class="bike-selection-list">
              <div
                v-for="bike in allBikes"
                :key="bike.id"
                class="bike-selection-item"
                :class="{ active: selectedBikeId == bike.id }"
                @click="selectBike(bike)"
              >
                <div class="bike-status-dot" :class="{ 'has-report': bike.hasDynamicReport }"></div>
                <div class="bike-main-info">
                  <span class="bike-name">{{ bike.brand }} {{ bike.model }}</span>
                  <span class="bike-remark">{{ bike.remark || '无备注' }}</span>
                </div>
                <i class="fas fa-check-circle active-check" :class="{ 'is-visible': selectedBikeId == bike.id }"></i>
              </div>

              <div v-if="allBikes.length === 0" class="empty-bikes">
                <p>车库中暂无现役车辆</p>
                <button class="btn-text" @click="router.push('/lab/garage/edit')">去添加单车</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：痛感诊断 -->
        <div class="view-card glass-card pain-assessment-card">
          <div class="card-header">
            <div class="card-icon"><i class="fas fa-heartbeat"></i></div>
            <h3>近期骑行痛点 <span class="subtitle-tip">(可选填)</span></h3>
          </div>
          <div class="card-body">
            <p class="desc-text-sm">勾选不适部位，Aries 将引入医疗级修正参数以优化动态建议。</p>

            <div class="pain-options-grid">
              <label class="pain-option-label" :class="{ active: session.painLevels.includes('knee_anterior') }">
                <input type="checkbox" v-model="session.painLevels" value="knee_anterior" hidden>
                <i class="fas fa-check-circle check-icon"></i>
                <span class="opt-text">膝盖正前侧/髌骨刺痛</span>
              </label>

              <label class="pain-option-label" :class="{ active: session.painLevels.includes('knee_posterior') }">
                <input type="checkbox" v-model="session.painLevels" value="knee_posterior" hidden>
                <i class="fas fa-check-circle check-icon"></i>
                <span class="opt-text">膝盖后侧/腘窝拉扯痛</span>
              </label>

              <label class="pain-option-label" :class="{ active: session.painLevels.includes('hand_numbness') }">
                <input type="checkbox" v-model="session.painLevels" value="hand_numbness" hidden>
                <i class="fas fa-check-circle check-icon"></i>
                <span class="opt-text">手掌发麻/手腕酸痛</span>
              </label>

              <label class="pain-option-label" :class="{ active: session.painLevels.includes('lower_back') }">
                <input type="checkbox" v-model="session.painLevels" value="lower_back" hidden>
                <i class="fas fa-check-circle check-icon"></i>
                <span class="opt-text">腰背部酸痛</span>
              </label>

              <label class="pain-option-label" :class="{ active: session.painLevels.includes('perineum_pain') }">
                <input type="checkbox" v-model="session.painLevels" value="perineum_pain" hidden>
                <i class="fas fa-check-circle check-icon"></i>
                <span class="opt-text">会阴/坐骨结节酸麻</span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 中层：视角选择 -->
      <div class="middle-selection-section fade-up delay-2">
        <div class="section-header">
          <div class="card-icon"><i class="fas fa-video"></i></div>
          <h3>选择采集视角</h3>
        </div>
        
        <div class="views-container">
          <!-- 侧面 (Side) -->
          <div class="view-card glass-card view-item" :class="{ active: selectedView === 'side', 'is-done': session.side }" @click="selectedView = 'side'">
            <div class="card-icon"><i class="fas fa-bicycle"></i></div>
            <div class="card-text">
              <h3>侧面视角 <i v-if="session.side" class="fas fa-check-circle text-success ml-1"></i></h3>
              <div v-if="session.side" class="data-preview">
                <div class="mini-dashboard">
                  <div class="m-item">
                      <span class="l">最大膝角(BDC)</span>
                    <span class="v">{{ session.side.summary?.maxKnee || '--' }}°</span>
                  </div>
                  <div class="m-item">
                      <span class="l">最小髋角(TDC)</span>
                    <span class="v">{{ session.side.summary?.minHip || '--' }}°</span>
                  </div>
                  <div class="m-item">
                      <span class="l">平均躯干角</span>
                    <span class="v">{{ session.side.summary?.avgTorso || '--' }}°</span>
                  </div>
                  <div class="m-item">
                    <span class="l">采集帧数</span>
                    <span class="v">{{ session.side.summary?.frameCount || '--' }}</span>
                  </div>
                </div>
              </div>
              <p v-else>核心几何与座高诊断</p>
            </div>
            <div class="check-circle" v-if="selectedView === 'side'"><i class="fas fa-check"></i></div>
          </div>

          <!-- 正面 (Front) -->
          <div class="view-card glass-card view-item" :class="{ active: selectedView === 'front', 'is-done': session.front }" @click="selectedView = 'front'">
            <div class="card-icon"><i class="fas fa-arrows-alt-h"></i></div>
            <div class="card-text">
              <h3>正面视角 <i v-if="session.front" class="fas fa-check-circle text-success ml-1"></i></h3>
              <div v-if="session.front" class="data-preview">
                <div class="mini-dashboard">
                  <div class="m-item">
                      <span class="l">平均膝角(L/R)</span>
                      <span class="v">{{ session.front.summary?.avgLeftKnee || '--' }}°/{{ session.front.summary?.avgRightKnee || '--' }}°</span>
                  </div>
                  <div class="m-item">
                    <span class="l">最大膝轨迹</span>
                    <span class="v">{{ session.front.summary?.maxWobble || '--' }}px</span>
                  </div>
                  <div class="m-item">
                    <span class="l">最大重心晃动</span>
                    <span class="v">{{ session.front.summary?.maxTilt || '--' }}px</span>
                  </div>
                </div>
              </div>
              <p v-else>膝盖轨迹与风阻分析</p>
            </div>
            <div class="check-circle" v-if="selectedView === 'front'"><i class="fas fa-check"></i></div>
          </div>

          <!-- 背面 (Back) -->
          <div class="view-card glass-card view-item" :class="{ active: selectedView === 'back', 'is-done': session.back }" @click="selectedView = 'back'">
            <div class="card-icon"><i class="fas fa-align-center"></i></div>
            <div class="card-text">
              <h3>背面视角 <i v-if="session.back" class="fas fa-check-circle text-success ml-1"></i></h3>
              <div v-if="session.back" class="data-preview">
                <div class="mini-dashboard">
                  <div class="m-item">
                      <span class="l">最大骨盆晃动</span>
                    <span class="v">{{ session.back.summary?.maxPelvisSway || '--' }}px</span>
                  </div>
                  <div class="m-item">
                      <span class="l">最大脊柱侧偏</span>
                    <span class="v">{{ session.back.summary?.maxSpineTilt || '--' }}°</span>
                  </div>
                </div>
              </div>
              <p v-else>骨盆与脊柱稳定性分析</p>
            </div>
            <div class="check-circle" v-if="selectedView === 'back'"><i class="fas fa-check"></i></div>
          </div>
        </div>
      </div>

      <!-- 3. 底部：操作按钮 -->
      <div class="bottom-action-section fade-up delay-3">
        <div class="action-panel glass-card-premium">
          <div class="task-info">
            <span class="label">当前任务</span>
            <div class="selected-text">
              准备{{ session[selectedView] ? '重录' : '启动' }}: <strong class="highlight">{{ getViewName(selectedView) }}</strong>
            </div>
            <p class="desc-text" v-if="session.painLevels.length > 0">
              已搭载 <strong class="highlight">{{ session.painLevels.length }}</strong> 项医疗修正参数。
            </p>
            <p class="desc-text" v-else>{{ getViewDesc(selectedView) }}</p>
          </div>

          <div class="btn-group">
            <button class="btn-action primary" @click="startSession('camera')">
              <div class="btn-icon"><i class="fas fa-camera"></i></div>
              <div class="btn-content">
                <span class="main">{{ session[selectedView] ? '重新录制' : '实时采集' }}</span>
                <span class="sub">打开摄像头</span>
              </div>
            </button>

            <button class="btn-action outline" @click="startSession('upload')">
              <div class="btn-icon"><i class="fas fa-upload"></i></div>
              <div class="btn-content">
                <span class="main">{{ session[selectedView] ? '重新上传' : '上传分析' }}</span>
                <span class="sub">导入录像</span>
              </div>
            </button>

            <transition name="fade-slide">
              <button
                  v-if="capturedCount > 0"
                  class="btn-action submit-action"
                  @click="submitAnalysis"
              >
                <div class="btn-icon"><i class="fas fa-bolt"></i></div>
                <div class="btn-content">
                  <span class="main">提交分析</span>
                  <span class="sub">{{ session.side ? '生成深度报告' : '生成基础分析' }}</span>
                </div>
              </button>
            </transition>
          </div>
        </div>

        <p class="env-tip content-end-tip">
          <i class="fas fa-info-circle"></i> 建议至少包含侧面视角以获得完整的几何调整建议。
        </p>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useLabStore } from '@/stores/lab.js';
import { useDynamicSessionStore } from "@/stores/dynamicSession.js";
import { showToast, showConfirm } from '@/utils/toast';

const route = useRoute();
const router = useRouter();
const labStore = useLabStore();
const sessionStore = useDynamicSessionStore();
const selectedView = ref('side');

// 状态
const selectedBikeId = ref(null);
const selectedBike = ref(null);
const allBikes = ref([]);

const session = computed(() => sessionStore.sessionData);
const capturedCount = computed(() => {
  return ['side', 'front', 'back'].filter(v => session.value[v]).length;
});

const fetchBikeInfo = async () => {
    try {
        const data = await labStore.fetchDashboard();
        const bikes = data.bikes || [];
        allBikes.value = bikes;

        if (sessionStore.sessionData.bikeId) {
            selectedBikeId.value = sessionStore.sessionData.bikeId;
            selectedBike.value = bikes.find(b => b.id === Number(selectedBikeId.value)) || null;
        } else if (route.query.bikeId) {
            selectedBikeId.value = Number(route.query.bikeId);
            selectedBike.value = bikes.find(b => b.id === selectedBikeId.value) || null;
            if (selectedBikeId.value) sessionStore.initSession(selectedBikeId.value);
        } else if (bikes.length > 0) {
            selectedBikeId.value = bikes[0].id;
            selectedBike.value = bikes[0];
            sessionStore.initSession(selectedBikeId.value);
        }
    } catch(err) {
        console.error("加载车辆信息失败", err);
    }
}

const selectBike = async (bike) => {
  if (selectedBikeId.value === bike.id) return;
  
  if (capturedCount.value > 0) {
    const confirm = await showConfirm('切换车辆将清空当前已录制视角，是否继续？');
    if (!confirm) return;
  }

  selectedBikeId.value = bike.id;
  selectedBike.value = bike;
  sessionStore.clearSession();
  sessionStore.initSession(bike.id);
};

onMounted(() => {
   fetchBikeInfo();
});

const startSession = (mode) => {
  if (!selectedBikeId.value) {
    showToast('请先选择一辆单车', 'warning');
    return;
  }
  router.push({
    path: '/lab/dynamic/capture',
    query: { view: selectedView.value, mode: mode, bikeId: selectedBikeId.value }
  });
};

const submitAnalysis = async () => {
  if (capturedCount.value === 0) {
    showToast('请至少采集一个视角的数据', 'warning');
    return;
  }

  if (!session.value.side) {
    const confirmBasic = await showConfirm(
        '未检测到侧面视角数据。Aries 将无法生成关于座垫高度和前伸量的精准建议。是否继续提交已有的视角分析？',
        '缺少侧面数据'
    );
    if (!confirmBasic) return;
  }

  try {
    showToast('正在上传原始数据...', 'info');
    const vo = await sessionStore.submit();
    if (vo && vo.id) {
      sessionStore.clearSession();
      router.push({
        path: '/lab/dynamic/session/submitted',
        query: { id: vo.id, status: vo.status, bikeName: vo.userBikeInfo?.brandModel }
      });
    }
  } catch (error) {
    showToast('网络波动，请检查后重试', 'error');
  }
};

const discardSession = async () => {
  if (await showConfirm('确定要清空刚才辛苦采集的数据吗？')) {
    sessionStore.clearSession();
    if (selectedBikeId.value) sessionStore.initSession(selectedBikeId.value);
    showToast('数据已清空', 'success');
  }
};

const handleBack = async () => {
  if (capturedCount.value > 0) {
    if (await showConfirm('退出将丢失已采集的数据，确认返回？')) {
      sessionStore.clearSession();
      router.push('/lab');
    }
  } else {
    router.push('/lab');
  }
};

const getViewName = (v) => {
  const map = { side: '侧面 (Side)', front: '正面 (Front)', back: '背面 (Back)' };
  return map[v];
};

const getViewDesc = (v) => {
  const map = {
    side: '分析座高、Reach设置及关节角度',
    front: '分析膝盖内扣轨迹及迎风面积',
    back: '分析骨盆左右晃动及脊柱中线'
  };
  return map[v];
};
</script>

<style scoped>
.hub-page {
  padding: 120px 0 80px;
  min-height: 100vh;
  background: transparent;
}

.main-content-area {
  max-width: 1200px;
}

/* Header */
.page-header {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 40px; padding-bottom: 25px; border-bottom: 1px solid rgba(0,0,0,0.05);
}
.title { font-family: var(--font-serif); font-size: 2.6rem; color: var(--text-main); margin: 0; }
.beta-tag { font-size: 0.65rem; background: var(--accent-gold); color: #fff; padding: 2px 8px; border-radius: 4px; vertical-align: middle; margin-left: 12px; font-weight: 800; letter-spacing: 1px; }
.subtitle { color: var(--text-muted); font-size: 1.1rem; margin-top: 8px; }
.header-actions { display: flex; align-items: center; }
.back-btn { color: var(--text-muted); font-size: 0.95rem; background: transparent; border: none; cursor: pointer; transition: all 0.4s var(--ease-out-expo); display: flex; align-items: center; gap: 8px; }
.back-btn:hover { color: var(--primary-color); transform: translateX(-5px); }

/* Layout: Top row (Bike + Pain) */
.top-selection-row {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 30px;
  margin-bottom: 40px;
}

.view-card {
  position: relative;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.6);
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.04);
  transition: all 0.5s var(--ease-out-expo);
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.view-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 60px rgba(0,0,0,0.08);
}

.card-header {
  padding: 24px 30px 15px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.card-header h3 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--text-main);
}

.card-body {
  padding: 0 30px 30px;
  flex: 1;
}

/* Bike selection */
.bike-selection-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bike-selection-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 14px 18px;
  background: rgba(255,255,255,0.5);
  border: 1px solid rgba(0,0,0,0.05);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.bike-selection-item.active {
  background: #fff;
  border-color: var(--primary-color);
  box-shadow: 0 5px 15px rgba(0,0,0,0.05);
}

.bike-main-info { display: flex; flex-direction: column; }
.bike-name { font-weight: 700; color: var(--text-main); font-size: 0.95rem; }
.bike-remark { font-size: 0.75rem; color: var(--text-muted); }

.active-check { position: absolute; right: 15px; color: var(--primary-color); opacity: 0; transform: scale(0.5); transition: 0.3s; }
.active-check.is-visible { opacity: 1; transform: scale(1); }

/* Middle: Views */
.middle-selection-section {
  margin-bottom: 50px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
  padding-left: 10px;
}

.section-header h3 { margin: 0; font-size: 1.4rem; color: var(--text-main); font-family: var(--font-serif); }

.views-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.view-item {
  cursor: pointer;
  padding: 25px;
  border: 2px solid transparent;
}

.view-item.active {
  border-color: var(--primary-color);
  background: #fff;
}

.card-icon {
  width: 50px; height: 50px;
  background: var(--bg-soft);
  color: var(--primary-color);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.4rem;
  margin-bottom: 5px;
}

.card-text h3 { margin: 10px 0 5px; font-size: 1.1rem; }
.card-text p { font-size: 0.85rem; color: var(--text-muted); }

.mini-dashboard {
  margin-top: 15px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.m-item .l { font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; }
.m-item .v { font-size: 1rem; font-weight: 700; color: var(--primary-color); }

.check-circle { position: absolute; top: 15px; right: 15px; color: var(--primary-color); }

/* Pain options */
.pain-options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.pain-option-label {
  padding: 10px 15px;
  background: rgba(255,255,255,0.4);
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,0.05);
  cursor: pointer;
  display: flex; align-items: center; gap: 10px;
  transition: 0.3s;
}

.pain-option-label.active { border-color: var(--primary-color); background: #fff; }
.pain-option-label .opt-text { font-size: 0.85rem; }

/* Bottom Action */
.bottom-action-section {
  background: var(--bg-soft);
  padding: 40px;
  border-radius: 30px;
}

.glass-card-premium {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
}

.task-info { flex: 1; }
.task-info .label { font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; }
.selected-text { font-size: 1.8rem; font-family: var(--font-serif); margin: 5px 0; }
.desc-text { color: var(--text-muted); font-size: 0.95rem; }

.btn-group { display: flex; gap: 15px; }
.btn-action {
  padding: 15px 30px;
  border-radius: 16px;
  border: none;
  cursor: pointer;
  display: flex; align-items: center; gap: 12px;
  transition: 0.3s;
}

.btn-action.primary { background: var(--primary-color); color: #fff; }
.btn-action.outline { background: #fff; border: 1px solid rgba(0,0,0,0.1); }
.btn-action.submit-action { background: var(--accent-gold); }

.btn-content .main { font-weight: 700; font-size: 1rem; }
.btn-content .sub { font-size: 0.7rem; opacity: 0.7; display: block; }

.content-end-tip { margin-top: 30px; text-align: center; font-size: 0.85rem; color: var(--text-muted); }

@media (max-width: 1024px) {
  .top-selection-row { grid-template-columns: 1fr; }
  .views-container { grid-template-columns: 1fr 1fr; }
  .glass-card-premium { flex-direction: column; text-align: center; }
  .btn-group { flex-direction: column; width: 100%; }
}
</style>