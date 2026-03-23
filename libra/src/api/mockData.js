/**
 * mockData.js
 * 集中管理所有测试数据
 */

export const homeData = {
    slides: [
        {
            bg: 'https://images.unsplash.com/photo-1511994298241-608e28f14fde?q=80&w=2070&auto=format&fit=crop',
            slogan: 'Fit Your Ride<br><span class="hero-highlight">Perfect Your Pride</span>',
            desc: '数据驱动表现 · 姿态成就荣耀',
            link: '#',
            btnText: null
        },
        {
            bg: '/image/index/fit.jpg',
            icon: 'fas fa-project-diagram',
            slogan: 'AI 精准设定<br><span class="hero-highlight">释放无限潜能</span>',
            desc: '利用计算机视觉技术捕捉骨骼关键点，在线生成 Fitting 报告。',
            btnText: '上传视频分析',
            btnIcon: 'fas fa-cloud-upload-alt',
            link: '/survey'
        },
        {
            bg: 'https://images.unsplash.com/photo-1474962558142-9ca83af74bb7?q=80&w=2070&auto=format&fit=crop',
            icon: 'fas fa-camera-retro',
            slogan: '记录热爱<br><span class="hero-highlight">见证蜕变</span>',
            desc: '晒出你的 Fitting 改装数据，分享沿途最美的风景。',
            btnText: '进入分享圈',
            btnIcon: 'fas fa-share-alt',
            link: '/sharecircle'
        },
        {
            bg: '/image/index/activities.jpg',
            icon: 'fas fa-flag-checkered',
            slogan: '拒绝平庸<br><span class="hero-highlight">直面挑战</span>',
            desc: '每一次出发，都是为了超越昨天的自己。',
            btnText: '查看活动日历',
            btnIcon: 'fas fa-calendar-alt',
            link: '#activities'
        },
        {
            bg: '/image/index/team.jpg',
            icon: 'fas fa-users-cog',
            slogan: '破风前行<br><span class="hero-highlight">岂止于快</span>',
            desc: '无兄弟，不骑行。加入本地精英骑士团，与队友轮转交替。',
            btnText: '寻找我的车队',
            btnIcon: 'fas fa-user-plus',
            link: '#knights'
        }
    ],
    galleryImages: [
        { id: 101, src: 'https://images.unsplash.com/photo-1541625602330-2277a4c46182?q=80&w=600&auto=format&fit=crop', likes: '1.2k' },
        { id: 102, src: 'https://images.unsplash.com/photo-1559166943-424a1b02661f?q=80&w=600&auto=format&fit=crop', likes: '856' },
        { id: 103, src: 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?q=80&w=600&auto=format&fit=crop', likes: '2.3k' },
        { id: 104, src: 'https://images.unsplash.com/photo-1475666675596-cca5a3a6a365?q=80&w=600&auto=format&fit=crop', likes: '902' },
        { id: 105, src: 'https://images.unsplash.com/photo-1526676037777-05a232554f77?q=80&w=600&auto=format&fit=crop', likes: '1.5k' },
        { id: 106, src: 'https://images.unsplash.com/photo-1618386477546-f947ee191244?q=80&w=600&auto=format&fit=crop', likes: '744' },
        { id: 107, src: 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?q=80&w=600&auto=format&fit=crop', likes: '3k' },
        { id: 108, src: 'https://images.unsplash.com/photo-1621340728987-a36c0a0c4f86?q=80&w=600&auto=format&fit=crop', likes: '1k' }
    ],
    eventsList: [
        { day: '15', month: 'OCT', title: '秋季环湖耐力赛', location: '千岛湖 · 140km' },
        { day: '22', month: 'OCT', title: '城市夜骑派对', location: '滨江公园 · 休闲骑' },
        { day: '05', month: 'NOV', title: '妙峰山爬坡赛', location: '北京 · 爬升800m' },
        { day: '12', month: 'NOV', title: '冬季海南集训营', location: '海口三亚 · 7天' },
        { day: '20', month: 'NOV', title: '机械师维修课', location: 'BikeFit 实验室' },
        { day: '10', month: 'DEC', title: 'Zwift 线上联赛', location: '虚拟骑行 · 功率比拼' }
    ],
    teamList: [
        { name: '破风猎人 (Aero)', avatar: 'https://images.unsplash.com/photo-1576082218084-25cbaf818c64?q=80&w=200&auto=format&fit=crop', city: '上海', members: '350+', achievement: '本周骑行 5,420 km' },
        { name: '山灵纵队', avatar: 'https://images.unsplash.com/photo-1558507312-320092288344?q=80&w=200&auto=format&fit=crop', city: '杭州', members: '120+', achievement: '本周爬升 32,000 m' },
        { name: 'Watt Monsters', avatar: 'https://images.unsplash.com/photo-1511994298241-608e28f14fde?q=80&w=200&auto=format&fit=crop', city: '深圳', members: '450+', achievement: '均功 280W 达成' },
        { name: '夜骑先锋 (Night)', avatar: 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?q=80&w=200&auto=format&fit=crop', city: '北京', members: '280+', achievement: '周末活动 2 场' }
    ]
};

export const teamData = {
    allTeams: [
        { id: 1, name: '破风猎人 (Aero Hunters)', city: '上海', slogan: '速度是唯一的信仰。', level: 'Pro', logo: 'https://images.unsplash.com/photo-1576082218084-25cbaf818c64?q=80&w=200&fit=crop', members: 342, weeklyKm: 12500, totalClimb: 45000, isJoined: false, isLeader: false },
        { id: 2, name: '山灵纵队 (Goats)', city: '杭州', slogan: '见坡就爬，不服就干。', level: 'Amateur', logo: 'https://images.unsplash.com/photo-1558507312-320092288344?q=80&w=200&fit=crop', members: 128, weeklyKm: 4200, totalClimb: 88000, isJoined: true, isLeader: false },
        { id: 3, name: '周末咖啡骑行社', city: '北京', slogan: '骑帅不骑快，咖啡第一名。', level: 'Casual', logo: 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?q=80&w=200&fit=crop', members: 560, weeklyKm: 1800, totalClimb: 500, isJoined: true, isLeader: true }
    ]
};

export const activityData = {
    allActivities: [
        {
            id: 101, isCreator: false, isHot: true, status: 'open', count: 450,
            title: '2026 千岛湖环湖耐力挑战赛', date: { day: '02', month: 'NOV' }, location: '千岛湖广场',
            desc: '140km 爬升1200m，年度收官之战。专业后勤保障，完赛奖牌。',
            cover: 'https://images.unsplash.com/photo-1517932301327-7ac9b0200786?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=1', 'https://i.pravatar.cc/100?img=2', 'https://i.pravatar.cc/100?img=3', 'https://i.pravatar.cc/100?img=4']
        },
        {
            id: 102, isCreator: false, isHot: true, status: 'open', count: 86,
            title: '城市追风·外滩夜骑派对', date: { day: '25', month: 'OCT' }, location: '外白渡桥',
            desc: '最美魔都夜景路线，摄影师跟拍，骑完一起吃夜宵！',
            cover: 'https://images.unsplash.com/photo-1565108422896-7b4c9197a72d?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=5', 'https://i.pravatar.cc/100?img=6']
        },
        {
            id: 103, isCreator: false, isHot: false, status: 'open', count: 15,
            title: '新手友好：15km 咖啡休闲骑', date: { day: '26', month: 'OCT' }, location: '西湖文化广场',
            desc: '不拉扯，纯休闲。适合折叠车、公路车新手。',
            cover: 'https://images.unsplash.com/photo-1506192135541-11b22264d673?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=7']
        },
        {
            id: 104, isCreator: false, isHot: true, status: 'open', count: 120,
            title: 'Zwift 线上联赛·第3站', date: { day: '28', month: 'OCT' }, location: '线上虚拟',
            desc: 'Watopia 火山爬坡赛，功率为王。',
            cover: 'https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=8', 'https://i.pravatar.cc/100?img=9']
        },
        {
            id: 105, isCreator: false, isHot: false, status: 'open', count: 30,
            title: '公路车机械维修基础课', date: { day: '30', month: 'OCT' }, location: '崔克旗舰店',
            desc: '学会自己换胎、调变速，告别路边推车。',
            cover: 'https://images.unsplash.com/photo-1582657233895-0f37a3ec7179?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=10']
        },
        {
            id: 106, isCreator: false, isHot: true, status: 'open', count: 200,
            title: '四明山 300km 挑战赛', date: { day: '05', month: 'NOV' }, location: '余姚',
            desc: '华东最美盘山公路，挑战你的爬坡极限。',
            cover: 'https://images.unsplash.com/photo-1511994298241-608e28f14fde?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=11', 'https://i.pravatar.cc/100?img=12', 'https://i.pravatar.cc/100?img=13']
        },
        {
            id: 201, isCreator: false, isHot: false, status: 'joined', count: 24,
            title: '周三夜骑·滨江休闲拉练', date: { day: '23', month: 'OCT' }, location: '滨江公园',
            desc: '均速30+，全平路段，适合新手进阶。请佩戴头盔。',
            cover: 'https://images.unsplash.com/photo-1558507312-320092288344?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=14', 'https://i.pravatar.cc/100?img=15']
        },
        {
            id: 202, isCreator: false, isHot: true, status: 'joined', count: 56,
            title: '周末 Fitting 专家讲座', date: { day: '27', month: 'OCT' }, location: '闪电概念店',
            desc: '讲解锁片调整与坐垫高度的基础逻辑，现场抽取一名幸运儿。',
            cover: 'https://images.unsplash.com/photo-1618386477546-f947ee191244?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=16']
        },
        {
            id: 203, isCreator: false, isHot: false, status: 'joined', count: 8,
            title: '晨间有氧·龙井恢复骑', date: { day: '29', month: 'OCT' }, location: '龙井山脚',
            desc: '早起呼吸新鲜空气，控制心率在 Z2 区间。',
            cover: 'https://images.unsplash.com/photo-1471506480208-91b3a4cc78be?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=17']
        },
        {
            id: 301, isCreator: true, isHot: false, status: 'open', count: 12,
            title: '龙井爬坡咖啡局', date: { day: '28', month: 'OCT' }, location: '龙井村牌坊',
            desc: '休闲爬坡，登顶后喝咖啡。欢迎带朋友一起来。',
            cover: 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=18', 'https://i.pravatar.cc/100?img=19']
        },
        {
            id: 302, isCreator: true, isHot: false, status: 'reviewing', count: 0,
            title: '【审核中】跨年 200km 挑战', date: { day: '31', month: 'DEC' }, location: '武林广场',
            desc: '用一场长途骑行迎接新年。路线规划中。',
            cover: 'https://images.unsplash.com/photo-1506192135541-11b22264d673?q=80&w=600',
            participants: []
        },
        {
            id: 303, isCreator: true, isHot: false, status: 'ended', count: 15,
            title: '二手装备交换市集', date: { day: '15', month: 'SEP' }, location: '社区公园',
            desc: '已结束。大家交换到了心仪的装备，很开心。',
            cover: 'https://images.unsplash.com/photo-1533561052604-c3beb6d55760?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=20', 'https://i.pravatar.cc/100?img=21']
        },
        {
            id: 401, isCreator: false, isHot: false, status: 'ended', count: 88,
            title: '妙峰山爬坡计时赛', date: { day: '10', month: 'SEP' }, location: '妙峰山牌楼',
            desc: '已结束。恭喜 @Alex 获得 KOM。',
            cover: 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?q=80&w=600',
            participants: ['https://i.pravatar.cc/100?img=22']
        }
    ]
};

