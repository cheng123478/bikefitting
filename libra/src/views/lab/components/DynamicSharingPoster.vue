<template>
  <div class="poster-wrapper">
    <div class="poster-container" ref="posterRef" data-dynamic-poster-capture>
      <span class="frame-corner corner-top-left"></span>
      <span class="frame-corner corner-top-right"></span>
      <span class="frame-corner corner-bottom-left"></span>
      <span class="frame-corner corner-bottom-right"></span>

      <div class="paper-panel">
        <header class="poster-header">
          <div class="brand-lockup">
            <span class="brand-kicker">{{ PROJECT_CONFIG.NAME_EN }} BIOMECHANICS</span>
            <h1>动态骑行适配报告</h1>
            <p>{{ headerSummary }}</p>
          </div>
          <div class="score-seal" :class="scoreClass">
            <span>{{ calculatedScore }}</span>
            <small>{{ scoreDisplayLabel }}</small>
          </div>
        </header>

        <section class="profile-strip">
          <div class="snapshot-tile">
            <img
              v-if="sideSnapshot"
              :src="sideSnapshot.url"
              crossorigin="anonymous"
              alt="侧面骑行关键帧"
            />
            <div v-else class="snapshot-fallback">
              <i class="fas fa-person-biking"></i>
            </div>
            <span>侧面动态关键帧</span>
          </div>
          <div class="profile-copy">
            <span>{{ reportDate }} · {{ reportBikeName }}</span>
            <strong>{{ posterUsername }}</strong>
            <p>{{ riderDynamicTitle }}，本次完成 {{ viewCountLabel }} 视角动态检测。</p>
          </div>
        </section>

        <section class="stat-band">
          <div v-for="item in posterStats" :key="item.label" class="stat-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
          </div>
        </section>

        <section class="insight-list">
          <article
            v-for="item in posterSections"
            :key="item.number"
            class="insight-row"
            :class="{ warn: item.tone === 'warn' }"
          >
            <div class="insight-copy">
              <h2>[ {{ item.title }} ]</h2>
              <p>{{ item.body }}</p>
            </div>
            <span class="insight-number">{{ item.number }}</span>
          </article>
        </section>

        <footer class="poster-footer">
          <div class="rider-footer">
            <img :src="riderAvatar" alt="avatar" class="avatar-img" />
            <div>
              <strong>{{ posterUsername }}</strong>
              <span>{{ PROJECT_CONFIG.SHARE_SLOGAN_DYNAMIC }}</span>
            </div>
          </div>
          <div class="qr-section">
            <div class="qr-box">
              <qrcode-vue
                :value="shareUrl"
                :size="74"
                level="H"
                render-as="canvas"
                background="#FFFFFF"
                foreground="#2D3430"
              />
            </div>
            <span>扫码生成骑行体检</span>
          </div>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue';
import QrcodeVue from 'qrcode.vue';
import request from '@/api/request.js';
import { PROJECT_CONFIG } from '@/config/constants';
import dayjs from 'dayjs';

const props = defineProps({
  report: {
    type: Object,
    default: () => ({})
  },
  snapshotsMap: {
    type: Object,
    default: () => ({})
  }
});

const posterRef = ref(null);
const inviteCode = ref('');

const fetchInviteCode = async () => {
  try {
    const res = await request.get('/membership/invite-code');
    if (res.code === 200 && res.data?.inviteCode) {
      inviteCode.value = res.data.inviteCode;
    }
  } catch (e) {
    console.warn('未获取到邀请码，分享链接将不附带邀请参数', e);
  }
};

onMounted(fetchInviteCode);

const toNumber = (value, fallback = 0) => {
  const n = Number.parseFloat(value);
  return Number.isFinite(n) ? n : fallback;
};

const cleanText = (text) => {
  return String(text || '')
    .replace(/\.{2,}|…/g, '')
    .replace(/\s+/g, ' ')
    .trim();
};

const asSentence = (text) => {
  const source = cleanText(text).replace(/[，。；、,.]+$/g, '');
  return source ? `${source}。` : '';
};

const shareUrl = computed(() => {
  const origin = window.location.origin;
  const sid = props.report.sessionId || props.report.id;
  const reportUrl = sid ? `${origin}/lab/report/dynamic?sessionId=${sid}` : `${origin}/`;
  if (inviteCode.value) {
    return `${reportUrl}${reportUrl.includes('?') ? '&' : '?'}inviter=${inviteCode.value}`;
  }
  return reportUrl;
});

