<template>
  <div class="create-team-page">
    <div class="container">
      <header class="create-header">
        <button class="back-btn" @click="handleBack">
          <i class="fas fa-arrow-left"></i> 返回
        </button>
        <h1 class="page-title serif-title">创建车队</h1>
      </header>

      <div class="placeholder-card glass-card">
        <div class="placeholder-icon">
          <i class="fas fa-users-cog"></i>
        </div>
        <h2 class="placeholder-title">创建车队功能开发中</h2>
        <p class="placeholder-desc">
          我们正在精心打磨车队创建流程，确保每位队长都能获得最佳体验。<br/>
          敬请期待！
        </p>
        <button class="placeholder-btn" @click="handleBack">
          <i class="fas fa-arrow-left"></i> 返回破风社
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const handleBack = () => {
  router.push('/peloton');
};
</script>

<style scoped>
.create-team-page {
  padding: 140px 0 120px;
  min-height: 100vh;
}

.create-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 48px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 30px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  color: var(--text-main);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
}

.page-title {
  font-family: var(--font-serif);
  font-size: 2.4rem;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}

/* --- 占位卡片 --- */
.placeholder-card {
  text-align: center;
  padding: 80px 40px;
  max-width: 520px;
  margin: 0 auto;
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(92, 110, 88, 0.12) 0%, rgba(92, 110, 88, 0.04) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 28px;
}

.placeholder-icon i {
  font-size: 2rem;
  color: var(--primary-color);
}

.placeholder-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 12px 0;
}

.placeholder-desc {
  font-size: 0.95rem;
  color: var(--text-muted);
  line-height: 1.8;
  margin: 0 0 36px 0;
}

.placeholder-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  border-radius: 30px;
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(92, 110, 88, 0.25);
}

.placeholder-btn:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(92, 110, 88, 0.35);
}

@media (max-width: 768px) {
  .create-team-page {
    padding: 110px 0 80px;
  }

  .page-title {
    font-size: 1.8rem;
  }

  .placeholder-card {
    padding: 60px 24px;
  }
}
</style>
