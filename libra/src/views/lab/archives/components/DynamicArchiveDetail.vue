<template>
  <div class="detail-card glass-card">


            <div class="detail-header">
              <div class="dh-left">
                <span class="meta-label">DYNAMIC ANALYSIS DETAIL</span>
                <h3>动态分析详情</h3>
              </div>
              <div class="dh-right">
                <div class="pain-tags" v-if="selectedSession.painLabels && selectedSession.painLabels.length > 0">
                  <span class="pain-tag" v-for="label in selectedSession.painLabels" :key="label">
                    [{{ label }}]
                  </span>
                </div>
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
                <h4>{{ PROJECT_CONFIG.MASCOT }} 引擎正在解算</h4>
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

              <template v-else>
                <div class="result-hero">
                  <div class="score-orbit">
            <svg viewBox="0 0 100 100" class="ring-svg">
              <circle class="ring-bg" cx="50" cy="50" r="45"></circle>
              <circle class="ring-fill" cx="50" cy="50" r="45" :style="scoreRingStyle"></circle>
            </svg>
            <div class="score-content">
              <span class="value">{{ selectedSession.matchScore || '--' }}</span>
              <span class="label">MATCH</span>
            </div>
          </div>

                    <div class="summary-info">
                      <h4 :class="getScoreClass(selectedSession.matchScore)">诊断摘要已就绪</h4>
                      
                      <!-- 总结性标语 -->
                      <p class="summary-hero-text">快速预览您的核心调整建议，如需查看关节角度与轨迹剖析，请点击“查看深度报告”。</p>

                      <!-- 详细清单 -->
                      <div class="summary-text-list" v-if="groupedEquipSuggestions.length">
                        <p class="summary-text" v-for="(g, idx) in groupedEquipSuggestions" :key="idx">
                          <template v-if="g.category && g.category !== '调整建议'">
                            <strong>{{ g.category }}</strong>：
                          </template>
                          {{ g.content }}
                        </p>
                      </div>
                      <p class="summary-text" v-else>
                        已根据您的车型标准完成生物力学评估，整体处于理想范围。
                      </p>

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
                <p>{{ PROJECT_CONFIG.MASCOT }} 无法在上传的视频中精确定位关键点，请确保环境光线充足且无遮挡后重新尝试。</p>
              </div>
              <div class="action-footer">
                <button class="btn btn-outline" @click="handleDelete(selectedSession.id)">清理无效记录</button>
              </div>
            </div>
          
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { PROJECT_CONFIG } from '@/config/constants';
import dayjs from 'dayjs';

