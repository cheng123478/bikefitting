<template>
  <div class="history-page">
    <div class="container">
      <div class="history-layout">
        <!-- 左侧：时间轴 (Timeline) - 侧重索引与状态 -->
        <aside class="timeline-column fade-up">
          <div v-if="loading" class="loading-text">
            <i class="fas fa-circle-notch fa-spin"></i> 调取历史档案...
          </div>
          <div v-else-if="sessions.length === 0" class="empty-text">尚无动态分析记录</div>

          <div v-else class="timeline">
            <div
                v-for="session in sessions"
                :key="session.id"
                class="timeline-item"
                :class="{
                  'is-active': selectedSession?.id === session.id,
                  'is-processing': session.status === 1
                }"
                @click="selectSession(session)"
            >
              <div class="timeline-marker"></div>
              <div class="timeline-content glass-card">
                <div class="record-head">
                  <!-- 上部：日期时间 -->
                  <span class="record-date">{{ formatDate(session.sessionTime, 'YYYY-MM-DD HH:mm') }}</span>
                  <!-- 右侧：状态标签 -->
                  <span v-if="session.status === 1" class="status-tag processing">分析中</span>
                  <span v-else-if="session.status === 3" class="status-tag failed">失败</span>
                </div>

                <div class="record-body">
                  <div class="bike-info-row">
                    <i class="fas fa-bicycle"></i>
                    <span class="bike-name">{{ session.bikeName || '未知战车' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <div class="pagination-box" v-if="hasMore">
              <button class="btn-load-more" @click="handleLoadMore" :disabled="isLoadingMore">
                <i class="fas" :class="isLoadingMore ? 'fa-spinner fa-spin' : 'fa-chevron-down'"></i>
                {{ isLoadingMore ? '正在读取' : '展开更早的记录' }}
              </button>
            </div>
          </div>
        </aside>

        <!-- 右侧：详情面板 (Details Panel) -->
        <main class="detail-column fade-up delay-1">
          <div v-if="selectedSession" class="detail-card glass-card">

            <div class="detail-header">
              <div class="dh-left">
                <span class="meta-label">DYNAMIC ANALYSIS DETAIL</span>
                <h3>动态分析详情</h3>
              </div>
              <div class="dh-right">
                <span class="bike-tag">
                   <i class="fas fa-tag"></i> {{ selectedSession.bikeName }}
                </span>
              </div>
            </div>

            <!-- 状态 1：正在分析 -->
            <div v-if="selectedSession.status === 1" class="state-container processing">
              <div class="zen-loader">
                <div class="loader-rings">
                  <div class="ring"></div>
                  <div class="ring"></div>
                  <i class="fas fa-microchip ai-icon"></i>
                </div>
                <h4>Aries 引擎正在解算</h4>
                <p>我们正在根据采集的视频流构建您的生物力学模型，请稍后刷新查看结果。</p>
              </div>
              <!-- 骨架屏 -->
              <div class="skeleton-content">
                <div class="sk-line"></div>
                <div class="sk-line short"></div>
                <div class="sk-blocks">
                  <div class="sk-b"></div><div class="sk-b"></div>
                </div>
              </div>
            </div>

            <!-- 状态 2：分析完成 -->
            <div v-else-if="selectedSession.status === 2" class="state-container completed">
              <div v-if="detailLoading" class="inner-loading">
                <i class="fas fa-spinner fa-spin"></i> 正在调取诊断结论...
              </div>

              <template v-else-if="diagnosisResult">
                <div class="result-hero">
                  <div class="score-viz">
                    <svg viewBox="0 0 100 100">
                      <circle class="rail" cx="50" cy="50" r="45"></circle>
                      <circle class="progress" cx="50" cy="50" r="45" :style="scoreRingStyle"></circle>
                    </svg>
                    <div class="score-text">
                      <span class="num">{{ diagnosisResult.matchScore }}</span>
                      <span class="unit">MATCH</span>
                    </div>
                  </div>

                  <div class="summary-info">
                    <h4 :class="getScoreClass(diagnosisResult.matchScore)">{{ diagnosisResult.matchLevel || '分析完成' }}</h4>
                    <p class="summary-text">{{ diagnosisResult.summary || '已根据您的车型标准完成生物力学评估，建议查看完整报告。' }}</p>
                    <div class="meta-info">
                      <span><i class="far fa-clock"></i> 采样时刻：{{ formatDate(selectedSession.sessionTime, 'HH:mm') }}</span>
                    </div>
                  </div>
                </div>

                <div class="action-footer">
                  <button class="btn btn-primary main-action" @click="handleViewReport(selectedSession)">
                    <i class="fas fa-file-alt"></i> 查看完整深度报告
                  </button>
                  <button class="btn btn-outline" @click="handleDelete(selectedSession.id)">
                    <i class="fas fa-trash-alt"></i> 删除存档
                  </button>
                </div>
              </template>
            </div>

            <!-- 状态 3：失败 -->
            <div v-else class="state-container failed">
              <div class="error-visual">
                <i class="fas fa-exclamation-triangle warn-icon"></i>
                <h4>任务解算失败</h4>
                <p>Aries 无法在上传的视频中精确定位关键点，请确保环境光线充足且无遮挡后重新尝试。</p>
              </div>
              <div class="action-footer">
                <button class="btn btn-outline" @click="handleDelete(selectedSession.id)">清理无效记录</button>
              </div>
            </div>
          </div>

          <!-- 未选中 -->
          <div v-else class="empty-detail glass-card">
            <div class="icon-circle"><i class="fas fa-history"></i></div>
            <p>请选择左侧日期以查看动态分析详情</p>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getSessionHistory, deleteSession, getDiagnosisResult } from '@/api/dynamicFitting';
import { showToast, showConfirm } from '@/utils/toast';
import dayjs from 'dayjs';

const router = useRouter();
const loading = ref(true);
const sessions = ref([]);
const selectedSession = ref(null);
const diagnosisResult = ref(null);
const detailLoading = ref(false);

// 分页逻辑
const page = ref(1);
const hasMore = ref(false);
const isLoadingMore = ref(false);

const loadHistory = async (isMore = false) => {
  if (isMore) isLoadingMore.value = true;
  else loading.value = true;

  try {
    const res = await getSessionHistory({ page: page.value, size: 10 });
    const data = res.data || res;

    if (isMore) sessions.value.push(...data.content);
    else {
      sessions.value = data.content;
      if (sessions.value.length > 0) selectSession(sessions.value[0]);
    }
    hasMore.value = sessions.value.length < data.totalElements;
  } catch (e) {
    showToast('调取历史记录失败', 'error');
  } finally {
    loading.value = false;
    isLoadingMore.value = false;
  }
};

const selectSession = async (session) => {
  selectedSession.value = session;
  diagnosisResult.value = null;

  // 只有分析完成时，才去后端查询具体的诊断结果（包含分值和建议）
  if (session.status === 2) {
    detailLoading.value = true;
    try {
      const res = await getDiagnosisResult(session.id);
      diagnosisResult.value = res.data || res;
    } catch (e) {
      console.error("加载诊断结果失败", e);
    } finally {
      detailLoading.value = false;
    }
  }
};

const handleLoadMore = () => {
  page.value++;
  loadHistory(true);
};

const formatDate = (date, format) => date ? dayjs(date).format(format) : '--';

const getScoreClass = (score) => {
  if (!score && score !== 0) return 'text-muted';
  if (score >= 90) return 'text-success';
  if (score >= 75) return 'text-warning';
  return 'text-danger';
};

const scoreRingStyle = computed(() => {
  const score = diagnosisResult.value?.matchScore || 0;
  const circ = 2 * Math.PI * 45;
  return {
    strokeDasharray: circ,
    strokeDashoffset: circ - (score / 100) * circ
  };
});

const handleViewReport = (session) => {
  router.push({path: '/lab/report/dynamic', query: {sessionId: session.id}});
};

const handleDelete = async (id) => {
  if (await showConfirm('确定要永久删除这条档案吗？')) {
    try {
      await deleteSession(id);
      showToast('历史记录已清理', 'success');
      loadHistory();
    } catch (error) {
      showToast('删除失败', 'error');
    }
  }
};

onMounted(loadHistory);
</script>

<style scoped>
.history-page {
  padding: 120px 0 60px;
}

.history-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 30px;
  align-items: start;
}

