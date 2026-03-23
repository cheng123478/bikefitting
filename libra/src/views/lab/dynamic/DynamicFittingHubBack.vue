<template>
  <div class="hub-page">
    <div class="container">

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

      <!-- 1. 视角选择区 -->
      <div class="view-grid fade-up delay-1">

        <!-- 侧面 (Side) -->
        <div class="view-card glass-card" :class="{ active: selectedView === 'side', 'is-done': session.side }" @click="selectedView = 'side'">
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
            <!-- 修改：不再显示“必选”，改为“核心视角” -->
            <p v-else>核心几何与座高诊断</p>
          </div>
          <div class="check-circle" v-if="selectedView === 'side'"><i class="fas fa-check"></i></div>
        </div>

        <!-- 正面 (Front) -->
        <div class="view-card glass-card" :class="{ active: selectedView === 'front', 'is-done': session.front }" @click="selectedView = 'front'">
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
        <div class="view-card glass-card" :class="{ active: selectedView === 'back', 'is-done': session.back }" @click="selectedView = 'back'">
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

      <!-- 2. 底部操作区 -->
      <div class="action-section fade-up delay-2">
        <div class="glass-card action-panel">

          <div class="task-info">
            <span class="label">CURRENT TASK</span>
            <div class="selected-text">
              准备{{ session[selectedView] ? '重录' : '启动' }}: <strong class="highlight">{{ getViewName(selectedView) }}</strong>
            </div>
            <p class="desc-text">{{ getViewDesc(selectedView) }}</p>
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
                <span class="main">{{ session[selectedView] ? '重新上传' : '上传视频' }}</span>
                <span class="sub">导入高清录像</span>
              </div>
            </button>

            <!-- 修改：提交分析按钮逻辑 -->
            <transition name="fade-slide">
              <button
                  v-if="capturedCount > 0"
                  class="btn-action submit-action"
                  @click="submitAnalysis"
              >
                <div class="btn-icon"><i class="fas fa-bolt"></i></div>
                <div class="btn-content">
                  <span class="main">提交分析</span>
                  <!-- 根据是否有侧面数据显示不同的副标题 -->
                  <span class="sub" v-if="session.side">生成深度报告</span>
                  <span class="sub" v-else>生成基础分析</span>
                </div>
              </button>
            </transition>
          </div>

        </div>

        <p class="env-tip">
          <i class="fas fa-info-circle"></i> 建议至少包含侧面视角以获得完整的几何调整建议。
        </p>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useDynamicSessionStore } from "@/stores/dynamicSession.js";
import { showToast, showConfirm } from '@/utils/toast';

const route = useRoute();
const router = useRouter();
const store = useDynamicSessionStore();
const selectedView = ref('side');

const currentBikeId = computed(() => route.query.bikeId || store.sessionData.bikeId);
const session = computed(() => store.sessionData);

const capturedCount = computed(() => {
  return ['side', 'front', 'back'].filter(v => session.value[v]).length;
});

onMounted(() => {
  if (currentBikeId.value && store.isEmpty()) {
    store.initSession(currentBikeId.value);
  }
});

const startSession = (mode) => {
  if (!currentBikeId.value) {
    showToast('缺失单车信息，请返回控制台重新选择', 'error');
    return;
  }
  router.push({
    path: '/lab/dynamic/capture',
    query: { view: selectedView.value, mode: mode, bikeId: currentBikeId.value }
  });
};

