<template>
  <div class="evolution-archives-page">
    <div class="container lab-container">

      <!-- 1. 页面头部 -->
      <div class="toolbar-row" v-scroll-reveal>
        <div class="text-group">
          <h1 class="page-title">进化档案</h1>
          <p class="page-subtitle">BIOMECHANICAL EVOLUTION / 见证每一次突破的力量</p>
        </div>

        <div class="tabs-group">
          <div class="header-actions">
            <!-- 只有在有数据的情况下才显示“隐藏/显示”按钮 -->
            <button v-if="hasVisibleData" class="btn btn-outline btn-icon-text" @click="isChartVisible = !isChartVisible">
              <i class="fas" :class="isChartVisible ? 'fa-eye-slash' : 'fa-chart-line'"></i>
              {{ isChartVisible ? '隐藏趋势图' : '显示趋势图' }}
            </button>
            <button class="btn btn-primary" @click="router.push('/lab/body')">
              <i class="fas fa-plus"></i> 校准身体档案
            </button>
          </div>
        </div>
      </div>

      <!-- 2. 机能进化走势图 (只有当存在有效数值时显示) -->
      <Transition name="expand">
        <section v-show="isChartVisible && hasVisibleData" class="chart-section glass-card">
          <div class="chart-inner-header">
            <h4 class="chart-title"><i class="fas fa-wave-square"></i> 机能进化走势</h4>
            <div class="chart-legend">
              <span class="l-item flex">柔韧性</span>
              <span class="l-item stable">稳定性</span>
            </div>
          </div>
          <div ref="lineChartRef" class="main-line-chart"></div>
        </section>
      </Transition>

      <!-- 3. 档案列表头部 -->
      <div class="list-header">
        <span class="count-info">共计 <strong>{{ pagination.totalElements }}</strong> 份档案记录</span>
        <span class="hint-text">
          <i class="fas fa-info-circle"></i>
          {{ hasVisibleData ? '勾选两项可开启深度对比' : '录入进阶身体数据后可解锁进化趋势图' }}
        </span>
      </div>

      <!-- 4. 进化时间轴列表 -->
      <div class="archives-timeline" :class="{ 'is-loading': loading }">
        <template v-if="historyList.length > 0">
          <div
              v-for="record in historyList"
              :key="record.id"
              class="archive-card glass-card"
              :class="{ 'is-selected': selectedIds.includes(record.id) }"
              @click="viewDetail(record)"
          >
            <div class="selection-box" @click.stop="toggleSelect(record.id)">
              <div class="custom-checkbox" :class="{ checked: selectedIds.includes(record.id) }">
                <i class="fas fa-check"></i>
              </div>
            </div>

            <div class="record-date">
              <span class="day">{{ formatDate(record.createTime, 'DD') }}</span>
              <span class="month">{{ formatDate(record.createTime, 'MMM') }}</span>
              <span class="year">{{ formatDate(record.createTime, 'YYYY') }}</span>
            </div>

            <div class="record-scores">
              <div class="score-main">
                <span class="num">{{ record.flexibilityScore ?? '--' }}</span>
                <span class="label">柔韧分</span>
              </div>
              <div class="score-sub">
                <span class="num">{{ record.stabilityScore ?? '--' }}</span>
                <span class="label">稳定性</span>
              </div>
            </div>

            <div class="record-portrait">
              <div class="mini-tags">
                <span v-for="tag in record.riderTags" :key="tag" class="evo-tag">{{ tag }}</span>
              </div>
              <p class="expert-preview">{{ record.expertComment || '该记录仅包含基础尺寸，未进行机能测试。' }}</p>
            </div>

            <div class="record-actions">
              <button class="btn-detail-text">
                查看报告 <i class="fas fa-arrow-right"></i>
              </button>
            </div>
          </div>
        </template>

        <div v-else-if="!loading" class="empty-state glass-card">
          <i class="fas fa-folder-open"></i>
          <p>尚未发现进化轨迹，去录入第一份档案吧</p>
        </div>
      </div>

      <!-- 分页控制 -->
      <div v-if="pagination.totalPages > 1" class="pagination-wrapper">
        <button class="btn-page" :disabled="pagination.currentPage === 0" @click="changePage(pagination.currentPage - 1)">
          <i class="fas fa-chevron-left"></i>
        </button>
        <div class="page-numbers">
          <span v-for="p in pagination.totalPages" :key="p" class="page-num"
                :class="{ active: pagination.currentPage === p - 1 }" @click="changePage(p - 1)">
            {{ p }}
          </span>
        </div>
        <button class="btn-page" :disabled="pagination.currentPage === pagination.totalPages - 1" @click="changePage(pagination.currentPage + 1)">
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>

      <!-- 底部浮动对比工具栏 -->
      <Transition name="slide-up">
        <div v-if="selectedIds.length > 0" class="selection-floating-bar">
          <div class="bar-container">
            <div class="selection-info">
              <span class="count-badge">{{ selectedIds.length }}</span>
              <span class="selection-text">已选中 {{ selectedIds.length }} 项记录</span>
            </div>
            <div class="bar-actions">
              <button class="btn-cancel" @click="selectedIds = []">取消</button>
              <button class="btn btn-primary" :disabled="selectedIds.length !== 2" @click="handleCompare">
                <i class="fas fa-columns"></i> 深度对比
              </button>
            </div>
          </div>
        </div>
      </Transition>

    </div>

    <ResultModal :visible="showDetail" :profile="currentDetail" @close="showDetail = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, watch, nextTick, onUnmounted } from 'vue';
