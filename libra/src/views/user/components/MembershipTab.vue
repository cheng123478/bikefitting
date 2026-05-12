<template>
  <div class="panel-section fade-in">
    <div class="panel-header">
      <h3 class="panel-title">会员与特权</h3>
      <p class="sub">管理您的权益、代币与邀请奖励</p>
    </div>

    <!-- 资产与额度区 -->
    <div class="glass-card stats-box-row mb-30">
      <div class="stat-item">
        <span class="lbl">当前身份</span>
        <span class="val" :class="{ 'text-primary': membershipInfo.memberType === 'PRO' }">
          {{ membershipInfo.memberType || 'FREE' }}
        </span>
        <span class="sub-detail" v-if="membershipInfo.memberType === 'PRO' && membershipInfo.proExpireTime">
          到期: {{ formatDate(membershipInfo.proExpireTime) }}
        </span>
      </div>
      <div class="divider-v"></div>
      <div class="stat-item">
        <span class="lbl">可用动态 Fitting</span>
        <div class="val-group">
          <span class="val">{{ membershipInfo.tokenBalance ?? 0 }}</span>
          <span class="unit">次</span>
        </div>
        <span class="sub-detail muted">系统每2天补充1次</span>
      </div>
      <div class="divider-v"></div>
      <div class="stat-item">
        <span class="lbl">成功邀请人数</span>
        <span class="val">{{ membershipInfo.totalInvites ?? 0 }}</span>
        <span class="sub-detail muted">已获奖励: {{ membershipInfo.totalInvites ?? 0 }} 次</span>
      </div>
    </div>

    <!-- 裂变任务舱 -->
    <div class="glass-card mb-30 task-card">
      <div class="card-header-flex">
        <div class="header-info">
          <h4 class="card-subtitle-small">邀请得好礼</h4>
          <p class="card-desc-mini">
            每邀请 1 位新骑友注册，即得 1 次动态分析机会；满 7 人立刻加赠 7 天 PRO 特权！
          </p>
        </div>
        <i class="fas fa-gift gift-icon"></i>
      </div>
      
      <!-- 能量槽 (进度条) -->
      <div class="progress-section">
        <div class="progress-meta">
          <span class="progress-label">当前进度: {{ (membershipInfo.totalInvites ?? 0) % 7 }} / 7</span>
          <span class="progress-target">目标: 送 7 天 PRO</span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
      </div>

      <div class="invite-action-area">
        <div class="input-wrapper mb-0">
          <label>我的专属邀请链接</label>
          <div class="input-group">
            <input type="text" :value="inviteLink" class="zen-input" readonly>
            <button class="btn btn-primary copy-btn" @click="copyLink">
              <i class="far fa-copy"></i> 复制
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 会员订阅卡片 -->
    <div class="glass-card pricing-section">
      <div class="section-intro">
        <h4 class="card-subtitle-small">升级 PRO 会员</h4>
        <p class="card-desc-mini">解锁 AI 实时监护、无限历史快照与不限次数的高频动态分析。</p>
      </div>
      
      <div class="pricing-grid">
        <div 
          class="pricing-card" 
          v-for="plan in pricingPlans" 
          :key="plan.id" 
          :class="{ 'best-value': plan.isBest }"
        >
          <div class="best-badge" v-if="plan.isBest">
            <i class="fas fa-star"></i> 最受欢迎
          </div>
          <h5 class="plan-name">{{ plan.name }}</h5>
          <div class="plan-price">
            <span class="currency">¥</span>
            <span class="amount">{{ plan.price }}</span>
            <span class="period">/{{ plan.period }}</span>
          </div>
          <ul class="plan-features">
            <li><i class="fas fa-check"></i> 无限次动态分析</li>
            <li><i class="fas fa-check"></i> AI 实时姿态纠正</li>
            <li v-if="plan.isBest"><i class="fas fa-check"></i> 专属车辆管家</li>
          </ul>
          <button 
            class="btn w-100" 
            :class="plan.isBest ? 'btn-primary btn-glow' : 'btn-outline'"
            @click="subscribe(plan)"
          >
            立即开通
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { showToast } from '@/utils/toast.js';
import request from '@/api/request.js';

const membershipInfo = ref({
  memberType: 'FREE',
  tokenBalance: 0,
  totalInvites: 0,
  proExpireTime: null
});
const inviteCode = ref('');

const pricingPlans = [
  { id: 1, name: '连续包月', price: '19.9', period: '月', isBest: false },
  { id: 2, name: '连续包季', price: '49.9', period: '季', isBest: false },
  { id: 3, name: '年度畅享', price: '169', period: '年', isBest: true },
];

const inviteLink = computed(() => {
  if (!inviteCode.value) return '生成中...';
  // 首页地址 + inviter 参数
  return `${window.location.origin}/?inviter=${inviteCode.value}`;
});

