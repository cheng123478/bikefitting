import request from '@/api/request';

// ============ API 函数 ============

/**
 * 获取车队车库
 * @param {Number} teamId - 车队ID
 */
export function getTeamGarage(teamId) {
  return request({
    url: `/team/${teamId}/garage`,
    method: 'get'
  });
}

/**
 * 将个人车库单车录入车队车库
 * @param {Number} teamId - 车队ID
 * @param {Number} bikeId - 个人车库单车ID
 */
export function addBikeToTeamGarage(teamId, bikeId) {
  return request({
    url: `/team/${teamId}/garage`,
    method: 'post',
    data: { bikeId }
  });
}

/**
 * 从车队车库移除单车
 * @param {Number} teamId - 车队ID
 * @param {Number} bikeId - 单车ID
 */
export function removeBikeFromTeamGarage(teamId, bikeId) {
  return request({
    url: `/team/${teamId}/garage/${bikeId}`,
    method: 'delete'
  });
}

/**
 * 获取车队列表
 * @param {Object} params
 * @param {Number} params.page - 页码
 * @param {Number} params.size - 每页数量
 * @param {String} params.keyword - 搜索关键词
 * @param {String} params.category - 分类: hot / nearby / new / mine
 */
export function getTeamList(params = {}) {
  return request({
    url: '/team/list',
    method: 'get',
    params
  });
}

/**
 * 获取车队详情
 * @param {Number} id - 车队ID
 */
export function getTeamDetail(id) {
  return request({
    url: `/team/${id}`,
    method: 'get'
  });
}

/**
 * 获取车队成员列表（分页 + 关键词搜索）
 * @param {Number} teamId - 车队ID
 * @param {Object} [params] - 查询参数
 * @param {Number} [params.page=1] - 页码
 * @param {Number} [params.size=20] - 每页数量
 * @param {String} [params.keyword] - 搜索关键词（匹配昵称或骑行标签）
 */
export function getTeamMembers(teamId, params = {}) {
  return request({
    url: `/team/${teamId}/members`,
    method: 'get',
    params
  });
}

/**
 * 获取我的车队（我创建的 + 我加入的）
 */
export function getMyTeams() {
  return request({
    url: '/team/mine',
    method: 'get'
  });
}

/**
 * 创建车队
 * @param {Object} data - 车队信息
 * @param {String} data.name - 车队名称（必填）
 * @param {String} data.location - 所在地
 * @param {String} data.description - 简介
 * @param {Array<String>} data.tags - 标签列表
 * @param {String} data.avatar - 头像 URL
 * @param {String} data.coverImage - 封面图 URL
 */
export function createTeam(data) {
  return request({
    url: '/team',
    method: 'post',
    data,
    silent: true
  });
}

/**
 * 编辑车队信息（仅创建者可操作）
 * @param {Number} teamId - 车队ID
 * @param {Object} data - 车队信息（同 createTeam）
 */
export function updateTeam(teamId, data) {
  return request({
    url: `/team/${teamId}`,
    method: 'put',
    data,
    silent: true
  });
}

/**
 * 上传车队图片（头像或封面）
 * @param {FormData} formData - 包含 file 对象的 FormData
 */
