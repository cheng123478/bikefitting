<template>
  <div class="detail-card glass-card">
    <div class="detail-header">
      <div class="dh-left">
        <h3>历史基准详情</h3>
        <span class="detail-date">{{ formatDate(record.fittingTime) }}</span>
      </div>
      <div class="dh-right">
        <div class="pain-tags" v-if="record.painLabels && record.painLabels.length > 0">
          <span class="pain-tag" v-for="label in record.painLabels" :key="label">
            [{{ label }}]
          </span>
        </div>
        <div class="dh-meta">
          <span class="target-bike-label">
            Target: <strong class="text-primary">{{ record.bikeName || '通用模型' }}</strong>
          </span>
          <span v-if="record.bikeRemark" class="remark-badge">{{ record.bikeRemark }}</span>
          <button class="btn-share" @click="$emit('share')">
            <i class="fas fa-share-alt"></i> 分享报表
          </button>
        </div>
      </div>
    </div>

    <!-- 黄金坐标 (针对该车型的理想值) -->
    <div class="golden-grid">
      <div class="data-box">
        <span class="label">理想座高</span>
        <span class="val">{{ record.recSeatHeight || '--' }}<small>mm</small></span>
      </div>
      <div class="data-box">
        <span class="label">有效前伸 (Reach+)</span>
        <span class="val highlight">{{ record.recReachPlus || '--' }}<small>mm</small></span>
      </div>
      <div class="data-box">
        <span class="label">车把落差 (Drop)</span>
        <span class="val">{{ record.recHandlebarDrop || '--' }}<small>mm</small></span>
      </div>
      <div class="data-box">
        <span class="label">把横宽度建议</span>
        <span class="val">{{ record.recHandlebarWidth || '--' }}<small>mm</small></span>
      </div>
    </div>

    <!-- 当时的诊断明细 -->
    <div class="historical-diagnostics" v-if="record.details && record.details.length">
      <h4 class="diag-title"><i class="fas fa-file-medical-alt"></i> 当时的生物力学诊断</h4>
      <div class="diag-list">
        <div v-for="(detail, idx) in record.details" :key="idx" class="diag-item">
          <div class="diag-part">【{{ detail.part }}】 {{ detail.bodyFeature }}</div>
          <div class="diag-logic">{{ detail.adjustmentLogic }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  record: {
    type: Object,
    required: true
  }
});
defineEmits(['share']);

const formatDate = (timeStr) => {
  if (!timeStr) return '';
  return new Date(timeStr).toLocaleString('zh-CN', {
    dateStyle: 'long',
    timeStyle: 'short',
    hour12: false
  });
};
</script>

<style scoped>
/* --- 右侧详情面板 --- */
.detail-card { padding: 40px; background: #fff; border-radius: 12px; }

.detail-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  border-bottom: 2px solid rgba(0,0,0,0.03); padding-bottom: 20px; margin-bottom: 30px;
}
.dh-left h3 { margin: 0 0 12px 0; font-size: 1.6rem; color: var(--text-main); font-family: var(--font-serif); }
.dh-meta { display: flex; align-items: center; gap: 15px; flex-wrap: wrap; justify-content: flex-end; }
.target-bike-label { font-size: 0.9rem; color: var(--text-muted); background: #f4f6f3; padding: 4px 10px; border-radius: 4px; }
.remark-badge { font-size: 0.85rem; color: var(--primary-color); font-style: italic; }
.btn-share {
  background: none; border: 1px solid var(--primary-color); color: var(--primary-color);
  padding: 4px 12px; border-radius: 6px; font-size: 0.85rem; cursor: pointer;
  transition: 0.3s; display: flex; align-items: center; gap: 6px;
}
.btn-share:hover { background: var(--primary-color); color: white; }
.detail-date { color: var(--text-muted); font-size: 1rem; font-family: var(--font-serif); }

/* 痛点标签样式 */
.pain-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  justify-content: flex-end;
}

.pain-tag {
  color: #CF1322;
  font-size: 0.85rem;
  font-weight: bold;
}

/* 黄金参数网格 */
.golden-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 40px; }
.data-box { background: #F9F9F7; padding: 25px 20px; border-radius: 8px; text-align: center; border: 1px solid #EEE; transition: 0.3s; }
.data-box:hover { border-color: var(--primary-light); background: #fff; box-shadow: var(--shadow-sm); }
.data-box .label { display: block; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 8px; text-transform: uppercase; letter-spacing: 1px; }
.data-box .val { font-size: 2.2rem; font-family: var(--font-serif); font-weight: bold; color: var(--text-main); line-height: 1; }
.data-box .val.highlight { color: var(--primary-color); }
.data-box small { font-size: 1rem; color: #999; margin-left: 4px; font-weight: normal; }

/* 诊断明细 */
.diag-title { font-size: 1.2rem; margin-bottom: 20px; color: var(--text-main); display: flex; align-items: center; gap: 10px; }
.diag-title i { color: var(--accent-gold); }
.diag-list { display: flex; flex-direction: column; gap: 15px; }
.diag-item { background: rgba(92, 110, 88, 0.03); padding: 20px; border-radius: 8px; border-left: 4px solid var(--primary-color); }
.diag-part { font-weight: bold; color: var(--primary-color); font-size: 0.95rem; margin-bottom: 8px; }
.diag-logic { font-size: 0.95rem; color: var(--text-body); line-height: 1.6; }

@media (max-width: 900px) {
  .detail-header {
    flex-direction: column;
    gap: 15px;
  }
  .dh-meta, .pain-tags {
    justify-content: flex-start;
  }
  .golden-grid { grid-template-columns: 1fr; gap: 10px; }
  .data-box { padding: 15px; }
  .data-box .val { font-size: 1.8rem; }
  .detail-card { padding: 20px; }
}
</style>
