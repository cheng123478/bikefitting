/**
 * @file request.js
 * @description 核心 API 服务层，支持自动 Token 刷新（无感刷新）。
 */
import axios from 'axios';
import { useUserStore } from '@/stores/user.js';
import { showToast } from '@/utils/toast.js';
import router from '@/router/index.js';
import { BIZ_CODE, HTTP_STATUS_MSG } from '@/config/constants';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/aries';
const TIMEOUT = 15000;

// Token 刷新状态
let isRefreshing = false;
let retryQueue = [];

/**
 * 创建具有基础配置的 Axios 实例。
 */
const service = axios.create({
    baseURL: BASE_URL,
    timeout: TIMEOUT,
    headers: {
        'Content-Type': 'application/json;charset=utf-8'
    }
});

// ================= 请求拦截器 =================
service.interceptors.request.use(
    (config) => {
        const userStore = useUserStore();
        if (userStore.accessToken) {
            // 确保统一添加 Bearer 前缀
            const token = userStore.accessToken.startsWith('Bearer ')
                ? userStore.accessToken
                : `Bearer ${userStore.accessToken}`;
            config.headers['Authorization'] = token;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// ================= 响应拦截器 =================
service.interceptors.response.use(
    async (response) => {
        const { config, data: res } = response;

        // 二进制数据直接放行
        if (config.responseType === 'blob' || config.responseType === 'arraybuffer') {
            return response;
        }

        // 标准业务成功
        if (res.code === BIZ_CODE.SUCCESS) {
            return res.data;
        }

        // 业务错误处理
        switch (res.code) {
            case BIZ_CODE.AUTH_TOKEN_EXPIRED:
                return handleTokenExpired(config);
            case BIZ_CODE.AUTH_TOKEN_INVALID:
            case BIZ_CODE.AUTH_SESSION_MISSING:
                performFullLogout('登录会话已过期，请重新登录');
                return Promise.reject(new Error(res.message));
            default:
                // 静默模式允许组件局部处理特定的错误
                if (!config.silent) {
                    showToast(res.message || '系统错误', 'error');
                }
                return Promise.reject(new Error(res.message || '未知错误'));
        }
    },
    async (error) => {
        const { config, response } = error;

        // 处理标准 HTTP 401 未授权
        if (response && response.status === 401) {
            const bizCode = response.data?.code;
            if (bizCode === BIZ_CODE.AUTH_TOKEN_EXPIRED) {
                return handleTokenExpired(config);
            } else {
                performFullLogout(response.data?.message || HTTP_STATUS_MSG[401]);
                return Promise.reject(error);
            }
        }

        // 通用网络/服务器错误处理
        const message = error.response?.data?.message || HTTP_STATUS_MSG[error.response?.status] || '网络连接失败';
        if (!config?.silent) showToast(message, 'error');
        return Promise.reject(error);
    }
);

/**
 * 执行完全的应用级登出。
 * 清除状态并重定向到首页。
 */
function performFullLogout(message = '登录已过期') {
    const userStore = useUserStore();
    showToast(message, 'error');
    userStore.logout();
    router.replace('/');

    // 清除刷新状态，为下一次尝试做准备
    isRefreshing = false;
    retryQueue = [];
}

/**
 * 编排针对过期访问令牌的“无感刷新”流程。
 * 在获取新令牌时将传入的请求挂起在队列中。
 */
async function handleTokenExpired(originalConfig) {
    const userStore = useUserStore();
    const refreshToken = userStore.refreshToken;

    // 如果刷新令牌本身缺失或刷新尝试失败，防止死循环
    if (!refreshToken || originalConfig.url.includes('/user/refresh')) {
        performFullLogout();
        return Promise.reject(new Error('认证失败'));
    }

    if (!isRefreshing) {
        isRefreshing = true;
        try {
            const refreshResponse = await axios.post(
                `${BASE_URL}/user/refresh`, { refreshToken });

            if (refreshResponse.data.code === BIZ_CODE.SUCCESS) {
                const { accessToken, refreshToken: newRefreshToken, user } = refreshResponse.data.data;
                userStore.refreshSession(accessToken, newRefreshToken, user);

                // 解决队列中挂起的请求
                retryQueue.forEach((callback) => callback(accessToken));
                retryQueue = [];

                // 将新令牌应用于原始失败的请求
                originalConfig.headers['Authorization'] = accessToken.startsWith('Bearer ') ? accessToken : `Bearer ${accessToken}`;
                return service(originalConfig);
            } else {
                throw new Error('Token 刷新业务失败');
            }
        } catch (err) {
            performFullLogout('认证已过期，请重新登录');
            return Promise.reject(err);
        } finally {
            isRefreshing = false;
        }
    } else {
        // 刷新期间将后续请求排队
        return new Promise((resolve, reject) => {
            retryQueue.push((token) => {
                originalConfig.headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
                service(originalConfig).then(resolve).catch(reject);
            });
        });
    }
}

export default service;
