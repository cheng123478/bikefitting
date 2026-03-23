<template>
  <div class="share-add-page">
    <div class="container page-container">
      <div class="header">
        <h2 class="page-title">发布新动态</h2>
        <p class="page-subtitle">记录骑行瞬间，分享 Fitting 数据</p>
      </div>

      <ShareCircleForm
          :is-loading="isSubmitting"
          :activity-info="linkedActivity"
          @submit="handleCreate"
          @cancel="goBack"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import ShareCircleForm from '@/components/ShareCircleForm.vue';
import { showToast } from '@/utils/toast';

const router = useRouter();
const route = useRoute();
const isSubmitting = ref(false);

const goBack = () => router.back();

const linkedActivity = computed(() => {
  if (route.query.activityId) {
    return {
      id: route.query.activityId,
      title: route.query.activityTitle
    };
  }
  return null;
});

const handleCreate = (formData) => {
  isSubmitting.value = true;
  console.log('发布数据:', formData);

  setTimeout(() => {
    isSubmitting.value = false;
    showToast('发布成功！', 'success');
    router.push('/sharecircle');
  });
};
</script>

<style scoped>
.share-add-page {
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