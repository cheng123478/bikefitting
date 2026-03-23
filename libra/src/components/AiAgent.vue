<template>
  <div class="ai-agent-wrapper">

    <!-- 1. AI 聊天窗口 (展开状态) -->
    <Transition name="dialog-pop">
      <div v-if="isOpen" class="chat-dialog glass-card">

        <!-- 窗口头部 -->
        <header class="dialog-header">
          <div class="header-info">
            <div class="status-dot"></div>
            <span class="title">Fittor 智脑</span>
            <span class="subtitle">Online</span>
          </div>
          <button class="btn-close" @click="toggleChat">
            <i class="fas fa-chevron-down"></i>
          </button>
        </header>

        <!-- 消息列表区 -->
        <div class="messages-container" ref="msgContainerRef">
          <div v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
            <!-- AI 头像 -->
            <div v-if="msg.role === 'ai'" class="avatar ai-avatar">
              <i class="fas fa-brain"></i>
            </div>

            <!-- 消息气泡 -->
            <div class="bubble">
              <p v-html="DOMPurify.sanitize(msg.content)"></p>
              <!-- 如果有操作指令 (Action) -->
              <div v-if="msg.action" class="action-card">
                <div class="action-icon"><i class="fas fa-sliders-h"></i></div>
                <div class="action-info">
                  <span class="action-title">{{ msg.action.title }}</span>
                  <span class="action-desc">{{ msg.action.desc }}</span>
                </div>
                <button class="btn-apply" @click="handleAction(msg.action)">执行</button>
              </div>
            </div>

            <!-- 用户头像 -->
            <div v-if="msg.role === 'user'" class="avatar user-avatar">
              <img :src="userAvatar" />
            </div>
          </div>

          <!-- 输入中状态 -->
          <div v-if="isThinking" class="message-row ai">
            <div class="avatar ai-avatar"><i class="fas fa-brain"></i></div>
            <div class="bubble thinking-bubble">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <!-- 底部输入区 -->
        <footer class="dialog-footer">
          <div class="input-wrapper">
            <input
                v-model="inputContent"
                type="text"
                placeholder="问点什么... (例如: 膝盖前侧疼怎么办?)"
                @keyup.enter="sendMessage"
            />
            <button class="btn-send" :disabled="!inputContent.trim() || isThinking" @click="sendMessage">
              <i class="fas fa-paper-plane"></i>
            </button>
          </div>
        </footer>

      </div>
    </Transition>

    <!-- 2. 悬浮触发球 (收起状态) -->
    <div class="fab-trigger" :class="{ 'is-active': isOpen }" @click="toggleChat">
      <!-- 呼吸光环 -->
      <div class="pulse-ring"></div>
      <div class="fab-icon">
        <i class="fas" :class="isOpen ? 'fa-times' : 'fa-robot'"></i>
      </div>
      <!-- 未读气泡 -->
      <div v-if="unreadCount > 0 && !isOpen" class="unread-badge">
        {{ unreadCount }}
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, computed } from 'vue';
import { useUserStore } from '@/stores/user';
import { useRoute } from 'vue-router'; // 用于感知上下文
import DOMPurify from 'dompurify';

const userStore = useUserStore();
const route = useRoute();

// --- 状态 ---
const isOpen = ref(false);
const isThinking = ref(false);
const inputContent = ref('');
const msgContainerRef = ref(null);
const unreadCount = ref(1); // 模拟初始有一条欢迎语

// 默认头像
const userAvatar = computed(() => userStore.userInfo?.avatar || '/image/default-avatar.png');

// 消息历史 (模拟数据)
const messages = reactive([
  {
    role: 'ai',
    content: '你好，我是 RideTrace 智脑。我已经加载了您的<b>身体图谱</b>和<b>在役战车数据</b>。<br>有什么可以帮您？'
  }
]);

// --- 交互逻辑 ---

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    unreadCount.value = 0;
    scrollToBottom();
  }
};

