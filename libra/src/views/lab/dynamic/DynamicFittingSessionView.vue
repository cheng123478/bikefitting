<template>
  <div class="session-page">
    <div class="container">
      <header class="page-header">
        <h1>动态采集会话</h1>
        <p>请确认采集数据质量，您可以继续补充其他视角，或提交进行深度分析。</p>
      </header>

      <div class="session-grid">
        <!-- 1. 侧面数据卡片 -->
        <div class="capture-card" :class="{ done: session.side, active: !session.side }">
          <div class="card-status">
            <i class="fas fa-check-circle" v-if="session.side"></i>
            <span v-else>未采集</span>
          </div>
          <h3>侧面视角 (核心)</h3>

          <!-- 已采集：显示预览数据 -->
          <div v-if="session.side" class="preview-box">
            <div class="mini-metric">
              <span class="l">膝角极值</span>
              <span class="v">{{ session.side.summary.maxKnee }}°</span>
            </div>
            <div class="mini-chart">
              <!-- 这里未来放简单的 ECharts 缩略图 -->
              <div class="fake-wave"></div>
            </div>
            <div class="preview-actions">
              <button class="btn-text" @click="startCapture('side')">重录</button>
            </div>
          </div>

          <!-- 未采集：显示去录制 -->
          <div v-else class="empty-box">
            <button class="btn-add" @click="startCapture('side')">
              <i class="fas fa-plus"></i> 开始采集
            </button>
          </div>
        </div>

        <!-- 2. 正面数据卡片 -->
        <div class="capture-card" :class="{ done: session.front }">
          <div class="card-status">
            <i class="fas fa-check-circle" v-if="session.front"></i>
            <span v-else>可选</span>
          </div>
          <h3>正面视角</h3>
          <div v-if="session.front" class="preview-box">
            <div class="mini-metric"><span class="l">KTI指数</span><span class="v">0.8</span></div>
            <button class="btn-text" @click="startCapture('front')">重录</button>
          </div>
          <div v-else class="empty-box">
            <button class="btn-add outline" @click="startCapture('front')"><i class="fas fa-plus"></i> 添加</button>
          </div>
        </div>

        <!-- 3. 背面数据卡片 -->
        <div class="capture-card glass-card" :class="{ done: session.back }">
          <div class="card-status">
            <i class="fas fa-check-circle text-success" v-if="session.back"></i>
            <span v-else class="text-muted">可选</span>
          </div>

          <h3>背面视角</h3>
          <p class="card-desc">评估骨盆摇摆与核心稳定性</p>

          <!-- 已采集状态 -->
          <div v-if="session.back" class="preview-box">
            <div class="mini-metric">
              <span class="l">骨盆稳定性</span>
              <span class="v text-success">良好</span>
            </div>
            <div class="preview-actions">
              <button class="btn-text text-muted" @click="openModeSelector('back')">
                <i class="fas fa-redo"></i> 重新采集
              </button>
            </div>
          </div>

          <!-- 未采集状态 -->
          <div v-else class="empty-box">
            <button class="btn-add outline" @click="openModeSelector('back')">
              <i class="fas fa-plus"></i> 添加
            </button>
          </div>
        </div>
      </div>

      <!-- 底部提交栏 -->
      <footer class="session-footer glass-card fade-up">
        <div class="footer-info">
          <!-- 状态 1：侧面还没录 -->
          <div v-if="!session.side" class="status-warning">
            <i class="fas fa-exclamation-circle"></i>
            <span>请至少完成<strong>「侧面视角」</strong>的采集以启动引擎</span>
          </div>

          <!-- 状态 2：侧面录了（可以提交了，并且鼓励多录） -->
          <div v-else class="status-success">
            <i class="fas fa-check-circle text-success"></i>
            <div class="status-text">
              <span class="main-msg text-success">核心基准数据已就绪，可随时提交分析。</span>
              <span class="sub-msg text-muted">
          (已采 {{ capturedCount }} 个视角，补充正/背面可获更全面诊断)
        </span>
            </div>
          </div>
        </div>

        <div class="footer-btns">
          <button class="btn-outline" @click="discardSession">清空并放弃</button>

          <!-- 只要 side 有数据，按钮就变成高亮的金色/绿色，鼓励点击 -->
          <button
              class="btn-submit"
              :class="{ ready: session.side }"
              :disabled="!session.side"
              @click="submitAnalysis"
          >
            <span>{{ capturedCount > 1 ? '提交多维联合分析' : '提交侧面基准分析' }}</span>
            <i class="fas fa-paper-plane"></i>
          </button>
        </div>
      </footer>

    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useDynamicSessionStore } from '@/stores/dynamicSession';
