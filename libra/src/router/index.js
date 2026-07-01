import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { showToast } from '@/utils/toast';
import HomeView from '@/views/HomeView.vue';
import { STORAGE_KEYS } from '@/config/constants.js';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        // --- 基础页面 ---
        {
            path: '/',
            name: 'home',
            component: HomeView,
            meta: { title: '首页' }
        },
        // 单车展厅
        {
            path: '/showroom',
            name: 'show-room',
            component: () => import('@/views/ShowroomView.vue'),
            meta: { title: '单车展厅', requiresAuth: false }
        },
        // 单车详情
        {
            path: '/showroom/:id',
            name: 'bike-detail',
            component: () => import('@/views/BikeDetailView.vue'),
            meta: { title: '单车详情', requiresAuth: false }
        },
        // --- 实验室 模块 ---
        {
            path: '/lab',
            name: 'lab',
            component: () => import('@/views/lab/LabView.vue'),
            meta: { title: '实验室', requiresAuth: false }
        },
        {
            path: '/lab/guide',
            name: 'lab-guide',
            component: () => import('@/views/lab/LabGuideView.vue'),
            meta: { title: '工作流导览', requiresAuth: false }
        },
        {
            path: '/lab/body',
            name: 'lab-body',
            component: () => import('@/views/lab/body/BodyProfileView.vue'),
            meta: { title: '完善身体信息', requiresAuth: true }
        },
        {
            path: '/lab/body/history',
            name: 'lab-body-history',
            component: () => import('@/views/lab/body/BodyHistoryView.vue'),
            meta: { title: '身体档案', requiresAuth: true }
        },
        {
            path: '/lab/body/compare',
            name: 'lab-body-compare',
            component: () => import('@/views/lab/body/BodyCompareView.vue'),
            meta: { title: '对比身体信息', requiresAuth: true }
        },
        {
            path: '/lab/garage',
            name: 'lab-garage',
            component: () => import('@/views/lab/garage/GarageView.vue'),
            meta: { title: '车库', requiresAuth: false }
        },
        {
            path: '/lab/garage/edit',
            name: 'lab-garage-edit',
            component: () => import('@/views/lab/garage/BikeEditView.vue'),
            meta: { title: '维护单车信息', requiresAuth: false }
        },
        {
            path: '/lab/report/static',
            name: 'lab-report-static',
            component: () => import('@/views/lab/static/StaticFittingReportView.vue'),
            meta: { title: '静态Fitting报告', requiresAuth: false }
        },
        {
            path: '/lab/report/dynamic',
            name: 'lab-report-dynamic',
            component: () => import('@/views/lab/dynamic/DynamicFittingReportView.vue'),
            meta: { title: '动态分析报告', requiresAuth: false }
        },
        {
            path: '/lab/static',
            name: 'lab-static-hub',
            component: () => import('@/views/lab/static/StaticFittingHub.vue'),
            meta: { title: '静态诊断引擎', requiresAuth: true }
        },
        {
            path: '/lab/dynamic',
            name: 'lab-dynamic',
            component: () => import('@/views/lab/dynamic/DynamicFittingHub.vue'),
            meta: { title: '动态Fitting', requiresAuth: false }
        },
        {
            path: '/lab/dynamic/capture',
            name: 'lab-dynamic-capture',
            component: () => import('@/views/lab/dynamic/PoseAnalyzer.vue'),
            meta: { hideNavbar: true, requiresAuth: false }
        },
        {
            path: '/lab/dynamic/session/submitted',
            name: 'lab-dynamic-session-submitted',
            component: () => import('@/views/lab/dynamic/DynamicFittingSubmitted.vue'),
            meta: { hideNavbar: true, requiresAuth: false }
        },
        {
            path: '/lab/dynamic/session',
            name: 'lab-dynamic-session',
            component: () => import('@/views/lab/dynamic/DynamicFittingSessionView.vue'),
            meta: { hideNavbar: true, requiresAuth: false }
        },
        {
            path: '/lab/realtime',
            name: 'RealtimeGuard',
            component: () => import('@/views/lab/realtime/RealtimeGuardView.vue'),
            meta: { title: '姿态实时监护', requiresAuth: true }
        },
        {
            path: '/lab/archives',
            component: () => import('@/views/lab/archives/ArchiveLayout.vue'),
            children: [
                {
                    path: 'static',
                    name: 'StaticArchive',
                    component: () => import('@/views/lab/archives/StaticArchiveView.vue'),
                    meta: { title: '静态Fitting报告历史记录', requiresAuth: true }
                },
                {
                    path: 'dynamic',
                    name: 'DynamicArchive',
                    component: () => import('@/views/lab/archives/DynamicArchiveView.vue'),
                    meta: { title: '动态Fitting报告历史记录', requiresAuth: true }
                },
                // {
                //     path: 'realtime',
                //     name: 'RealtimeArchive',
                //     component: () => import('@/views/lab/archives/RealtimeArchiveView.vue'),
                //     meta: { title: '实时监测历史记录', requiresAuth: true }
                // }
                {
                    path: '',
                    redirect: '/lab/archives/static'
                }
            ]
        },
        // --- 用户模块 ---
        {
            path: '/profile',
            name: 'profile',
            component: () => import('@/views/user/ProfileView.vue'),
            meta: { title: '个人中心', requiresAuth: true }
        },
        // --- 法律相关 ---
        {
            path: '/user-agreement',
            name: 'user-agreement',
            component: () => import('@/views/legal/UserAgreementView.vue'),
            meta: { title: '用户协议' }
        },
        {
            path: '/privacy-policy',
            name: 'privacy-policy',
            component: () => import('@/views/legal/PrivacyPolicyView.vue'),
            meta: { title: '隐私政策' }
        },
        // --- 微信授权回调 ---
        {
            path: '/wechat/callback',
            name: 'wechat-callback',
            component: () => import('@/views/WeChatCallback.vue'),
            meta: { title: '微信授权登录中' }
        },
        // --- 404 捕获路由 ---
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            redirect: '/' // 也可以重定向到专门的 404 页面
        }
    ],

    // 滚动行为控制
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition;
        }
        if (to.hash) {
            return { el: to.hash, behavior: 'smooth' };
        }
        return { top: 0, behavior: 'smooth' };
    }
});

// --- 全局路由守卫 ---
router.beforeEach((to, from, next) => {
    if (to.query.inviter) {
        localStorage.setItem(STORAGE_KEYS.INVITER_CODE, to.query.inviter);
    }
    const userStore = useUserStore();

    // 1. 设置网页标题
    const appTitle = '骑迹';
    document.title = to.meta.title ? `${to.meta.title} | ${appTitle}` : appTitle;

    // 2. 鉴权逻辑
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
        showToast('请先登录', 'warning');
        userStore.openAuthModal();
        next(false);
    } else {
        next();
    }
});

export default router;