const props = defineProps({
  selectedSession: {
    type: Object,
    required: true
  },
  detailLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['viewReport', 'delete']);

const formatDate = (date, format) => date ? dayjs(date).format(format) : '--';

const getScoreClass = (score) => {
  if (!score && score !== 0) return 'text-muted';
  if (score >= 90) return 'text-success';
  if (score >= 75) return 'text-warning';
  return 'text-danger';
};

const scoreRingStyle = computed(() => {
  const score = props.selectedSession?.matchScore || 0;
  const circ = 2 * Math.PI * 45;
  return {
    strokeDasharray: circ,
    strokeDashoffset: circ - (score / 100) * circ
  };
});

// 过滤并分组器材建议，排除纯动作指导类内容
const groupedEquipSuggestions = computed(() => {
  const suggestions = props.selectedSession?.equipmentSuggestions || [];
  return suggestions
    .filter(text => {
      const s = String(text);
      return !s.includes('相位一致性') && !s.includes('踏频') && !s.includes('脊柱侧偏波动');
    })
    .map(text => {
      const match = String(text).match(/^【(.+?)】(.+)$/);
      if (match) {
        return { category: match[1], content: match[2] };
      }
      return { category: '调整建议', content: text };
    });
});

const handleViewReport = (session) => emit('viewReport', session);
const handleDelete = (id) => emit('delete', id);
</script>

<style scoped>
/* Copying relevant styles */
.detail-card {
  padding: 45px;
  min-height: 550px;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
}
.detail-header { margin-bottom: 35px; border-bottom: 1px solid #f2f2f2; padding-bottom: 25px; display: flex; justify-content: space-between; align-items: center; }
.meta-label { font-size: 0.7rem; color: #AAA; letter-spacing: 2px; font-weight: 800; }
.detail-header h3 { font-size: 1.8rem; font-family: var(--font-serif); color: var(--text-main); margin-top: 5px; margin-bottom:0;}
.bike-tag { background: #f5f7f4; color: var(--primary-color); padding: 6px 14px; border-radius: 6px; font-weight: bold; font-size: 0.9rem; }
.pain-tags { display: flex; gap: 6px; margin-bottom: 8px; justify-content: flex-end; }
.pain-tag { background-color: #fff1f0; color: #ff4d4f; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: bold; }
.zen-loader { text-align: center; padding: 40px 0; }
.loader-rings { position: relative; width: 80px; height: 80px; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; }
.ring { position: absolute; inset: 0; border: 2px solid var(--accent-gold); border-radius: 50%; animation: pulse 2s infinite; }
.ring:nth-child(2) { animation-delay: 1s; }
.ai-icon { font-size: 1.8rem; color: var(--accent-gold); }
@keyframes pulse { 0% { transform: scale(0.7); opacity: 1; } 100% { transform: scale(1.4); opacity: 0; } }
.skeleton-content { padding: 0 20px; opacity: 0.3; }
.sk-line { height: 12px; background: #eee; border-radius: 6px; margin-bottom: 15px; }
.sk-line.short { width: 50%; }
.sk-blocks { display: flex; gap: 15px; margin-top: 20px; }
.sk-b { flex: 1; height: 60px; background: #f5f5f5; border-radius: 8px; }
.result-hero { display: flex; align-items: center; gap: 40px; padding: 25px; background: #fbfbf9; border-radius: 20px; margin-bottom: 40px; }
.score-orbit { position: relative; width: 120px; height: 120px; flex-shrink: 0; }
.ring-svg { width: 100%; height: 100%; }
.ring-bg { fill: none; stroke: rgba(92, 110, 88, 0.1); stroke-width: 8; }
.ring-fill { fill: none; stroke: var(--primary-color); stroke-width: 8; stroke-linecap: round; transform: rotate(-90deg); transform-origin: 50% 50%; transition: stroke-dashoffset 1.2s ease-out; filter: drop-shadow(0 0 8px rgba(92, 110, 88, 0.3)); }
.score-content { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.score-content .value { font-family: var(--font-serif); font-size: 2.5rem; font-weight: 800; line-height: 1; color: var(--text-main); }
.score-content .label { font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; text-transform: uppercase; margin-top: 4px; display: block; }
.summary-info h4 { font-size: 1.5rem; font-family: var(--font-serif); margin-bottom: 10px; margin-top: 0;}
.summary-text-list { display: flex; flex-direction: column; gap: 5px; margin-top: 5px; margin-bottom: 5px; }
.summary-hero-text { font-size: 1.1rem; color: var(--text-main); font-weight: 600; line-height: 1.5; margin-bottom: 12px; font-family: var(--font-serif); }
.summary-text { color: var(--text-muted); line-height: 1.6; font-size: 0.95rem; margin:0;}
.meta-info { margin-top: 10px; font-size: 0.75rem; color: #AAA; }
.action-footer { margin-top: auto; display: flex; gap: 15px; }
.main-action { flex: 1; height: 50px; font-weight: bold; border-radius: 30px; }
.text-success { color: var(--primary-color); }
.text-warning { color: var(--accent-gold); }
.text-danger { color: #e74c3c; }
.error-visual { text-align: center; padding: 40px 0; }
.warn-icon { font-size: 3rem; color: #ff4d4f; margin-bottom: 15px; }

@media (max-width: 900px) {
  .detail-card { padding: 20px; min-height: auto; }
  .detail-header { flex-direction: column; align-items: flex-start; gap: 10px; }
  .dh-right { width: 100%; display: flex; justify-content: space-between; align-items: center;}
  .result-hero { flex-direction: column; text-align: center; gap: 20px; padding: 20px; }
  .action-footer { flex-direction: column; }
}
</style>
