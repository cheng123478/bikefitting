import request from '@/api/request';

/**
 * 步骤 1: 模糊搜索品牌和型号名 (去重列表)
 * @param {String} keyword 支持品牌、中文品牌、型号的模糊搜索
 */
export const searchModelNames = (keyword) => {
    return request({
        url: '/bike-model/search',
        method: 'get',
        params: { keyword }
    });
};

/**
 * 步骤 2: 获取选定型号的完整“变体树”
 * @param {String} brand 品牌
 * @param {String} model 型号
 * 返回该型号下的所有年份、尺码、及对应的 Stack/Reach 等几何数据
 */
export const getBikeModelTree = (brand, model) => {
    return request({
        url: '/bike-model/tree',
        method: 'get',
        params: { brand, model }
    });
};

/**
 * 保存单车数据 (新增或校准现有数据)
 * @param {Object} data UserBikeInfoDTO
 */
export function saveBikeModel(data) {
    return request({
        url: '/bike-model/save',
        method: 'post',
        data
    });
}