export const shareData = {
    allPosts: [
        {
            id: 1, type: 'image', category: 'riding', isMine: true, isEvent: false,
            title: '千岛湖破风之旅 🚴‍♂️',
            desc: '今天的均速终于突破了35，风景太美了，腿也废了。',
            cover: 'https://images.unsplash.com/photo-1544191696-102dbdaeeaa0?q=80&w=600&fit=crop',
            userAvatar: 'https://i.pravatar.cc/100?img=12', userName: '我 (User)', likes: 124, isLiked: false
        },
        {
            id: 2, type: 'video', category: 'fitting', isMine: true, isEvent: false, duration: '0:45',
            title: 'Tarmac SL7 姿态微调记录',
            desc: '调整了锁片位置和座垫后移量，膝盖舒服多了。',
            cover: 'https://images.unsplash.com/photo-1618386477546-f947ee191244?q=80&w=600&fit=crop',
            userAvatar: 'https://i.pravatar.cc/100?img=12', userName: '我 (User)', likes: 89, isLiked: true,
            data: { height: 178, saddleHeight: 735, drop: 95 }
        },
        {
            id: 3, type: 'image', category: 'riding', isEvent: true,
            isTeamPrivate: false, teamId: 'team_public',
            title: '2024 环法中国挑战赛',
            desc: '官方报名开启！在这个周末，让我们一起挑战自我。',
            cover: 'https://images.unsplash.com/photo-1517932301327-7ac9b0200786?q=80&w=600&fit=crop',
            userAvatar: 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?q=80&w=100',
            userName: '赛事官方', likes: 450, isLiked: false
        },
        {
            id: 4, type: 'image', category: 'riding', isEvent: true, isMine: true,
            isTeamPrivate: true, teamId: 'team_001',
            title: '【内部】周三夜骑拉练',
            desc: '集合地点：滨江公园。强度：休闲骑 (均速30+)。',
            cover: 'https://images.unsplash.com/photo-1558507312-320092288344?q=80&w=600&fit=crop',
            userAvatar: 'https://i.pravatar.cc/100?img=12',
            userName: '我 (User)', likes: 24, isLiked: true
        }
    ]
};
