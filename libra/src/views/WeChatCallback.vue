<template>
  <div class="wechat-callback">
    <div class="loading-state">
      <i class="fas fa-spinner fa-spin fa-3x"></i>
      <p>正在微信登录，请稍候...</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { loginByWeChat } from '@/api/auth';
import { showToast } from '@/utils/toast';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

onMounted(async () => {
  const code = route.query.code;
  if (!code) {
    showToast('微信登录失败：未获取到授权码', 'error');
    router.replace('/');
    return;
  }

  try {
    const user = await loginByWeChat({ code, type: 'web' });
    userStore.login(user); // 存储用户信息和 token 到 pinia
    showToast('微信登录成功', 'success');
    router.replace('/');
  } catch (error) {
    console.error('WeChat login failed', error);
    showToast('微信登录失败：' + (error.message || '系统错误'), 'error');
    router.replace('/');
  }
});
</script>

<style scoped>
.wechat-callback {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: var(--bg-main, #f5f7fa);
}
.loading-state {
  text-align: center;
  color: #07C160;
}
.loading-state i {
  margin-bottom: 20px;
}
.loading-state p {
  font-size: 1.2rem;
  color: var(--text-main, #333);
  font-weight: 500;
}
</style>
