<template>
  <div class="realtime-guard-page">
    <div class="coming-soon-container">
      <div class="icon-wrapper">
        <i class="fas fa-heartbeat"></i>
      </div>
      <h1 class="title">姿态实时监护</h1>
      <p class="subtitle">敬请期待...</p>
      <p class="desc">手机传感器联动，在长距离骑行过程中监控并预警核心疲劳与代偿姿态。</p>
      <button class="btn-back" @click="goBack">
        <i class="fas fa-arrow-left"></i> 返回实验室
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goBack = () => {
  router.push('/lab');
};
</script>

<style scoped>
.realtime-guard-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.coming-soon-container {
  text-align: center;
  background: white;
  padding: 60px 80px;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  max-width: 500px;
}

.icon-wrapper {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 30px;
  animation: pulse 2s infinite;
}

.icon-wrapper i {
  font-size: 48px;
  color: white;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 20px rgba(102, 126, 234, 0);
  }
}

.title {
  font-size: 2rem;
  color: #333;
  margin-bottom: 10px;
  font-weight: 600;
}

.subtitle {
  font-size: 1.5rem;
  color: #667eea;
  margin-bottom: 20px;
  font-weight: 500;
}

.desc {
  font-size: 1rem;
  color: #666;
  line-height: 1.6;
  margin-bottom: 30px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-back:hover {
  background: #5a6fd6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

@media (max-width: 768px) {
  .coming-soon-container {
    padding: 40px 30px;
  }
  
  .title {
    font-size: 1.5rem;
  }
  
  .subtitle {
    font-size: 1.2rem;
  }
}
</style>
