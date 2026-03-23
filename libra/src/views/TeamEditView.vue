<template>
  <div class="team-edit-page">
    <div class="container page-container">
      <div class="header">
        <h2 class="page-title">管理骑士团</h2>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="zen-loader"><i class="fas fa-spinner fa-spin"></i></div>
        <span>读取数据中...</span>
      </div>

      <TeamForm
          v-else
          :initial-data="teamData"
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
import TeamForm from '@/components/TeamForm.vue';

const route = useRoute();
const router = useRouter();
const isSubmitting = ref(false);
const loading = ref(true);
const teamData = ref(null);

const goBack = () => router.back();

onMounted(() => {
  setTimeout(() => {
    teamData.value = {
      name: '周末咖啡骑行社',
      slogan: '骑帅不骑快，咖啡第一名。',
      city: '北京',
      level: 'Casual',
      desc: '我们是一个主打休闲骑行的组织...',
      logo: 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?q=80&w=200'
    };
    loading.value = false;
  }, 500);
});

const handleUpdate = (formData) => {
  isSubmitting.value = true;
  console.log('更新车队:', formData);

  setTimeout(() => {
    isSubmitting.value = false;
    alert('设置已保存！');
    router.push('/teams');
  }, 1000);
};
</script>

<style scoped>
.team-edit-page {
  padding-top: calc(var(--header-height) + 40px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.page-container {
  max-width: 800px;
}

.header {
  text-align: center;
  margin-bottom: var(--space-xl);
}

.page-title {
  font-family: var(--font-serif);
  font-size: 2rem;
  color: var(--text-main);
}

.loading-state {
  text-align: center;
  padding: 50px;
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.zen-loader {
  font-size: 2rem;
  color: var(--primary-color);
}
</style>