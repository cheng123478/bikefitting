<template>
  <div class="add-activity-page">
    <div class="container page-container">

      <!-- 页面头部 -->
      <div class="header">
        <h2 class="page-title">{{ teamInfo ? '发布车队活动' : '发起新活动' }}</h2>

        <p v-if="teamInfo" class="page-subtitle">
          正在为 <span class="team-highlight">{{ teamInfo.name }}</span> 召集伙伴
        </p>
        <p v-else class="page-subtitle">召集伙伴，开启一段新的旅程</p>
      </div>

      <!-- 表单组件 -->
      <ActivityForm
          :is-loading="isSubmitting"
          :team-info="teamInfo"
          @submit="handleCreate"
          @cancel="goBack"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import ActivityForm from '@/views/activity/components/ActivityForm.vue';

const router = useRouter();
const route = useRoute();
const isSubmitting = ref(false);

const goBack = () => router.back();

const teamInfo = computed(() => {
  if (route.query.teamId) {
    return {
      id: route.query.teamId,
      name: route.query.teamName || '未知车队'
    };
  }
  return null;
});

const handleCreate = (formData) => {
  isSubmitting.value = true;
  const payload = { ...formData, teamId: teamInfo.value?.id || null };
  console.log('提交活动数据:', payload);

  setTimeout(() => {
    isSubmitting.value = false;
    alert('活动发布成功！需等待管理员审核。');
    router.push('/activity');
  }, 1000);
};
</script>

<style scoped>
.add-activity-page {
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
  line-height: 1.1;
}

.page-subtitle { color: var(--text-muted); font-size: 1rem; }

/* 高亮车队名称 */
.team-highlight {
  color: var(--primary-color);
  font-weight: 600;
  background: rgba(92, 110, 88, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}
</style>