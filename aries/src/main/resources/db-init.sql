-- ============================================================
-- 破风社-车队测试数据（team / team_member / usr_user）
-- 使用方式：将 application-dev.yml 中 mode: never 改为 always，
--          启动服务后自动执行，灌入数据后可改回 never
-- ============================================================

-- 清空旧数据
DELETE FROM team_member;
DELETE FROM team_join_application;
DELETE FROM team;
DELETE FROM usr_user WHERE id >= 10001;

-- ============================================================
-- 测试用户（10个虚拟用户，ID 10001~10010）
-- ============================================================
INSERT INTO usr_user (id, phone, password, name, avatar, gender, registration_time, is_deleted, account_non_expired, account_non_locked, credentials_non_expired, enabled, login_failure_count) VALUES
(10001, '13800000001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '风行者',   'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop', 1, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10002, '13800000002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '山岳骑士', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop', 1, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10003, '13800000003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '追风少年', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop', 1, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10004, '13800000004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '铁腿李',   'https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=100&h=100&fit=crop', 1, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10005, '13800000005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '破风姐',   'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop', 0, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10006, '13800000006', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '爬坡王',   'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop', 1, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10007, '13800000007', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '闪电侠',   'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop', 1, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10008, '13800000008', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '夜骑精灵', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100&h=100&fit=crop', 0, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10009, '13800000009', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '铁人三项', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=100&h=100&fit=crop', 1, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0),
(10010, '13800000010', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2xh4BQz2Y6lF0.g7WC0kOa', '小折玩家', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop', 0, NOW(), FALSE, TRUE, TRUE, TRUE, TRUE, 0);

-- ============================================================
-- 车队数据（10支，设置 creator_id）
-- ============================================================
INSERT INTO team (id, name, avatar, cover_image, location, description, member_count, activity_level, tags, creator_id, create_time, update_time) VALUES
(1, '破风者联盟',
 'https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800&h=300&fit=crop',
 '北京', '北京最具活力的公路骑行社群，每周组织妙峰山、潭王路等经典路线团练，欢迎热爱爬坡和长途的你加入。',
 8, 'hot', '公路,爬坡,长途', 10001, '2025-03-15 10:00:00', NOW()),

(2, '山野骑士团',
 'https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1551972254-29570f61f7e1?w=800&h=300&fit=crop',
 '成都', '扎根川西，专注山地越野与林道探险。蒲虹路、龙泉山是我们的主场，带你体验真正的山地乐趣。',
 6, 'hot', '山地,越野,林道', 10002, '2025-05-20 10:00:00', NOW()),

(3, '城市巡航者',
 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&h=300&fit=crop',
 '上海', '都市通勤与周末休闲骑行的温暖社群。每周浦东滨江、浦西梧桐区city ride，节奏轻松，新手友好。',
 5, 'hot', '通勤,休闲,城市', 10003, '2025-01-10 10:00:00', NOW()),

(4, '竞速先锋',
 'https://images.unsplash.com/photo-1509027572446-af8401acfdc3?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1452573992436-6d508f200b30?w=800&h=300&fit=crop',
 '广州', '竞速爱好者的聚集地，定期组织ITT计时赛和绕圈赛训练，配备专业功率计分析和赛道复盘。',
 4, 'normal', '竞速,计时赛,训练', 10004, '2025-07-01 10:00:00', NOW()),

(5, '骑迹远征队',
 'https://images.unsplash.com/photo-1471506480208-91b3a4cc78be?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1508193638397-1c4234db14d8?w=800&h=300&fit=crop',
 '昆明', '热爱长途骑游与 bikepacking，已经完成环洱海、丽江-香格里拉等多条经典线路。下一站：川藏线。',
 3, 'normal', '长途,旅行,Bikepacking', 10005, '2025-04-01 10:00:00', NOW()),

(6, '铁三训练营',
 'https://images.unsplash.com/photo-1530549387789-4c1017266635?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1461896836934-bd45baecf68b?w=800&h=300&fit=crop',
 '深圳', '铁人三项专项训练车队，涵盖游泳、骑行、跑步交叉训练。每周大鹏半岛实战拉练，助力PB突破。',
 3, 'new', '铁三,综合训练,竞速', 10009, '2025-09-15 10:00:00', NOW()),

(7, '夜骑小分队',
 'https://images.unsplash.com/photo-1576435728678-68d0fbf94e91?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1515634928627-2a4e0dae3ddf?w=800&h=300&fit=crop',
 '杭州', '西湖夜骑是我们的保留节目。下班后绕湖一圈，龙井爬坡加餐，骑完夜宵才是正经事。氛围轻松，随时上车。',
 4, 'hot', '夜骑,休闲,社交', 10008, '2025-06-01 10:00:00', NOW()),

(8, '爬坡研究所',
 'https://images.unsplash.com/photo-1526726538690-5cbf956ae2fd?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1536637706725-c96e883a6f9e?w=800&h=300&fit=crop',
 '重庆', '山城重庆爬坡专精车队，研究每一座山的坡度、齿比与踏频策略。歌乐山、南山是我们的实验室。',
 3, 'new', '爬坡,技术,训练', 10006, '2025-08-20 10:00:00', NOW()),

(9, '女子骑行社',
 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?w=800&h=300&fit=crop',
 '南京', '专属于女性骑行爱好者的社群，定期举办骑行入门教学、装备分享和闺蜜骑游活动，温暖且专业。',
 3, 'normal', '女子,入门,社交', 10005, '2025-02-14 10:00:00', NOW()),

(10, '折叠车俱乐部',
 'https://images.unsplash.com/photo-1534307671554-9a6d81f4d629?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1501147830916-ce44a6359892?w=800&h=300&fit=crop',
 '厦门', 'Brompton、Dahon 玩家的聚集地，4+2出行、环岛漫游，探索城市每一个角落。小而美，自由自在。',
 3, 'new', '折叠车,城市,休闲', 10010, '2025-10-01 10:00:00', NOW());

-- ============================================================
-- 车队成员数据（team_member）
-- role: 0=队长 1=管理员 2=成员  |  status: 0=正常 1=已退出 2=被踢出
-- ============================================================
INSERT INTO team_member (team_id, user_id, role, tag, join_date, status) VALUES

-- 车队1: 破风者联盟（8人）
(1, 10001, 0, '全能',       '2025-03-15', 0),  -- 队长
(1, 10002, 1, '爬坡手',     '2025-03-20', 0),  -- 管理员
(1, 10003, 2, '破风手',     '2025-04-01', 0),
(1, 10004, 2, '冲刺手',     '2025-04-10', 0),
(1, 10005, 2, '耐力王',     '2025-04-15', 0),
(1, 10006, 2, '爬坡手',     '2025-05-01', 0),
(1, 10007, 2, '计时赛专家', '2025-05-10', 0),
(1, 10008, 2, '新秀',       '2025-06-01', 0),

-- 车队2: 山野骑士团（6人）
(2, 10002, 0, '山地达人',   '2025-05-20', 0),  -- 队长
(2, 10006, 1, '山地达人',   '2025-05-25', 0),  -- 管理员
(2, 10003, 2, '全能',       '2025-06-01', 0),
(2, 10004, 2, '爬坡手',     '2025-06-10', 0),
(2, 10007, 2, '新秀',       '2025-06-20', 0),
(2, 10009, 2, '铁三选手',   '2025-07-01', 0),

-- 车队3: 城市巡航者（5人）
(3, 10003, 0, '通勤专家',   '2025-01-10', 0),  -- 队长
(3, 10008, 1, '夜骑达人',   '2025-01-15', 0),  -- 管理员
(3, 10010, 2, '新秀',       '2025-02-01', 0),
(3, 10005, 2, '耐力王',     '2025-02-10', 0),
(3, 10001, 2, '全能',       '2025-02-20', 0),

-- 车队4: 竞速先锋（4人）
(4, 10004, 0, '冲刺手',     '2025-07-01', 0),  -- 队长
(4, 10007, 1, '计时赛专家', '2025-07-05', 0),  -- 管理员
(4, 10001, 2, '全能',       '2025-07-10', 0),
(4, 10009, 2, '铁三选手',   '2025-07-15', 0),

-- 车队5: 骑迹远征队（3人）
(5, 10005, 0, '耐力王',     '2025-04-01', 0),  -- 队长
(5, 10003, 2, '全能',       '2025-04-10', 0),
(5, 10006, 2, '爬坡手',     '2025-04-20', 0),

-- 车队6: 铁三训练营（3人）
(6, 10009, 0, '铁三选手',   '2025-09-15', 0),  -- 队长
(6, 10004, 2, '冲刺手',     '2025-09-20', 0),
(6, 10007, 2, '新秀',       '2025-09-25', 0),

-- 车队7: 夜骑小分队（4人）
(7, 10008, 0, '夜骑达人',   '2025-06-01', 0),  -- 队长
(7, 10001, 2, '全能',       '2025-06-05', 0),
(7, 10010, 2, '新秀',       '2025-06-10', 0),
(7, 10003, 2, '破风手',     '2025-06-15', 0),

-- 车队8: 爬坡研究所（3人）
(8, 10006, 0, '爬坡手',     '2025-08-20', 0),  -- 队长
(8, 10002, 2, '山地达人',   '2025-08-25', 0),
(8, 10004, 2, '爬坡手',     '2025-09-01', 0),

-- 车队9: 女子骑行社（3人）
(9, 10005, 0, '全能',       '2025-02-14', 0),  -- 队长
(9, 10008, 2, '夜骑达人',   '2025-02-20', 0),
(9, 10010, 2, '新秀',       '2025-03-01', 0),

-- 车队10: 折叠车俱乐部（3人）
(10, 10010, 0, '新秀',      '2025-10-01', 0),  -- 队长
(10, 10003, 2, '通勤专家',  '2025-10-05', 0),
(10, 10008, 2, '夜骑达人',  '2025-10-10', 0);
