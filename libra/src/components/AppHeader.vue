<template>
  <nav class="navbar" :class="{ 'nav-scrolled': isScrolled }">
    <div class="container nav-container">

      <!-- 1. 品牌标识：增强字间距与呼吸感 -->
      <div class="brand-wrapper">
        <router-link to="/" class="brand-link">
          <img src="@/assets/image/logo.png" alt="Logo" class="logo-img">
          <div class="logo-text-group">
            <span class="brand-text">骑迹</span>
            <span class="brand-en-text">RIDETRACE</span>
          </div>
        </router-link>
      </div>

      <!-- 2. 移动端切换器 (保持原逻辑) -->
      <div class="mobile-toggle" @click="toggleMobileMenu">
        <div class="burger-icon" :class="{ 'is-open': isMobileMenuOpen }">
          <span></span><span></span><span></span>
        </div>
      </div>

      <!-- 3. 7大导航模块：优化动态间距 -->
      <ul class="nav-menu" :class="{ 'is-active': isMobileMenuOpen }">
        <li @click="closeMobileMenu">
          <router-link to="/" class="nav-link" active-class="active-link" exact>首页</router-link>
        </li>
        <li @click="closeMobileMenu">
          <router-link to="/showroom" class="nav-link" active-class="active-link">单车展厅</router-link>
        </li>
        <li @click="closeMobileMenu">
          <router-link to="/lab" class="nav-link" :class="{ 'active-link': $route.path.startsWith('/lab') }">实验室</router-link>
        </li>

        <!-- 4. 登录/用户信息区域 (保持原逻辑，优化样式) -->
        <li class="login-item" @click.stop>
          <button v-if="!userStore.isLoggedIn" class="btn-login" @click="openAuthModal">
            <i class="far fa-user-circle"></i>
            <span>登录 / 注册</span>
          </button>

          <div v-else class="user-dropdown-wrapper">
            <div class="user-avatar-btn" @click="toggleUserMenu" :class="{ active: isUserMenuOpen }">
              <img :src="userStore.userInfo.avatar || defaultAvatar" alt="User">
              <span class="user-name">{{ userStore.userInfo.name || '骑行者' }}</span>
              <i class="fas fa-chevron-down arrow"></i>
            </div>

            <transition name="dropdown-fade">
              <div v-if="isUserMenuOpen" class="dropdown-menu glass-card">
                <ul class="menu-list">
                  <li @click="handleMenuClick('/profile')">
                    <i class="fas fa-chart-area"></i> 个人中心
                  </li>
                  <li @click="handleMenuClick('/lab/body')">
                    <i class="fas fa-fingerprint"></i> 身体图谱
                  </li>
                  <li @click="handleMenuClick('/lab/garage')">
                    <i class="fas fa-bicycle"></i> 我的车库
                  </li>
                  <div class="divider"></div>
                  <li @click="handleLogout" class="logout-item">
                    <i class="fas fa-power-off"></i> 退出登录
                  </li>
                </ul>
              </div>
            </transition>
          </div>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { logout } from "@/api/auth.js";
import { showToast, showConfirm } from "@/utils/toast.js";

const emit = defineEmits(['open-auth']);
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const defaultAvatar = '/src/assets/image/default-avatar.png';
const isMobileMenuOpen = ref(false);
const isScrolled = ref(false);
const isUserMenuOpen = ref(false);

const openAuthModal = () => { closeMobileMenu(); emit('open-auth'); };
const toggleMobileMenu = () => isMobileMenuOpen.value = !isMobileMenuOpen.value;
const closeMobileMenu = () => isMobileMenuOpen.value = false;
const toggleUserMenu = () => isUserMenuOpen.value = !isUserMenuOpen.value;
const closeDropdown = () => isUserMenuOpen.value = false;

const handleMenuClick = (path) => {
  router.push(path);
  closeDropdown();
  closeMobileMenu();
};

const handleLogout = async () => {
  const isConfirmed = await showConfirm(`确认要退出"骑迹"并结束本次会话吗？`, '安全退出');
  if (isConfirmed) {
    logout().then(() => {
      userStore.logout();
      closeDropdown();
      closeMobileMenu();
      router.push('/');
    }).catch(error => {
      showToast('注销失败，请稍后重试', 'error');
    });
  }
};

const handleScroll = () => { isScrolled.value = window.scrollY > 30; };

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.user-dropdown-wrapper')) closeDropdown();
  });
});
onUnmounted(() => window.removeEventListener('scroll', handleScroll));
</script>

