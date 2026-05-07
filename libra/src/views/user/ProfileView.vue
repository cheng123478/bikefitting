<template>
  <div class="profile-page">

    <div class="container profile-container">

      <!-- 1. 顶部个人概览卡片 -->
      <div class="glass-card profile-header">
        <div class="header-left">
          <div class="avatar-wrapper">
            <img :src="userForm.avatar || defaultAvatar" class="avatar-lg">
            <div class="edit-avatar-btn">
              <i class="fas fa-camera"></i>
              <input type="file" accept="image/*" class="file-input" @change="handleAvatarChange">
            </div>
          </div>
          <div class="user-identity">
            <h2 class="username">{{ userForm.name }}</h2>
            <i v-if="userForm.gender == 1" class="fas fa-mars" style="color: #6495ED; margin-right: 10px;"></i>
            <i v-if="userForm.gender == 2" class="fas fa-venus" style="color: #FF69B4; margin-right: 10px;"></i>
            <span v-if="userForm.birthday" style="background: #eee; border-radius: 4px; font-size: 0.8rem;">
              {{ getConstellation(userForm.birthday) }}
            </span>
            <p class="introduction">
              {{ userForm.introduction || '这家伙很懒，什么也没写' }}
            </p>
            <div class="badges">
              <span class="role-badge">普通会员</span>
              <span class="id-badge">ID: {{ userStore.userInfo?.id || '888888' }}</span>
            </div>
          </div>
        </div>

        <div class="header-right stats-box">
          <div class="stat-item">
            <span class="val">12</span><span class="lbl">Fitting 报告</span>
          </div>
          <div class="divider-v"></div>
          <div class="stat-item">
            <span class="val">3</span><span class="lbl">我的车辆</span>
          </div>
          <div class="divider-v"></div>
          <div class="stat-item">
            <span class="val">1,240</span><span class="lbl">累计里程(km)</span>
          </div>
        </div>
      </div>

      <!-- 2. 主体内容区 -->
      <div class="main-layout">

        <!-- 左侧导航 -->
        <aside class="sidebar glass-card">
          <ul class="menu-list">
            <li
                v-for="item in menuItems"
                :key="item.id"
                class="menu-item"
                :class="{ active: currentTab === item.id }"
                @click="currentTab = item.id"
            >
              <i :class="item.icon"></i>
              <span>{{ item.name }}</span>
            </li>
          </ul>
        </aside>

        <!-- 右侧内容面板 -->
        <div class="content-panel glass-card">

          <!-- TAB 1: 基础信息 -->
          <div v-if="currentTab === 'basic'" class="panel-section fade-in">
            <div class="panel-header">
              <h3 class="panel-title">编辑个人资料</h3>
              <p class="sub">完善资料让骑友更容易认识你</p>
            </div>
            <form @submit.prevent="updateProfile">
              <div class="grid-2">
                <div class="input-wrapper">
                  <label>昵称</label>
                  <div class="input-group">
                    <input type="text" v-model="userForm.name" class="zen-input">
                  </div>
                </div>
                <div class="input-wrapper">
                  <label>当前手机号</label>
                  <div class="input-group disabled">
                    <input type="text" v-model="userForm.phone" class="zen-input" disabled>
                    <i class="fas fa-lock input-icon"></i>
                  </div>
                  <small class="helper-text" @click="currentTab = 'security'">去“账号安全”修改 ></small>
                </div>
              </div>
              <div class="grid-2">
                <div class="input-wrapper">
                  <label>性别</label>
                  <div class="input-group">
                    <select v-model="userForm.gender" class="zen-input select-arrow">
                      <option :value="0">保密</option>
                      <option :value="1">男</option>
                      <option :value="2">女</option>
                    </select>
                    <!-- 下拉箭头图标 -->
                    <i class="fas fa-chevron-down input-icon right-icon"></i>
                  </div>
                </div>
                <div class="input-wrapper">
                  <label>生日</label>
                  <div class="input-group icon-input">
                    <i class="fas fa-birthday-cake input-icon"></i>
                    <input
                        type="date"
                        v-model="userForm.birthday"
                        class="zen-input"
                        :max="today"
                    >
                  </div>
                </div>
              </div>
              <div class="input-wrapper">
                <label>所在城市</label>
                <div class="input-group icon-input">
                  <i class="fas fa-map-marker-alt input-icon"></i>
                  <input type="text" v-model="userForm.city" class="zen-input" placeholder="例如：杭州">
                </div>
              </div>
              <div class="input-wrapper">
                <label>个人简介</label>
                <textarea class="zen-textarea" v-model="userForm.introduction" rows="3" placeholder="写一句你的骑行座右铭..."></textarea>
              </div>
              <div class="form-footer">
                <button type="submit" class="btn btn-primary">保存修改</button>
              </div>
            </form>
          </div>

          <!-- TAB 2: 账号安全 -->
          <div v-if="currentTab === 'security'" class="panel-section fade-in">
            <div class="panel-header">
              <h3 class="panel-title">账号安全</h3>
            </div>

            <!-- 密码修改 -->
            <div class="security-block">
              <h4 class="block-title">重置登录密码</h4>
              <p class="block-desc">为了您的账号安全，修改密码需要验证手机号：{{ userStore.userInfo?.phone }}</p>

              <form @submit.prevent="updatePassword">
                <!-- 第一步：验证码 -->
                <div class="input-wrapper">
                  <label>短信验证码</label>
                  <div class="input-group code-group">
                    <input type="text" v-model="passwordForm.code" class="zen-input" placeholder="输入验证码" maxlength="6">
                    <button type="button" class="get-code-btn" @click="handleSendCode" :disabled="timer > 0">
                      {{ timer > 0 ? `${timer}s 后重发` : '获取验证码' }}
                    </button>
                  </div>
                </div>

                <!-- 第二步：新密码 -->
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

                <div class="form-footer left">
                  <button type="submit" class="btn btn-sm btn-outline text-dark" :disabled="isSubmitting">
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
                <div class="grid-2">
                  <div class="input-wrapper">
                    <label>短信验证码</label>
                    <div class="input-group code-group">
                      <input type="text" v-model="phoneForm.code" class="zen-input" placeholder="输入验证码">
                      <button type="button" class="get-code-btn" @click="sendPhoneCode">获取验证码</button>
                    </div>
                  </div>
                </div>
                <div class="form-footer left">
                  <button type="submit" class="btn btn-sm btn-outline text-primary">更换手机号</button>
                </div>
              </form>
            </div>
            <div class="divider"></div>
            <!-- 注销账号区域 -->
            <div class="security-block danger-zone">
              <h4 class="block-title text-danger">注销账号</h4>
              <p class="block-desc">
                注销后，您的账号将进入 <strong>7天冷静期</strong>。在此期间登录可自动恢复账号。
                <br>
                超过 7 天未登录，账号信息、车辆数据、Fitting 报告将被<strong>永久删除</strong>且无法找回。
              </p>

              <!-- 默认显示按钮，点击展开确认表单 -->
              <div v-if="!showDeleteForm">
                <button class="btn btn-sm btn-danger-outline" @click="showDeleteForm = true">
                  申请注销
                </button>
              </div>

              <!-- 确认表单 (展开后显示) -->
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

                <div class="form-footer left gap-10">
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

          <!-- TAB 3: 会员特权 -->
          <MembershipTab v-if="currentTab === 'membership'" />

        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/user.js';