const posterUsername = computed(() => {
  if (props.report.username) return props.report.username;
  try {
    const userInfoStr = localStorage.getItem('user_info');
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      return userInfo.name || userInfo.nickname || userInfo.username || `${PROJECT_CONFIG.MASCOT} Rider`;
    }
  } catch (e) {}
  return `${PROJECT_CONFIG.MASCOT} Rider`;
});

const riderAvatar = computed(() => {
  try {
    const userInfoStr = localStorage.getItem('user_info');
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      if (userInfo.avatar) return userInfo.avatar;
    }
  } catch (e) {}
  return new URL('@/assets/image/default-avatar.png', import.meta.url).href;
});

const reportDate = computed(() => {
  if (props.report.createTime) {
    return dayjs(props.report.createTime).format('YYYY.MM.DD');
  }
  return dayjs().format('YYYY.MM.DD');
});

const reportBikeName = computed(() => {
  return props.report.bikeName || props.report.bikeModel || props.report.bike?.name || '当前单车';
});

const sideSnapshot = computed(() => {
  const list = props.snapshotsMap?.side || props.snapshotsMap?.SIDE;
  if (!Array.isArray(list) || list.length === 0) return null;

  const priority = ['k_ext', 'bdc', 'max_drop'];
  for (const key of priority) {
    const match = list.find(s => s.label && s.label.toLowerCase().includes(key));
    if (match) return match;
  }
  return list[0];
});

const allMetrics = computed(() => {
  const r = props.report || {};
  return [
    ...(r.sideMetrics || r.sideData?.metrics || []),
    ...(r.frontMetrics || r.frontData?.metrics || []),
    ...(r.backMetrics || r.backData?.metrics || [])
  ].filter(m => m && m.label);
});

const fallbackMetrics = computed(() => {
  return allMetrics.value.filter(m => {
    const label = String(m.label || '');
    return label.includes('角') || label.includes('倾') || label.includes('Tilt') || label.includes('KOPS') || label.includes('横摆');
  });
});

const safePedaling = computed(() => props.report.pedalingData || {});
const hasPedalingData = computed(() => Object.keys(safePedaling.value).length > 0);

const riderDynamicTitle = computed(() => {
  if (hasPedalingData.value) {
    const p = safePedaling.value;
    if (toNumber(p.upstrokeDragIndex, 100) < 15) return '踩踏节奏平顺';
    if (Math.abs(toNumber(p.leftPowerRatio, 50) - 50) <= 0.5) return '双侧轨迹均衡';
    if (toNumber(p.lateralWobbleLeft) < 2 && toNumber(p.lateralWobbleRight) < 2) return '膝盖轨迹稳定';
    if (toNumber(p.cadenceRpm) > 95) return '高踏频探索者';
    return '动态控制进阶';
  }
  return '进阶探索骑士';
});

const calculatedScore = computed(() => Math.round(toNumber(props.report.matchScore, 85)));
const scoreClass = computed(() => {
  if (calculatedScore.value >= 90) return 'text-success';
  if (calculatedScore.value >= 75) return 'text-warning';
  return 'text-danger';
});
const scoreLabel = computed(() => props.report.matchLevel || (calculatedScore.value >= 90 ? '优秀' : calculatedScore.value >= 75 ? '可进阶' : '待优化'));
const scoreDisplayLabel = computed(() => {
  const label = cleanText(scoreLabel.value).replace(/[（(].*?[）)]/g, '').trim();
  return label || scoreLabel.value;
});

const getUnit = (label = '') => {
  if (label.includes('踏频')) return 'RPM';
  if (label.includes('角') || label.includes('倾') || label.includes('Tilt') || label.includes('度')) return '°';
  if (label.includes('KOPS') || label.includes('横摆') || label.includes('外张') || label.includes('摇摆') || label.includes('下坠') || label.includes('Flare')) return '%';
  if (label.includes('跨度') || label.includes('偏离') || label.includes('距')) return 'mm';
  return '';
};

const getMetricShortLabel = (label = '') => {
  if (label.includes('伸展')) return '膝伸展角';
  if (label.includes('横摆') || label.includes('Wobble') || label.includes('晃')) return '膝横摆';
  if (label.includes('KOPS')) return '膝前后位';
  if (label.includes('躯干') || label.includes('Trunk')) return '躯干角度';
  if (label.includes('髋') || label.includes('Hip')) return '髋角';
  if (label.includes('踝') || label.includes('Ankle')) return '踝角';
  if (label.includes('肘') || label.includes('外张') || label.includes('Flare')) return '肘部外张';
  if (label.includes('肩')) return '肩部稳定';
  if (label.includes('骨盆')) return '骨盆稳定';
  return String(label).replace(/\s+/g, '').slice(0, 8);
};

