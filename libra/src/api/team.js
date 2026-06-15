import request from '@/api/request';

// ============ Mock 数据 ============
const MOCK_TEAMS = [
  {
    id: 1,
    name: '破风者联盟',
    avatar: 'https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800&h=300&fit=crop',
    location: '北京',
    description: '北京最具活力的公路骑行社群，每周组织妙峰山、潭王路等经典路线团练，欢迎热爱爬坡和长途的你加入。',
    memberCount: 328,
    activityLevel: 'hot',
    tags: ['公路', '爬坡', '长途'],
    createdAt: '2025-03-15'
  },
  {
    id: 2,
    name: '山野骑士团',
    avatar: 'https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1551972254-29570f61f7e1?w=800&h=300&fit=crop',
    location: '成都',
    description: '扎根川西，专注山地越野与林道探险。蒲虹路、龙泉山是我们的主场，带你体验真正的山地乐趣。',
    memberCount: 215,
    activityLevel: 'hot',
    tags: ['山地', '越野', '林道'],
    createdAt: '2025-05-20'
  },
  {
    id: 3,
    name: '城市巡航者',
    avatar: 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&h=300&fit=crop',
    location: '上海',
    description: '都市通勤与周末休闲骑行的温暖社群。每周浦东滨江、浦西梧桐区city ride，节奏轻松，新手友好。',
    memberCount: 456,
    activityLevel: 'hot',
    tags: ['通勤', '休闲', '城市'],
    createdAt: '2025-01-10'
  },
  {
    id: 4,
    name: '竞速先锋',
    avatar: 'https://images.unsplash.com/photo-1509027572446-af8401acfdc3?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1452573992436-6d508f200b30?w=800&h=300&fit=crop',
    location: '广州',
    description: '竞速爱好者的聚集地，定期组织ITT计时赛和绕圈赛训练，配备专业功率计分析和赛道复盘。',
    memberCount: 187,
    activityLevel: 'normal',
    tags: ['竞速', '计时赛', '训练'],
    createdAt: '2025-07-01'
  },
  {
    id: 5,
    name: '骑迹远征队',
    avatar: 'https://images.unsplash.com/photo-1471506480208-91b3a4cc78be?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1508193638397-1c4234db14d8?w=800&h=300&fit=crop',
    location: '昆明',
    description: '热爱长途骑游与 bikepacking，已经完成环洱海、丽江-香格里拉等多条经典线路。下一站：川藏线。',
    memberCount: 142,
    activityLevel: 'normal',
    tags: ['长途', '旅行', 'Bikepacking'],
    createdAt: '2025-04-01'
  },
  {
    id: 6,
    name: '铁三训练营',
    avatar: 'https://images.unsplash.com/photo-1530549387789-4c1017266635?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1461896836934-bd45baecf68b?w=800&h=300&fit=crop',
    location: '深圳',
    description: '铁人三项专项训练车队，涵盖游泳、骑行、跑步交叉训练。每周大鹏半岛实战拉练，助力PB突破。',
    memberCount: 98,
    activityLevel: 'new',
    tags: ['铁三', '综合训练', '竞速'],
    createdAt: '2025-09-15'
  },
  {
    id: 7,
    name: '夜骑小分队',
    avatar: 'https://images.unsplash.com/photo-1576435728678-68d0fbf94e91?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1515634928627-2a4e0dae3ddf?w=800&h=300&fit=crop',
    location: '杭州',
    description: '西湖夜骑是我们的保留节目。下班后绕湖一圈，龙井爬坡加餐，骑完夜宵才是正经事。氛围轻松，随时上车。',
    memberCount: 267,
    activityLevel: 'hot',
    tags: ['夜骑', '休闲', '社交'],
    createdAt: '2025-06-01'
  },
  {
    id: 8,
    name: '爬坡研究所',
    avatar: 'https://images.unsplash.com/photo-1526726538690-5cbf956ae2fd?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1536637706725-c96e883a6f9e?w=800&h=300&fit=crop',
    location: '重庆',
    description: '山城重庆爬坡专精车队，研究每一座山的坡度、齿比与踏频策略。歌乐山、南山是我们的实验室。',
    memberCount: 156,
    activityLevel: 'new',
    tags: ['爬坡', '技术', '训练'],
    createdAt: '2025-08-20'
  },
  {
    id: 9,
    name: '女子骑行社',
    avatar: 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?w=800&h=300&fit=crop',
    location: '南京',
    description: '专属于女性骑行爱好者的社群，定期举办骑行入门教学、装备分享和闺蜜骑游活动，温暖且专业。',
    memberCount: 203,
    activityLevel: 'normal',
    tags: ['女子', '入门', '社交'],
    createdAt: '2025-02-14'
  },
  {
    id: 10,
    name: '折叠车俱乐部',
    avatar: 'https://images.unsplash.com/photo-1534307671554-9a6d81f4d629?w=200&h=200&fit=crop',
    coverImage: 'https://images.unsplash.com/photo-1501147830916-ce44a6359892?w=800&h=300&fit=crop',
    location: '厦门',
    description: 'Brompton、Dahon 玩家的聚集地，4+2出行、环岛漫游，探索城市每一个角落。小而美，自由自在。',
    memberCount: 134,
    activityLevel: 'new',
    tags: ['折叠车', '城市', '休闲'],
    createdAt: '2025-10-01'
  }
];

