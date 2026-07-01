import request from '@/api/request.js';

/**
 * 更新个人基本信息 (昵称、简介、城市等)
 * @param {Object} data { name, city, bio, ... }
 */
export function updateUserProfile(data) {
    return request({
        url: '/user/edit',
        method: 'put',
        data
    });
}

export function getCurrentUserProfile() {
    return request({
        url: '/user/profile',
        method: 'get'
    });
}

/**
 * 上传头像
 * @param {FormData} formData 包含 file 对象的 FormData
 */
export function uploadAvatar(formData) {
    return request({
        url: '/user/avatar',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data' // 明确标识文件上传
        }
    });
}
