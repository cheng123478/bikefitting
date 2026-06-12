-- ============================================================
-- 破风社-车队测试数据（plt_team / plt_team_member）
-- 使用方式：将 application-dev.yml 中 mode: never 改为 always，
--          启动服务后自动执行，灌入数据后可改回 never
-- ============================================================

-- 清空旧数据（防止重复启动时报主键冲突）
DELETE FROM plt_team_member;
DELETE FROM plt_team;

-- ============================================================
-- 车队数据（10支）
-- ============================================================
INSERT INTO plt_team (id, name, avatar, cover_image, location, description, member_count, activity_level, tags, creator_id, create_time, update_time) VALUES
(1, '破风者联盟',
 'https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1517649763962-0c623066013b?w=800&h=300&fit=crop',
 '北京', '北京最具活力的公路骑行社群，每周组织妙峰山、潭王路等经典路线团练，欢迎热爱爬坡和长途的你加入。',
 328, 'hot', '公路,爬坡,长途', NULL, '2025-03-15 10:00:00', NOW()),

(2, '山野骑士团',
 'https://images.unsplash.com/photo-1571068316344-75bc76f77890?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1551972254-29570f61f7e1?w=800&h=300&fit=crop',
 '成都', '扎根川西，专注山地越野与林道探险。蒲虹路、龙泉山是我们的主场，带你体验真正的山地乐趣。',
 215, 'hot', '山地,越野,林道', NULL, '2025-05-20 10:00:00', NOW()),

(3, '城市巡航者',
 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&h=300&fit=crop',
 '上海', '都市通勤与周末休闲骑行的温暖社群。每周浦东滨江、浦西梧桐区city ride，节奏轻松，新手友好。',
 456, 'hot', '通勤,休闲,城市', NULL, '2025-01-10 10:00:00', NOW()),

(4, '竞速先锋',
 'https://images.unsplash.com/photo-1509027572446-af8401acfdc3?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1452573992436-6d508f200b30?w=800&h=300&fit=crop',
 '广州', '竞速爱好者的聚集地，定期组织ITT计时赛和绕圈赛训练，配备专业功率计分析和赛道复盘。',
 187, 'normal', '竞速,计时赛,训练', NULL, '2025-07-01 10:00:00', NOW()),

(5, '骑迹远征队',
 'https://images.unsplash.com/photo-1471506480208-91b3a4cc78be?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1508193638397-1c4234db14d8?w=800&h=300&fit=crop',
 '昆明', '热爱长途骑游与 bikepacking，已经完成环洱海、丽江-香格里拉等多条经典线路。下一站：川藏线。',
 142, 'normal', '长途,旅行,Bikepacking', NULL, '2025-04-01 10:00:00', NOW()),

(6, '铁三训练营',
 'https://images.unsplash.com/photo-1530549387789-4c1017266635?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1461896836934-bd45baecf68b?w=800&h=300&fit=crop',
 '深圳', '铁人三项专项训练车队，涵盖游泳、骑行、跑步交叉训练。每周大鹏半岛实战拉练，助力PB突破。',
 98, 'new', '铁三,综合训练,竞速', NULL, '2025-09-15 10:00:00', NOW()),

(7, '夜骑小分队',
 'https://images.unsplash.com/photo-1576435728678-68d0fbf94e91?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1515634928627-2a4e0dae3ddf?w=800&h=300&fit=crop',
 '杭州', '西湖夜骑是我们的保留节目。下班后绕湖一圈，龙井爬坡加餐，骑完夜宵才是正经事。氛围轻松，随时上车。',
 267, 'hot', '夜骑,休闲,社交', NULL, '2025-06-01 10:00:00', NOW()),

(8, '爬坡研究所',
 'https://images.unsplash.com/photo-1526726538690-5cbf956ae2fd?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1536637706725-c96e883a6f9e?w=800&h=300&fit=crop',
 '重庆', '山城重庆爬坡专精车队，研究每一座山的坡度、齿比与踏频策略。歌乐山、南山是我们的实验室。',
 156, 'new', '爬坡,技术,训练', NULL, '2025-08-20 10:00:00', NOW()),

(9, '女子骑行社',
 'https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?w=800&h=300&fit=crop',
 '南京', '专属于女性骑行爱好者的社群，定期举办骑行入门教学、装备分享和闺蜜骑游活动，温暖且专业。',
 203, 'normal', '女子,入门,社交', NULL, '2025-02-14 10:00:00', NOW()),

(10, '折叠车俱乐部',
 'https://images.unsplash.com/photo-1534307671554-9a6d81f4d629?w=200&h=200&fit=crop',
 'https://images.unsplash.com/photo-1501147830916-ce44a6359892?w=800&h=300&fit=crop',
 '厦门', 'Brompton、Dahon 玩家的聚集地，4+2出行、环岛漫游，探索城市每一个角落。小而美，自由自在。',
 134, 'new', '折叠车,城市,休闲', NULL, '2025-10-01 10:00:00', NOW());
