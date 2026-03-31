<template>
  <div class="submitted-page">
    <div class="container zen-flex-center">

      <!-- 使用 Transition 切换状态，增加平滑感 -->
      <transition name="zen-fade" mode="out-in">

        <!-- 状态 1：Aries 引擎分析中 -->
        <div v-if="currentStatus === 1" :key="1" class="content-card glass-card">
          <div class="visual-area">
            <div class="zen-pulse-container">
              <div class="ring"></div>
              <div class="ring ring-2"></div>
              <i class="fas fa-microchip icon-core"></i>
            </div>
          </div>

          <div class="text-area">
            <h2 class="state-title">数据正在建模</h2>
            <p class="state-desc">
              Aries 引擎正在解析 <strong>{{ bikeName }}</strong> 的生物力学数据。<br>
              您可以稍后在“存档中心”查收完整的调教建议。
            </p>
          </div>

          <div class="action-area">
            <button class="btn btn-outline" @click="router.push('/lab/garage')">回到我的车库</button>
            <button class="btn btn-primary" @click="router.push('/lab/archive/dynamic')">前往存档中心</button>
          </div>

          <div class="sync-status">
            <i class="fas fa-sync-alt fa-spin"></i> 后端计算同步中...
          </div>
        </div>

        <!-- 状态 2：分析完成（就绪） -->
        <div v-else :key="2" class="content-card glass-card success-card">
          <div class="visual-area">
            <div class="success-pop">
              <i class="fas fa-check"></i>
            </div>
            <div class="gold-sparkle"><i class="fas fa-bolt"></i></div>
          </div>

          <div class="text-area">
            <h2 class="state-title">调教方案已就绪</h2>
            <p class="state-desc">
              计算已完成！基于您的 <strong>{{ bikeName }}</strong> 骑行姿态，<br>
              Aries 已经准备好了您的精密优化报告。
            </p>
          </div>

          <div class="action-area">
            <button class="btn btn-primary btn-xl btn-glow" @click="goToReport">
              立即查看报告 <i class="fas fa-chevron-right ml-2"></i>
            </button>
          </div>
        </div>

      </transition>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getFittingSessionDetail } from '@/api/dynamicFitting'; // 请确保路径正确

const route = useRoute();
const router = useRouter();

const sessionId = route.query.id;
const bikeName = route.query.bikeName || '您的单车';
const currentStatus = ref(Number(route.query.status) || 1);

onMounted(async () => {
  if (currentStatus.value === 2) return;

  // 1. 落地瞬间查一次（捕捉极速完成的情况）
  await syncStatus();

  // 2. 如果还没好，4秒后最后尝试“收割”一次结果
  if (currentStatus.value === 1) {
    setTimeout(() => {
      syncStatus();
    }, 4000);
  }
});

/**
 * 与后端同步最新状态
 */
const syncStatus = async () => {
  try {
    const res = await getFittingSessionDetail(sessionId);
    // 兼容可能的数据结构
    const statusFromServer = res.data?.status ?? res.status;
    if (statusFromServer === 2) {
      currentStatus.value = 2;
    }
  } catch (error) {
    console.warn('Aries Status Sync Interrupted');
  }
};

const goToReport = () => {
  router.push({
    path: '/lab/report/dynamic',
    query: { sessionId: sessionId }
  });
};
</script>

<style scoped>
.submitted-page {
  padding-top: var(--header-height);
  min-height: 100vh;
  display: flex;
  align-items: center;
}

.zen-flex-center {
  display: flex;
  justify-content: center;
  width: 100%;
}

.content-card {
  width: 100%;
  max-width: 540px;
  padding: 60px 40px;
  text-align: center;
  border-radius: 28px;
  position: relative;
}

.success-card {
  border: 1px solid rgba(92, 110, 88, 0.2);
}

/* 视觉动画区 */
.visual-area {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto 35px;
}

/* 计算中动画 */
.zen-pulse-container {
  position: relative;
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
}

.ring {
  position: absolute;
  inset: 0;
  border: 2px solid var(--primary-color);
  border-radius: 50%;
  animation: zen-pulse 2s infinite;
}
.ring-2 { animation-delay: 1s; }

.icon-core {
  font-size: 2.2rem;
  color: var(--primary-color);
}

/* 完成动画 */
.success-pop {
  width: 100%; height: 100%;
  background: var(--primary-color);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 2.5rem;
  box-shadow: 0 10px 25px rgba(92, 110, 88, 0.2);
  animation: zen-pop 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.gold-sparkle {
  position: absolute; top: -5px; right: -5px;
  color: var(--accent-gold); font-size: 1.5rem;
  filter: drop-shadow(0 0 5px rgba(212, 175, 55, 0.5));
}

/* 文字排版 */
.state-title {
  font-family: var(--font-serif);
  font-size: 1.8rem;
  color: var(--text-main);
  margin-bottom: 12px;
}

.state-desc {
  color: var(--text-muted);
  font-size: 0.95rem;
  line-height: 1.7;
  margin-bottom: 35px;
}

.action-area {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.btn-xl {
  padding: 16px 40px;
  font-size: 1rem;
  border-radius: var(--radius-pill);
  font-weight: 700;
}

.btn-glow {
  box-shadow: 0 8px 20px rgba(92, 110, 88, 0.2);
}

.sync-status {
  margin-top: 30px;
  font-size: 0.75rem;
  color: var(--text-muted);
  opacity: 0.5;
}

.ml-2 { margin-left: 8px; }

/* 关键帧 */
@keyframes zen-pulse {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.6); opacity: 0; }
}

@keyframes zen-pop {
  0% { transform: scale(0); }
  100% { transform: scale(1); }
}

/* 过渡 */
.zen-fade-enter-active, .zen-fade-leave-active { transition: all 0.5s ease; }
.zen-fade-enter-from { opacity: 0; transform: translateY(10px); }
.zen-fade-leave-to { opacity: 0; transform: translateY(-10px); }
</style>