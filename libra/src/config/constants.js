/**
 * constants.js
 * 全局常量配置文件
 */

export const STORAGE_KEYS = {
    USER_INFO: 'user_info',
    ACCESS_TOKEN: 'access_token',
    REFRESH_TOKEN: 'refresh_token',
    INVITER_CODE: 'inviter_code'
};

export const BIZ_CODE = {
    SUCCESS: 200,
    AUTH_TOKEN_EXPIRED: 40101,      // 令牌过期
    AUTH_TOKEN_INVALID: 40102,      // 令牌无效
    AUTH_SESSION_MISSING: 40103,    // 会话失效
    UNAUTHORIZED: 401,              // 未授权
    MEMBERSHIP_REQUIRED: 40301,     // 会员权限不足（业务自定义，HTTP 403）
};

export const HTTP_STATUS_MSG = {
    400: '请求参数错误',
    401: '未授权，请重新登录',
    403: '拒绝访问，没有权限',
    404: '请求资源不存在',
    500: '服务器内部错误'
};