import {createRouter as $router, useRouter} from 'vue-router';
import { useUserStore } from '@/stores/user.js';
import { getUserBodyHistory } from '@/api/userBody.js';
import ResultModal from '@/components/lab/ResultModal.vue';
import dayjs from 'dayjs';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

const router = useRouter();
const userStore = useUserStore();

// --- 状态 ---
const historyList = ref([]);
const selectedIds = ref([]);
const showDetail = ref(false);
const currentDetail = ref(null);
const loading = ref(false);
const isChartVisible = ref(true);

const pagination = reactive({
  currentPage: 0,
  totalPages: 0,
  totalElements: 0,
  size: 10
});

const formatDate = (date, fmt) => dayjs(date).format(fmt);

// --- ECharts 核心修复逻辑 ---
const lineChartRef = ref(null);
let myChart = null;

/**
 * 转换报表数据
 * 逻辑：如果数据是 null/undefined，保持为 null；如果是 0，则为 0。
 */
const chartData = computed(() => {
  if (!historyList.value.length) return [];
  return [...historyList.value].reverse().map(r => ({
    date: dayjs(r.createTime).format('MM-DD'),
    flex: (r.flexibilityScore === null || r.flexibilityScore === undefined) ? null : r.flexibilityScore,
    stable: (r.stabilityScore === null || r.stabilityScore === undefined) ? null : r.stabilityScore
  }));
});

/**
 * 判断是否具有可展示的图表数据
 */
const hasVisibleData = computed(() => {
  return chartData.value.some(d => d.flex !== null || d.stable !== null);
});

const initChart = () => {
  if (!lineChartRef.value || lineChartRef.value.offsetWidth === 0) return;
  if (!myChart) myChart = echarts.init(lineChartRef.value);

  const option = {
    color: ['#5C6E58', '#B5A495'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      formatter: function (params) {
        let res = params[0].name + '<br/>';
        params.forEach(item => {
          // 如果数据是 null，显示为“未测量”
          const val = item.value === null ? '未测量' : item.value + ' 分';
          res += item.marker + item.seriesName + ': ' + val + '<br/>';
        });
        return res;
      }
    },
    grid: { left: '3%', right: '4%', bottom: '5%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartData.value.map(d => d.date),
      axisLine: { lineStyle: { color: '#EEE' } }
    },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { type: 'dashed', color: '#F0F0F0' } } },
    series: [
      {
        name: '柔韧性', type: 'line', smooth: true,
        data: chartData.value.map(d => d.flex),
        connectNulls: false, // 核心设置：数据为空时不连接线条，形成断点
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(92, 110, 88, 0.1)' },
            { offset: 1, color: 'transparent' }
          ])}
      },
      {
        name: '稳定性', type: 'line', smooth: true,
        data: chartData.value.map(d => d.stable),
        connectNulls: false // 核心设置
      }
    ]
  };
  myChart.setOption(option);
};

