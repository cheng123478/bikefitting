<template>
  <div class="panel-section fade-in">
    <div class="panel-header">
      <h3 class="panel-title">会员与开源支持</h3>
      <p class="sub">项目成长初期，感谢每一位参与共建的骑友</p>
    </div>

    <!-- 1. 当前权益看板 (顶部看板) -->
    <div class="glass-card membership-banner mb-30">
      <div class="banner-content">
        <div class="banner-info">
          <span class="banner-tag">{{ membershipInfo.memberType === 'PRO' ? 'PRO' : 'FREE' }}</span>
          <h4 class="banner-title">
            {{ membershipInfo.memberType === 'PRO' ? '您正在享受 PRO 特权' : '初级骑友' }}
          </h4>
          <p class="banner-desc">
            {{ membershipInfo.memberType === 'PRO' ? '到期时间：' + formatDate(membershipInfo.proExpireTime) : '升级 PRO 解锁无限次 AI 动态分析与历史对比' }}
          </p>
        </div>
        <div class="banner-stats">
          <div class="b-stat">
            <span class="b-val">{{ membershipInfo.availableTimes ?? 0 }}</span>
            <span class="b-lbl">可用次数</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 开源打赏区块 (SecurityTab 风格) -->
    <div class="security-block">
      <h4 class="block-title">开源打赏</h4>
      <p class="block-desc">{{ PROJECT_CONFIG.NAME }} 是一个纯粹的开源项目。如果您喜欢它，可以通过打赏支持服务器带宽与算法优化。</p>
      
      <div class="donation-horizontal-box">
        <div class="donation-options-bar">
          <div 
            v-for="opt in donationOptions" 
            :key="opt.id"
            class="donation-item"
            :class="{ active: selectedDonation === opt.id }"
            @click="selectedDonation = opt.id"
          >
            <span class="d-icon">{{ opt.icon }}</span>
            <div class="d-txt">
              <span class="d-name">{{ opt.name }}</span>
              <span class="d-price">{{ opt.price }}</span>
            </div>
          </div>
        </div>

        <div class="donation-detail-area fade-in" :key="selectedDonation">
          <div class="donation-msg">
            <i class="fas fa-quote-left"></i>
            {{ currentDonationMsg }}
          </div>
          <div class="donation-qr-strip">
            <img src="@/assets/image/wechat-donation.png" alt="微信打赏" class="mini-qr">
            <div class="qr-info">
              <span class="qr-title">微信扫码支付</span>
              <span class="qr-tip">您的每一分支持都将用于项目维护</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="divider"></div>

    <!-- 3. 邀请奖励区块 (SecurityTab 风格) -->
    <div class="security-block">
      <h4 class="block-title" style="border-left-color: var(--primary-color);">邀请激励</h4>
      <p class="block-desc">独乐乐不如众乐乐，每邀请一位新用户注册即可获得分析额度。</p>
      
      <div class="grid-2">
        <div class="reward-status-card">
          <div class="rs-header">
            <span class="rs-label">累计成功邀请</span>
            <span class="rs-count">{{ membershipInfo.totalInvites ?? 0 }} 人</span>
          </div>
          <!-- 进度条 -->
            <div class="progress-section">
            <div class="progress-meta">
              <span>距离下一级奖励还差 {{ REWARD_CONFIG.INVITE_THRESHOLD_PRO - (membershipInfo.totalInvites % REWARD_CONFIG.INVITE_THRESHOLD_PRO) }} 人</span>
              <span class="target-text">目标：{{ REWARD_CONFIG.PRO_DURATION_DAYS }}天 PRO</span>
            </div>
            <div class="progress-bar-bg">
              <div class="progress-bar-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
          </div>
        </div>

        <div class="invite-action-block">
          <label class="input-label">专属邀请链接</label>
          <div class="input-group code-group">
            <input type="text" :value="inviteLink" readonly class="zen-input">
            <button class="get-code-btn" @click="copyLink">复制链接</button>
          </div>
          <p class="helper-text">奖励说明：1人 = +{{ REWARD_CONFIG.INVITE_FOR_TOKEN }}次分析额度 | {{ REWARD_CONFIG.INVITE_THRESHOLD_PRO }}人 = +{{ REWARD_CONFIG.PRO_DURATION_DAYS }}天 PRO</p>
        </div>
      </div>
    </div>

    <div class="divider"></div>

    <!-- 4. 权益说明 (页脚参考) -->
    <div class="security-block">
      <h4 class="block-title" style="border-left-color: var(--accent-gold);">PRO 会员权益清单</h4>
      <div class="benefit-strip-grid">
        <div class="b-strip">
          <i class="fas fa-check-circle"></i>
          <span>无限次动态分析</span>
        </div>
        <div class="b-strip">
          <i class="fas fa-check-circle"></i>
          <span>AI 实时姿态建议</span>
        </div>
        <div class="b-strip">
          <i class="fas fa-check-circle"></i>
          <span>无限历史快照保存</span>
        </div>
        <div class="b-strip">
          <i class="fas fa-check-circle"></i>
          <span>跨设备同步报告</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { showToast } from '@/utils/toast.js';
