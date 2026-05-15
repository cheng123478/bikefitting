import request from '@/api/request'

/**
 * 静态Fitting
 * @param bikeId 单车主键ID
 * @param painLevels 痛感标签列表
 */
export function staticFitting(bikeId, painLevels = []) {
    return request({
        url: `/static-fitting/calculate`,
        method: 'post',
        data: {
            userBike: bikeId ? { id: bikeId } : null,
            targetType: null,
            painLevels: painLevels
        }
    });
}

/**
 * 获取静态Fitting报告
 * @param bikeId
 * @param config 额外配置（如 { silent: true }）
 * @returns
 */
export function getFittingReport(bikeId, config = {}) {
    return request({
        url: `/static-fitting/report/${bikeId}`,
        method: 'get',
        ...config
    });
}

/**
 * 按报告ID获取特定的静态Fitting报告
 * @param resultId
 * @param config
 * @returns
 */
export function getFittingReportById(resultId, config = {}) {
    return request({
        url: `/static-fitting/results/${resultId}`,
        method: 'get',
        ...config
    });
}

/**
 * 静态Fitting历史
 * @param currentPage
 * @returns
 */
export function getFittingResultHistory(currentPage) {
    return request({
        url: `/static-fitting/history`,
        method: 'get',
        params: { page: currentPage, size: 10 }
    });
}

/**
 * 获取用户的静态Fitting报告总数
 */
export function getStaticFittingCount() {
    return request({
        url: '/static-fitting/count',
        method: 'get'
    }).then(res => {
        console.log('[静态报告] 原始响应:', res);
        return res;
    });
}