// 监听数据与显隐
watch(chartData, (newVal) => {
  if (newVal.length > 0 && hasVisibleData.value) {
    nextTick(() => {
      if (!myChart) initChart();
      else {
        myChart.setOption({
          xAxis: { data: newVal.map(d => d.date) },
          series: [{ data: newVal.map(d => d.flex) }, { data: newVal.map(d => d.stable) }]
        });
        myChart.resize();
      }
    });
  }
}, { immediate: true });

watch(isChartVisible, (val) => { if (val) nextTick(() => myChart?.resize()); });

// --- 业务方法 ---
const loadHistory = async (page = 0) => {
  loading.value = true;
  try {
    const res = await getUserBodyHistory(page, pagination.size);
    const data = res.data || res;
    historyList.value = data.content || [];
    pagination.currentPage = data.number || 0;
    pagination.totalPages = data.totalPages || 0;
    pagination.totalElements = data.totalElements || 0;
  } catch (error) {
    console.error("加载失败", error);
  } finally {
    loading.value = false;
  }
};

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id);
  if (index > -1) selectedIds.value.splice(index, 1);
  else {
    if (selectedIds.value.length < 2) selectedIds.value.push(id);
    else { selectedIds.value.shift(); selectedIds.value.push(id); }
  }
};

const viewDetail = (record) => {
  currentDetail.value = record;
  showDetail.value = true;
};

const handleCompare = () => {
  router.push({ path: '/lab/body/compare', query: { ids: selectedIds.value.join(',') } });
};

