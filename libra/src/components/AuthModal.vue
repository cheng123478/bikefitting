<template>
  <div class="modal-overlay" :class="{ 'is-visible': isOpen }" @click.self="close">
    <div class="modal-content">
      <button class="close-btn" @click="close">&times;</button>

      <div class="auth-tabs">
        <div class="tab" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">登录</div>
        <div class="tab" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">注册</div>
      </div>

      <!-- 登录表单 -->
      <form v-if="activeTab === 'login'" class="auth-form animate-fade" @submit.prevent="handleLogin" novalidate>
        <h3 class="form-title">欢迎回来</h3>
        <div class="input-field-group">
          <i class="fas fa-mobile-alt input-icon"></i>
          <input type="tel" v-model="loginForm.phone" class="input-field" placeholder="请输入手机号" maxlength="11"
                 required>
        </div>

        <!-- 密码登录模式 -->
        <div v-if="loginMode === 'password'">
          <div class="input-field-group">
            <i class="fas fa-lock input-icon"></i>
            <input :type="showPassword ? 'text' : 'password'" v-model="loginForm.password" class="input-field" placeholder="输入密码" required>
            <i :class="['fas', showPassword ? 'fa-eye-slash' : 'fa-eye', 'password-toggle']" @click="showPassword = !showPassword"></i>
          </div>
        </div>

        <!-- 验证码登录模式 -->
        <div v-else>
          <div class="input-field-group code-group">
            <i class="fas fa-shield-alt input-icon"></i>
            <input type="text" v-model="loginForm.code" class="input-field" placeholder="输入验证码" maxlength="6"
                   required>
            <button
                type="button"
                class="get-code-btn"
                @click="sendCode('login')"
                :disabled="timer > 0 || !isValidPhone(loginForm.phone)"
            >
              {{ timer > 0 ? `${timer}s 后重发` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div class="form-options">
          <a href="#" @click.prevent="toggleLoginMode">
            {{ loginMode === 'password' ? '切换验证码登录' : '切换密码登录' }}
          </a>
          <a href="#">忘记密码?</a>
        </div>

        <div class="agreement-section">
          <label class="checkbox-container">
            <input type="checkbox" v-model="isAgreed">
            <span class="checkmark"></span>
            <span class="agreement-text">
              我已阅读并同意
              <router-link to="/user-agreement" target="_blank" @click.stop>《用户协议》</router-link>
              和
              <router-link to="/privacy-policy" target="_blank" @click.stop>《隐私政策》</router-link>
            </span>
          </label>
        </div>

        <button type="submit" class="btn btn-primary btn-block" :disabled="isLoading">
          <i v-if="isLoading" class="fas fa-spinner fa-spin"></i>
          {{ isLoading ? '登录中...' : '立即登录' }}
        </button>

<!--        <div class="social-divider"><span>或者</span></div>-->
<!--        <div class="social-login-btns">-->
<!--          <button type="button" class="wechat-btn" @click="wechatLogin">-->
<!--            <i class="fab fa-weixin"></i> 微信一键登录-->
<!--          </button>-->
<!--        </div>-->
      </form>

      <!-- 注册表单 -->
      <form v-if="activeTab === 'register'" class="auth-form animate-fade" @submit.prevent="handleRegister" novalidate>
        <h3 class="form-title">加入 BikeFit Pro</h3>
        <div class="input-field-group">
          <i class="fas fa-mobile-alt input-icon"></i>
          <input type="tel" v-model="registerForm.phone" class="input-field" placeholder="请输入手机号" maxlength="11"
                 required>
        </div>
        <div class="input-field-group code-group">
          <i class="fas fa-shield-alt input-icon"></i>
          <input type="text" v-model="registerForm.code" class="input-field" placeholder="输入短信验证码" maxlength="6"
                 required>
          <button
              type="button"
              class="get-code-btn"
              @click="sendCode('register')"
              :disabled="timer > 0 || !isValidPhone(registerForm.phone)"
          >
            {{ timer > 0 ? `${timer}s 后重发` : '获取验证码' }}
          </button>
        </div>
        <div class="agreement-section">
          <label class="checkbox-container">
            <input type="checkbox" v-model="isAgreed">
            <span class="checkmark"></span>
            <span class="agreement-text">
              我已阅读并同意
              <router-link to="/user-agreement" target="_blank" @click.stop>《用户协议》</router-link>
              和
              <router-link to="/privacy-policy" target="_blank" @click.stop>《隐私政策》</router-link>
            </span>
          </label>
        </div>

        <button type="submit" class="btn btn-primary btn-block" :disabled="isLoading">
          <i v-if="isLoading" class="fas fa-spinner fa-spin"></i>
          {{ isLoading ? '注册中...' : '注册并登录' }}
        </button>

<!--        <div class="social-divider"><span>社交账号快捷注册</span></div>-->
<!--        <div class="social-login-btns">-->
<!--          <button type="button" class="wechat-btn" @click="wechatLogin">-->
<!--            <i class="fab fa-weixin"></i> 微信授权注册-->
<!--          </button>-->
<!--        </div>-->
      </form>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, onUnmounted} from 'vue';
import {showToast} from "@/utils/toast.js";
import {useUserStore} from '@/stores/user';
import {rsaEncrypt} from "@/utils/rsa.js";
import {sendSmsCode, registerUser, loginByPassword} from '@/api/auth';

const props = defineProps(['isOpen']);
const emit = defineEmits(['close']);
const userStore = useUserStore();

// 状态管理
const activeTab = ref('login');
const loginMode = ref('password'); // 'password' | 'sms'
const showPassword = ref(false);
const timer = ref(0);
let timerInterval = null; // 用于存储定时器ID
const isLoading = ref(false);
const isAgreed = ref(false);
// 表单数据
const loginForm = reactive({phone: '', password: '', code: ''});
const registerForm = reactive({phone: '', code: ''});
// 手机号正则校验
const isValidPhone = (phone) => /^1[3-9]\d{9}$/.test(phone);
// 切换 Tab 时重置表单
const switchTab = (tab) => {
  activeTab.value = tab;
  resetTimer();
};
// 切换登录模式
const toggleLoginMode = () => {
  loginMode.value = (loginMode.value === 'password' ? 'sms' : 'password');
};

// 清理定时器
const resetTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
  timer.value = 0;
};