import {showConfirm, showToast} from '@/utils/toast.js';
import {sendSmsCode, resetPassword, changeUserPhone, deleteUserAccount, logout} from '@/api/auth.js';
import { rsaEncrypt } from '@/utils/rsa.js';
import {
  updateUserProfile,
  uploadAvatar
} from '@/api/profile.js';

// ================== 基础状态 ==================
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const currentTab = ref(route.query.tab || 'basic');
const defaultAvatar = '/src/assets/image/default-avatar.png';
const isSubmitting = ref(false); // 通用提交 loading 状态

const menuItems = [
  { id: 'basic', name: '个人资料', icon: 'far fa-id-card' },
  { id: 'security', name: '账号安全', icon: 'fas fa-shield-alt' },
  { id: 'membership', name: '会员特权', icon: 'fas fa-crown' }
];

import MembershipTab from './components/MembershipTab.vue';

// ================== 个人资料逻辑 ==================
const userForm = reactive({ name: '', phone: '', city: '', introduction: '', avatar: '', gender: 0, birthday: '' });
const today = computed(() => {
  const date = new Date();
  return date.toISOString().split('T')[0];
});
const getConstellation = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const s = "魔羯水瓶双鱼白羊金牛双子巨蟹狮子处女天秤天蝎射手魔羯";
  const arr = [20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22];
  return s.substr(month * 2 - (day < arr[month - 1] ? 2 : 0), 2) + "座";
}
// 处理头像上传
const handleAvatarChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;

  // 1. 本地预览 (立即反馈)
  const reader = new FileReader();
  reader.readAsDataURL(file);
  reader.onload = (evt) => { userForm.avatar = evt.target.result; };

  // 2. 构造 FormData 上传
  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await uploadAvatar(formData);
    // 假设后端返回 { code: 200, data: { url: '...' } }
    console.log('上传结果', res);
    userForm.avatar = res;

    // 更新 Store
    userStore.userInfo.avatar = res;
    localStorage.setItem('user_info', JSON.stringify(userStore.userInfo));
    showToast('头像更新成功', 'success');
  } catch (error) {
    // 上传失败还原默认头像或保持原样，视需求而定
    showToast('头像上传失败', 'error');
  }
};

