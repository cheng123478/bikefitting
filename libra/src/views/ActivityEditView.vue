<template>
  <div class="edit-activity-page">
    <div class="container page-container">

      <div class="header">
        <h2 class="page-title">编辑活动</h2>
        <p class="page-subtitle">更新活动详情或调整行程安排</p>
      </div>

      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-wrapper">
        <div class="zen-spinner"></div>
        <p>正在读取活动信息...</p>
      </div>

      <!-- 表单组件 -->
      <ActivityForm
          v-else
          :initial-data="activityData"
          :is-loading="isSubmitting"
          @submit="handleUpdate"
          @cancel="goBack"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ActivityForm from '@/components/ActivityForm.vue';

const route = useRoute();
const router = useRouter();

const isSubmitting = ref(false);
const loading = ref(true);
const activityData = ref(null);

const goBack = () => router.back();

onMounted(() => {
  const activityId = route.params.id;
  setTimeout(() => {
    activityData.value = {
      id: activityId,
      title: '龙井爬坡咖啡局',
      time: '2026-10-28T09:00',
      location: '龙井村牌坊',
      maxCount: 15,
      price: 0,
      difficulty: 'L2',
      desc: '休闲爬坡，登顶后喝咖啡。欢迎带朋友一起来。',
      cover: 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?q=80&w=600',
      teamId: 'team_001',
      itinerary: [
        { time: '09:00', title: '集合', detail: '龙井山脚' },
        { time: '10:00', title: '登顶', detail: '翁家山休息' }
      ]
    };
    loading.value = false;
  }, 600);
});

const handleUpdate = (formData) => {
  isSubmitting.value = true;
  console.log('更新活动数据:', formData);
  setTimeout(() => {
    isSubmitting.value = false;
    alert('活动更新成功！');
    router.back();
  }, 1000);
};
</script>

<style scoped>
.edit-activity-page {
  padding-top: calc(var(--header-height) + 40px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.page-container { max-width: 900px; }

.header { text-align: center; margin-bottom: var(--space-xl); }

.page-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  margin-bottom: var(--space-xs);
  color: var(--text-main);
}

.page-subtitle { color: var(--text-muted); }

/* 加载动画 */
.loading-wrapper {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60px 0; color: var(--text-muted); gap: 15px;
}

.zen-spinner {
  width: 40px; height: 40px;
  border: 3px solid rgba(92, 110, 88, 0.1);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>