const formatMetricValue = (metric) => {
  if (!metric) return '暂无关键指标';
  const unit = getUnit(metric.label);
  return `${getMetricShortLabel(metric.label)} ${metric.currentValue ?? '--'}${unit}`;
};

const keyMetric = computed(() => {
  const list = fallbackMetrics.value;
  if (!list.length) return null;
  const match = list.find(m => String(m.label).includes('伸展') || String(m.label).includes('前倾') || String(m.label).includes('侧倾'));
  return match || list[0];
});

const secondaryMetric = computed(() => {
  return fallbackMetrics.value.find(m => m !== keyMetric.value) || null;
});

const viewCount = computed(() => {
  if (props.report.viewsCount) return props.report.viewsCount;
  const metricViews = ['side', 'front', 'back'].filter(v => {
    return (props.report[`${v}Metrics`]?.length || props.report[`${v}Data`]?.metrics?.length);
  }).length;
  const snapshotViews = Object.values(props.snapshotsMap || {}).filter(list => Array.isArray(list) && list.length).length;
  return Math.max(metricViews, snapshotViews, 1);
});

const viewCountLabel = computed(() => `${viewCount.value} 个`);

const snapshotCount = computed(() => {
  return Object.values(props.snapshotsMap || {}).reduce((sum, list) => {
    return sum + (Array.isArray(list) ? list.length : 0);
  }, 0);
});

const injuryWarning = computed(() => {
  if (hasPedalingData.value) {
    const lWobble = toNumber(safePedaling.value.lateralWobbleLeft);
    const rWobble = toNumber(safePedaling.value.lateralWobbleRight);
    const maxWobble = Math.max(lWobble, rWobble);
    const badLeg = maxWobble === rWobble ? '右膝' : '左膝';
    if (maxWobble > 15) {
      return `检测到${badLeg}存在 ${maxWobble.toFixed(1)}% 侧向晃动，建议复核锁片角度、鞋垫支撑与髋部稳定性。`;
    }
  }

  const ext = fallbackMetrics.value.find(m => String(m.label).includes('伸展'));
  const extValue = ext ? toNumber(ext.currentValue, null) : null;
  if (extValue != null && extValue > 155) return `膝关节伸展度 ${ext.currentValue}° 偏大，建议复核座高并关注膝后侧不适反馈。`;
  if (extValue != null && extValue < 145) return `膝关节伸展度 ${ext.currentValue}° 偏小，建议复核座高并关注膝前侧压力感。`;
  return null;
});

const powerPotential = computed(() => {
  if (hasPedalingData.value) {
    const drag = toNumber(safePedaling.value.upstrokeDragIndex);
    if (drag > 20) return `上提阶段轨迹还不够平顺，建议用轻齿比稳定踏频，减少无效阻力。`;
  }
  return '躯干与下肢控制表现稳定，当前运动轨迹处于较理想区间。';
});

const criticalInsightText = computed(() => injuryWarning.value || powerPotential.value);

const issueCount = computed(() => {
  return allMetrics.value.filter(m => m.status && m.status !== 'OK').length;
});

const issueSummaryText = computed(() => {
  if (issueCount.value > 0) return `发现 ${issueCount.value} 项可优化指标`;
  return '关键姿态表现稳定';
});

const headerSummary = computed(() => {
  return `完成 ${viewCount.value} 个视角动态检测，${issueSummaryText.value}。`;
});

const headerLines = computed(() => [
  `完成 ${viewCount.value} 个视角动态检测`,
  issueSummaryText.value
]);

const profileLine = computed(() => {
  return `${riderDynamicTitle.value} · ${viewCount.value} 个视角动态检测`;
});

const firstActionAdvice = computed(() => {
  if (injuryWarning.value) {
    if (injuryWarning.value.includes('晃动')) return '复核锁片、鞋垫和髋部稳定，短距离试骑验证。';
    if (injuryWarning.value.includes('偏大')) return '微降座高，观察膝后侧反馈并复测。';
    if (injuryWarning.value.includes('偏小')) return '微升座高，观察膝前侧压力并复测。';
  }
  if (calculatedScore.value >= 90) return '保持当前设定，周期复测踩踏对称性。';
  if (calculatedScore.value >= 75) return '微调关键参数，短距离试骑验证舒适度。';
  return '先处理风险项，再复核座高、锁片与把位。';
});

