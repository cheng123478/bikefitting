import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { showToast } from '@/utils/toast';
import HomeView from '@/views/HomeView.vue';
import { STORAGE_KEYS } from '@/config/constants.js';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView,
            meta: { title: '首页' }
        },
        {
            path: '/showroom',
            name: 'show-room',
            component: () => import('@/views/ShowroomView.vue'),
            meta: { title: '单车展厅', requiresAuth: false }
        },
        {
            path: '/showroom/:id',
            name: 'bike-detail',
            component: () => import('@/views/BikeDetailView.vue'),
            meta: { title: '单车详情', requiresAuth: false }
        },
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
                {
                    path: '',
                    redirect: '/lab/archives/static'
                }
            ]
        },
        {
            path: '/peloton',
            name: 'peloton',
            component: () => import('@/views/peloton/PelotonView.vue'),
            meta: { title: '破风社', requiresAuth: false }
        },
        {
            path: '/peloton/create',
            name: 'team-create',
            component: () => import('@/views/peloton/CreateTeamView.vue'),
            meta: { title: '创建车队', requiresAuth: true }
        },
        {
            path: '/peloton/:id',
            name: 'team-detail',
            component: () => import('@/views/peloton/TeamDetailView.vue'),
            meta: { title: '车队详情', requiresAuth: false }
        },
        {
            path: '/peloton/:id/activity/create',
            name: 'activity-create',
            component: () => import('@/views/peloton/ActivityCreateView.vue'),
            meta: { title: '发布活动', requiresAuth: true }
        },
        {
            path: '/peloton/:id/activity/edit/:activityId',
            name: 'activity-edit',
            component: () => import('@/views/peloton/ActivityEditView.vue'),
            meta: { title: '编辑活动', requiresAuth: true }
        },
        {
            path: '/peloton/:id/notice/create',
            name: 'notice-create',
            component: () => import('@/views/peloton/NoticeEditView.vue'),
            meta: { title: '发布公告', requiresAuth: true }
        },
        {
            path: '/peloton/:id/notice/edit/:noticeId',
            name: 'notice-edit',
            component: () => import('@/views/peloton/NoticeEditView.vue'),
            meta: { title: '编辑公告', requiresAuth: true }
        },
        {
            path: '/profile',
            name: 'profile',
            component: () => import('@/views/user/ProfileView.vue'),
            meta: { title: '个人中心', requiresAuth: true }
        },
        {
            path: '/messages',
            name: 'message-center',
            component: () => import('@/views/message/MessageCenterView.vue'),
            meta: { title: '消息中心', requiresAuth: true }
        },
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
        {
            path: '/wechat/callback',
            name: 'wechat-callback',
            component: () => import('@/views/WeChatCallback.vue'),
            meta: { title: '微信授权登录中' }
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            redirect: '/'
        }
    ],

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

router.beforeEach((to, from, next) => {
    if (to.query.inviter) {
        localStorage.setItem(STORAGE_KEYS.INVITER_CODE, to.query.inviter);
    }
    const userStore = useUserStore();

    const appTitle = '骑迹';
    document.title = to.meta.title ? `${to.meta.title} | ${appTitle}` : appTitle;

    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
        showToast('请先登录', 'warning');
        userStore.openAuthModal();
        next(false);
    } else {
        next();
    }
});

export default router;