import { showConfirm, showToast } from '@/utils/toast';
// import { uploadDynamicSession } from '@/api/fitting'; // 你的后端接口

const router = useRouter();
const store = useDynamicSessionStore();
const session = computed(() => store.sessionData);

const capturedCount = computed(() => {
  let c = 0;
  if (session.value.side) c++;
  if (session.value.front) c++;
  if (session.value.back) c++;
  return c;
});

const startCapture = (view) => {
  router.push({
    path: '/lab/dynamic/capture',
    query: { view, mode: 'camera', bikeId: session.value.bikeId }
  });
};

const discardSession = async () => {
  if (await showConfirm('确定要放弃当前采集的数据吗？')) {
    store.clearSession();
    router.push('/lab/dynamic');
  }
};

const submitAnalysis = async () => {
  try {
    showToast('正在上传数据包...', 'loading');

    // 组装大对象发送给后端
    // await uploadDynamicSession(session.value);

    // 模拟成功
    setTimeout(() => {
      store.clearSession(); // 清空缓存
      showToast('分析完成！', 'success');
      router.push('/lab/archives?tab=dynamic'); // 跳转到历史/结果页
    }, 1500);

  } catch (e) {
    showToast('提交失败', 'error');
  }
};
</script>

<style scoped>
.session-page { padding: 100px 0; min-height: 100vh; background: transparent; }
.page-header { text-align: center; margin-bottom: 50px; }
.session-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 30px; margin-bottom: 80px; }

.capture-card {
  background: rgba(255,255,255,0.5); border: 2px dashed #ddd; border-radius: 16px;
  padding: 30px; position: relative; transition: 0.3s; min-height: 250px;
  display: flex; flex-direction: column; justify-content: center;
}
.capture-card.done { border-style: solid; border-color: var(--success-color); background: #fff; }
.capture-card.active { border-color: var(--primary-color); background: #fff; box-shadow: 0 10px 30px rgba(0,0,0,0.05); }

.card-status { position: absolute; top: 20px; right: 20px; font-size: 0.8rem; color: #aaa; }
.card-status i { color: var(--success-color); font-size: 1.2rem; }

.empty-box { display: flex; justify-content: center; margin-top: 20px; }
.btn-add {
  background: var(--primary-color); color: #fff; border: none; padding: 10px 25px;
  border-radius: 30px; cursor: pointer; font-weight: 600; display: flex; align-items: center; gap: 8px;
}
.btn-add.outline { background: transparent; border: 1px solid var(--text-muted); color: var(--text-main); }

.preview-box { margin-top: 20px; }
.mini-metric { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 0.9rem; }
.mini-metric .v { font-weight: bold; color: var(--primary-color); }
.fake-wave { height: 40px; background: url('data:image/svg+xml;utf8,<svg ...>') repeat-x; opacity: 0.5; } /* 模拟波形 */

.session-footer {
  position: fixed; bottom: 40px; left: 50%; transform: translateX(-50%);
  width: 90%; max-width: 900px; padding: 20px 35px;
  display: flex; justify-content: space-between; align-items: center;
  border-radius: 20px; background: rgba(255,255,255,0.95);
  box-shadow: 0 15px 40px rgba(0,0,0,0.1); z-index: 100;
}

.status-warning { color: var(--warning-color); display: flex; align-items: center; gap: 8px; font-size: 0.95rem; }
.status-success { display: flex; align-items: center; gap: 12px; }
.status-success i { font-size: 1.8rem; }
.status-text { display: flex; flex-direction: column; }
.main-msg { font-weight: 700; font-size: 0.95rem; }
.sub-msg { font-size: 0.75rem; }

.footer-btns { display: flex; gap: 15px; }

.btn-submit {
  padding: 12px 30px; border-radius: 30px; border: none;
  background: #ccc; color: #fff; font-weight: 700;
  display: flex; align-items: center; gap: 10px; cursor: not-allowed;
  transition: all 0.3s;
}
/* 核心就绪时的按钮状态 */
.btn-submit.ready {
  background: var(--primary-color);
  cursor: pointer;
  box-shadow: 0 5px 15px rgba(92, 110, 88, 0.3);
}
.btn-submit.ready:hover {
  background: var(--primary-dark);
  transform: translateY(-2px);
}
</style>