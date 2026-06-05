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
 * @param {String} brand 品牌名称（可选，用于精确匹配）
 */
export function getBikeByModel(modelName, brand) {
    return request({
        url: `/bike-model/model/${encodeURIComponent(modelName)}`,
        method: 'get',
        params: brand ? { brand } : {}
    });
}

/**
 * 智能推荐单车
 * @param {Object} params 推荐参数
 * @param {Array} params.scenes 使用场景列表
 * @param {Number} params.minPrice 最低价格（可选）
 * @param {Number} params.maxPrice 最高价格（可选）
 */
export function recommendBikes(params) {
    return request({
        url: '/bike-model/recommend',
        method: 'post',
        data: params
    });
}

/**
 * 切换单车收藏状态
 * 首次调用会自动检查收藏状态并切换
 * @param {Long} bikeId 单车ID
 * @returns {Promise<Boolean>} 返回当前是否已收藏
 */
export async function toggleFavorite(bikeId) {
    // 先检查是否已收藏
    const statusRes = await request({
        url: `/favorite/bike-model/${bikeId}/status`,
        method: 'get'
    });
    const isFavorited = statusRes.data !== undefined ? statusRes.data : statusRes;
    if (isFavorited) {
        // 已收藏，取消收藏
        await request({
            url: `/favorite/bike-model/${bikeId}`,
            method: 'delete'
        });
        return false;
    } else {
        // 未收藏，添加收藏
        await request({
            url: '/favorite/bike-model',
            method: 'post',
            data: { bikeModelId: bikeId }
        });
        return true;
    }
}
