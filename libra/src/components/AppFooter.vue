<template>
  <footer id="footer" class="site-footer">
    <div class="container footer-content">
      <!-- 1. 品牌栏 -->
      <div class="footer-col brand-col">
        <h4 class="footer-brand">{{ PROJECT_CONFIG.NAME }}</h4>
        <p class="brand-desc">
          {{ PROJECT_CONFIG.TAGLINE }}
        </p>
      </div>

      <!-- 2. 快速链接 -->
      <div class="footer-col">
        <h4 class="footer-label">快速链接</h4>
        <nav class="footer-nav">
          <a href="#">关于我们</a>
          <a href="#">服务条款</a>
          <a href="#">隐私政策</a>
        </nav>
      </div>

      <!-- 3. 联系方式 -->
      <div class="footer-col">
        <h4 class="footer-label">关注我们</h4>
        <div class="social-links">
          <a :href="'mailto:' + CONTACT_CONFIG.SUPPORT_EMAIL" target="_blank" title="Email"><i class="fas fa-envelope"></i></a>
          <a v-if="CONTACT_CONFIG.GITHUB_REPO" :href="CONTACT_CONFIG.GITHUB_REPO" target="_blank" title="GitHub"><i class="fab fa-github"></i></a>
          <a v-if="CONTACT_CONFIG.OFFICIAL_WEBSITE" :href="CONTACT_CONFIG.OFFICIAL_WEBSITE" target="_blank" title="Website"><i class="fas fa-globe"></i></a>
        </div>
      </div>
    </div>

    <!-- 版权信息 -->
    <div class="copyright-bar">
      <div class="container">
        <p>
          &copy; {{ currentYear }} {{ PROJECT_CONFIG.NAME }}.
          <span>All Rights Reserved.</span>
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { ref } from 'vue';
import { PROJECT_CONFIG, CONTACT_CONFIG } from '@/config/constants';
const currentYear = ref(new Date().getFullYear());
</script>

<style scoped>
.site-footer {
  background-color: var(--bg-page);
  border-top: 1px solid rgba(92, 110, 88, 0.15);
  /* 字体改为深色 */
  color: var(--text-main);
  padding: var(--space-xl) 0 var(--space-sm);
  margin-top: var(--space-xxl);
  font-family: var(--font-sans);
}

.footer-content {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: var(--space-xl);
  margin-bottom: var(--space-lg);
}

.footer-brand {
  font-family: var(--font-serif);
  font-size: 1.8rem;
  color: var(--primary-color);
  margin-bottom: var(--space-sm);
  letter-spacing: -0.01em;
  font-weight: 700;
}

.brand-desc {
  font-family: var(--font-serif);
  font-size: 1rem;
  line-height: 1.7;
  color: var(--text-muted);
  font-style: italic;
}

.footer-label {
  font-size: 0.85rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-muted);
  font-weight: 700;
  margin-bottom: var(--space-md);
  opacity: 0.8;
}

.footer-nav {
  display: flex; flex-direction: column; gap: 12px;
}

.footer-nav a {
  color: var(--text-main); /* 链接平时为深灰 */
  font-size: 0.8rem;
  transition: all 0.3s var(--ease-out-expo);
  width: fit-content;
  position: relative;
}

.footer-nav a:hover {
  color: var(--primary-color);
  transform: translateX(6px);
}

.social-links {
  display: flex; gap: var(--space-sm);
}

.social-links a {
  background: rgba(92, 110, 88, 0.08);
  color: var(--primary-color);
  font-size: 1.2rem;
  transition: all 0.3s var(--ease-out-expo);
  display: flex; align-items: center; justify-content: center;
  width: 42px; height: 42px;
  border-radius: 50%;
}

.social-links a:hover {
  background: var(--primary-color);
  color: #fff;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.3);
}

.copyright-bar {
  border-top: 1px solid rgba(0,0,0,0.05);
  padding-top: var(--space-md);
  margin-top: var(--space-lg);
  text-align: center;
}

.copyright-bar p {
  font-size: 0.8rem;
  color: var(--text-muted);
  opacity: 0.8;
}

@media (max-width: 768px) {
  .footer-content {
    grid-template-columns: 1fr;
    text-align: center;
    gap: var(--space-lg);
  }
  .footer-nav, .social-links {
    align-items: center; justify-content: center;
  }
}
</style>