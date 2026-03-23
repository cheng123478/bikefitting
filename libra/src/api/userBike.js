import request from '@/api/request';

/**
 * 获取我的现役单车列表
 */
export function getMyGarage() {
    return request({
        url: '/user-bike/list/actived',
        method: 'get'
    });
}

/**
 * 获取我的退役单车列表
 */
export function getRetiredGarage() {
    return request({
        url: '/user-bike/list/retired',
        method: 'get'
    });
}

/**
 * 保存单车数据 (新增或校准现有数据)
 * @param {Object} data UserBikeInfoDTO
 */
export function saveUserBike(data) {
    return request({
        url: '/user-bike/save',
        method: 'post',
        data
    });
}

/**
 * 上传单车图片
 * @param {FormData} formData 包含 file 对象的 FormData
 */
export function uploadBikePic(formData) {
    return request({
        url: '/user-bike/upload/pic',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data' // 明确标识文件上传
        }
    });
}

/**
 * 获取单车详情规格
 * @param {Long} id 单车主键ID
 */
export function getBikeDetail(id) {
    return request({
        url: `/user-bike/${id}`,
        method: 'get'
    });
}

/**
 * 单车退役 (软删除)
 * @param {Long} id 单车主键ID
 */
export function retireUserBike(id) {
    return request({
        url: `/user-bike/${id}`,
        method: 'delete'
    });
}

/**
 * 恢复已单车退役)
 * @param {Long} id 单车主键ID
 */
export function restoreUserBike(id) {
    return request({
        url: `/user-bike/${id}`,
        method: 'put'
    });
}