// 保存个人资料
const updateProfile = async () => {
  if (!userForm.name) return showToast('昵称不能为空', 'warning');

  try {
    isSubmitting.value = true;
    await updateUserProfile(userForm);

    // 更新本地 Store，保证页面其他位置的数据同步
    userStore.setUserInfo({ ...userStore.userInfo, ...userForm });
    localStorage.setItem('user_info', JSON.stringify(userStore.userInfo));
    showToast('个人资料已保存', 'success');
  } catch (error) {
    // 错误处理由 request.js 统一弹窗，这里通常不需要额外处理，除非有特殊逻辑
  } finally {
    isSubmitting.value = false;
  }
};

// ================== 账号安全逻辑 ==================
const passwordForm = reactive({ code: '', newPassword: '', confirmPassword: '' });
const phoneForm = reactive({ newPhone: '', password: '', code: '' });
const timer = ref(0);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

const startTimer = () => {
  timer.value = 60;
  const interval = setInterval(() => {
    timer.value--;
    if (timer.value <= 0) clearInterval(interval);
  }, 1000);
};

// 发送验证码
const handleSendCode = async (phone, type) => {
  const phoneNum = userStore.userInfo?.phone;
  if (!phoneNum) return showToast('未绑定手机号，无法重置', 'error');
  if (timer.value > 0) return;

  await sendSmsCode(phoneNum).then(response => {
    showToast('验证码已发送', 'success');
    // 倒计时
    timer.value = 60;
    const interval = setInterval(() => {
      timer.value--;
      if (timer.value <= 0) clearInterval(interval);
    }, 1000);
  }).catch(error => {
    console.error(error);
  });
};

// 提交重置密码
const updatePassword = async () => {
  if (!passwordForm.code) return showToast('请输入验证码', 'error');
  if (!passwordForm.newPassword) return showToast('请输入新密码', 'error');
  // 密码强度校验：6-20位，包含字母和数字
  const passwordReg = /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/;
  if (!passwordReg.test(passwordForm.newPassword)) {
    return showToast('密码需为6-20位，且包含字母和数字', 'error');
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) return showToast('两次密码输入不一致', 'error');

  isSubmitting.value = true;

  resetPassword({
    code: passwordForm.code,
    password: rsaEncrypt(passwordForm.newPassword)
  }).then((response) => {
    userStore.logout();
    showToast('修改成功，请重新登录！', 'success');
    router.push('/');
  }).catch((error) => {
    console.log(error);
  }).finally(() => {
    isSubmitting.value = false;
  });
};