export function uploadTeamImage(formData) {
  return request({
    url: '/team/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 解散车队（仅创建者可操作）
 * @param {Number} teamId - 车队ID
 */
export function disbandTeam(teamId) {
  return request({
    url: `/team/${teamId}`,
    method: 'delete',
    silent: true
  });
}

/**
 * 申请加入车队
 * @param {Number} teamId - 车队ID
 * @param {String} reason - 申请理由
 */
export function joinTeam(teamId, reason) {
  return request({
    url: `/team/${teamId}/join`,
    method: 'post',
    data: { reason },
    silent: true
  });
}

/**
 * 获取车队待审核申请列表
 * @param {Number} teamId - 车队ID
 */
export function getPendingApplications(teamId) {
  return request({
    url: `/team/${teamId}/applications`,
    method: 'get',
    silent: true
  });
}

/**
 * 同意加入申请
 * @param {Number} applicationId - 申请记录ID
 */
export function approveJoin(applicationId) {
  return request({
    url: `/team/applications/${applicationId}/approve`,
    method: 'post',
    silent: true
  });
}

/**
 * 拒绝加入申请
 * @param {Number} applicationId - 申请记录ID
 */
export function rejectJoin(applicationId) {
  return request({
    url: `/team/applications/${applicationId}/reject`,
    method: 'post',
    silent: true
  });
}

/**
 * 修改成员骑行标签
 * @param {Number} teamId - 车队ID
 * @param {Number} targetUserId - 目标用户ID
 * @param {String} tag - 新标签
 */
export function updateMemberTag(teamId, targetUserId, tag) {
  return request({
    url: `/team/${teamId}/members/${targetUserId}/tag`,
    method: 'put',
    data: { tag },
    silent: true
  });
}

/**
 * 设为管理员（仅队长可操作）
 * @param {Number} teamId - 车队ID
 * @param {Number} targetUserId - 目标用户ID
 */
export function promoteAdmin(teamId, targetUserId) {
  return request({
    url: `/team/${teamId}/members/${targetUserId}/admin`,
    method: 'put',
    silent: true
  });
}

/**
 * 撤销管理员（仅队长可操作）
 * @param {Number} teamId - 车队ID
 * @param {Number} targetUserId - 目标用户ID
 */
export function revokeAdmin(teamId, targetUserId) {
  return request({
    url: `/team/${teamId}/members/${targetUserId}/admin`,
    method: 'delete',
    silent: true
  });
}

/**
 * 请离车队（仅队长可操作）
 * @param {Number} teamId - 车队ID
 * @param {Number} targetUserId - 目标用户ID
 */
export function kickMember(teamId, targetUserId) {
  return request({
    url: `/team/${teamId}/members/${targetUserId}/kick`,
    method: 'delete',
    silent: true
  });
}

/**
 * 队长退出车队（需指定新队长）
 * @param {Number} teamId - 车队ID
 * @param {Number} newCaptainUserId - 新队长的用户ID
 */
export function captainLeaveTeam(teamId, newCaptainUserId) {
  return request({
    url: `/team/${teamId}/captain/leave`,
    method: 'put',
    data: { newCaptainUserId },
    silent: true
  });
}

// ============ 车队公告 API ============

/**
 * 发布公告
 * @param {Number} teamId - 车队ID
 * @param {Object} data - 公告信息
 * @param {String} data.title - 公告标题
 * @param {String} data.content - 公告内容
 */
export function publishNotice(teamId, data) {
  return request({
    url: `/team/${teamId}/notices`,
    method: 'post',
    data,
    silent: true
  });
}

/**
 * 编辑公告
 * @param {Number} teamId - 车队ID
 * @param {Number} noticeId - 公告ID
 * @param {Object} data - 公告信息
 * @param {String} data.title - 公告标题
 * @param {String} data.content - 公告内容
 */
export function updateNotice(teamId, noticeId, data) {
  return request({
    url: `/team/${teamId}/notices/${noticeId}`,
    method: 'put',
    data,
    silent: true
  });
}

/**
 * 删除公告（软删除）
 * @param {Number} teamId - 车队ID
 * @param {Number} noticeId - 公告ID
 */
export function deleteNotice(teamId, noticeId) {
  return request({
    url: `/team/${teamId}/notices/${noticeId}`,
    method: 'delete',
    silent: true
  });
}

/**
 * 获取公告栏（分页查询）
 * @param {Number} teamId - 车队ID
 * @param {Object} [params] - 查询参数
 * @param {Number} [params.page=1] - 页码
 * @param {Number} [params.size=10] - 每页数量
 */
export function getNoticeBoard(teamId, params = {}) {
  return request({
    url: `/team/${teamId}/notices`,
    method: 'get',
    params,
    silent: true
  });
}

// ============ 车队动态 API ============

/**
 * 发布动态（仅队内成员）
 * @param {Number} teamId - 车队ID
 * @param {Object} data - 动态信息
 * @param {String} data.content - 动态文本内容（必填，最多500字）
 * @param {Array<String>} [data.images] - 图片URL列表（可选，最多9张）
 */
export function publishPost(teamId, data) {
  return request({
    url: `/team/${teamId}/posts`,
    method: 'post',
    data,
    silent: true
  });
}

/**
 * 获取动态列表（分页，含点赞状态与评论）
 * @param {Number} teamId - 车队ID
 * @param {Object} [params] - 查询参数
 * @param {Number} [params.page=1] - 页码
 * @param {Number} [params.size=10] - 每页数量
 */
export function getPosts(teamId, params = {}) {
  return request({
    url: `/team/${teamId}/posts`,
    method: 'get',
    params
  });
}

/**
 * 删除动态（仅发布者可操作，软删除）
 * @param {Number} teamId - 车队ID
 * @param {Number} postId - 动态ID
 */
export function deletePost(teamId, postId) {
  return request({
    url: `/team/${teamId}/posts/${postId}`,
    method: 'delete',
    silent: true
  });
}

/**
 * 点赞/取消点赞（切换模式，仅队内成员）
 * @param {Number} teamId - 车队ID
 * @param {Number} postId - 动态ID
 * @returns {Promise<{liked: Boolean}>} liked=true 表示点赞成功，false 表示取消成功
 */
export function toggleLike(teamId, postId) {
  return request({
    url: `/team/${teamId}/posts/${postId}/like`,
    method: 'post',
    silent: true
  });
}

/**
 * 发表评论（仅队内成员）
 * @param {Number} teamId - 车队ID
 * @param {Number} postId - 动态ID
 * @param {Object} data - 评论信息
 * @param {String} data.content - 评论内容（必填，最多200字）
 */
export function addComment(teamId, postId, data) {
  return request({
    url: `/team/${teamId}/posts/${postId}/comments`,
    method: 'post',
    data,
    silent: true
  });
}

/**
 * 删除评论（仅评论发布者可操作，软删除）
 * @param {Number} teamId - 车队ID
 * @param {Number} postId - 动态ID
 * @param {Number} commentId - 评论ID
 */
export function deleteComment(teamId, postId, commentId) {
  return request({
    url: `/team/${teamId}/posts/${postId}/comments/${commentId}`,
    method: 'delete',
    silent: true
  });
}
