<template>
  <div class="history-page">
    <div class="container">
<!--      <header class="page-header fade-up">-->
<!--        <h1 class="title">静态几何档案</h1>-->
<!--        <p class="subtitle">记录每一阶段的骑行生物力学演变</p>-->
<!--        <button class="btn btn-outline back-btn" @click="$router.push('/lab')">-->
<!--          <i class="fas fa-arrow-left"></i> 返回控制台-->
<!--        </button>-->
<!--      </header>-->

      <div class="history-layout">
        <!-- 左侧：时间轴 (Timeline) -->
        <aside class="timeline-column fade-up delay-1">
          <div v-if="loading" class="loading-text">
            <i class="fas fa-circle-notch fa-spin"></i> 读取历史档案中...
          </div>
          <div v-else-if="historyList.length === 0" class="empty-text">暂无调教历史。</div>

          <div v-else class="timeline">
            <div
                v-for="(record, index) in historyList"
                :key="record.id"
                class="timeline-item"
                :class="{ 'is-active': selectedRecord?.id === record.id }"
                @click="selectRecord(record)"
            >
              <div class="timeline-marker"></div>
              <div class="timeline-content glass-card">

                <!-- 头部：日期与最新标记 -->
                <div class="record-head">
                  <span class="record-date">{{ formatDate(record.fittingTime) }}</span>
                  <span class="meta-tag" v-if="index === 0">LATEST</span>
                </div>

                <!-- 核心：这是为哪台车做的！ -->
                <div class="record-bike">
                  <i class="fas fa-bicycle"></i>
                  <div class="rb-text">
                    <span class="bike-name">{{ record.bikeName || '未绑定车辆 (纯人体测量)' }}</span>
                  </div>
                </div>

                <!-- 辅助数据 -->
                <div class="record-meta">
                  <span>柔韧得分: <strong>{{ record.thoracicScore || '--' }}</strong></span>
                </div>
              </div>
            </div>
            <div class="pagination-actions">
              <!-- 场景1: 正在加载更多 -->
              <div v-if="isLoadingMore" class="loading-more">
                <i class="fas fa-spinner fa-spin"></i> 加载中...
              </div>
              <!-- 场景2: 还有更多数据，显示按钮 -->
              <button
                  v-else-if="hasMore"
                  class="btn-load-more"
                  @click="handleLoadMore"
              >
                加载更多历史 <i class="fas fa-chevron-down"></i>
              </button>
              <!-- 场景3: 没有更多了，显示提示 -->
              <div v-else class="no-more-data">
                <span>●</span> 已显示全部记录
              </div>
            </div>
          </div>
        </aside>

        <!-- 右侧：详情面板 (Details Panel) -->
        <main class="detail-column fade-up delay-2">
          <div v-if="selectedRecord" class="detail-card glass-card">

              <div class="dh-right">
                <h3>历史基准详情</h3>
                <!-- 再次强调车辆目标 -->
                <div class="dh-meta">
                  <span class="target-bike-label">
                    Target: <strong class="text-primary">{{ selectedRecord.bikeName || '通用模型' }}</strong>
                  </span>
                  <span v-if="selectedRecord.bikeRemark" class="remark-badge">{{ selectedRecord.bikeRemark }}</span>
                  <button class="btn-share" @click="handleShare">
                    <i class="fas fa-share-alt"></i> 分享报表
                  </button>
                </div>
              </div>
              <span class="detail-date">{{ formatDate(selectedRecord.fittingTime) }}</span>

            <!-- 黄金坐标 (针对该车型的理想值) -->
            <div class="golden-grid">
              <div class="data-box">
                <span class="label">理想座高</span>
                <span class="val">{{ selectedRecord.recSeatHeight || '--' }}<small>mm</small></span>
              </div>
              <div class="data-box">
                <span class="label">有效前伸 (Reach+)</span>
                <span class="val highlight">{{ selectedRecord.recReachPlus || '--' }}<small>mm</small></span>
              </div>
              <div class="data-box">
                <span class="label">车把落差 (Drop)</span>
                <span class="val">{{ selectedRecord.recHandlebarDrop || '--' }}<small>mm</small></span>
              </div>
              <div class="data-box">
                <span class="label">把横宽度建议</span>
                <span class="val">{{ selectedRecord.recHandlebarWidth || '--' }}<small>mm</small></span>
              </div>
            </div>

            <!-- 当时的诊断明细 -->
            <div class="historical-diagnostics" v-if="selectedRecord.details && selectedRecord.details.length">
              <h4 class="diag-title"><i class="fas fa-file-medical-alt"></i> 当时的生物力学诊断</h4>
              <div class="diag-list">
                <div v-for="(detail, idx) in selectedRecord.details" :key="idx" class="diag-item">
                  <div class="diag-part">【{{ detail.part }}】 {{ detail.bodyFeature }}</div>
                  <div class="diag-logic">{{ detail.adjustmentLogic }}</div>
                </div>
              </div>
            </div>

          </div>

          <!-- 无选中状态 -->
          <div v-else class="empty-panel glass-card">
            <i class="fas fa-history empty-icon"></i>
            <p>请在左侧选择一条历史记录以查看当时算出的理想几何。</p>
          </div>
        </main>
      </div>
    </div>

    <!-- Sharing Preview Modal -->
    <Transition name="modal-fade">
      <div v-if="showSharePreview" class="share-preview-overlay" @click.self="showSharePreview = false">
        <div class="preview-card">
          <div class="preview-header">
            <h4>海报预览</h4>
            <button @click="showSharePreview = false">&times;</button>
          </div>
          <div class="preview-content">
            <img v-if="posterImage" :src="posterImage" alt="Poster Image" class="generated-poster">
            <div v-else class="loading-state-icon">
              <i class="fas fa-spinner fa-spin"></i> 正在生成硬核海报...
            </div>
          </div>
          <div class="preview-footer">
            <button class="btn-download" @click="downloadPoster" :disabled="!posterImage">
              <i class="fas fa-download"></i> 保存到相册
            </button>
            <p class="share-hint">长按图片也可直接发送给好友</p>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Hidden Poster for Capture -->
    <div class="hidden-poster">
      <SharingPoster v-if="selectedRecord" ref="posterRef" :result="selectedRecord" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import {getFittingResultHistory} from '@/api/staticFitting.js';
