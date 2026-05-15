import request from '@/api/request';

/**
 * 获取单车展厅列表
 * @param {Object} params 查询参数
 * @param {String} params.keyword 搜索关键词
 * @param {String} params.bikeTypes 单车类型筛选（支持多个，逗号分隔）
 * @param {String} params.brands 品牌筛选（支持多个，逗号分隔）
 * @param {String} params.sortBy 排序方式
 * @param {Number} params.page 页码
 * @param {Number} params.size 每页数量
 */
export function getShowroomBikes(params) {
    return request({
        url: '/bike-model/showroom',
        method: 'get',
        params
    });
}

/**
 * 获取单车详情
 * @param {Long} id 单车ID
 */
export function getBikeDetail(id) {
    return request({
        url: `/bike-model/${id}`,
        method: 'get'
    });
}

/**
 * 获取同一车型的所有尺寸数据
 * @param {String} modelName 车型名称
 */
export function getBikeByModel(modelName) {
    return request({
        url: `/bike-model/model/${encodeURIComponent(modelName)}`,
        method: 'get'
    });
}