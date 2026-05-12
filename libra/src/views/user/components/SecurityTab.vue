<template>
  <div class="panel-section fade-in">
    <div class="panel-header">
      <h3 class="panel-title">账号安全</h3>
      <p class="sub">保护您的账户资产与隐私安全</p>
    </div>

    <!-- 密码修改 -->
    <div class="security-block">
      <h4 class="block-title">重置登录密码</h4>
      <p class="block-desc">验证手机号：{{ userStore.userInfo?.phone }}</p>

      <form @submit.prevent="updatePassword">
        <div class="input-wrapper">
          <label>短信验证码</label>
          <div class="input-group code-group">
            <input type="text" v-model="passwordForm.code" class="zen-input" placeholder="输入验证码" maxlength="6">
            <button type="button" class="get-code-btn" @click="handleSendCode" :disabled="timer > 0">
              {{ timer > 0 ? `${timer}s 后重发` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div class="grid-2">
          <div class="input-wrapper">
            <label>新密码</label>
            <div class="input-group">
              <input :type="showNewPassword ? 'text' : 'password'" v-model="passwordForm.newPassword" class="zen-input" placeholder="6-20位，须包含字母和数字">
              <i :class="['fas', showNewPassword ? 'fa-eye-slash' : 'fa-eye', 'password-toggle']" @click="showNewPassword = !showNewPassword"></i>
            </div>
          </div>
          <div class="input-wrapper">
            <label>确认新密码</label>
            <div class="input-group">
              <input :type="showConfirmPassword ? 'text' : 'password'" v-model="passwordForm.confirmPassword" class="zen-input" placeholder="再次输入">
              <i :class="['fas', showConfirmPassword ? 'fa-eye-slash' : 'fa-eye', 'password-toggle']" @click="showConfirmPassword = !showConfirmPassword"></i>
            </div>
          </div>
        </div>

        <div class="form-footer-left">
          <button type="submit" class="btn btn-sm btn-outline" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : '确认重置' }}
          </button>
        </div>
      </form>
    </div>

    <div class="divider"></div>

    <!-- 手机号修改 -->
    <div class="security-block">
      <h4 class="block-title">手机号码绑定</h4>
      <p class="block-desc">当前绑定：{{ userStore.userInfo?.phone || '未绑定' }}</p>

      <form @submit.prevent="updatePhone">
        <div class="grid-2">
          <div class="input-wrapper">
            <label>新手机号</label>
            <div class="input-group">
              <input type="tel" v-model="phoneForm.newPhone" class="zen-input" placeholder="请输入新手机号" maxlength="11">
            </div>
          </div>
          <div class="input-wrapper">
            <label>验证登录密码</label>
            <div class="input-group">
              <input type="password" v-model="phoneForm.password" class="zen-input" placeholder="请输入当前登录密码以验证">
            </div>
          </div>
        </div>
        <div class="input-wrapper">
          <label>短信验证码</label>
          <div class="input-group code-group">
            <input type="text" v-model="phoneForm.code" class="zen-input" placeholder="输入验证码">
            <button type="button" class="get-code-btn" @click="sendPhoneCode">获取验证码</button>
          </div>
        </div>
        <div class="form-footer-left">
          <button type="submit" class="btn btn-sm btn-outline">更换手机号</button>
        </div>
      </form>
    </div>

    <div class="divider"></div>

    <!-- 注销账号区域 -->
    <div class="security-block danger-zone">
      <h4 class="block-title text-danger">注销账号</h4>
      <p class="block-desc">
        注销后，您的账号将进入 <strong>7天冷静期</strong>。在此期间登录可自动恢复账号。<br>
        超过 7 天未登录，账号信息、车辆数据、Fitting 报告将被<strong>永久删除</strong>且无法找回。
      </p>

      <div v-if="!showDeleteForm">
        <button class="btn btn-sm btn-danger-outline" @click="showDeleteForm = true">
          申请注销
        </button>
      </div>

      <form v-else @submit.prevent="handleDeleteAccount" class="delete-confirm-box fade-in">
        <div class="input-wrapper">
          <label>验证登录密码</label>
          <div class="input-group">
            <input
                type="password"
                v-model="deleteForm.password"
                class="zen-input"
                placeholder="请输入当前密码以确认身份"
            >
          </div>
        </div>

        <div class="checkbox-wrapper">
          <input type="checkbox" id="confirm-check" v-model="deleteForm.checked">
          <label for="confirm-check">我已了解注销风险，并自愿放弃账号权益</label>
        </div>

        <div class="form-footer-left gap-10">
          <button
              type="submit"
              class="btn btn-sm btn-danger"
              :disabled="!deleteForm.checked || !deleteForm.password || isSubmitting"
          >
            {{ isSubmitting ? '注销中...' : '确认注销' }}
          </button>
          <button type="button" class="btn btn-sm btn-text" @click="showDeleteForm = false">
            取消
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user.js';
import { showConfirm, showToast } from '@/utils/toast.js';
import { sendSmsCode, resetPassword, changeUserPhone, deleteUserAccount } from '@/api/auth.js';
import { rsaEncrypt } from '@/utils/rsa.js';