// 提交更换手机号
const updatePhone = async () => {
  if (!phoneForm.newPhone || !phoneForm.code || !phoneForm.password) return showToast('信息不完整', 'error');

  try {
    await changeUserPhone({
      newPhone: phoneForm.newPhone,
      code: phoneForm.code,
      password: rsaEncrypt(phoneForm.password) // 验证当前密码，需加密
    });

    showToast('手机号更换成功', 'success');
    // 更新本地存储的手机号
    userStore.setUserInfo({ ...userStore.userInfo, phone: phoneForm.newPhone });

    // 清空表单
    Object.assign(phoneForm, { newPhone: '', password: '', code: '' });
  } catch (error) {
    // handled
  }
};

// ================== 注销账号逻辑 ==================
const showDeleteForm = ref(false);
const deleteForm = reactive({
  password: '',
  checked: false
});
const handleDeleteAccount = async () => {
  // 1. 基础校验
  if (!deleteForm.checked) return showToast('请先勾选确认协议', 'warning');
  if (!deleteForm.password) return showToast('请输入密码', 'warning');
  // 2. 弹出确认框
  const isConfirmed = await showConfirm(
      '确定要注销当前账号吗？注销后将进入7天冷静期，在此期间登录可恢复账号。',
      '确认注销'
  );
  // 3. 用户点击确认后才执行
  if (isConfirmed) {
    try {
      isSubmitting.value = true;
      // 调用接口，传入加密后的密码
      await deleteUserAccount({
        password: rsaEncrypt(deleteForm.password)
      });
      showToast('申请注销成功', 'success');
      // 清除本地状态（AccessToken/UserInfo等）
      userStore.logout();
      // 延迟跳转
      setTimeout(() => {
        router.replace('/'); // 使用 replace 更好
      }, 1500);
    } catch (error) {
      // 这里的错误通常会被 request.js 拦截并报错（比如密码错误）
      console.error('注销请求失败:', error);
    } finally {
      isSubmitting.value = false;
    }
  }
};

// ================== 初始化 ==================
onMounted(() => {
  // 1. 回显个人基本信息
  if (userStore.userInfo) {
    Object.assign(userForm, {
      name: userStore.userInfo.name || '',
      phone: userStore.userInfo.phone || '',
      avatar: userStore.userInfo.avatar || '',
      city: userStore.userInfo.city || '',
      introduction: userStore.userInfo.introduction || '',
      gender: userStore.userInfo.gender !== undefined ? userStore.userInfo.gender : 0,
      birthday: userStore.userInfo.birthday || ''
    });
  }
});
</script>