/* --- 左侧时间轴样式 --- */
.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(92, 110, 88, 0.1);
}

.timeline-item {
  position: relative;
  margin-bottom: 20px;
  cursor: pointer;
}

.timeline-marker {
  position: absolute;
  left: -26px;
  top: 22px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--primary-color);
  z-index: 2;
  transition: 0.3s;
}

.timeline-item.is-active .timeline-marker {
  background: var(--primary-color);
  transform: scale(1.3);
}

.timeline-content {
  padding: 20px;
  border: 1.5px solid transparent;
  transition: 0.3s;
}

.timeline-item.is-active .timeline-content {
  border-color: var(--primary-color);
  background: #fff;
  box-shadow: var(--shadow-md);
}

.record-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-date {
  font-size: 1rem;
  color: var(--text-muted);
  font-family: var(--font-serif);
}

.status-tag {
  font-size: 0.65rem;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
}

.status-tag.processing {
  background: rgba(212, 175, 55, 0.1);
  color: var(--accent-gold);
}

.status-tag.failed {
  background: #fff1f0;
  color: #ff4d4f;
}

.score-mini {
  font-family: var(--font-serif);
  font-weight: 800;
  font-size: 1.2rem;
}

.bike-info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.1rem;
  color: var(--text-main);
  font-weight: 600;
}