const progressPercent = computed(() => {
  const invites = membershipInfo.value?.totalInvites || 0;
  return ((invites % 7) / 7) * 100;
});

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

const fetchMembershipInfo = async () => {
  try {
    const res = await request.get('/membership/info');
    // 拦截器已返回 res.data，直接合并即可
    membershipInfo.value = { ...membershipInfo.value, ...res };
  } catch (e) {
    showToast('获取会员信息失败', 'error');
    console.error('获取会员信息失败', e);
  }
};

const fetchInviteCode = async () => {
  try {
    const res = await request.get('/membership/invite-code');
    // 拦截器已返回 res.data，直接获取 inviteCode
    if (res && res.inviteCode) {
      inviteCode.value = res.inviteCode;
    }
  } catch (e) {
    showToast('获取邀请码失败', 'error');
    console.error('获取邀请码失败', e);
  }
};

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteLink.value);
    showToast('链接已复制到剪贴板', 'success');
  } catch (err) {
    showToast('复制失败，请手动复制', 'error');
  }
};

const subscribe = (plan) => {
  showToast('支付接口开发中，敬请期待！', 'info');
};

onMounted(() => {
  fetchMembershipInfo();
  fetchInviteCode();
});
</script>

<style scoped>
.mb-30 {
  margin-bottom: 30px;
}

.mb-0 {
  margin-bottom: 0;
}

.w-100 {
  width: 100%;
}

/* 资产看板样式 */
.stats-box-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 30px;
  background: var(--bg-surface);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  flex: 1;
}

.stat-item .lbl {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.stat-item .val {
  font-size: 1.8rem;
  font-weight: 600;
  color: var(--text-main);
  font-family: var(--font-serif);
  line-height: 1;
}

.stat-item .val-group {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-item .unit {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.sub-detail {
  font-size: 0.75rem;
  margin-top: 6px;
  color: var(--secondary-color);
  font-style: italic;
}

.sub-detail.muted {
  color: var(--text-muted);
  font-style: normal;
}

.divider-v {
  width: 1px;
  height: 40px;
  background: var(--border-subtle);
}

/* 任务卡片样式 */
.task-card {
  padding: 25px;
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.card-subtitle-small {
  font-size: 1.2rem;
  color: var(--text-main);
  margin-bottom: 5px;
  border-left: 3px solid var(--primary-color);
  padding-left: 10px;
}

.card-desc-mini {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.gift-icon {
  font-size: 2rem;
  color: var(--secondary-color);
  opacity: 0.6;
}

/* 进度条 */
.progress-section {
  margin-bottom: 25px;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 0.8rem;
}

.progress-label {
  color: var(--text-main);
  font-weight: 500;
}

.progress-target {
  color: var(--primary-color);
  font-style: italic;
}

.progress-bar-bg {
  background: rgba(0, 0, 0, 0.05);
  height: 10px;
  border-radius: 5px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-light), var(--primary-color));
  border-radius: 5px;
  transition: width 0.8s var(--ease-out-expo);
}

/* 邀请区域 */
.invite-action-area {
  background: rgba(92, 110, 88, 0.03);
  padding: 15px;
  border-radius: var(--radius-sm);
}

.copy-btn {
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  padding: 0 20px;
}

/* 定价部分 */
.pricing-section {
  padding: 25px;
}

.section-intro {
  margin-bottom: 25px;
}

.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
}

.pricing-card {
  padding: 30px 20px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  background: #fff;
  text-align: center;
  position: relative;
  transition: all var(--duration-normal) var(--ease-out-expo);
  display: flex;
  flex-direction: column;
}

.pricing-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary-light);
}

.pricing-card.best-value {
  border: 2px solid var(--primary-color);
  background: rgba(92, 110, 88, 0.02);
}

.best-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--primary-color);
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: var(--shadow-sm);
}

.plan-name {
  font-size: 1.1rem;
  margin-bottom: 15px;
  color: var(--text-muted);
}

.plan-price {
  margin-bottom: 20px;
}

.plan-price .currency {
  font-size: 1.2rem;
  color: var(--text-main);
  font-weight: 600;
}

.plan-price .amount {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-main);
  font-family: var(--font-serif);
}

.plan-price .period {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.plan-features {
  margin-bottom: 25px;
  text-align: left;
  list-style: none;
  padding: 0;
  flex: 1;
}

.plan-features li {
  font-size: 0.85rem;
  color: var(--text-body);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.plan-features li i {
  color: var(--success-color);
  font-size: 0.8rem;
}

@media (max-width: 600px) {
  .stats-box-row {
    flex-direction: column;
    gap: 20px;
  }
  .divider-v {
    width: 80%;
    height: 1px;
  }
}
</style>