<style scoped>
/* --- 1. 基础布局 --- */
.navbar {
  height: var(--header-height);
  background: transparent;
  transition: all 0.5s var(--ease-out-expo);
  border-bottom: 1px solid transparent;
}

.nav-scrolled {
  background: rgba(243, 242, 237, 0.85); /* 亚麻白 */
  backdrop-filter: blur(20px) saturate(180%);
  box-shadow: var(--shadow-sm);
  border-bottom: var(--border-glass);
}

.nav-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

/* --- 2. 品牌样式 --- */
.brand-link {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo-img { height: 42px; object-fit: contain; }
.logo-text-group { display: flex; flex-direction: column; line-height: 1; }
.brand-text {
  font-family: var(--font-serif);
  font-weight: 700; font-size: 1.4rem;
  color: var(--text-main); letter-spacing: 0.1em;
}
.brand-en-text {
  font-family: var(--font-sans);
  font-size: 0.65rem; font-weight: 700;
  color: var(--primary-color); letter-spacing: 0.25em;
  margin-top: 4px;
}

/* --- 3. 导航菜单 (呼吸感的关键) --- */
.nav-menu {
  display: flex;
  align-items: center;
  /* 动态间距：在 1rem 到 2.5rem 之间随屏幕宽度变化 */
  gap: clamp(1rem, 2.5vw, 2.5rem);
}

.nav-link {
  font-family: var(--font-sans);
  font-weight: 500; font-size: 1rem;
  color: var(--primary-color);
  position: relative;
  padding: 8px 0;
  transition: color 0.3s ease;
}

/* 呼吸感动画下划线 */
.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0; left: 50%;
  width: 0; height: 2px;
  background: var(--primary-dark);
  transition: all 0.4s var(--ease-out-expo);
  transform: translateX(-50%);
}

.nav-link:hover, .active-link {
  color: var(--primary-dark) !important;
}

.active-link::after {
  width: 100%;
}

/* --- 4. 登录与用户信息 (保持原逻辑) --- */
.btn-login {
  background: var(--primary-color);
  color: #fff; border: none;
  padding: 8px 22px; border-radius: var(--radius-pill);
  font-weight: 600; font-size: 0.9rem;
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; transition: 0.3s;
}
.btn-login:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(92, 110, 88, 0.2);
}

.user-avatar-btn {
  display: flex; align-items: center; gap: 10px;
  padding: 5px 14px 5px 6px; border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease; cursor: pointer;
}
.user-avatar-btn:hover, .user-avatar-btn.active {
  background: #fff;
  box-shadow: var(--shadow-sm);
}
.user-avatar-btn img { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; border: 2px solid #fff; }
.user-name { font-size: 0.9rem; font-weight: 700; color: var(--text-main); }
.arrow { font-size: 0.7rem; color: var(--text-muted); transition: 0.3s; }
.active .arrow { transform: rotate(180deg); }

/* --- 5. 下拉菜单 --- */
.dropdown-menu {
  position: absolute; top: 55px; right: 0;
  width: 200px; padding: 8px;
  background: #fff; border: var(--border-subtle);
  transform-origin: top right;
  z-index: var(--z-modal);
}
.menu-list li {
  padding: 12px 16px; display: flex; align-items: center; gap: 12px;
  font-size: 0.95rem; color: var(--text-body);
  border-radius: var(--radius-sm); cursor: pointer; transition: 0.2s;
}
.menu-list li:hover { background: var(--bg-page); color: var(--primary-color); }
.divider { height: 1px; background: rgba(0, 0, 0, 0.05); margin: 6px 0; }
.logout-item { color: var(--danger-color) !important; }

/* --- 6. 响应式微调 --- */
@media (max-width: 1024px) {
  .nav-link { font-size: 0.95rem; }
  .brand-text { display: none; }
}

@media (max-width: 768px) {
  .mobile-toggle { display: block; font-size: 1.5rem; cursor: pointer; }

  .nav-menu {
    position: fixed; top: var(--header-height); left: 0;
    width: 100%; background: var(--bg-page);
    flex-direction: column; padding: 30px 0;
    gap: 1.5rem; box-shadow: var(--shadow-md);
    transform: translateY(-150%); transition: transform 0.5s var(--ease-out-expo);
  }
  .nav-menu.is-active { transform: translateY(0); }

  .login-item { margin-top: 20px; }
}

/* 动画 */
.dropdown-fade-enter-active, .dropdown-fade-leave-active {
  transition: all 0.3s var(--ease-out-expo);
}
.dropdown-fade-enter-from, .dropdown-fade-leave-to {
  opacity: 0; transform: translateY(10px) scale(0.95);
}
</style>