const router = useRouter();
const userStore = useUserStore();
const isSubmitting = ref(false);

const passwordForm = reactive({ code: '', newPassword: '', confirmPassword: '' });
const phoneForm = reactive({ newPhone: '', password: '', code: '' });
const deleteForm = reactive({ password: '', checked: false });

const timer = ref(0);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);
const showDeleteForm = ref(false);

const handleSendCode = async () => {
  const phoneNum = userStore.userInfo?.phone;
  if (!phoneNum) return showToast('未绑定手机号，无法重置', 'error');
  if (timer.value > 0) return;

  try {
    await sendSmsCode(phoneNum);
    showToast('验证码已发送', 'success');
    timer.value = 60;
    const interval = setInterval(() => {
      timer.value--;
      if (timer.value <= 0) clearInterval(interval);
    }, 1000);
  } catch (error) {
    console.error(error);
  }
};

const updatePassword = async () => {
  if (!passwordForm.code) return showToast('请输入验证码', 'error');
  if (!passwordForm.newPassword) return showToast('请输入新密码', 'error');
  
  const passwordReg = /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/;
  if (!passwordReg.test(passwordForm.newPassword)) {
    return showToast('密码需为6-20位，且包含字母和数字', 'error');
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) return showToast('两次密码输入不一致', 'error');

  try {
    isSubmitting.value = true;
    await resetPassword({
      code: passwordForm.code,
      password: rsaEncrypt(passwordForm.newPassword)
    });
    userStore.logout();
    showToast('修改成功，请重新登录！', 'success');
    router.push('/');
  } finally {
    isSubmitting.value = false;
  }
};

const updatePhone = async () => {
  if (!phoneForm.newPhone || !phoneForm.code || !phoneForm.password) return showToast('信息不完整', 'error');

  try {
    await changeUserPhone({
      newPhone: phoneForm.newPhone,
      code: phoneForm.code,
      password: rsaEncrypt(phoneForm.password)
    });
    showToast('手机号更换成功', 'success');
    userStore.setUserInfo({ ...userStore.userInfo, phone: phoneForm.newPhone });
    Object.assign(phoneForm, { newPhone: '', password: '', code: '' });
  } catch (error) {
    // handled
  }
};

const handleDeleteAccount = async () => {
  if (!deleteForm.checked) return showToast('请先勾选确认协议', 'warning');
  if (!deleteForm.password) return showToast('请输入密码', 'warning');

  const isConfirmed = await showConfirm(
      '确定要注销当前账号吗？注销后将进入7天冷静期，在此期间登录可恢复账号。',
      '确认注销'
  );

  if (isConfirmed) {
    try {
      isSubmitting.value = true;
      await deleteUserAccount({ password: rsaEncrypt(deleteForm.password) });
      showToast('申请注销成功', 'success');
      userStore.logout();
      setTimeout(() => router.replace('/'), 1500);
    } finally {
      isSubmitting.value = false;
    }
  }
};
</script>

<style scoped>
.security-block { margin-bottom: 40px; }
.block-title {
  font-size: 1.1rem; color: var(--text-main); margin-bottom: 15px;
  border-left: 3px solid var(--secondary-color); padding-left: 10px; line-height: 1;
}
.block-desc { font-size: 0.9rem; color: var(--text-muted); margin-bottom: 20px; }

.input-group { position: relative; display: flex; align-items: stretch; }
.code-group .zen-input { border-radius: var(--radius-sm) 0 0 var(--radius-sm); border-right: none; flex: 1; }
.get-code-btn {
  color: var(--primary-color);
  background: #fff;
  border: 1px solid #eee;
  border-left: none;
  font-weight: 600;
  cursor: pointer;
  padding: 0 20px;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  white-space: nowrap;
  transition: 0.3s;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.get-code-btn:hover:not(:disabled) { background: rgba(92, 110, 88, 0.05); color: var(--primary-dark); }
.get-code-btn:disabled { color: var(--text-muted); cursor: not-allowed; background: #fafafa; }

.password-toggle {
  position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
  color: var(--text-muted); cursor: pointer; transition: 0.3s;
}

.divider { margin: 40px 0; height: 1px; background: rgba(0,0,0,0.05); }
.form-footer-left { display: flex; justify-content: flex-start; gap: 10px; margin-top: 20px; }

.delete-confirm-box {
  background: rgba(193, 140, 126, 0.05); padding: 20px; border-radius: var(--radius-sm); border: 1px solid rgba(193, 140, 126, 0.1);
}

.checkbox-wrapper {
  display: flex; align-items: center; gap: 8px; font-size: 0.9rem; color: var(--text-main); margin: 15px 0; cursor: pointer;
}

.text-danger { color: var(--danger-color) !important; }
.text-primary { color: var(--primary-color) !important; }
.gap-10 { gap: 10px; }
</style>
