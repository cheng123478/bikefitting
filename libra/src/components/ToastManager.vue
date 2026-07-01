<template>
  <div class="toast-manager-wrapper">
    <div class="toast-container">
      <TransitionGroup name="toast-anim">
        <div
            v-for="item in toastState.messages"
            :key="item.id"
            class="toast-card"
            :class="item.type"
        >
          <!-- 图标 -->
          <i v-if="item.type === 'success'" class="fas fa-check-circle icon"></i>
          <i v-else-if="item.type === 'error'" class="fas fa-times-circle icon"></i>
          <i v-else-if="item.type === 'warning'" class="fas fa-exclamation-triangle icon"></i>
          <i v-else class="fas fa-info-circle icon"></i>

          <span class="text">{{ item.message }}</span>
        </div>
      </TransitionGroup>
    </div>
    <Transition name="modal-fade">
      <div v-if="toastState.dialog.visible" class="confirm-overlay" @click.self="handleCancel">
        <div class="confirm-box glass-card">
          <h3 class="confirm-title">{{ toastState.dialog.title }}</h3>
          <p class="confirm-content">{{ toastState.dialog.content }}</p>

          <div class="confirm-actions">
            <button class="btn btn-sm btn-primary" @click="handleConfirm">
              {{ toastState.dialog.confirmText }}
            </button>
            <button v-if="toastState.dialog.cancelText" class="btn btn-sm btn-outline" @click="handleCancel">
              {{ toastState.dialog.cancelText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { toastState, closeDialog } from '@/utils/toast';
// 处理确认
const handleConfirm = () => {
  closeDialog(true);
};

// 处理取消
const handleCancel = () => {
  closeDialog(false);
};
</script>

<style scoped>
.toast-container {
  position: fixed; top: 30px; left: 50%; transform: translateX(-50%);
  z-index: var(--z-toast); /* 使用新变量 3000 */
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  pointer-events: none;
}

.toast-card {
  pointer-events: auto;
  background: var(--bg-surface); /* 磨砂背景 */
  backdrop-filter: blur(12px);
  padding: 12px 28px;
  border-radius: var(--radius-pill);
  box-shadow: var(--shadow-md);
  border: var(--border-glass);
  display: flex; align-items: center; gap: 10px;
  min-width: 200px;
}

.text {
  font-family: var(--font-sans);
  font-size: 0.95rem; color: var(--text-main);
  font-weight: 500; letter-spacing: 0.5px;
}

.icon { font-size: 1.1rem; }

/* 颜色适配新变量 */
.toast-card.success .icon { color: #6E8565; /* Success Green */ }
.toast-card.success { border-left: 4px solid #6E8565; }

.toast-card.error .icon { color: #C18C7E; /* Danger Red */ }
.toast-card.error { border-left: 4px solid #C18C7E; }

.toast-card.warning .icon { color: #E89F6E; /* Warning Orange */ }
.toast-card.warning { border-left: 4px solid #E89F6E; }

.toast-card.info .icon { color: var(--primary-color); }
.toast-card.info { border-left: 4px solid var(--primary-color); }

/* 动画 */
.toast-anim-enter-active, .toast-anim-leave-active {
  transition: all 0.4s var(--ease-out-expo);
}
.toast-anim-enter-from { opacity: 0; transform: translateY(-30px) scale(0.9); }
.toast-anim-leave-to { opacity: 0; transform: translateY(-30px); }
.confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(43, 42, 41, 0.4); /* 遮罩层 */
  backdrop-filter: blur(4px);
  z-index: 3001; /* 比 Toast 更高 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.confirm-box {
  background: var(--bg-surface);
  width: 320px;
  padding: 24px;
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(255, 255, 255, 0.6);
  text-align: center;
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.confirm-title {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  color: var(--text-main);
  margin-bottom: 12px;
  font-weight: 600;
}

.confirm-content {
  font-size: 0.95rem;
  color: var(--text-muted);
  margin-bottom: 24px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}

/* 按钮复用全局样式，这里稍微自定义一下宽度 */
.confirm-actions button {
  min-width: 80px;
}

/* 动画 */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.3s ease;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.9) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>