const MOCK_MEMBERS = [
  { id: 1, name: '风速骑士', avatar: 'https://i.pravatar.cc/100?img=1', role: '队长', tag: '全能', joinDate: '2025-03-15' },
  { id: 2, name: '爬坡达人', avatar: 'https://i.pravatar.cc/100?img=2', role: '管理员', tag: '爬坡手', joinDate: '2025-03-20' },
  { id: 3, name: '山道飞侠', avatar: 'https://i.pravatar.cc/100?img=3', role: '成员', tag: '破风手', joinDate: '2025-04-01' },
  { id: 4, name: '骑行小白', avatar: 'https://i.pravatar.cc/100?img=4', role: '成员', tag: '新秀', joinDate: '2025-05-10' },
  { id: 5, name: '铁三铁粉', avatar: 'https://i.pravatar.cc/100?img=5', role: '成员', tag: '铁三选手', joinDate: '2025-06-15' },
  { id: 6, name: '都市骑手', avatar: 'https://i.pravatar.cc/100?img=6', role: '成员', tag: '冲刺手', joinDate: '2025-07-01' },
  { id: 7, name: '夜骑侠', avatar: 'https://i.pravatar.cc/100?img=7', role: '成员', tag: '夜骑达人', joinDate: '2025-07-20' },
  { id: 8, name: '爬坡小王子', avatar: 'https://i.pravatar.cc/100?img=8', role: '成员', tag: '爬坡手', joinDate: '2025-08-01' },
  { id: 9, name: '长途旅者', avatar: 'https://i.pravatar.cc/100?img=9', role: '成员', tag: '耐力王', joinDate: '2025-08-15' },
  { id: 10, name: '竞速新星', avatar: 'https://i.pravatar.cc/100?img=10', role: '成员', tag: '计时赛专家', joinDate: '2025-09-01' },
  { id: 11, name: '山地勇士', avatar: 'https://i.pravatar.cc/100?img=11', role: '成员', tag: '山地达人', joinDate: '2025-09-10' },
  { id: 12, name: '折叠达人', avatar: 'https://i.pravatar.cc/100?img=12', role: '成员', tag: '通勤专家', joinDate: '2025-10-01' }
];

const MOCK_ACTIVITIES = [
  { id: 1, type: 'activity', title: '妙峰山团练', desc: '本周六早7点金顶街集合，全程约60km，爬升1200m', date: '2025-06-08', participants: 28 },
  { id: 2, type: 'checkin', userId: 3, userName: '山道飞侠', content: '今天PR了潭王路，开心！', image: 'https://images.unsplash.com/photo-1509315703195-529879416a7d?w=400&h=300&fit=crop', date: '2025-06-07' },
  { id: 3, type: 'notice', title: '车队新规', content: '自本月起，参加活动必须佩戴头盔，安全第一。', date: '2025-06-05' },
  { id: 4, type: 'checkin', userId: 5, userName: '铁三铁粉', content: '完成首个半铁，骑行段配速35km/h', image: 'https://images.unsplash.com/photo-1461896836934-bd45baecf68b?w=400&h=300&fit=crop', date: '2025-06-04' },
  { id: 5, type: 'activity', title: '周末LSD拉练', desc: '周日平路有氧长距离，全程120km，均速28-30km/h，中途补给两次', date: '2025-06-01', participants: 35 }
];