// 关闭弹窗
const close = () => {
  emit('close');
  setTimeout(() => {
    loginForm.phone = '';
    loginForm.password = '';
    loginForm.code = '';
    registerForm.phone = '';
    registerForm.code = '';
    showPassword.value = false;
    resetTimer(); // 关闭时必须停止计时器，防止后台运行
    isLoading.value = false;
  }, 300);
};

// 发送验证码逻辑
const sendCode = async (type) => {
  const phoneNum = type === 'login' ? loginForm.phone : registerForm.phone;

  if (!isValidPhone(phoneNum)) {
    showToast('请输入正确的手机号码', 'error');
    return;
  }

  if (timer.value > 0) return; // 防止重复点击

  await sendSmsCode(phoneNum).then(() => {
    resetTimer();
    showToast('验证码已发送', 'success');
  });

  // --- 倒计时核心逻辑 ---
  timer.value = 60;
  timerInterval = setInterval(() => {
    timer.value--;
    if (timer.value <= 0) {
      resetTimer(); // 倒计时结束清理
    }
  }, 1000);
};

// 登录处理
const handleLogin = async () => {
  console.log('[登录] 开始校验', { phone: loginForm.phone, mode: loginMode.value });
  
  // 校验手机号
  if (!loginForm.phone) {
    console.log('[登录] 未输入手机号');
    showToast('请输入手机号', 'error');
    return;
  }
  if (!isValidPhone(loginForm.phone)) {
    console.log('[登录] 手机号格式错误');
    showToast('请输入正确的手机号码', 'error');
    return;
  }
  
  // 根据登录模式校验
  if (loginMode.value === 'password') {
    // 密码登录模式
    if (!loginForm.password) {
      console.log('[登录] 未输入密码');
      showToast('请输入密码', 'error');
      return;
    }
  } else {
    // 验证码登录模式
    if (!loginForm.code) {
      console.log('[登录] 未输入验证码');
      showToast('请先获取验证码', 'error');
      return;
    }
  }
  
  // 检查是否勾选协议
  if (!isAgreed.value) {
    console.log('[登录] 未勾选协议');
    showToast('请勾选阅读并同意《用户协议》和《隐私政策》', 'warning');
    return;
  }
  
  console.log('[登录] 校验通过，开始提交');
  isLoading.value = true;
  if (loginMode.value === 'password') {
    await loginByPassword({
      phone: loginForm.phone,
      password: rsaEncrypt(loginForm.password)
    }).then((user) => {
      userStore.login(user);
      showToast('登录成功', 'success');
      isLoading.value = false;
      close();
    }).catch((error) => {
      isLoading.value = false;
    }).finally(() => {
      resetTimer();
    });

  } else if (loginMode.value === 'sms') {
    isLoading.value = true;
    await registerUser({
      phone: loginForm.phone,
      code: loginForm.code
    }).then((user) => {
      userStore.login(user);
      showToast('登录成功', 'success');
      isLoading.value = false;
      close();
    }).catch((error) => {
      isLoading.value = false;
    }).finally(() => {
      resetTimer();
    });
  }
};

