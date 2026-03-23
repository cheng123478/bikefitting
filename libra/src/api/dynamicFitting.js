import request from '@/api/request'

/**
 * 提交动态 Fitting 会话 (一次性提交多视角数据)
 * @param {Object} payload { bikeId, sessionTime, views: [{ type: 'SIDE', data: {...} }, ...] }
 */
export function submitDynamicSession(payload) {
    return request({
        url: '/dynamic-fitting/sessions',
        method: 'post',
        data: payload
    });
}

/**
 * 获取分析会话详情（包含状态）
 * @param {Long} sessionId
 */
export const getFittingSessionDetail = (sessionId) => {
    return request({
        url: `/dynamic-fitting/sessions/${sessionId}`,
        method: 'get'
    });
};

/**
 * 获取核心诊断结论 (得分、汇总、调整建议)
 * @param {String} sessionId 
 */
export function getDiagnosisResult(sessionId) {
    return request({
        url: `/dynamic-fitting/results/${sessionId}`,
        method: 'get'
    });
}

/**
 * 获取关键帧快照 URL 列表 (视觉证据)
 * @param {String} sessionId
 * @param {String} viewType SIDE | FRONT | BACK
 */
export function getSnapshots(sessionId, viewType) {
    return request({
        url: `/dynamic-fitting/snapshots/${sessionId}/${viewType}`,
        method: 'get'
    });
}

/**
 * 按视角获取海量原始时序数据 (用于 ECharts 深度分析)
 * @param {String} sessionId 
 * @param {String} viewType SIDE | FRONT | BACK
 */
export function getMotionData(sessionId, viewType) {
    return request({
        url: `/dynamic-fitting/motion-data/${sessionId}/${viewType}`,
        method: 'get'
    });
}

/**
 * 获取轻量级会话列表 (历史记录)
 * @param {Object} params { userId, bikeId }
 */
export function getSessionHistory(params) {
    return request({
        url: '/dynamic-fitting/sessions',
        method: 'get',
        params
    });
}

/**
 * 级联删除 Session 关联的所有数据
 * @param {String} sessionId 
 */
export function deleteSession(sessionId) {
    return request({
        url: `/dynamic-fitting/sessions/${sessionId}`,
        method: 'delete'
    });
}
