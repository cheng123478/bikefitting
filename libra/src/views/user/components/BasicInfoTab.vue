<template>
  <div class="panel-section fade-in">
    <div class="panel-header">
      <h3 class="panel-title">编辑个人资料</h3>
      <p class="sub">完善资料让骑友更容易认识你</p>
    </div>
    
    <form @submit.prevent="updateProfile">
      <!-- 头像上传区 (移动到资料内部以加强关联) -->
      <div class="avatar-edit-section mb-30">
        <div class="avatar-wrapper">
          <img :src="userForm.avatar || defaultAvatar" class="avatar-lg">
          <div class="edit-overlay">
            <i class="fas fa-camera"></i>
            <input type="file" accept="image/*" class="file-input" @change="handleAvatarChange">
          </div>
        </div>
        <div class="avatar-info">
          <span class="label">修改个人头像</span>
          <span class="tip">建议尺寸 200x200，支持 JPG/PNG</span>
        </div>
      </div>

      <div class="grid-2">
        <div class="input-wrapper">
          <label>昵称</label>
          <div class="input-group">
            <input type="text" v-model="userForm.name" class="zen-input" placeholder="起个响亮的名字">
          </div>
        </div>
        <div class="input-wrapper">
          <label>当前手机号</label>
          <div class="input-group disabled icon-input">
            <i class="fas fa-lock input-icon"></i>
            <input type="text" v-model="userForm.phone" class="zen-input" disabled>
          </div>
          <small class="helper-text" @click="$emit('switch-tab', 'security')">去“账号安全”修改 ></small>
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
            <i class="fas fa-chevron-down input-icon right-icon"></i>
          </div>
        </div>
        <div class="input-wrapper">
          <label>生日 ({{ getConstellation(userForm.birthday) }})</label>
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
        <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
          {{ isSubmitting ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { useUserStore } from '@/stores/user.js';
import { showToast } from '@/utils/toast.js';
import { updateUserProfile, uploadAvatar } from '@/api/profile.js';
import { STORAGE_KEYS } from '@/config/constants';

const emit = defineEmits(['switch-tab']);
const userStore = useUserStore();
const isSubmitting = ref(false);
const defaultAvatar = '/src/assets/image/default-avatar.png';

const userForm = reactive({
  name: '',
  phone: '',
  city: '',
  introduction: '',
  avatar: '',
  gender: 0,
  birthday: ''
});

const today = computed(() => new Date().toISOString().split('T')[0]);

const getConstellation = (dateStr) => {
  if (!dateStr) return '星座未知';
  const date = new Date(dateStr);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const s = "魔羯水瓶双鱼白羊金牛双子巨蟹狮子处女天秤天蝎射手魔羯";
  const arr = [20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22];
  return s.substr(month * 2 - (day < arr[month - 1] ? 2 : 0), 2) + "座";
};

const handleAvatarChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await uploadAvatar(formData);
    userForm.avatar = res;
    // 同步更新 Store
    userStore.userInfo.avatar = res;
    localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(userStore.userInfo));
    showToast('头像更新成功', 'success');
  } catch (error) {
    showToast('头像上传失败', 'error');
  }
};

const updateProfile = async () => {
  if (!userForm.name) return showToast('昵称不能为空', 'warning');

  try {
    isSubmitting.value = true;
    await updateUserProfile(userForm);
    userStore.setUserInfo({ ...userStore.userInfo, ...userForm });
    localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(userStore.userInfo));
    showToast('个人资料已保存', 'success');
  } catch (error) {
    // handled by request interceptor
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(() => {
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
.mb-30 { margin-bottom: 30px; }
.mb-0 { margin-bottom: 0; }

.avatar-edit-section {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: rgba(92, 110, 88, 0.03);
  border-radius: var(--radius-md);
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  cursor: pointer;
}

.avatar-lg {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #fff;
  box-shadow: var(--shadow-sm);
}

.edit-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: 0.3s;
}

.avatar-wrapper:hover .edit-overlay {
  opacity: 1;
}

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-info .label {
  display: block;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}

.avatar-info .tip {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.input-wrapper { margin-bottom: 25px; }
.input-wrapper label { display: block; margin-bottom: 8px; font-size: 0.9rem; color: var(--text-muted); }

.input-group { position: relative; }
.input-group.disabled .zen-input { background: #f8f8f8; cursor: not-allowed; opacity: 0.8; }
.input-group .right-icon { left: auto; right: 15px; pointer-events: none; font-size: 0.75rem; color: var(--text-muted); }

/* 隐藏 Select 默认箭头 */
.select-arrow {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 40px !important; /* 为右侧自定义图标留空 */
}

.helper-text { font-size: 0.8rem; color: var(--primary-color); cursor: pointer; margin-top: 5px; display: inline-block; }
.form-footer { margin-top: 30px; display: flex; justify-content: flex-end; }

@media (max-width: 600px) {
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