// 注册处理
const handleRegister = async () => {
  // 校验手机号
  if (!registerForm.phone) {
    return showToast('请输入手机号', 'error');
  }
  if (!isValidPhone(registerForm.phone)) {
    return showToast('请输入正确的手机号码', 'error');
  }
  
  // 校验验证码
  if (!registerForm.code) {
    return showToast('请先获取验证码', 'error');
  }
  
  // 检查是否勾选协议
  if (!isAgreed.value) {
    return showToast('请勾选阅读并同意《用户协议》和《隐私政策》', 'warning');
  }
  
  isLoading.value = true;
  await registerUser({
    phone: registerForm.phone,
    code: registerForm.code
  }).then((user) => {
    userStore.login(user);
    showToast('注册成功', 'success');
    isLoading.value = false;
    close();
  }).catch((error) => {
    isLoading.value = false;
  }).finally(() => {
    resetTimer();
  });



};

const wechatLogin = () => {
  showToast('正在前往微信授权...', 'info');
  const appId = import.meta.env.VITE_WECHAT_WEB_APP_ID || 'APPID_PLACEHOLDER';
  const redirectUri = encodeURIComponent(window.location.origin + '/wechat/callback');
  const isWechatBrowser = /MicroMessenger/i.test(navigator.userAgent);
  
  if (isWechatBrowser) {
    window.location.href = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${appId}&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect`;
  } else {
    window.location.href = `https://open.weixin.qq.com/connect/qrconnect?appid=${appId}&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_login&state=STATE#wechat_redirect`;
  }
};

// 组件销毁时确保定时器被清除
onUnmounted(() => {
  resetTimer();
});
</script>

<style scoped>
.form-title {
  text-align: center;
  font-size: 1.5rem;
  margin-bottom: var(--space-md);
  font-family: var(--font-serif);
  color: var(--text-main);
}

.auth-tabs {
  display: flex;
  margin-bottom: var(--space-lg);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.tab {
  flex: 1;
  text-align: center;
  padding: 12px;
  cursor: pointer;
  font-weight: 500;
  color: var(--text-muted);
  transition: 0.3s;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  font-family: var(--font-serif);
  font-size: 1.2rem;
}

.tab.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
  font-weight: 600;
  font-style: italic;
}

/* 验证码输入框优化 */
.code-group {
  display: flex;
  align-items: center;
  position: relative;
}

.code-group .input-field {
  padding-right: 120px;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  cursor: pointer;
  transition: 0.3s;
  z-index: 2;
}

.password-toggle:hover {
  color: var(--primary-color);
}

/* 留出按钮空间 */

.get-code-btn {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: var(--primary-color);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 12px;
  border-left: 1px solid #eee;
  transition: all 0.3s;
  height: 30px;
  line-height: 1;
}

.get-code-btn:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.get-code-btn:not(:disabled):hover {
  color: var(--primary-dark);
}

.form-options {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  color: var(--text-muted);
  margin-bottom: var(--space-md);
}

.form-options a:hover {
  color: var(--primary-color);
  text-decoration: underline;
}

/* 按钮全宽 */
.btn-block {
  width: 100%;
  justify-content: center;
}

.social-divider {
  margin: 25px 0 15px;
  position: relative;
  text-align: center;
}

.social-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  z-index: 1;
}

.social-divider span {
  background: #fff;
  padding: 0 15px;
  position: relative;
  z-index: 2;
  color: #AAA;
  font-size: 0.85rem;
}

.wechat-btn {
  width: 100%;
  background: #07C160;
  color: white;
  border: none;
  padding: 10px;
  border-radius: var(--radius-pill);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: var(--ease-out-expo);
}

.wechat-btn:hover {
  background: #06AD56;
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.animate-fade {
  animation: fadeIn 0.4s var(--ease-out-expo);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 协议复选框样式 */
.agreement-section {
  margin-bottom: var(--space-md);
  padding: 0 4px;
}

.checkbox-container {
  display: flex;
  align-items: flex-start;
  position: relative;
  cursor: pointer;
  font-size: 0.85rem;
  user-select: none;
  gap: 10px;
  line-height: 1.4;
}

.checkbox-container input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  flex-shrink: 0;
  height: 18px;
  width: 18px;
  background-color: #eee;
  border-radius: 4px;
  border: 1px solid #ddd;
  transition: all 0.2s;
  position: relative;
}

.checkbox-container:hover input ~ .checkmark {
  background-color: #e0e0e0;
}

.checkbox-container input:checked ~ .checkmark {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
}

.checkbox-container input:checked ~ .checkmark:after {
  display: block;
}

.checkbox-container .checkmark:after {
  left: 6px;
  top: 2px;
  width: 4px;
  height: 9px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.agreement-text {
  color: var(--text-muted);
}

.agreement-text a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.agreement-text a:hover {
  text-decoration: underline;
}
</style>