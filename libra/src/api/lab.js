import request from '@/api/request';

/**
 * 获取实验室控制台聚合数据 (Dashboard)
 * 包含：身体概览、车辆统计、报告状态、完整度评分
 */
export function getLabDashboard() {
    return request({
        url: '/lab/dashboard',
        method: 'get'
    });
}

/**
 * 获取痛感选项列表
 */
export function getPainOptions() {
    return request({
        url: `/lab/pain-options`,
        method: 'get'
    });
}