import request from '@/api/request.js';
import { PROJECT_CONFIG, REWARD_CONFIG } from '@/config/constants.js';

const membershipInfo = ref({
  memberType: 'FREE',
  tokenBalance: 0,
  totalInvites: 0,
  proExpireTime: null
});
const inviteCode = ref('');
const selectedDonation = ref('coffee');

const donationOptions = [
  { id: 'coffee', icon: '☕', name: '一杯咖啡', price: '¥10', msg: '感谢您的“咖啡因”支持，我们将熬夜优化算法！' },
  { id: 'lunch', icon: '🍱', name: '一份午餐', price: '¥30', msg: '开发者吃饱了，更有力气写出无 Bug 的代码了！' },
  { id: 'love', icon: '⚡', name: '为爱发电', price: '任意', msg: '您的热爱是项目最强大的动力，千言万语汇成一句感谢！' }
];

const currentDonationMsg = computed(() => {
  return donationOptions.find(o => o.id === selectedDonation.value)?.msg || '';
});

const inviteLink = computed(() => {
  if (!inviteCode.value) return '生成中...';
  return `${window.location.origin}/?inviter=${inviteCode.value}`;
});

const progressPercent = computed(() => {
  const invites = membershipInfo.value?.totalInvites || 0;
  return ((invites % REWARD_CONFIG.INVITE_THRESHOLD_PRO) / REWARD_CONFIG.INVITE_THRESHOLD_PRO) * 100;
});

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

const fetchMembershipInfo = async () => {
  try {
    const res = await request.get('/membership/info');
    membershipInfo.value = { ...membershipInfo.value, ...res };
  } catch (e) {
    showToast('获取会员信息失败', 'error');
  }
};

const fetchInviteCode = async () => {
  try {
    const res = await request.get('/membership/invite-code');
    if (res && res.inviteCode) {
      inviteCode.value = res.inviteCode;
    }
  } catch (e) {
    showToast('获取邀请码失败', 'error');
  }
};

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteLink.value);
    showToast('邀请链接已复制', 'success');
  } catch (err) {
    showToast('复制失败，请手动复制', 'error');
  }
};

onMounted(() => {
  fetchMembershipInfo();
  fetchInviteCode();
});
</script>

<style scoped>
.mb-30 { margin-bottom: 30px; }

