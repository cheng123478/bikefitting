/**
 * constants.js
 * 全局常量配置文件
 */

export const BASE_URL = 'http://127.0.0.1:9200';

export const HTTP_STATUS_MSG = {
    400: '请求参数错误',
    401: '未授权，请重新登录',
    403: '拒绝访问，没有权限',
    404: '请求资源不存在',
    500: '服务器内部错误'
};

export const BIZ_CODE = {
    SUCCESS: 200,
    AUTH_TOKEN_EXPIRED: 40101,      // 令牌过期
    AUTH_TOKEN_INVALID: 40102,      // 令牌无效
    AUTH_SESSION_MISSING: 40103,    // 会话失效
    MEMBERSHIP_REQUIRED: 40301     // 会员权限不足（业务自定义，HTTP 403）
};

export const STORAGE_KEYS = {
    USER_INFO: 'user_info',
    ACCESS_TOKEN: 'access_token',
    REFRESH_TOKEN: 'refresh_token',
    INVITER_CODE: 'inviter_code'
};

// 项目基础配置
export const PROJECT_CONFIG = {
    NAME: '骑迹',
    NAME_EN: 'RideTrace',
    MASCOT: 'Aries', // 吉祥物名称
    TAGLINE: '专业、高效、智能的自行车调适系统',
    DESCRIPTION: '骑迹是一个致力于提供专业、智能的自行车调适建议的开源项目。如果您喜欢它，可以通过打赏支持服务器带宽与算法优化。',
    VERSION: '1.0.0',
    COPYRIGHT: `© ${new Date().getFullYear()} 骑迹`
};

// 奖励与邀请配置
export const REWARD_CONFIG = {
    INVITE_FOR_TOKEN: 1,         // 每邀请 1 人奖励的分析额度
    INVITE_THRESHOLD_PRO: 7,     // 升级 PRO 所需的邀请人数
    PRO_DURATION_DAYS: 30        // 达到门槛后赠送的 PRO 天数
};

// 联系与社交配置
export const CONTACT_CONFIG = {
    SUPPORT_EMAIL: 'support@bikefitting.ai',
    WECHAT_ID: 'bikefitting_helper',
    OFFICIAL_WEBSITE: 'https://bikefitting.ai',
    GITHUB_REPO: 'https://github.com/example/bikefitting'
};

// UI 表现配置
export const UI_CONFIG = {
    DEFAULT_PAGE_SIZE: 10,
    DATE_FORMAT: 'YYYY-MM-DD',
    DATETIME_FORMAT: 'YYYY-MM-DD HH:mm:ss'
};