const changePage = (newPage) => {
  if (newPage >= 0 && newPage < pagination.totalPages) {
    loadHistory(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

onMounted(() => { loadHistory(0); window.addEventListener('resize', () => myChart?.resize()); });
onUnmounted(() => { window.removeEventListener('resize', () => myChart?.resize()); myChart?.dispose(); });
</script>

<style scoped>
/* 样式与上版本一致，增加了断点展示的支持 */
.evolution-archives-page {
  padding-top: var(--header-height);
  min-height: 100vh;
}

.lab-container {
  padding-top: 40px;
  padding-bottom: 80px;
}

/* Toolbar */
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.page-title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  margin-bottom: 5px;
  color: var(--text-main);
  line-height: 1.1;
}

.page-subtitle {
  font-size: 1rem;
  color: var(--text-muted);
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
}

.header-actions { display: flex; gap: 15px; }

.list-header { display: flex; justify-content: space-between; align-items: center; padding: 0 10px; margin-bottom: 20px; }
.count-info { font-size: 0.95rem; color: var(--text-muted); }
.count-info strong { color: var(--primary-color); font-size: 1.2rem; margin: 0 4px; }
.hint-text { font-size: 0.85rem; color: var(--text-muted); opacity: 0.7; }

.chart-section { padding: 30px; margin-bottom: 40px; border: none; background: white; border-radius: var(--radius-md); }
.main-line-chart { height: 280px; width: 100%; }
.chart-inner-header { display: flex; justify-content: space-between; margin-bottom: 20px; }
.chart-title { font-family: var(--font-serif); color: var(--text-main); font-size: 1.1rem; }
.l-item { font-size: 0.75rem; margin-left: 15px; display: inline-flex; align-items: center; gap: 5px; }
.l-item.flex::before { content: ''; width: 8px; height: 8px; background: var(--primary-color); border-radius: 50%; }
.l-item.stable::before { content: ''; width: 8px; height: 8px; background: var(--secondary-color); border-radius: 50%; }

.archives-timeline { display: flex; flex-direction: column; gap: 16px; }
.archive-card { display: flex; align-items: center; padding: 20px 30px; cursor: pointer; transition: all 0.4s var(--ease-out-expo); border: 1px solid transparent; border-radius: var(--radius-md); background: rgba(255,255,255,0.7); }
.archive-card:hover { transform: translateX(8px); background: white; box-shadow: var(--shadow-sm); }
.archive-card.is-selected { border-color: var(--primary-color); background: rgba(92, 110, 88, 0.05); }

.selection-box { padding: 10px; margin-right: 15px; cursor: pointer; }
.custom-checkbox { width: 22px; height: 22px; border: 2px solid #D1D1D1; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: white; transition: 0.3s; background: white; }
.custom-checkbox.checked { background: var(--primary-color); border-color: var(--primary-color); }
.custom-checkbox i { font-size: 0.7rem; opacity: 0; }
.custom-checkbox.checked i { opacity: 1; }

.record-date { display: flex; flex-direction: column; line-height: 1; padding-right: 25px; border-right: 1px solid #EEE; margin-right: 25px; min-width: 70px; }
.record-date .day { font-size: 1.6rem; font-weight: 800; color: var(--text-main); }
.record-date .month { font-size: 0.75rem; text-transform: uppercase; color: var(--primary-color); font-weight: 700; margin: 4px 0; }
.record-date .year { font-size: 0.7rem; color: var(--text-muted); }

.record-scores { display: flex; gap: 20px; margin-right: 30px; }
.score-main, .score-sub { text-align: center; }
.score-main .num { font-family: var(--font-serif); font-size: 1.6rem; color: var(--primary-color); font-weight: 700; display: block; }
.score-sub .num { font-family: var(--font-serif); font-size: 1.6rem; color: var(--secondary-color); font-weight: 700; display: block; }
.record-scores .label { font-size: 0.65rem; color: var(--text-muted); }

.record-portrait { flex: 1; overflow: hidden; padding-right: 20px; }
.evo-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; background: var(--bg-page); font-size: 0.7rem; color: var(--primary-color); font-weight: 600; margin-right: 6px; margin-bottom: 6px; }
.expert-preview { font-size: 0.85rem; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; font-style: italic; }

.record-actions { margin-left: auto; }
.btn-detail-text { background: none; border: none; color: var(--primary-color); font-weight: 700; cursor: pointer; font-size: 0.9rem; display: flex; align-items: center; gap: 8px; padding: 8px 16px; transition: 0.3s; }
.btn-detail-text:hover { transform: translateX(3px); }

.selection-floating-bar { position: fixed; bottom: 30px; left: 50%; transform: translateX(-50%); width: 90%; max-width: 600px; z-index: 1000; background: var(--text-main); color: white; border-radius: 50px; padding: 12px 12px 12px 30px; box-shadow: var(--shadow-lg); }
.bar-container { display: flex; justify-content: space-between; align-items: center; }
.selection-info { display: flex; align-items: center; gap: 12px; }
.count-badge { background: var(--primary-color); color: white; width: 26px; height: 26px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: 800; }
.selection-text { font-weight: 600; font-size: 1rem; }
.bar-actions { display: flex; align-items: center; gap: 15px; }
.btn-cancel { background: none; border: none; color: #BBB; cursor: pointer; font-size: 0.9rem; }

.expand-enter-active, .expand-leave-active { transition: all 0.5s ease; max-height: 400px; }
.expand-enter-from, .expand-leave-to { max-height: 0; opacity: 0; }
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.4s var(--ease-out-expo); }
.slide-up-enter-from, .slide-up-leave-to { transform: translate(-50%, 100px); opacity: 0; }
.pagination-wrapper { margin-top: 40px; display: flex; justify-content: center; align-items: center; gap: 15px; }
.page-numbers { display: flex; gap: 6px; }
.page-num { width: 36px; height: 36px; border-radius: 6px; display: flex; align-items: center; justify-content: center; cursor: pointer; background: white; font-size: 0.85rem; transition: 0.3s; border: 1px solid #EEE; }
.page-num.active { background: var(--primary-color); color: white; border-color: var(--primary-color); }
.btn-page { background: white; border: 1px solid #EEE; width: 36px; height: 36px; border-radius: 6px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.is-loading { opacity: 0.6; pointer-events: none; }
</style>