<style scoped>
.profile-page {
  padding-top: calc(var(--header-height) + 20px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.profile-container { max-width: 1000px; }

/* Header */
.profile-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 40px; margin-bottom: 30px; border: var(--border-glass);
  /* glass-card已提供背景圆角 */
}

.header-left { display: flex; align-items: center; gap: 30px; }

.avatar-wrapper { position: relative; width: 100px; height: 100px; }
.avatar-lg { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; border: 4px solid var(--bg-page); }

.edit-avatar-btn {
  position: absolute; bottom: 0; right: 0; width: 32px; height: 32px;
  background: var(--primary-color); color: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  border: 2px solid #fff; transition: 0.3s;
}
.edit-avatar-btn:hover { background: var(--primary-dark); }
.file-input { position: absolute; width: 100%; height: 100%; opacity: 0; cursor: pointer; }

.user-identity .username {
  font-family: var(--font-serif); font-size: 2rem; color: var(--text-main);
  margin-bottom: 5px; line-height: 1.2;
}
.user-identity .introduction { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 10px; }

.badges { display: flex; gap: 10px; }
.role-badge { background: var(--primary-color); color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; }
.id-badge { background: #eee; color: #888; padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; font-family: monospace; }

.stats-box { display: flex; align-items: center; gap: 30px; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-item .val { font-size: 1.6rem; font-weight: 600; color: var(--text-main); font-family: var(--font-serif); }
.stat-item .lbl { font-size: 0.8rem; color: var(--text-muted); margin-top: 2px; }
.divider-v { width: 1px; height: 30px; background: rgba(0,0,0,0.1); }

/* Main Layout */
.main-layout { display: grid; grid-template-columns: 240px 1fr; gap: 30px; }

.sidebar { padding: 20px 0; height: fit-content; border: var(--border-glass); }

.menu-item {
  padding: 15px 25px; cursor: pointer; display: flex; align-items: center; gap: 12px;
  color: var(--text-muted); transition: 0.3s; border-left: 3px solid transparent;
}
.menu-item:hover { background: var(--bg-page); color: var(--primary-color); }
.menu-item.active { background: rgba(92, 110, 88, 0.08); color: var(--primary-color); border-left-color: var(--primary-color); font-weight: 600; }
.menu-item i { width: 20px; text-align: center; }

/* Content Panel */
.content-panel { padding: 40px; border: var(--border-glass); min-height: 500px; }

.panel-header { margin-bottom: 30px; border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 15px; }
.panel-title { font-family: var(--font-serif); font-size: 1.6rem; margin-bottom: 5px; color: var(--text-main); }
.panel-header .sub { font-size: 0.9rem; color: var(--text-muted); }
.row-between { display: flex; justify-content: space-between; align-items: flex-end; }

/* Form Overrides & Specifics */
.input-wrapper { margin-bottom: 25px; }
.input-wrapper label { display: block; margin-bottom: 8px; font-size: 0.9rem; color: var(--text-muted); }

.input-group.disabled .zen-input { background: #f5f5f5; cursor: not-allowed; }
.input-group.disabled .input-icon { right: 12px; left: auto; } /* Lock icon on right */
.input-group .right-icon {
  left: auto;
  right: 12px;
  pointer-events: none; /* 让点击穿透到 select 上 */
  font-size: 0.8rem;
}

/* 优化 Date input 的 placeholder 颜色 */
input[type="date"] {
  font-family: var(--font-sans);
  color: var(--text-main);
}
.helper-text { font-size: 0.8rem; color: var(--primary-color); cursor: pointer; margin-top: 5px; display: inline-block; }

.form-footer { margin-top: 40px; display: flex; justify-content: flex-end; }
.form-footer.left { justify-content: flex-start; margin-top: 20px; }
.select-arrow {
  appearance: none; /* 移除浏览器默认样式 */
  -webkit-appearance: none;
  -moz-appearance: none;
  cursor: pointer;
  padding-right: 35px; /* 给右侧图标留位置 */
}

/* 身体数据 Grid */
.body-data-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 30px; }
.data-input-card {
  background: var(--bg-page); padding: 20px; border-radius: var(--radius-md);
  text-align: center; border: 1px solid transparent; transition: 0.3s;
}
.data-input-card:focus-within { border-color: var(--primary-color); background: #fff; box-shadow: var(--shadow-sm); }
.data-input-card.highlight { background: rgba(92, 110, 88, 0.05); }

.data-input-card label { display: flex; justify-content: center; align-items: center; gap: 5px; font-size: 0.8rem; color: var(--text-muted); margin-bottom: 10px; }
.data-input-card input {
  width: 100%; text-align: center; border: none; background: transparent;
  font-size: 1.6rem; font-family: var(--font-serif); font-weight: 600; color: var(--primary-color); outline: none;
}
.info-icon { font-size: 0.9rem; cursor: help; }

.illustration-box {
  background: rgba(92, 110, 88, 0.05); padding: 15px; border-radius: var(--radius-sm);
  display: flex; align-items: center; gap: 15px; color: var(--primary-color); font-size: 0.9rem;
}

/* 账号安全 */
.security-block { margin-bottom: 40px; }
.block-title {
  font-size: 1.1rem; color: var(--text-main); margin-bottom: 15px;
  border-left: 3px solid var(--secondary-color); padding-left: 10px; line-height: 1;
}
.block-desc { font-size: 0.9rem; color: var(--text-muted); margin-bottom: 20px; }

.code-group { display: flex; align-items: center; }
.code-group .zen-input { border-radius: var(--radius-sm) 0 0 var(--radius-sm); border-right: none; }
.get-code-btn {
  color: var(--primary-color); background: #fff; border: 1px solid #eee; border-left: none;
  font-weight: 600; cursor: pointer; padding: 0 15px; height: 43px; /* Match input height */
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0; white-space: nowrap; transition: 0.3s;
}
.get-code-btn:hover { background: rgba(92, 110, 88, 0.05); }

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  cursor: pointer;
  transition: 0.3s;
}
.password-toggle:hover {
  color: var(--primary-color);
}

.divider { margin: 40px 0; } /* Keep custom margin if needed, but background is global */
.text-dark { color: var(--text-main); border-color: var(--text-main); }
.text-dark:hover { background: var(--text-main); color: #fff; }
.text-primary { color: var(--primary-color); border-color: var(--primary-color); }
.text-primary:hover { background: var(--primary-color); color: #fff; }

/* 车库列表 */
.bike-list { display: flex; flex-direction: column; gap: 15px; }
.bike-card { display: flex; align-items: center; padding: 15px; border: 1px solid #eee; border-radius: var(--radius-md); transition: 0.3s; }
.bike-card:hover { border-color: var(--primary-color); box-shadow: var(--shadow-sm); }

.bike-img { width: 80px; height: 60px; object-fit: contain; background: #f5f5f5; border-radius: 6px; padding: 5px; }
.bike-info { flex: 1; margin-left: 20px; }
.bike-info h4 { font-size: 1.1rem; margin-bottom: 5px; color: var(--text-main); }
.bike-size { font-size: 0.85rem; color: var(--text-muted); }
.tag.default { font-size: 0.7rem; background: var(--primary-color); color: #fff; padding: 2px 6px; border-radius: 4px; margin-top: 5px; display: inline-block; }

.bike-actions { display: flex; gap: 10px; }
.btn-icon {
  width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid transparent; color: var(--text-muted); cursor: pointer; transition: 0.2s;
}
.btn-icon:hover { background: rgba(0,0,0,0.05); color: var(--primary-color); }
.btn-icon.text-danger:hover { color: var(--danger-color); background: rgba(193, 140, 126, 0.1); }

@media (max-width: 768px) {
  .profile-header { flex-direction: column; text-align: center; gap: 30px; }
  .header-left { flex-direction: column; }
  .main-layout { grid-template-columns: 1fr; }
  .sidebar { overflow-x: auto; padding: 10px; white-space: nowrap; }
  .menu-list { display: flex; }
  .menu-item { border-left: none; border-bottom: 3px solid transparent; padding: 10px 15px; }
  .menu-item.active { border-left-color: transparent; border-bottom-color: var(--primary-color); background: transparent; }
  .body-data-grid { grid-template-columns: 1fr 1fr; }
  .grid-2 { grid-template-columns: 1fr; }
}

/* 危险区域样式 */
.danger-zone .block-title {
  border-left-color: var(--danger-color);
  color: var(--danger-color);
}

/* 确认框容器 */
.delete-confirm-box {
  background: #fff5f5; /* 浅红色背景 */
  padding: 20px;
  border-radius: var(--radius-sm);
  border: 1px solid #ffebeb;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: var(--text-main);
  margin: 15px 0;
  cursor: pointer;
}

.gap-10 { gap: 10px; }
</style>