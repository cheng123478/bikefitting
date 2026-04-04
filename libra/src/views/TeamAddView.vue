<template>
  <div class="team-add-page">
    <div class="container page-container">
      <div class="header">
        <h2 class="page-title">创建骑士团</h2>
        <p class="page-subtitle">建立你的旗帜，召集志同道合的伙伴</p>
      </div>

      <TeamForm
          :is-loading="isSubmitting"
          @submit="handleCreate"
          @cancel="goBack"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import TeamForm from '@/views/team/components/TeamForm.vue';

const router = useRouter();
const isSubmitting = ref(false);

const goBack = () => router.back();

const handleCreate = (formData) => {
  isSubmitting.value = true;
  console.log('创建车队:', formData);

  setTimeout(() => {
    isSubmitting.value = false;
    alert('骑士团创建成功！');
    router.push('/teams');
  }, 1000);
};
</script>

<style scoped>
.team-add-page {
  padding-top: calc(var(--header-height) + 40px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.page-container { max-width: 800px; }

.header { text-align: center; margin-bottom: var(--space-xl); }

.page-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  color: var(--text-main);
  margin-bottom: var(--space-xs);
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 1rem;
}
</style>