const scrollToBottom = async () => {
  await nextTick();
  if (msgContainerRef.value) {
    msgContainerRef.value.scrollTop = msgContainerRef.value.scrollHeight;
  }
};

const sendMessage = async () => {
  if (!inputContent.value.trim()) return;

  // 1. 推入用户消息
  messages.push({ role: 'user', content: inputContent.value });
  const query = inputContent.value;
  inputContent.value = '';
  scrollToBottom();

  // 2. 模拟 AI 思考 (实际对接后端 API)
  isThinking.value = true;

  // TODO: 这里将来调用 /api/v1/ai/consult
  // const context = { route: route.path, userId: ... }

  setTimeout(() => {
    isThinking.value = false;

    // 模拟回复 (根据上下文)
    let reply = { role: 'ai', content: '根据您的描述，这可能是<b>座垫过低</b>导致的髌骨压力过大。' };

    // 模拟带指令的回复
    if (query.includes('调整')) {
      reply = {
        role: 'ai',
        content: '检测到您的 [Tarmac SL7] 当前座高为 710mm，基于最新的柔韧性评分，建议微调。',
        action: { title: '升高座垫', desc: '目标: 715mm (+5mm)' }
      };
    }

    messages.push(reply);
    scrollToBottom();
  }, 1500);
};

const handleAction = (action) => {
  // 这里将来处理自动调整逻辑
  alert(`正在执行: ${action.title}`);
};
</script>
<style scoped>
   /* 1. 核心定位调整：向上抬升，避开底部的“回到顶部”按钮 */
 .ai-agent-wrapper {
   position: fixed;
   bottom: 110px; /* 从 40px 改为 110px */
   right: 40px;
   z-index: 9999;
 }

/* --- 悬浮球 (FAB) --- */
.fab-trigger {
  width: 60px; height: 60px; border-radius: 50%;
  background: var(--text-main); color: var(--accent-gold);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; position: relative;
  box-shadow: 0 10px 25px rgba(0,0,0,0.25); /* 增加一点阴影浓度，因为悬浮得更高了 */
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.fab-trigger:hover { transform: scale(1.1); }
.fab-trigger.is-active { background: white; color: var(--text-main); transform: rotate(90deg); }

.fab-icon { font-size: 1.6rem; }

/* 呼吸光环 */
.pulse-ring {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  border-radius: 50%; border: 2px solid var(--accent-gold);
  opacity: 0; animation: ripple 3s infinite; pointer-events: none;
}
@keyframes ripple {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(1.5); opacity: 0; }
}

.unread-badge {
  position: absolute; top: 0; right: 0;
  width: 20px; height: 20px; background: var(--danger-color);
  color: white; font-size: 0.7rem; font-weight: 700;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  border: 2px solid white;
}

/* --- 聊天窗口 --- */
.chat-dialog {
  position: absolute;
  /*
     相对于 wrapper 定位。
     wrapper 在 bottom: 110px。
     dialog 设置 bottom: 75px，意味着对话框底部距离 wrapper 底部 75px。
     这样对话框展开时，正好在悬浮球上方一点点。
  */
  bottom: 75px;
  right: 0;
  width: 380px; height: 600px;
  background: rgba(255, 255, 255, 0.95); /* 稍微增加不透明度 */
  backdrop-filter: blur(20px);
  border-radius: 20px; border: 1px solid rgba(255,255,255,0.8);
  box-shadow: 0 20px 50px rgba(0,0,0,0.15);
  display: flex; flex-direction: column; overflow: hidden;
  transform-origin: bottom right;
}

