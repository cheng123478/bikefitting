import request from '@/api/request';

// ============ 消息中心 API ============

/**
 * 获取通知列表（分页）
 * @param {Object} params - 查询参数
 * @param {String} [params.category] - 分类筛选: team/interact/system，不传则全部
 * @param {Number} [params.page] - 页码（从1开始）
 * @param {Number} [params.size] - 每页条数
 */
export function getNotifications(params) {
  return request({
    url: '/notifications',
    method: 'get',
    params
  });
}

/**
 * 获取各分类未读消息数量
 */
export function getUnreadCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  });
}

/**
 * 标记单条通知为已读
 * @param {Number} notificationId - 通知ID
 */
export function markAsRead(notificationId) {
  return request({
    url: `/notifications/${notificationId}/read`,
    method: 'put'
  });
}

/**
 * 全部标记为已读
 */
export function markAllAsRead() {
  return request({
    url: '/notifications/read-all',
    method: 'put'
  });
}