const riskLines = computed(() => {
  const warning = injuryWarning.value;
  if (!warning) {
    return ['躯干与下肢控制较稳定。', '保持节奏，定期复测。'];
  }

  const wobbleMatch = warning.match(/(左膝|右膝).*?([\d.]+)%/);
  if (wobbleMatch) {
    return [`${wobbleMatch[1]}侧向晃动 ${wobbleMatch[2]}%。`, '建议复核锁片角度与髋部稳定。'];
  }

  if (warning.includes('偏大')) {
    return ['膝伸展角偏大，座高可能偏高。', '关注膝后侧反馈并复测。'];
  }

  if (warning.includes('偏小')) {
    return ['膝伸展角偏小，座高可能偏低。', '关注膝前侧压力并复测。'];
  }

  return [asSentence(warning), firstActionAdvice.value];
});

const posterStats = computed(() => {
  const p = safePedaling.value;
  const balance = hasPedalingData.value
    ? `${Math.round(toNumber(p.leftPowerRatio, 50))}/${Math.round(toNumber(p.rightPowerRatio, 50))}`
    : `${snapshotCount.value || '--'}`;

  return [
    { label: '综合评分', value: calculatedScore.value, unit: '分' },
    {
      label: hasPedalingData.value ? '平均踏频' : '采集视角',
      value: hasPedalingData.value ? Math.round(toNumber(p.cadenceRpm, 90)) : viewCount.value,
      unit: hasPedalingData.value ? 'RPM' : '个'
    },
    {
      label: hasPedalingData.value ? '左右平衡' : '关键帧',
      value: balance,
      unit: hasPedalingData.value ? '' : '张'
    }
  ];
});

const posterSections = computed(() => {
  const mainMetric = formatMetricValue(keyMetric.value);
  const secondMetric = secondaryMetric.value ? formatMetricValue(secondaryMetric.value) : '暂无辅助异常';
  const p = safePedaling.value;
  const cadence = Math.round(toNumber(p.cadenceRpm, 90));
  const left = Math.round(toNumber(p.leftPowerRatio, 50));
  const right = Math.round(toNumber(p.rightPowerRatio, 50));
  const pedalingText = hasPedalingData.value
    ? `踏频 ${cadence} RPM，左右协调 ${left}:${right}`
    : `已采集 ${viewCountLabel.value}视角，关键帧 ${snapshotCount.value || 0} 张`;

  const sections = [
    {
      number: '01',
      title: '个人动态画像',
      bodyLines: [
        `本次评分 ${calculatedScore.value} 分，状态：${scoreDisplayLabel.value}。`,
        asSentence(issueSummaryText.value)
      ]
    },
    {
      number: '02',
      title: '关键参数解读',
      bodyLines: [
        `${mainMetric}；${secondMetric}。`,
        '用于复核座高、把位与膝部压力。'
      ]
    },
    {
      number: '03',
      title: injuryWarning.value ? '风险预警重点' : '稳定性亮点',
      tone: injuryWarning.value ? 'warn' : 'good',
      bodyLines: riskLines.value
    },
    {
      number: '04',
      title: '下一步优化方案',
      bodyLines: [
        asSentence(pedalingText),
        firstActionAdvice.value
      ]
    }
  ];

  return sections.map(item => ({
    ...item,
    body: item.bodyLines.join('')
  }));
});

const drawWrappedText = (ctx, text, x, y, maxWidth, lineHeight, maxLines) => {
  const source = String(text || '').trim();
  if (!source) return y;

  let line = '';
  let lines = 0;
  for (const char of source) {
    const testLine = line + char;
    if (ctx.measureText(testLine).width > maxWidth && line) {
      ctx.fillText(line, x, y);
      y += lineHeight;
      lines += 1;
      line = char;
      if (lines >= maxLines - 1) break;
    } else {
      line = testLine;
    }
  }

  if (line && lines < maxLines) {
    ctx.fillText(line, x, y);
    y += lineHeight;
  }

  return y;
};

