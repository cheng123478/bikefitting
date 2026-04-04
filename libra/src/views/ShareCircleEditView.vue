<template>
  <div class="share-edit-page">
    <div class="container page-container">
      <div class="header">
        <h2 class="page-title">缂栬緫鍔ㄦ€?/h2>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="zen-loader"><i class="fas fa-spinner fa-spin"></i></div>
        <span>姝ｅ湪鍔犺浇...</span>
      </div>

      <ShareCircleForm
          v-else
          :initial-data="postData"
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
import ShareCircleForm from '@/views/sharecircle/components/ShareCircleForm.vue';

const route = useRoute();
const router = useRouter();
const isSubmitting = ref(false);
const loading = ref(true);
const postData = ref(null);

const goBack = () => router.back();

onMounted(() => {
  setTimeout(() => {
    postData.value = {
      title: 'Tarmac SL7 濮挎€佸井璋冭褰?,
      desc: '璋冩暣浜嗛攣鐗囦綅缃拰搴у灚鍚庣Щ閲忥紝鑶濈洊鑸掓湇澶氫簡銆?,
      category: 'fitting',
      cover: 'https://images.unsplash.com/photo-1618386477546-f947ee191244?q=80&w=600',
      data: { stack: 540, reach: 385 }
    };
    loading.value = false;
  }, 500);
});

const handleUpdate = (formData) => {
  isSubmitting.value = true;
  console.log('鏇存柊鏁版嵁:', formData);
  setTimeout(() => {
    isSubmitting.value = false;
    alert('淇敼鎴愬姛锛?);
    router.push('/share');
  }, 1000);
};
</script>

<style scoped>
.share-edit-page {
  padding-top: calc(var(--header-height) + 40px);
  padding-bottom: 60px;
  min-height: 100vh;
}
.page-container { max-width: 800px; }
.header { text-align: center; margin-bottom: 30px; }
.page-title {
  font-family: var(--font-serif); font-size: 2rem; color: var(--text-main);
}
.loading-state {
  text-align: center; padding: 50px; color: var(--text-muted);
  display: flex; flex-direction: column; align-items: center; gap: 10px;
}
.zen-loader { font-size: 2rem; color: var(--primary-color); }
</style>