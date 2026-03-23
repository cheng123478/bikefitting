import request from '@/api/request.js';

/**
 * 发送短信验证码
 * @param {string} phone 手机号
 */
export function sendSmsCode(phone) {
    return request.get('/sms/send', {
        params: {
            phone: phone
        }
    });
}

/**
 * 用户注册/登录 (短信验证码模式)
 * @param {Object} data - 包含 { phone, code }
 */
export function registerUser(data) {
    // 对应后端 SmsCodeAuthenticationFilter 处理的路径
    return request.post('/user/login/sms', data);
}

/**
 * 微信/小程序 登录
 * @param {Object} data - 包含 { code, type: 'web'|'mini' }
 */
export function loginByWeChat(data) {
    return request.post('/user/login/wechat', data);
}

/**
 * 修改/设置密码 (通过验证码)
 * @param {Object} data - 包含 { code, password } (密码建议已加密)
 */
export function resetPassword(data) {
    return request.post('/user/password/add/code', data);
}

/**
 * 账号密码登录
 * @param {Object} data - 包含 { phone, password }
 */
export function loginByPassword(data) {
    const params = new URLSearchParams();
    params.append('phone', data.phone);
    params.append('password', data.password);
    return request.post('/user/login', params, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
}

/**
 * 更换绑定手机号
 * @param {Object} data { newPhone, code, password }
 */
export function changeUserPhone(data) {
    return request({
        url: '/user/phone/edit',
        method: 'post',
        data
    });
}

/**
 * 退出登录
 */
export function logout() {
    return request.get('/user/logout');
}

/**
 * 申请注销账号
 * @param {Object} data { password } 验证当前密码
 */
export function deleteUserAccount(data) {
    return request({
        url: '/user/delete',
        method: 'post',
        data
    });
}