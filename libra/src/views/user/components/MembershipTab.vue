<template>
  <div class="panel-section fade-in">
    <div class="panel-header">
      <h3 class="panel-title">会员与特权</h3>
      <p class="sub">管理您的权益、代币与邀请奖励</p>
    </div>

    <!-- 资产与额度区 -->
    <div class="glass-card stats-box mb-20" style="padding: 20px;">
      <div class="stat-item">
        <span class="lbl">当前身份</span>
        <span class="val" :class="membershipInfo.memberType === 'PRO' ? 'text-primary' : ''">
          {{ membershipInfo.memberType || 'FREE' }}
        </span>
        <span class="sub" style="font-size: 0.8rem; margin-top: 5px;" v-if="membershipInfo.memberType === 'PRO' && membershipInfo.proExpireTime">
          到期: {{ formatDate(membershipInfo.proExpireTime) }}
        </span>
      </div>
      <div class="divider-v"></div>
      <div class="stat-item">
        <span class="lbl">可用动态 Fitting (次)</span>
        <span class="val">{{ membershipInfo.tokenBalance || 0 }}</span>
        <span class="sub" style="font-size: 0.8rem; margin-top: 5px; color: #666;">
          系统每2天补充1次
        </span>
      </div>
      <div class="divider-v"></div>
      <div class="stat-item">
        <span class="lbl">成功邀请人数</span>
        <span class="val">{{ membershipInfo.totalInvites || 0 }}</span>
      </div>
    </div>

    <!-- 裂变任务舱 -->
    <div class="glass-card mb-20" style="padding: 20px;">
      <h4 style="margin-bottom: 15px; font-weight: 500;">邀请得好礼</h4>
      <p style="font-size: 0.9rem; color: #666; margin-bottom: 15px;">
        每邀请 1 位新骑友注册，即得 1 次动态分析机会；满 7 人立刻加赠 7 天 PRO 特权！
      </p>
      
      <!-- 能量槽 (进度条) -->
      <div class="progress-container" style="margin-bottom: 20px;">
        <div class="progress-bar-bg" style="background: #eee; height: 12px; border-radius: 6px; overflow: hidden; position: relative;">
          <div class="progress-bar-fill" :style="{ width: progressPercent + '%', background: '#3498db', height: '100%', transition: 'width 0.5s ease' }"></div>
        </div>
        <div style="display: flex; justify-content: space-between; margin-top: 5px; font-size: 0.8rem; color: #999;">
          <span>当前进度: {{ (membershipInfo.totalInvites || 0) % 7 }} / 7</span>
          <span>目标: 送 7 天 PRO</span>
        </div>
      </div>

      <div class="grid-2" style="gap: 15px;">
        <div class="input-wrapper">
          <label>我的专属邀请链接</label>
          <div class="input-group">
            <input type="text" :value="inviteLink" class="zen-input" readonly>
            <button class="btn-secondary" @click="copyLink" style="padding: 0 15px; white-space: nowrap; border-radius: 0 8px 8px 0; border-left: none;">
              <i class="far fa-copy"></i> 复制
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 会员订阅卡片 -->
    <div class="glass-card" style="padding: 20px;">
      <h4 style="margin-bottom: 15px; font-weight: 500;">升级 PRO 会员</h4>
      <p style="font-size: 0.9rem; color: #666; margin-bottom: 20px;">
        解锁 AI 实时监护、无限历史快照与不限次数的高频动态分析。
      </p>
      
      <div class="pricing-grid">
        <div class="pricing-card" v-for="plan in pricingPlans" :key="plan.id" :class="{ 'best-value': plan.isBest }">
          <div class="best-badge" v-if="plan.isBest">Best Value</div>
          <h5 class="plan-name">{{ plan.name }}</h5>
          <div class="plan-price">
            <span class="currency">¥</span><span class="amount">{{ plan.price }}</span><span class="period">/{{ plan.period }}</span>
          </div>
          <button class="btn-primary w-100 mt-15" @click="subscribe(plan)">立即开通</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { showToast } from '@/utils/toast.js';
import request from '@/api/request.js';

const membershipInfo = ref({});
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
    if (res.code === 200) {
      membershipInfo.value = res.data;
    }
  } catch (e) {
    console.error('获取会员信息失败', e);
  }
};

const fetchInviteCode = async () => {
  try {
    const res = await request.get('/membership/invite-code');
    if (res.code === 200) {
      inviteCode.value = res.data.inviteCode;
    }
  } catch (e) {
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
.mb-20 {
  margin-bottom: 20px;
}
.mt-15 {
  margin-top: 15px;
}
.w-100 {
  width: 100%;
}
.text-primary {
  color: #3498db;
  font-weight: bold;
}
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}
.pricing-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  position: relative;
  transition: all 0.3s ease;
  background: #fff;
}
.pricing-card:hover {
  box-shadow: 0 8px 20px rgba(0,0,0,0.05);
  transform: translateY(-2px);
}
.pricing-card.best-value {
  border: 2px solid #3498db;
}
.best-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: #fff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: bold;
  box-shadow: 0 2px 5px rgba(52, 152, 219, 0.3);
}
.plan-name {
  font-size: 1.1rem;
  color: #333;
  margin-bottom: 10px;
}
.plan-price {
  color: #333;
  margin-bottom: 10px;
}
.plan-price .currency {
  font-size: 1rem;
  font-weight: bold;
}
.plan-price .amount {
  font-size: 2rem;
  font-weight: bold;
}
.plan-price .period {
  font-size: 0.9rem;
  color: #999;
}
</style>