import SharingPoster from '@/components/lab/SharingPoster.vue';

const loading = ref(true);
const historyList = ref([]);
const selectedRecord = ref(null);

// Sharing logic
const posterRef = ref(null);
const posterImage = ref(null);
const showSharePreview = ref(false);

// 分页相关状态
const page = ref(1);
const pageSize = 10; // 每页显示条数
const hasMore = ref(false); // 是否还有更多数据
const isLoadingMore = ref(false); // 底部加载中的状态锁

const formatDate = (timeStr) => {
  if (!timeStr) return '';
  return new Date(timeStr).toLocaleString('zh-CN', {
    dateStyle: 'long',
    timeStyle: 'short',
    hour12: false
  });
};

const selectRecord = (record) => {
  selectedRecord.value = record;
};

const handleShare = async () => {
  showSharePreview.value = true;
  posterImage.value = null; // Reset
  
  // Wait for DOM
  await nextTick();
  
  if (posterRef.value) {
    // Small delay to ensure ECharts animation or rendering is stable
    setTimeout(async () => {
      const img = await posterRef.value.generateImage();
      posterImage.value = img;
    }, 500);
  }
};

const downloadPoster = () => {
  if (!posterImage.value) return;
  const link = document.createElement('a');
  link.href = posterImage.value;
  link.download = `RideTrace_Archive_${selectedRecord.value.fittingResultId || Date.now()}.png`;
  link.click();
};

// 核心数据获取逻辑
const fetchHistory = async (isLoadMore = false) => {
  if (isLoadMore) {
    isLoadingMore.value = true;
  } else {
    loading.value = true;
    page.value = 1; // 重新加载时重置页码
  }

  try {
    const res = await getFittingResultHistory(page.value, pageSize);
    const data = res.data || res; // 适配不同拦截器
    const newList = data.content || [];
    const total = data.page.totalElements || 0;
    if (isLoadMore) {
      // 如果是加载更多，追加到数组末尾
      historyList.value.push(...newList);
    } else {
      // 如果是首次加载，覆盖数组
      historyList.value = newList;
      // 默认选中第一条
      if (historyList.value.length > 0) {
        selectedRecord.value = historyList.value[0];
      }
    }

    // 判断是否还有下一页
    hasMore.value = historyList.value.length < total;

  } catch (err) {
    console.error('获取历史记录失败', err);
  } finally {
    loading.value = false;
    isLoadingMore.value = false;
  }
};

// 点击加载更多按钮触发
const handleLoadMore = () => {
  page.value++;
  fetchHistory(true);
};

onMounted(() => {
  fetchHistory(false); // 首次加载
});
</script>

<style scoped>
.history-page {
  padding: 120px 0 80px;
  background: transparent;
  min-height: 100vh;
}

.page-header {
  position: relative;
  margin-bottom: 50px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  padding-bottom: 20px;
}
.title { font-family: var(--font-serif); font-size: 2.5rem; color: var(--text-main); }
.subtitle { color: var(--text-muted); margin-top: 10px; font-size: 1.1rem; }
.back-btn { position: absolute; right: 0; top: 10px; }

.history-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 40px;
  align-items: start;
}

/* --- 左侧时间轴 --- */
.timeline {
  position: relative;
  padding-left: 20px;
}
.timeline::before {
  content: '';
  position: absolute; left: 0; top: 0; bottom: 0; width: 2px;
  background: rgba(92, 110, 88, 0.2);
}

.timeline-item {
  position: relative;
  margin-bottom: 25px;
  cursor: pointer;
  transition: 0.3s;
}
.timeline-item:hover { transform: translateX(5px); }