const fillRoundRect = (ctx, x, y, width, height, radius) => {
  ctx.beginPath();
  if (typeof ctx.roundRect === 'function') {
    ctx.roundRect(x, y, width, height, radius);
  } else {
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    ctx.lineTo(x + radius, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
  }
  ctx.fill();
};

const drawStatBox = (ctx, item, x, y, width, height, isLast) => {
  ctx.fillStyle = '#EEF1EA';
  ctx.fillRect(x, y, width, height);
  if (!isLast) {
    ctx.strokeStyle = 'rgba(92, 110, 88, 0.2)';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(x + width, y + 18);
    ctx.lineTo(x + width, y + height - 18);
    ctx.stroke();
  }

  ctx.textAlign = 'center';
  ctx.fillStyle = '#787673';
  ctx.font = '700 24px "Microsoft YaHei", sans-serif';
  ctx.fillText(item.label, x + width / 2, y + 38);
  ctx.fillStyle = '#465543';
  ctx.font = '900 54px Georgia, "Times New Roman", serif';
  ctx.fillText(String(item.value), x + width / 2 - (item.unit ? 12 : 0), y + 92);
  if (item.unit) {
    ctx.font = '700 22px "Microsoft YaHei", sans-serif';
    ctx.fillStyle = '#787673';
    ctx.fillText(item.unit, x + width / 2 + 48, y + 92);
  }
  ctx.textAlign = 'left';
};

const drawPosterSection = (ctx, item, x, y, width, height) => {
  ctx.strokeStyle = 'rgba(92, 110, 88, 0.3)';
  ctx.lineWidth = 2;
  ctx.setLineDash([8, 8]);
  ctx.beginPath();
  ctx.moveTo(x, y + height);
  ctx.lineTo(x + width, y + height);
  ctx.stroke();
  ctx.setLineDash([]);

  ctx.fillStyle = item.tone === 'warn' ? '#C18C7E' : '#465543';
  ctx.font = '900 32px "Microsoft YaHei", sans-serif';
  ctx.fillText(`[ ${item.title} ]`, x, y + 39);

  ctx.fillStyle = '#3A3A38';
  ctx.font = '500 25px "Microsoft YaHei", sans-serif';
  const lines = item.bodyLines || [item.body];
  lines.slice(0, 2).forEach((line, index) => {
    ctx.fillText(line, x, y + 76 + index * 34);
  });

  ctx.textAlign = 'right';
  ctx.fillStyle = 'rgba(181, 164, 149, 0.72)';
  ctx.font = 'italic 96px Georgia, "Times New Roman", serif';
  ctx.fillText(item.number, x + width, y + 104);
  ctx.textAlign = 'left';
};

const generateCanvasPoster = async () => {
  await nextTick();
  await new Promise(resolve => setTimeout(resolve, 80));

  const width = 1200;
  const height = 1600;
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;

  const ctx = canvas.getContext('2d');

  ctx.fillStyle = '#5C6E58';
  ctx.fillRect(0, 0, width, height);

  ctx.fillStyle = 'rgba(255, 255, 255, 0.08)';
  for (let i = -200; i < width; i += 88) {
    ctx.fillRect(i, 0, 3, height);
  }

  ctx.fillStyle = '#465543';
  ctx.beginPath();
  ctx.moveTo(96, 96);
  ctx.lineTo(250, 96);
  ctx.lineTo(96, 250);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(width - 96, 96);
  ctx.lineTo(width - 250, 96);
  ctx.lineTo(width - 96, 250);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(96, height - 96);
  ctx.lineTo(250, height - 96);
  ctx.lineTo(96, height - 250);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(width - 96, height - 96);
  ctx.lineTo(width - 250, height - 96);
  ctx.lineTo(width - 96, height - 250);
  ctx.closePath();
  ctx.fill();

  ctx.strokeStyle = '#D4AF37';
  ctx.lineWidth = 8;
  ctx.strokeRect(96, 86, 1008, 1428);
  ctx.fillStyle = '#F9F8F6';
  ctx.fillRect(126, 116, 948, 1368);
  ctx.strokeStyle = '#E1DDD3';
  ctx.lineWidth = 24;
  ctx.strokeRect(112, 102, 976, 1396);
  ctx.strokeStyle = 'rgba(92, 110, 88, 0.16)';
  ctx.lineWidth = 3;
  ctx.strokeRect(150, 136, 900, 1328);

  const x = 190;
  const contentWidth = 820;

  ctx.fillStyle = '#D4AF37';
  ctx.font = '800 22px "Microsoft YaHei", sans-serif';
  ctx.fillText(`${PROJECT_CONFIG.NAME_EN} BIOMECHANICS`, x, 210);
  ctx.fillStyle = '#465543';
  ctx.font = '900 74px "Microsoft YaHei", sans-serif';
  ctx.fillText('动态骑行适配报告', x, 292);

  ctx.fillStyle = '#3A3A38';
  ctx.font = '500 27px "Microsoft YaHei", sans-serif';
  headerLines.value.forEach((line, index) => {
    ctx.fillText(line, x, 338 + index * 42);
  });

  ctx.textAlign = 'right';
  ctx.font = 'italic 144px Georgia, "Times New Roman", serif';
  ctx.fillStyle = '#8FA08B';
  ctx.fillText(String(calculatedScore.value), 990, 296);
  ctx.textAlign = 'left';
  ctx.fillStyle = '#465543';
  ctx.font = '800 28px "Microsoft YaHei", sans-serif';
  drawWrappedText(ctx, scoreDisplayLabel.value, 820, 330, 170, 36, 2);

  ctx.strokeStyle = 'rgba(92, 110, 88, 0.36)';
  ctx.lineWidth = 2;
  ctx.setLineDash([8, 8]);
  ctx.beginPath();
  ctx.moveTo(x, 410);
  ctx.lineTo(x + contentWidth, 410);
  ctx.stroke();
  ctx.setLineDash([]);

  ctx.fillStyle = '#5C6E58';
  ctx.font = '800 24px "Microsoft YaHei", sans-serif';
  ctx.fillText(`${reportDate.value} · ${reportBikeName.value}`, x, 464);
  ctx.fillStyle = '#2D3430';
  ctx.font = '900 38px "Microsoft YaHei", sans-serif';
  ctx.fillText(posterUsername.value, x, 516);
  ctx.fillStyle = '#3A3A38';
  ctx.font = '500 27px "Microsoft YaHei", sans-serif';
  ctx.fillText(profileLine.value, x, 558);

  const statY = 620;
  const statW = contentWidth / 3;
  posterStats.value.forEach((item, index) => {
    drawStatBox(ctx, item, x + index * statW, statY, statW, 122, index === posterStats.value.length - 1);
  });

  const sections = posterSections.value;
  const rowStart = 750;
  const rowH = 126;
  sections.forEach((item, index) => {
    drawPosterSection(ctx, item, x, rowStart + index * rowH, contentWidth, rowH);
  });

  ctx.strokeStyle = 'rgba(92, 110, 88, 0.36)';
  ctx.lineWidth = 2;
  ctx.setLineDash([8, 8]);
  ctx.beginPath();
  ctx.moveTo(x, 1278);
  ctx.lineTo(x + contentWidth, 1278);
  ctx.stroke();
  ctx.setLineDash([]);

  // Bottom footer area - balanced layout
  const footerY = 1300;
  const footerHeight = 180;

  // Left side: slogan area
  ctx.textAlign = 'left';
  ctx.fillStyle = '#465543';
  ctx.font = '900 36px "Microsoft YaHei", sans-serif';
  ctx.fillText(PROJECT_CONFIG.SHARE_SLOGAN_DYNAMIC, x, footerY + 58);

  ctx.fillStyle = '#787673';
  ctx.font = '600 22px "Microsoft YaHei", sans-serif';
  ctx.fillText(`RideTrace AI · 守护您的每一次踩踏`, x, footerY + 100);
  ctx.fillText(`${reportDate.value} · ${posterUsername.value}`, x, footerY + 134);

  // Right side: QR code card
  const qrSize = 156;
  const qrPadding = 16;
  const qrCardWidth = qrSize + qrPadding * 2;
  const qrCardHeight = qrSize + qrPadding * 2 + 36; // extra for text
  const qrX = x + contentWidth - qrCardWidth;
  const qrY = footerY + (footerHeight - qrCardHeight) / 2 - 10;

  // QR card white background with rounded corners and subtle border
  ctx.fillStyle = '#FFFFFF';
  ctx.beginPath();
  if (typeof ctx.roundRect === 'function') {
    ctx.roundRect(qrX, qrY, qrCardWidth, qrCardHeight, 12);
  } else {
    const r = 12;
    ctx.moveTo(qrX + r, qrY);
    ctx.lineTo(qrX + qrCardWidth - r, qrY);
    ctx.quadraticCurveTo(qrX + qrCardWidth, qrY, qrX + qrCardWidth, qrY + r);
    ctx.lineTo(qrX + qrCardWidth, qrY + qrCardHeight - r);
    ctx.quadraticCurveTo(qrX + qrCardWidth, qrY + qrCardHeight, qrX + qrCardWidth - r, qrY + qrCardHeight);
    ctx.lineTo(qrX + r, qrY + qrCardHeight);
    ctx.quadraticCurveTo(qrX, qrY + qrCardHeight, qrX, qrY + qrCardHeight - r);
    ctx.lineTo(qrX, qrY + r);
    ctx.quadraticCurveTo(qrX, qrY, qrX + r, qrY);
    ctx.closePath();
  }
  ctx.fill();
  ctx.strokeStyle = 'rgba(92, 110, 88, 0.12)';
  ctx.lineWidth = 1;
  ctx.stroke();

  const qrCanvas = posterRef.value?.querySelector('.qr-box canvas');
  if (qrCanvas) {
    ctx.drawImage(qrCanvas, qrX + qrPadding, qrY + qrPadding, qrSize, qrSize);
  } else {
    ctx.strokeStyle = '#465543';
    ctx.lineWidth = 4;
    ctx.strokeRect(qrX + qrPadding, qrY + qrPadding, qrSize, qrSize);
    ctx.fillStyle = '#465543';
    ctx.font = '700 28px "Microsoft YaHei", sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('QR', qrX + qrCardWidth / 2, qrY + qrPadding + qrSize / 2 + 10);
    ctx.textAlign = 'left';
  }

  // QR tip text centered under QR code
  ctx.fillStyle = '#5C6E58';
  ctx.font = '700 20px "Microsoft YaHei", sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('扫码查看完整报告', qrX + qrCardWidth / 2, qrY + qrCardHeight - 14);
  ctx.textAlign = 'left';

  return canvas.toDataURL('image/jpeg', 0.94);
};

defineExpose({
  generateImage: async () => {
    try {
      if (document.fonts && document.fonts.ready) {
        await document.fonts.ready;
      }
      return await generateCanvasPoster();
    } catch (err) {
      console.warn('海报画布生成失败:', err);
      return await generateCanvasPoster();
    }
  }
});
</script>

<style scoped>
.poster-wrapper {
  width: 375px;
  background-color: transparent;
  display: flex;
  justify-content: center;
}

.poster-container {
  width: 375px;
  height: 500px;
  position: relative;
  overflow: hidden;
  padding: 14px;
  box-sizing: border-box;
  background:
    linear-gradient(135deg, rgba(255,255,255,0.16) 0 1px, transparent 1px 22px),
    #5C6E58;
  color: #2D3430;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.frame-corner {
  position: absolute;
  width: 60px;
  height: 60px;
  background: #465543;
  z-index: 2;
}

.corner-top-left {
  top: 14px;
  left: 14px;
  clip-path: polygon(0 0, 100% 0, 0 100%);
}

.corner-top-right {
  top: 14px;
  right: 14px;
  clip-path: polygon(0 0, 100% 0, 100% 100%);
}

.corner-bottom-left {
  bottom: 14px;
  left: 14px;
  clip-path: polygon(0 0, 0 100%, 100% 100%);
}

.corner-bottom-right {
  right: 14px;
  bottom: 14px;
  clip-path: polygon(100% 0, 0 100%, 100% 100%);
}

.paper-panel {
  position: relative;
  z-index: 3;
  height: 472px;
  overflow: hidden;
  padding: 16px 16px 12px;
  border: 8px solid #E1DDD3;
  background:
    linear-gradient(90deg, rgba(92,110,88,0.08) 1px, transparent 1px) 38px 0 / 1px 100% no-repeat,
    #F9F8F6;
  box-shadow: inset 0 0 0 1px rgba(92,110,88,0.12);
}

.paper-panel::before,
.paper-panel::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  border-top: 1px dashed rgba(45,52,48,0.45);
  pointer-events: none;
}