const MOCK_GARAGE = [
  { id: 101, ownerId: 1, ownerName: '风速骑士', ownerAvatar: 'https://i.pravatar.cc/100?img=1',
    brand: 'Specialized', model: 'Tarmac SL8', type: '公路车', frameSize: '56', color: '#1a1a2e', addedAt: '2025-04-10' },
  { id: 102, ownerId: 1, ownerName: '风速骑士', ownerAvatar: 'https://i.pravatar.cc/100?img=1',
    brand: 'Canyon', model: 'Aeroad CFR', type: '公路车', frameSize: '56', color: '#c0392b', addedAt: '2025-05-01' },
  { id: 103, ownerId: 2, ownerName: '爬坡达人', ownerAvatar: 'https://i.pravatar.cc/100?img=2',
    brand: 'Trek', model: 'Emonda SLR', type: '公路车', frameSize: '54', color: '#2c3e50', addedAt: '2025-04-15' },
  { id: 104, ownerId: 3, ownerName: '山道飞侠', ownerAvatar: 'https://i.pravatar.cc/100?img=3',
    brand: 'Santa Cruz', model: 'Nomad V5', type: '山地车', frameSize: 'M', color: '#27ae60', addedAt: '2025-05-08' },
  { id: 105, ownerId: 5, ownerName: '铁三铁粉', ownerAvatar: 'https://i.pravatar.cc/100?img=5',
    brand: 'Cervélo', model: 'P5', type: 'TT车', frameSize: '54', color: '#1abc9c', addedAt: '2025-06-20' },
  { id: 106, ownerId: 5, ownerName: '铁三铁粉', ownerAvatar: 'https://i.pravatar.cc/100?img=5',
    brand: 'Specialized', model: 'Shiv TT', type: 'TT车', frameSize: '54', color: '#34495e', addedAt: '2025-07-01' },
  { id: 107, ownerId: 6, ownerName: '都市骑手', ownerAvatar: 'https://i.pravatar.cc/100?img=6',
    brand: 'Pinarello', model: 'Dogma F', type: '公路车', frameSize: '53', color: '#e74c3c', addedAt: '2025-07-10' },
  { id: 108, ownerId: 7, ownerName: '夜骑侠', ownerAvatar: 'https://i.pravatar.cc/100?img=7',
    brand: 'Cannondale', model: 'SuperSix EVO', type: '公路车', frameSize: '56', color: '#8e44ad', addedAt: '2025-07-25' },
  { id: 109, ownerId: 8, ownerName: '爬坡小王子', ownerAvatar: 'https://i.pravatar.cc/100?img=8',
    brand: 'BMC', model: 'Teammachine SLR', type: '公路车', frameSize: '52', color: '#2980b9', addedAt: '2025-08-05' },
  { id: 110, ownerId: 9, ownerName: '长途旅者', ownerAvatar: 'https://i.pravatar.cc/100?img=9',
    brand: 'Specialized', model: 'Diverge STR', type: '砾石车', frameSize: '54', color: '#16a085', addedAt: '2025-08-20' },
  { id: 111, ownerId: 10, ownerName: '竞速新星', ownerAvatar: 'https://i.pravatar.cc/100?img=10',
    brand: 'Factor', model: 'O2 VAM', type: '公路车', frameSize: '54', color: '#d35400', addedAt: '2025-09-05' },
  { id: 112, ownerId: 11, ownerName: '山地勇士', ownerAvatar: 'https://i.pravatar.cc/100?img=11',
    brand: 'YT', model: 'Capra Core', type: '山地车', frameSize: 'L', color: '#f39c12', addedAt: '2025-09-15' },
  { id: 113, ownerId: 12, ownerName: '折叠达人', ownerAvatar: 'https://i.pravatar.cc/100?img=12',
    brand: 'Brompton', model: 'C Line Explore', type: '折叠车', frameSize: 'M', color: '#2ecc71', addedAt: '2025-10-08' }
];

// ============ API 函数 ============

/**
 * 获取车队车库
 * @param {Number} teamId - 车队ID
 */
export function getTeamGarage(teamId) {
  return request({
    url: `/team/${teamId}/garage`,
    method: 'get'
  }).catch(() => {
    return { records: MOCK_GARAGE, total: MOCK_GARAGE.length };
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
  }).catch(() => {
    // 后端未就绪时使用 mock
    let filtered = [...MOCK_TEAMS];
    const { keyword, category, page = 1, size = 8 } = params;

    if (keyword) {
      const kw = keyword.toLowerCase();
      filtered = filtered.filter(t =>
        t.name.toLowerCase().includes(kw) ||
        t.location.toLowerCase().includes(kw) ||
        t.description.toLowerCase().includes(kw) ||
        t.tags.some(tag => tag.toLowerCase().includes(kw))
      );
    }

    if (category === 'hot') {
      filtered = filtered.filter(t => t.activityLevel === 'hot');
    } else if (category === 'new') {
      filtered = filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }

    const total = filtered.length;
    const start = (page - 1) * size;
    const records = filtered.slice(start, start + size);

    return { records, total, page, size };
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
  }).catch(() => {
    const team = MOCK_TEAMS.find(t => t.id === Number(id));
    if (!team) throw new Error('车队不存在');
    return { ...team, activities: MOCK_ACTIVITIES };
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
  }).catch(() => {
    return { records: MOCK_MEMBERS, total: MOCK_MEMBERS.length, page: 1, size: 20, pages: 1 };
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
    params
  });
}