.timeline-marker {
  position: absolute; left: -26px; top: 24px; width: 14px; height: 14px;
  border-radius: 50%; background: var(--bg-page); border: 3px solid var(--primary-color);
  transition: 0.3s;
}
.timeline-item.is-active .timeline-marker {
  background: var(--primary-color); box-shadow: 0 0 10px rgba(92, 110, 88, 0.4);
}

.timeline-content { padding: 20px; border: 1px solid transparent; transition: 0.3s; }
.timeline-item.is-active .timeline-content { border-color: var(--primary-color); box-shadow: var(--shadow-md); background: #fff; }

.record-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.record-date { font-weight: bold; font-size: 1.1rem; color: var(--text-main); font-family: var(--font-serif); }
.meta-tag { background: var(--accent-gold); color: white; padding: 2px 8px; border-radius: 4px; font-size: 0.7rem; font-weight: bold; letter-spacing: 1px; }

/* 关键新增：车辆展示 */
.record-bike {
  background: rgba(0,0,0,0.03);
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 12px;
  font-size: 0.9rem;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 10px;
}
.record-bike i { color: var(--primary-color); font-size: 1.1rem; }
.rb-text { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.bike-name { font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bike-remark { font-size: 0.75rem; color: var(--primary-color); opacity: 0.7; font-style: italic; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.record-meta { font-size: 0.85rem; color: var(--text-muted); }

/* --- 右侧详情面板 --- */
.detail-card { padding: 40px; background: #fff; border-radius: 12px; }

.detail-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  border-bottom: 2px solid rgba(0,0,0,0.03); padding-bottom: 20px; margin-bottom: 30px;
}
.dh-left h3 { margin: 0 0 12px 0; font-size: 1.6rem; color: var(--text-main); font-family: var(--font-serif); }
.dh-meta { display: flex; align-items: center; gap: 15px; flex-wrap: wrap; }
.target-bike-label { font-size: 0.9rem; color: var(--text-muted); background: #f4f6f3; padding: 4px 10px; border-radius: 4px; }
.remark-badge { font-size: 0.85rem; color: var(--primary-color); font-style: italic; }
.btn-share {
  background: none; border: 1px solid var(--primary-color); color: var(--primary-color);
  padding: 4px 12px; border-radius: 6px; font-size: 0.85rem; cursor: pointer;
  transition: 0.3s; display: flex; align-items: center; gap: 6px;
}
.btn-share:hover { background: var(--primary-color); color: white; }
.detail-date { color: var(--text-muted); font-size: 1rem; font-family: var(--font-serif); }

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
/* 分页控制区样式 */
.pagination-actions {
  margin-top: 30px;
  padding-left: 20px; /* 对齐时间轴内容 */
  display: flex;
  justify-content: center;
  padding-bottom: 40px;
}

.btn-load-more {
  background: transparent;
  border: 1px solid rgba(92, 110, 88, 0.3); /* 鼠尾草绿淡色边框 */
  color: var(--text-muted);
  padding: 10px 24px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-load-more:hover {
  background: rgba(92, 110, 88, 0.05);
  color: var(--primary-color);
  border-color: var(--primary-color);
  transform: translateY(2px);
}

.loading-more {
  color: var(--primary-color);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

.no-more-data {
  font-size: 0.8rem;
  color: #CCC;
  display: flex;
  align-items: center;
  gap: 8px;
}
.no-more-data span {
  font-size: 0.5rem; /* 小圆点 */
}
/* 空面板 */
.empty-panel { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 500px; color: var(--text-muted); }
.empty-icon { font-size: 3.5rem; margin-bottom: 20px; opacity: 0.3; }

@media (max-width: 900px) {
  .history-layout { grid-template-columns: 1fr; }
  .golden-grid { grid-template-columns: 1fr; }
}

/* Sharing Preview Styles (Sync with ResultModal) */
.share-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(10px);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.preview-card {
  background: #FFF;
  border-radius: 20px;
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 40px 100px rgba(0,0,0,0.5);
}

.preview-header {
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #EEE;
}

.preview-header h4 { margin: 0; font-weight: 700; font-family: var(--font-serif); }
.preview-header button { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #999; }

.preview-content {
  padding: 20px;
  background: #F5F5F3;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.generated-poster {
  max-width: 100%;
  max-height: 60vh;
  box-shadow: 0 10px 40px rgba(0,0,0,0.15);
  border-radius: 8px;
}

.loading-state-icon {
  color: #999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  font-size: 1rem;
}

.preview-footer {
  padding: 20px;
  text-align: center;
}

.btn-download {
  background: var(--primary-color);
  color: white;
  border: none;
  padding: 14px 40px;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
  margin-bottom: 12px;
  transition: 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.btn-download:hover:not(:disabled) {
  filter: brightness(0.9);
  transform: translateY(-2px);
}

.btn-download:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.share-hint {
  font-size: 0.75rem;
  color: #999;
  margin: 0;
}

.hidden-poster {
  position: absolute;
  left: -9999px;
  top: 0;
  pointer-events: none;
}

/* Modal Transition */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>