const submitAnalysis = async () => {
  // 修改：只要捕获数量大于 0 即可提交
  if (capturedCount.value === 0) {
    showToast('请至少采集一个视角的数据', 'warning');
    return;
  }

  // 修改：如果缺少侧面，给一个二次确认，提醒用户可能缺少核心建议
  if (!session.value.side) {
    const confirmBasic = await showConfirm(
        '未检测到侧面视角数据。Aries 将无法生成关于座垫高度和前伸量的精准建议。是否继续提交已有的视角分析？',
        '缺少侧面数据'
    );
    if (!confirmBasic) return;
  }

  try {
    showToast('正在上传原始数据...', 'info');
    const vo = await store.submit();
    if (vo && vo.id) {
      store.clearSession();
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
    store.clearSession();
    showToast('数据已清空', 'success');
  }
};

const handleBack = async () => {
  if (capturedCount.value > 0) {
    if (await showConfirm('退出将丢失已采集的数据，确认返回？')) {
      store.clearSession();
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
  padding: 140px 0 80px;
  min-height: 100vh;
  background: transparent;
}

/* Header */
.page-header {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: 40px; padding-bottom: 20px; border-bottom: 1px solid rgba(0,0,0,0.05);
}
.title { font-family: var(--font-serif); font-size: 2.2rem; color: var(--text-main); margin: 0; }
.beta-tag { font-size: 0.6rem; background: var(--accent-gold); color: #fff; padding: 2px 6px; border-radius: 4px; vertical-align: middle; margin-left: 8px; }
.subtitle { color: var(--text-muted); font-size: 1rem; margin-top: 5px; }
.header-actions { display: flex; align-items: center; }
.back-btn { color: var(--text-muted); font-size: 0.9rem; background: transparent; border: none; cursor: pointer; transition: 0.3s; }
.back-btn:hover { color: var(--primary-color); transform: translateX(-5px); }
.text-danger { color: #ff4d4f; background: none; border: none; cursor: pointer; font-size: 0.9rem; transition: 0.3s; }
.text-danger:hover { text-decoration: underline; }

/* 1. 紧凑的三列布局 */
.view-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 25px;
  margin-bottom: 40px;
}

.view-card {
  position: relative;
  padding: 25px;
  border-radius: 16px;
  background: rgba(255,255,255,0.6);
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 15px;
  text-align: left;
}

.view-card:hover { transform: translateY(-3px); background: #fff; box-shadow: 0 8px 20px rgba(0,0,0,0.05); }

/* 选中与完成状态 */
.view-card.active { border-color: var(--primary-color); background: #fff; box-shadow: 0 8px 25px rgba(92, 110, 88, 0.15); }
.view-card.is-done { background: #fdfdfc; border-color: rgba(92, 110, 88, 0.2); }

.card-icon {
  width: 50px; height: 50px;
  background: rgba(92, 110, 88, 0.1);
  color: var(--primary-color);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem;
}

.card-text h3 { margin: 0 0 4px 0; font-size: 1.1rem; color: var(--text-main); display: flex; align-items: center; }
.card-text p { margin: 0; font-size: 0.8rem; color: var(--text-muted); }
.data-preview { font-size: 0.85rem; color: var(--text-muted); margin-top: 4px; }
.data-preview .val { font-weight: bold; font-size: 1rem; color: var(--primary-color); }
.ml-1 { margin-left: 8px; font-size: 0.9rem; }
.text-success { color: #52c41a; }

/* 选中对勾 */
.check-circle {
  position: absolute; top: -10px; right: -10px;
  width: 24px; height: 24px; background: var(--primary-color);
  color: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.8rem; border: 2px solid #fff;
  animation: pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* 2. 底部操作面板 */
.action-panel {
  padding: 30px 40px;
  border-radius: 20px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 40px;
}

.task-info { flex: 1; }
.task-info .label { font-size: 0.7rem; color: var(--text-muted); letter-spacing: 1px; }
.selected-text { font-size: 1.4rem; color: var(--text-main); margin: 5px 0; font-family: var(--font-serif); }
.highlight { color: var(--primary-color); }
.desc-text { font-size: 0.9rem; color: var(--text-muted); }

.btn-group { display: flex; gap: 15px; }

.btn-action {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; border-radius: 12px;
  border: 1px solid transparent; cursor: pointer; transition: 0.3s;
  min-width: 160px;
}

.btn-icon { font-size: 1.2rem; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.btn-content { display: flex; flex-direction: column; text-align: left; }
.btn-content .main { font-weight: 700; font-size: 0.95rem; line-height: 1.2; }
.btn-content .sub { font-size: 0.7rem; opacity: 0.8; font-weight: 400; margin-top: 2px; }

/* 按钮颜色体系 */
.btn-action.primary { background: var(--primary-color); color: #fff; box-shadow: 0 4px 15px rgba(92, 110, 88, 0.2); }
.btn-action.primary .btn-icon { background: rgba(255,255,255,0.2); }
.btn-action.primary:hover { background: var(--primary-dark); transform: translateY(-2px); }

.btn-action.outline { background: #fff; border-color: #eee; color: var(--text-main); }
.btn-action.outline .btn-icon { background: #f5f5f5; color: var(--text-muted); }
.btn-action.outline:hover { border-color: var(--primary-color); color: var(--primary-color); transform: translateY(-2px); }

/* 新增的提交按钮 */
.submit-action { background: var(--accent-gold); color: #000; box-shadow: 0 4px 15px rgba(212, 175, 55, 0.3); }
.submit-action .btn-icon { background: rgba(0,0,0,0.1); }
.submit-action:hover { background: #fff; transform: translateY(-2px); border-color: var(--accent-gold); }
.submit-action.disabled { background: #eee; color: #aaa; cursor: not-allowed; box-shadow: none; border-color: transparent; }
.submit-action.disabled:hover { transform: none; }

.env-tip { text-align: center; margin-top: 20px; color: var(--text-muted); font-size: 0.85rem; opacity: 0.8; }
.mini-dashboard {
  display: grid;
  grid-template-columns: 1fr 1fr; /* 2x2 网格 */
  gap: 12px;
  background: rgba(92, 110, 88, 0.04);
  padding: 15px;
  border-radius: 12px;
  margin-bottom: 15px;
}

.m-item {
  display: flex;
  flex-direction: column;
}

.m-item .l {
  font-size: 0.7rem;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.m-item .v {
  font-family: var(--font-serif);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--primary-color);
  line-height: 1;
}
/* 动画 */
@keyframes pop { from { transform: scale(0); } to { transform: scale(1); } }
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.4s ease; width: 160px; overflow: hidden; opacity: 1; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; width: 0; transform: translateX(20px); margin-left: -15px; padding: 0; }

@media (max-width: 1024px) {
  .view-grid { grid-template-columns: 1fr; }
  .action-panel { flex-direction: column; text-align: center; }
  .btn-group { width: 100%; flex-direction: column; }
  .btn-action { justify-content: center; }
}
</style>