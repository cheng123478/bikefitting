<template>
  <div class="archives-wrapper">
    <!-- 紧凑的单页眉 -->
    <header class="archives-compact-header">
      <div class="container header-grid">

        <!-- 左侧：标题与状态 -->
        <div class="header-left">
          <span class="en-tag">LAB ARCHIVES</span>
          <h1 class="page-title">实验室档案室</h1>
          <p class="page-subtitle" v-if="$route.path.includes('static')">记录每一阶段的骑行生物力学演变</p>
          <p class="page-subtitle" v-if="$route.path.includes('dynamic')">AI 视觉运动轨迹历史存档</p>
        </div>

        <!-- 中间：扁平化 Tab (不再是浮动的) -->
        <nav class="header-center">
          <div class="capsule-nav">
            <router-link to="/lab/archives/static" class="nav-item" active-class="active">静态几何</router-link>
            <router-link to="/lab/archives/dynamic" class="nav-item" active-class="active">动态视觉</router-link>
            <router-link to="/lab/archives/realtime" class="nav-item" active-class="active">实时日志</router-link>
          </div>
        </nav>

        <!-- 右侧：唯一退出按钮 -->
        <div class="header-right">
          <button class="btn btn-outline back-btn" @click="$router.push('/lab')">
            <i class="fas fa-arrow-left"></i> 返回控制台
          </button>
        </div>
      </div>
    </header>

    <!-- 内容区域 -->
    <main class="archives-content">
      <router-view v-slot="{ Component, route }">
        <transition name="fade-quick" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.archives-wrapper {
  min-height: 100vh;
  background-color: transparent;
  padding-top: 80px; /* 对应主导航高度 */
}

/* 紧凑型页眉 */
.archives-compact-header {
  background: transparent;
  backdrop-filter: blur(10px);
  padding: 30px 0 15px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  position: sticky;
  top: 80px;
  z-index: 100;
}

.header-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1.2fr; /* 三列等高布局 */
  align-items: center;
}

/* 左侧标题 */
.en-tag { font-size: 0.65rem; letter-spacing: 2px; color: var(--primary-color); font-weight: 800; display: block; }

/* 中间导航：胶囊设计 */
.header-center { display: flex; justify-content: center; }
.capsule-nav {
  background: rgba(0,0,0,0.05);
  padding: 4px;
  border-radius: 30px;
  display: flex;
  gap: 4px;
}
.nav-item {
  padding: 8px 20px;
  border-radius: 25px;
  text-decoration: none;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-muted);
  transition: 0.3s;
}
.nav-item.active {
  background: var(--primary-color);
  color: white;
  box-shadow: 0 4px 10px rgba(92, 110, 88, 0.2);
}

/* 右侧退出 */
.header-right { display: flex; justify-content: flex-end; }
.close-circle-btn {
  width: 44px; height: 44px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: white;
  color: var(--text-muted);
  cursor: pointer;
  transition: 0.3s;
}
.close-circle-btn:hover {
  background: #eee;
  color: var(--text-main);
  transform: rotate(90deg);
}

/* 动画切换 */
.fade-quick-enter-active, .fade-quick-leave-active { transition: opacity 0.2s; }
.fade-quick-enter-from, .fade-quick-leave-to { opacity: 0; }

@media (max-width: 1024px) {
  .header-grid { grid-template-columns: 1fr; gap: 20px; text-align: center; }
  .header-right { display: none; } /* 移动端靠顶部导航返回 */
}
</style>