/* ... (Header, Messages, Footer 等内部样式保持不变) ... */
/* Header */
.dialog-header {
  padding: 15px 20px; background: rgba(255,255,255,0.5);
  border-bottom: 1px solid rgba(0,0,0,0.05);
  display: flex; justify-content: space-between; align-items: center;
}
.header-info { display: flex; align-items: center; gap: 8px; }
.status-dot { width: 8px; height: 8px; background: var(--success-color); border-radius: 50%; box-shadow: 0 0 5px var(--success-color); }
.title { font-weight: 700; color: var(--text-main); font-size: 1rem; }
.subtitle { font-size: 0.75rem; color: var(--text-muted); opacity: 0.8; }
.btn-close { background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 0.9rem; }

/* Messages */
.messages-container {
  flex: 1; overflow-y: auto; padding: 20px;
  display: flex; flex-direction: column; gap: 20px;
  background: linear-gradient(to bottom, rgba(255,255,255,0.5), rgba(255,255,255,0.8));
}

.message-row { display: flex; gap: 12px; align-items: flex-end; }
.message-row.user { flex-direction: row-reverse; }

.avatar {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm);
}
.ai-avatar { background: var(--text-main); color: var(--accent-gold); font-size: 1rem; }
.user-avatar img { width: 100%; height: 100%; border-radius: 50%; }

.bubble {
  max-width: 80%; padding: 12px 16px; border-radius: 12px; font-size: 0.95rem; line-height: 1.5;
  position: relative; box-shadow: var(--shadow-sm);
}
.ai .bubble { background: white; color: var(--text-body); border-bottom-left-radius: 2px; }
.user .bubble { background: var(--primary-color); color: white; border-bottom-right-radius: 2px; }

/* 思考动画 */
.thinking-bubble { display: flex; gap: 4px; padding: 15px; }
.thinking-bubble span {
  width: 6px; height: 6px; background: #CCC; border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}
.thinking-bubble span:nth-child(1) { animation-delay: -0.32s; }
.thinking-bubble span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

/* 行动卡片 */
.action-card {
  margin-top: 10px; background: #F8F9F7; border-radius: 8px; padding: 10px;
  display: flex; align-items: center; gap: 10px; border: 1px solid rgba(92, 110, 88, 0.1);
}
.action-icon { color: var(--primary-color); font-size: 1.2rem; }
.action-info { flex: 1; display: flex; flex-direction: column; }
.action-title { font-weight: 700; font-size: 0.85rem; color: var(--text-main); }
.action-desc { font-size: 0.75rem; color: var(--text-muted); }
.btn-apply {
  background: var(--primary-color); color: white; border: none;
  padding: 4px 10px; border-radius: 4px; font-size: 0.8rem; cursor: pointer;
}

/* Footer */
.dialog-footer {
  padding: 15px; background: white; border-top: 1px solid rgba(0,0,0,0.05);
}
.input-wrapper {
  display: flex; gap: 10px; background: #F5F5F5; padding: 8px 12px; border-radius: 25px;
  border: 1px solid transparent; transition: 0.3s;
}
.input-wrapper:focus-within { border-color: var(--primary-light); background: white; box-shadow: 0 0 0 3px rgba(92, 110, 88, 0.1); }
.input-wrapper input {
  flex: 1; border: none; background: transparent; outline: none; font-size: 0.95rem; color: var(--text-main);
}
.btn-send {
  background: var(--primary-color); color: white; border: none; width: 32px; height: 32px;
  border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: 0.3s;
}
.btn-send:disabled { background: #DDD; cursor: not-allowed; }
.btn-send:hover:not(:disabled) { transform: scale(1.1); background: var(--primary-dark); }

/* 窗口动画 */
.dialog-pop-enter-active, .dialog-pop-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.dialog-pop-enter-from, .dialog-pop-leave-to { opacity: 0; transform: scale(0.8) translateY(20px); }

/* 移动端适配：移动端通常没有 Hover 状态的 BackToTop，可以适当放低 */
@media (max-width: 768px) {
  .ai-agent-wrapper {
    bottom: 90px; /* 移动端稍微低一点 */
    right: 20px;
  }
  .chat-dialog { width: calc(100vw - 40px); height: 60vh; bottom: 75px; }
}
</style>