/* Banner 风格卡片 */
.membership-banner {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  padding: 30px;
  border: none;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.banner-tag {
  background: rgba(255,255,255,0.2);
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  margin-bottom: 10px;
  display: inline-block;
}

.banner-title { font-family: var(--font-serif); font-size: 1.6rem; margin-bottom: 8px; color: #fff; }
.banner-desc { font-size: 0.9rem; opacity: 0.8; margin: 0; }

.banner-stats { text-align: right; }
.b-stat { display: flex; flex-direction: column; align-items: flex-end; }
.b-val { font-size: 2.5rem; font-weight: 600; font-family: var(--font-serif); line-height: 1; }
.b-lbl { font-size: 0.85rem; opacity: 0.7; margin-top: 5px; }

/* SecurityBlock 风格复刻 */
.security-block { margin-bottom: 40px; }
.block-title {
  font-size: 1.1rem; color: var(--text-main); margin-bottom: 15px;
  border-left: 3px solid var(--secondary-color); padding-left: 10px; line-height: 1;
}
.block-desc { font-size: 0.9rem; color: var(--text-muted); margin-bottom: 20px; }
.divider { margin: 40px 0; height: 1px; background: rgba(0,0,0,0.05); }

/* 横向打赏设计 */
.donation-horizontal-box {
  background: rgba(0,0,0,0.02);
  border-radius: var(--radius-md);
  padding: 20px;
  border: 1px solid var(--border-subtle);
}

.donation-options-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
}

.donation-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.3s;
}

.donation-item:hover { transform: translateY(-2px); box-shadow: var(--shadow-sm); }
.donation-item.active { border-color: var(--primary-color); background: rgba(92, 110, 88, 0.05); }

.d-icon { font-size: 1.5rem; }
.d-txt { display: flex; flex-direction: column; }
.d-name { font-size: 0.9rem; font-weight: 600; color: var(--text-main); }
.d-price { font-size: 0.75rem; color: var(--primary-color); }

.donation-detail-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px dashed var(--border-subtle);
}

.donation-msg {
  font-style: italic;
  color: var(--text-body);
  font-size: 0.95rem;
  max-width: 60%;
}
.donation-msg i { color: var(--primary-light); margin-right: 8px; font-size: 1.2rem; }

.donation-qr-strip {
  display: flex;
  align-items: center;
  gap: 15px;
  background: #fff;
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-xs);
}

.mini-qr { width: 60px; height: 60px; border-radius: 4px; }
.qr-info { display: flex; flex-direction: column; }
.qr-title { font-size: 0.85rem; font-weight: 600; color: var(--success-color); }
.qr-tip { font-size: 0.7rem; color: var(--text-muted); }

/* 邀请区域 */
.reward-status-card {
  background: #fff;
  padding: 20px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-subtle);
}

.rs-header { display: flex; flex-direction: column; margin-bottom: 20px; }
.rs-label { font-size: 0.85rem; color: var(--text-muted); }
.rs-count { font-size: 1.8rem; font-weight: 600; color: var(--text-main); font-family: var(--font-serif); }

.progress-section { margin-top: 10px; }
.progress-meta { display: flex; justify-content: space-between; font-size: 0.75rem; margin-bottom: 8px; }
.target-text { color: var(--primary-color); font-weight: 600; }
.progress-bar-bg { height: 6px; background: #eee; border-radius: 3px; overflow: hidden; }
.progress-bar-fill { height: 100%; background: var(--primary-color); transition: width 1s ease; }

.invite-action-block { display: flex; flex-direction: column; justify-content: center; padding-left: 20px; }
.input-label { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 10px; display: block; }
.code-group { display: flex; }
.code-group .zen-input { flex: 1; border-radius: var(--radius-sm) 0 0 var(--radius-sm); border-right: none; }
.get-code-btn {
  background: var(--primary-color);
  color: #fff;
  border: none;
  padding: 0 20px;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  cursor: pointer;
  font-weight: 600;
  transition: 0.3s;
}
.get-code-btn:hover { background: var(--primary-dark); }
.helper-text { font-size: 0.75rem; color: var(--text-muted); margin-top: 10px; }

/* 权益说明 */
.benefit-strip-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 15px; }
.b-strip { display: flex; align-items: center; gap: 10px; font-size: 0.9rem; color: var(--text-main); }
.b-strip i { color: var(--success-color); }

@media (max-width: 800px) {
  .grid-2 { grid-template-columns: 1fr; gap: 20px; }
  .invite-action-block { padding-left: 0; }
  .donation-options-bar { flex-direction: column; }
  .donation-detail-area { flex-direction: column; gap: 20px; align-items: stretch; }
  .donation-msg { max-width: 100%; }
}
</style>
