import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { showToast } from '@/utils/toast';
import HomeView from '@/views/HomeView.vue';

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
        {
            path: '/survey',
            name: 'survey',
            component: () => import('@/views/SurveyView.vue'),
            meta: { title: 'Fitting 问卷' }
        },
        {
            path: '/showroom',
            name: 'show-room',
            component: () => import('@/views/SurveyView.vue'),
            meta: { title: '单车展厅', requiresAuth: true }
        },
        // --- 实验室 模块 ---
        {
            path: '/lab',
            name: 'lab',
            component: () => import('@/views/lab/LabView.vue'),
            meta: { title: '实验室', requiresAuth: false }
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
            // 为兼容旧路径，可以做一个通用跳转或保留
            path: '/lab/report',
            redirect: (to) => {
                if (to.query.type === 'dynamic') return '/lab/report/dynamic';
                return '/lab/report/static';
            }
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
            path: '/lab/dynamic/entrance',
            name: 'lab-dynamic-entrance',
            component: () => import('@/views/DynamicFittingView.vue'),
            meta: { title: '动态分析准备', requiresAuth: true }
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
        // {
        //     path: '/lab/static/',
        //     name: 'static-fitting',
        //     component: () => import('@/views/StaticFittingView.vue'),
        //     meta: { title: '静态分析历史', requiresAuth: true }
        // },
        // {
        //     path: '/lab/dynamic',
        //     name: 'dynamic-fitting',
        //     component: () => import('@/views/DynamicFittingView.vue'),
        //     meta: { title: '动态分析', requiresAuth: true }
        // },
        {
            path: '/lab/realtime',
            name: 'realtime-fitting',
            component: () => import('@/views/RealTimeFittingView.vue'),
            meta: { title: '实时监测', requiresAuth: true }
        },

        // --- 分享圈模块 ---
        {
            path: '/logbook',
            name: 'logbook',
            component: () => import('@/views/ShareCircleView.vue'),
            meta: { title: '骑行圈' }
        },
        {
            path: '/logbook/:id',
            name: 'logbook-detail',
            component: () => import('@/views/ShareCircleDetailView.vue'),
            meta: { title: '动态详情' }
        },
        {
            path: '/logbook/add',
            name: 'logbook-add',
            component: () => import('@/views/ShareCircleAddView.vue'),
            meta: { title: '发布动态', requiresAuth: true }
        },
        {
            path: '/logbook/edit/:id',
            name: 'logbook-edit',
            component: () => import('@/views/ShareCircleEditView.vue'),
            meta: { title: '编辑动态', requiresAuth: true }
        },

        // --- 活动模块 ---
        {
            path: '/rally',
            name: 'rally',
            component: () => import('@/views/ActivityView.vue'),
            meta: { title: '活动中心' }
        },
        {
            path: '/rally/:id',
            name: 'rally-detail',
            component: () => import('@/views/ActivityDetailView.vue'),
            meta: { title: '活动详情' }
        },
        {
            path: '/rally/add',
            name: 'rally-add',
            component: () => import('@/views/ActivityAddView.vue'),
            meta: { title: '发起活动', requiresAuth: true }
        },
        {
            path: '/rally/edit/:id',
            name: 'rally-edit',
            component: () => import('@/views/ActivityEditView.vue'),
            meta: { title: '编辑活动', requiresAuth: true }
        },

        // --- 骑士团模块 ---
        {
            path: '/peloton',
            name: 'peloton',
            component: () => import('@/views/TeamView.vue'),
            meta: { title: '骑士团' }
        },
        {
            path: '/peloton/:id',
            name: 'peloton-detail',
            component: () => import('@/views/TeamDetailView.vue'),
            meta: { title: '车队详情' }
        },
        {
            path: '/peloton/add',
            name: 'peloton-add',
            component: () => import('@/views/TeamAddView.vue'),
            meta: { title: '创建车队', requiresAuth: true }
        },
        {
            path: '/peloton/edit/:id',
            name: 'peloton-edit',
            component: () => import('@/views/TeamEditView.vue'),
            meta: { title: '管理车队', requiresAuth: true }
        },

        // --- 用户模块 ---
        {
            path: '/profile',
            name: 'profile',
            component: () => import('@/views/user/ProfileView.vue'),
            meta: { title: '个人中心', requiresAuth: true }
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