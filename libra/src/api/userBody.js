import request from '@/api/request';

/**
 * 用户身体档案相关 API
 * 对应后端 UserBodyController
 */

/**
 * 保存/更新身体档案
 * @param {Object} data 身体测量 DTO 数据
 */
export function saveUserBody(data) {
    return request({
        url: '/user-body/save',
        method: 'post',
        data
    });
}

/**
 * 获取用户最新的身体档案（画像报告）
 */
export function getLatestUserBody() {
    return request({
        url: `/user-body/latest`,
        method: 'get'
    });
}

/**
 * 获取用户身体档案历史列表
 *
 */
export function getUserBodyHistory(page = 0, size = 10) {
    return request({
        url: `/user-body/history`,
        method: 'get',
        params: {
            page: page,
            size: size
        }
    });
}

/**
 * 深度对比两条记录
 * @param {number} id1
 * @param {number} id2
 */
export function compareUserBody(id1, id2) {
    return request({
        url: '/user-body/compare',
        method: 'get',
        params: { id1, id2 }
    });
}