.paper-panel::before {
  top: 126px;
}

.paper-panel::after {
  bottom: 78px;
}

.poster-header {
  display: grid;
  grid-template-columns: 1fr 64px;
  gap: 8px;
  align-items: start;
  padding-bottom: 8px;
}

.brand-lockup {
  min-width: 0;
}

.brand-kicker {
  display: block;
  margin-bottom: 4px;
  font-size: 0.54rem;
  font-weight: 800;
  color: #D4AF37;
  letter-spacing: 1.4px;
}

.brand-lockup h1 {
  margin: 0;
  color: #465543;
  font-size: 1.46rem;
  line-height: 1.05;
  font-weight: 900;
  letter-spacing: 0;
}

.brand-lockup p {
  margin: 6px 0 0;
  max-width: 218px;
  color: #77716C;
  font-size: 0.62rem;
  line-height: 1.45;
}

.score-seal {
  width: 64px;
  height: 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(92,110,88,0.28);
  border-radius: 50%;
  background: #EEF1EA;
  color: #465543;
}

.score-seal span {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 1.78rem;
  font-weight: 800;
  line-height: 0.9;
  font-style: italic;
}

.score-seal small {
  margin-top: 3px;
  max-width: 58px;
  font-size: 0.48rem;
  line-height: 1.15;
  text-align: center;
  color: #787673;
}

