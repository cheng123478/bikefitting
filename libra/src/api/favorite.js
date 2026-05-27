import request from '@/api/request';

/**
 * 收藏单车车型
 * @param {Long} bikeModelId 单车车型ID
 */
export function favoriteBikeModel(bikeModelId) {
    return request({
        url: '/favorite/bike-model',
        method: 'post',
        data: {
            bikeModelId
        }
    });
}

// 别名函数,保持向后兼容
export function favoriteBikeModelWithSize(bikeModelId, size) {
    return favoriteBikeModel(bikeModelId);
}

/**
 * 取消收藏单车车型
 * @param {Long} bikeModelId 单车车型ID
 */
export function unfavoriteBikeModel(bikeModelId) {
    return request({
        url: `/favorite/bike-model/${bikeModelId}`,
        method: 'delete'
    });
}

/**
 * 检查是否已收藏
 * @param {Long} bikeModelId 单车车型ID
 */
export function checkFavoriteStatus(bikeModelId) {
    return request({
        url: `/favorite/bike-model/${bikeModelId}/status`,
        method: 'get'
    });
}

/**
 * 获取用户收藏列表
 * @param {Object} params 查询参数
 * @param {Number} params.page 页码
 * @param {Number} params.size 每页数量
 */
export function getFavoriteList(params) {
    return request({
        url: '/favorite/list',
        method: 'get',
        params
    });
}

/**
 * 取消收藏(通用)
 * @param {Long} favoriteId 收藏记录ID
 */
export function removeFavorite(favoriteId) {
    return request({
        url: `/favorite/${favoriteId}`,
        method: 'delete'
    });
}