.bike-info-row i {
  color: var(--primary-color);
  opacity: 0.7;
}

/* --- 右侧详情面板样式 --- */
.detail-card {
  padding: 45px;
  min-height: 550px;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.detail-header {
  margin-bottom: 35px;
  border-bottom: 1px solid #f2f2f2;
  padding-bottom: 25px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-label {
  font-size: 0.7rem;
  color: #AAA;
  letter-spacing: 2px;
  font-weight: 800;
}

.detail-header h3 {
  font-size: 1.8rem;
  font-family: var(--font-serif);
  color: var(--text-main);
  margin-top: 5px;
}

.bike-tag {
  background: #f5f7f4;
  color: var(--primary-color);
  padding: 6px 14px;
  border-radius: 6px;
  font-weight: bold;
  font-size: 0.9rem;
}

/* 建模动画区域 */
.zen-loader {
  text-align: center;
  padding: 40px 0;
}

.loader-rings {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring {
  position: absolute;
  inset: 0;
  border: 2px solid var(--accent-gold);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.ring:nth-child(2) {
  animation-delay: 1s;
}

.ai-icon {
  font-size: 1.8rem;
  color: var(--accent-gold);
}

@keyframes pulse {
  0% {
    transform: scale(0.7);
    opacity: 1;
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
  }
}

/* 骨架屏样式 */
.skeleton-content {
  padding: 0 20px;
  opacity: 0.3;
}

.sk-line {
  height: 12px;
  background: #eee;
  border-radius: 6px;
  margin-bottom: 15px;
}

.sk-line.short {
  width: 50%;
}

.sk-blocks {
  display: flex;
  gap: 15px;
  margin-top: 20px;
}

.sk-b {
  flex: 1;
  height: 60px;
  background: #f5f5f5;
  border-radius: 8px;
}

/* 结果展示 */
.result-hero {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 25px;
  background: #fbfbf9;
  border-radius: 20px;
  margin-bottom: 40px;
}

.score-viz {
  position: relative;
  width: 130px;
  height: 130px;
}

.score-viz svg {
  transform: rotate(-90deg);
  width: 100%;
  height: 100%;
}

.score-viz .rail {
  fill: none;
  stroke: #eee;
  stroke-width: 8;
}

.score-viz .progress {
  fill: none;
  stroke: var(--primary-color);
  stroke-width: 8;
  stroke-linecap: round;
  transition: 1s ease;
}

.score-text {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-text .num {
  font-size: 2.5rem;
  font-family: var(--font-serif);
  font-weight: 800;
  color: var(--text-main);
  line-height: 1;
}

.score-text .unit {
  font-size: 0.6rem;
  color: var(--text-muted);
  margin-top: 4px;
  letter-spacing: 1px;
}

.summary-info h4 {
  font-size: 1.5rem;
  font-family: var(--font-serif);
  margin-bottom: 10px;
}

.summary-text {
  color: var(--text-muted);
  line-height: 1.6;
  font-style: italic;
  font-size: 0.95rem;
}

.meta-info {
  margin-top: 10px;
  font-size: 0.75rem;
  color: #AAA;
}

.action-footer {
  margin-top: auto;
  display: flex;
  gap: 15px;
}

.main-action {
  flex: 1;
  height: 50px;
  font-weight: bold;
  border-radius: var(--radius-pill);
}

.empty-detail {
  height: 550px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  background: #fff;
}

.icon-circle {
  width: 70px;
  height: 70px;
  background: #f8f8f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  margin-bottom: 15px;
  opacity: 0.4;
}

.text-success {
  color: var(--primary-color);
}

.text-warning {
  color: var(--accent-gold);
}

.text-danger {
  color: #e74c3c;
}

.pagination-box {
  margin-top: 20px;
}

.btn-load-more {
  width: 100%;
  padding: 12px;
  background: none;
  border: 1px dashed #ddd;
  border-radius: 8px;
  color: var(--text-muted);
  cursor: pointer;
  transition: 0.3s;
}

.btn-load-more:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}
</style>