.score-seal.text-success {
  color: #557761;
  border-color: rgba(85,119,97,0.28);
}

.score-seal.text-warning {
  color: #B58A2B;
  border-color: rgba(181,138,43,0.3);
}

.score-seal.text-danger {
  color: #C18C7E;
  border-color: rgba(193,140,126,0.34);
}

.profile-strip {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 10px;
  align-items: center;
  padding: 4px 0 8px;
}

.snapshot-tile {
  position: relative;
  height: 62px;
  overflow: hidden;
  border: 1px solid rgba(92,110,88,0.18);
  background: #EEF1EA;
}

.snapshot-tile img,
.snapshot-fallback {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.snapshot-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(92,110,88,0.24);
  font-size: 1.8rem;
}

.snapshot-tile span {
  position: absolute;
  left: 6px;
  bottom: 6px;
  padding: 3px 6px;
  background: rgba(45,52,48,0.72);
  color: #fff;
  font-size: 0.46rem;
  font-weight: 700;
}

.profile-copy {
  min-width: 0;
}

.profile-copy span {
  display: block;
  margin-bottom: 4px;
  color: #5C6E58;
  font-size: 0.52rem;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.profile-copy strong {
  display: block;
  color: #2D3430;
  font-size: 0.9rem;
  font-weight: 900;
  line-height: 1.15;
}

.profile-copy p {
  margin: 4px 0 0;
  color: #69635F;
  font-size: 0.58rem;
  line-height: 1.4;
}

.stat-band {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 0 0 4px;
  background: #E9F1EC;
  border: 1px solid rgba(85,119,97,0.18);
}

.stat-item {
  padding: 6px 6px;
  text-align: center;
  border-right: 1px solid rgba(85,119,97,0.16);
}

.stat-item:last-child {
  border-right: none;
}

.stat-item span {
  display: block;
  color: #687970;
  font-size: 0.48rem;
  font-weight: 800;
}

.stat-item strong {
  display: block;
  margin-top: 2px;
  color: #465543;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 1.12rem;
  line-height: 1;
  font-weight: 900;
}

.stat-item small {
  margin-left: 2px;
  font-family: inherit;
  font-size: 0.46rem;
  color: #6B7D74;
}

.insight-list {
  padding-top: 2px;
}

.insight-row {
  display: grid;
  grid-template-columns: 1fr 48px;
  gap: 8px;
  align-items: center;
  height: 62px;
  padding: 4px 0;
  border-bottom: 1px dashed rgba(45,52,48,0.45);
}

.insight-copy h2 {
  margin: 0 0 3px;
  color: #465543;
  font-size: 0.72rem;
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: 0;
}

.insight-row.warn .insight-copy h2 {
  color: #C18C7E;
}

.insight-copy p {
  margin: 0;
  color: #67615D;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: 0.52rem;
  line-height: 1.35;
  word-break: break-word;
}

.insight-number {
  justify-self: end;
  color: rgba(181,164,149,0.72);
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 2.2rem;
  line-height: 1;
  font-style: italic;
}

.poster-footer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: center;
  padding-top: 8px;
}

.rider-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.avatar-img {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #E1DDD3;
  background: #EEF1EA;
}

.rider-footer strong {
  display: block;
  color: #2D3430;
  font-size: 0.72rem;
  line-height: 1.15;
}

.rider-footer span {
  display: block;
  margin-top: 3px;
  max-width: 170px;
  color: #787673;
  font-size: 0.5rem;
  line-height: 1.25;
}

.qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.qr-box {
  display: flex;
  padding: 4px;
  background: #fff;
  border: 1px solid rgba(45,52,48,0.12);
  box-shadow: 0 4px 12px rgba(45,52,48,0.08);
}

.qr-section span {
  color: #787673;
  font-size: 0.46rem;
  font-weight: 700;
}
</style>
