<template>
  <section class="pedaling-section glass-card fade-up" v-if="data">
    <div class="view-header">
      <h2>
        <i class="fas fa-bolt"></i>
        踩踏运动学代理分析 (Pedaling Pro)
      </h2>
      <span class="badge" style="background: rgba(212, 175, 55, 0.1); color: var(--accent-gold);">KINEMATICS</span>
      <!-- 数据可信度标签 -->
      <span v-if="data.bilateralConfidence === 'HIGH'" class="badge confidence-high">
        <i class="fas fa-check-circle"></i> 正面双侧实测
      </span>
      <span v-else-if="data.bilateralConfidence === 'LOW'" class="badge confidence-low">
        <i class="fas fa-exclamation-triangle"></i> 侧面单腿估算
      </span>
    </div>

    <!-- 低可信度时显示说明 -->
    <div v-if="data.bilateralConfidence === 'LOW' && data.analysisNote" class="data-note">
      <i class="fas fa-info-circle"></i> {{ data.analysisNote }}
    </div>

    <!-- 1. Left/Right motion balance proxy -->
    <div class="balance-card">
      <div class="balance-header">
        <h4 class="sub-label">双侧运动幅度占比 (Motion Balance Proxy)</h4>
      </div>
      <div class="balance-track">
        <div class="balance-fill left-fill" :style="{ width: data.leftPowerRatio + '%' }">
          <span class="val">{{ data.leftPowerRatio }}%</span>
          <span class="lbl">L (左)</span>
        </div>
        <div class="balance-fill right-fill" :style="{ width: data.rightPowerRatio + '%' }">
          <span class="lbl">R (右)</span>
          <span class="val">{{ data.rightPowerRatio }}%</span>
        </div>
        <!-- Center line indicator -->
        <div class="center-mark"></div>
      </div>
      <p class="balance-desc" v-if="Math.abs(data.leftPowerRatio - 50) > 3">
        检测到双侧运动幅度偏移：右侧轨迹振幅 <template v-if="data.rightPowerRatio > 50">较大</template><template v-else>较小</template>。该值来自 2D 运动轨迹代理，不等同于功率计或压力设备数据；建议结合长短腿差异、坐垫偏移或单侧代偿习惯复核。
      </p>
      <p class="balance-desc" v-else>
        双侧运动轨迹较对称。
      </p>
    </div>

    <div class="kinematics-grid">
      <!-- 2. Motion proxy curve chart -->
      <div class="chart-box">
        <h4 class="sub-label">360° 运动代理平顺度概览 (Motion Proxy Curve)</h4>
        <div class="layman-desc">
          <strong>怎么看懂这幅图？</strong><br/>
          蓝色曲线代表左侧运动轨迹代理，红色代表右侧。曲线由视觉关键点时序生成，用来观察节奏和平顺度趋势，不代表功率计或压力设备数据。
        </div>
        <div ref="curveChartDom" class="echarts-dom"></div>
      </div>

      <!-- 2.5 360 Radar/Polar Chart -->
      <div class="chart-box">
        <h4 class="sub-label">360° 运动相位极坐标盘 (Polar Phase)</h4>
        <div class="layman-desc">
          <strong>图里的“花瓣”代表什么？</strong><br/>
          这张图把视觉轨迹代理映射到参考相位。左右面积差异提示轨迹节奏不对称，但不能单独判断哪条腿真实输出更高。
        </div>
        <div ref="radarChartDom" class="echarts-dom"></div>
      </div>
      
      <!-- 3. Key Indices -->
      <div class="indices-box full-row-idx">
        <h4 class="sub-label">踩踏工况诊断</h4>
        <div class="index-grid">
          <div class="idx-card">
            <span class="idx-lbl">左侧代理峰值参考相位</span>
            <span class="idx-v">{{ data.leftPeakAngle }}°</span>
            <span class="idx-eval" :class="getAngleClass(data.leftPeakAngle)">{{ getAngleDesc(data.leftPeakAngle) }}</span>
          </div>
          <div class="idx-card">
            <span class="idx-lbl">右侧代理峰值参考相位</span>
            <span class="idx-v">{{ data.rightPeakAngle }}°</span>
            <span class="idx-eval" :class="getAngleClass(data.rightPeakAngle)">{{ getAngleDesc(data.rightPeakAngle) }}</span>
          </div>
          <div class="idx-card">
            <span class="idx-lbl">上提不平顺代理指数</span>
            <div class="drag-meter mt-2">
              <span class="idx-v" :class="data.upstrokeDragIndex > 30 ? 'text-danger' : 'text-success'">
                {{ data.upstrokeDragIndex }}
              </span>
              <span class="text-sm ml-2" style="color:var(--text-muted)"> (越小越平顺) </span>
            </div>
            <span class="idx-eval" style="color:var(--text-muted); font-size: 0.75rem; margin-top: 5px;">
              数值偏大意味着视觉轨迹在上提阶段不够平顺，需结合实际脚感复核。
            </span>
          </div>
          
          <div class="idx-card" v-if="data.lateralWobbleLeft != null && data.lateralWobbleRight != null">
            <span class="idx-lbl">膝盖侧向位移/晃动 (Knee Wobble)</span>
            <div class="flex-row mt-2" style="display:flex; justify-content:space-between; width: 100%;">
              <div class="wobble-item text-center">
                <div class="val text-warning">{{ data.lateralWobbleLeft.toFixed(1) }} %</div>
                <div class="lbl">左膝晃幅</div>
              </div>
              <div class="wobble-item text-center">
                <div class="val text-warning">{{ data.lateralWobbleRight.toFixed(1) }} %</div>
                <div class="lbl">右膝晃幅</div>
              </div>
            </div>
            <span class="idx-eval" style="color:var(--text-muted); font-size: 0.75rem; margin-top: 8px;">
              从正面视角测量膝关节踩踏时的横向轨迹偏移（% 骨盆宽）。数值越小，说明膝部轨迹越稳定；仍需结合锁片、鞋垫和髋部控制复核。
            </span>
          </div>

          <div class="idx-card" v-if="data.cadenceRpm">
            <span class="idx-lbl">踏频 (Cadence)</span>
            <span class="idx-v" :class="data.cadenceRpm >= 80 && data.cadenceRpm <= 100 ? 'text-success' : 'text-warning'">
              {{ data.cadenceRpm }} <small style="font-size:1rem">RPM</small>
            </span>
            <span class="idx-eval" :class="data.cadenceRpm >= 80 && data.cadenceRpm <= 100 ? 'text-success' : 'text-warning'">
              {{ data.cadenceRpm >= 80 && data.cadenceRpm <= 100 ? '✓ 节奏理想 (80–100 RPM)' : data.cadenceRpm < 80 ? '偏低 (建议加快节奏)' : '偏高 (建议确认牙盘配比)' }}
            </span>
          </div>

        </div>
      </div>
    </div>

  </section>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, onUnmounted } from 'vue';
import * as echarts from 'echarts';

const props = defineProps({
  data: {
    type: Object,
    default: null
  }
});

const curveChartDom = ref(null);
const radarChartDom = ref(null);
let echartInstance = null;
let radarInstance = null;

const getAngleClass = (angle) => {
  const relativeAngle = angle % 180;
  if (relativeAngle >= 85 && relativeAngle <= 105) return 'text-success';
  return 'text-warning';
};

const getAngleDesc = (angle) => {
  const relativeAngle = angle % 180;
  if (relativeAngle < 85) return '代理峰值偏早';
  if (relativeAngle > 105) return '代理峰值偏后';
  return '参考相位正常';
};

const renderChart = () => {
  if (!props.data || !props.data.forceCurvePoints) return;
  
  const points = props.data.forceCurvePoints;
  const xAxisData = points.map(p => p.angle);
  const leftValues = points.map(p => p.leftVelocity ?? p.leftV);
  const rightValues = points.map(p => p.rightVelocity ?? p.rightV);
  const totalValues = points.map(p => p.totalVelocity ?? p.total);

  // 1. Render Linear Curve Chart
  if (curveChartDom.value) {
    if (!echartInstance) {
      echartInstance = echarts.init(curveChartDom.value);
    }
    const option = {
      color: ['#007BFF', '#DC3545', '#888888'],
      tooltip: { trigger: 'axis' },
      legend: {
        data: ['左侧代理值', '右侧代理值', '合成代理值'],
        bottom: 0,
        icon: 'roundRect'
      },
      grid: { left: '10%', right: '5%', top: '10%', bottom: '15%' },
      xAxis: {
        type: 'category',
        data: xAxisData,
        boundaryGap: false,
        axisLabel: { formatter: '{value}°' }
      },
      yAxis: {
        type: 'value',
        name: '代理指数',
        nameTextStyle: { color: 'rgba(0,0,0,0.3)', align: 'right' }
      },
      series: [
        {
          name: '左侧代理值',
          type: 'line',
          data: leftValues,
          smooth: true,
          lineStyle: { color: '#007BFF', width: 3 },
          symbol: 'none',
          markPoint: {
            data: [{ type: 'max', name: 'Max' }],
            symbolSize: 35,
            label: { formatter: '{c}', color: '#fff' },
            itemStyle: { color: '#007BFF' }
          }
        },
        {
          name: '右侧代理值',
          type: 'line',
          data: rightValues,
          smooth: true,
          lineStyle: { color: '#DC3545', width: 3 },
          symbol: 'none',
          markPoint: {
            data: [{ type: 'max', name: 'Max' }],
            symbolSize: 35,
            label: { formatter: '{c}', color: '#fff' },
            itemStyle: { color: '#DC3545' }
          }
        },
        {
          name: '合成代理值',
          type: 'line',
          data: totalValues,
          smooth: true,
          lineStyle: { color: '#888', width: 1, type: 'dashed' },
          symbol: 'none'
        }
      ]
    };
    echartInstance.setOption(option);
  }

  // 2. Render Polar Radar Chart
  if (radarChartDom.value) {
    if (!radarInstance) {
      radarInstance = echarts.init(radarChartDom.value);
    }
    
    // Process points into [value, angle] arrays for polar
    const leftPolar = points.map(p => [Math.max(0, p.leftVelocity ?? p.leftV), p.angle]);
    const rightPolar = points.map(p => [Math.max(0, p.rightVelocity ?? p.rightV), p.angle]);

    const polarOption = {
      color: ['#007BFF', '#DC3545', '#888888'],
      tooltip: { trigger: 'item' },
      legend: { show: false },
      polar: { center: ['50%', '50%'], radius: '75%' },
      angleAxis: {
        type: 'value',
        min: 0,
        max: 360,
        interval: 90,
        startAngle: 90,
        clockwise: true,
        axisLabel: {
          formatter: function(val) {
            if(val === 0) return '0° (上死点)';
            if(val === 90) return '90° (前)';
            if(val === 180) return '180° (下死点)';
            if(val === 270) return '270° (后)';
            return val + '°';
          }
        }
      },
      radiusAxis: {
        min: 0,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false }
      },
      series: [
        {
          coordinateSystem: 'polar',
          name: '左侧代理值',
          type: 'line',
          data: leftPolar,
          showSymbol: false,
          areaStyle: { color: 'rgba(0, 123, 255, 0.2)' },
          lineStyle: { color: '#007BFF', width: 2 }
        },
        {
          coordinateSystem: 'polar',
          name: '右侧代理值',
          type: 'line',
          data: rightPolar,
          showSymbol: false,
          areaStyle: { color: 'rgba(220, 53, 69, 0.2)' },
          lineStyle: { color: '#DC3545', width: 2 }
        }
      ]
    };
    radarInstance.setOption(polarOption);
  }
};

const handleResize = () => {
  if (echartInstance) echartInstance.resize();
  if (radarInstance) radarInstance.resize();
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  if (props.data) {
    nextTick(() => renderChart());
  }
});

watch(() => props.data, (newVal) => {
  if (newVal) {
    nextTick(() => renderChart());
  }
}, { deep: true });

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (echartInstance) echartInstance.dispose();
  if (radarInstance) radarInstance.dispose();
});
</script>

<style scoped>
.pedaling-section { padding: 40px; margin-bottom: 25px; }

.confidence-high { background: rgba(82, 196, 26, 0.12) !important; color: #52c41a !important; }
.confidence-low  { background: rgba(250, 173, 20, 0.12) !important; color: #faad14 !important; }

.data-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 18px;
  margin-bottom: 24px;
  border-left: 3px solid var(--accent-gold);
  background: rgba(212, 175, 55, 0.06);
  border-radius: 0 8px 8px 0;
  font-size: 0.88rem;
  color: var(--text-muted);
  line-height: 1.5;
}
.data-note i { color: var(--accent-gold); margin-top: 2px; flex-shrink: 0; }

.view-header { display: flex; align-items: center; gap: 15px; margin-bottom: 30px; border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 15px;}
.view-header h2 { font-size: 1.4rem; color: var(--text-main); display: flex; align-items: center; gap: 12px; margin: 0; }
.badge { padding: 4px 10px; border-radius: 12px; font-size: 0.7rem; font-weight: 700; letter-spacing: 1px;}

.sub-label { font-size: 1rem; color: var(--text-main); margin-bottom: 15px; }

.balance-card { margin-bottom: 30px; background: rgba(0,0,0,0.02); padding: 25px; border-radius: 12px; }
.balance-track {
  display: flex;
  height: 45px;
  border-radius: 25px;
  overflow: hidden;
  background: #eee;
  position: relative;
}
.center-mark {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(0,0,0,0.2);
  z-index: 5;
}
.balance-fill {
  display: flex;
  align-items: center;
  padding: 0 20px;
  color: white;
  font-weight: 800;
  font-size: 1.2rem;
  font-family: var(--font-serif);
  transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
}
.left-fill { background: linear-gradient(135deg, #007BFF, #00B4DB); justify-content: space-between; }
.right-fill { background: linear-gradient(135deg, #FF416C, #FF4B2B); justify-content: space-between; }
.balance-fill .lbl { font-size: 0.8rem; font-weight: 500; opacity: 0.8; font-family: sans-serif; }
.balance-desc { font-size: 0.9rem; color: var(--text-muted); margin-top: 15px; }

.kinematics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 25px; }
@media (max-width: 900px) {
  .kinematics-grid { grid-template-columns: 1fr; }
}

.chart-box { background: var(--bg-surface); padding: 20px; border-radius: 12px; border: 1px solid rgba(0,0,0,0.05); }
.layman-desc {
  background: rgba(0, 123, 255, 0.05);
  border-left: 3px solid #007BFF;
  padding: 12px 15px;
  border-radius: 4px;
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 5px;
}
.layman-desc strong { color: var(--text-main); font-size: 0.9rem; }
.echarts-dom { width: 100%; height: 350px; }

.indices-box { background: var(--bg-surface); padding: 20px; border-radius: 12px; border: 1px solid rgba(0,0,0,0.05); }
.full-row-idx { grid-column: span 2; }
@media (max-width: 900px) {
  .full-row-idx { grid-column: span 1; }
}
.index-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.idx-card { background: rgba(0,0,0,0.03); padding: 15px; border-radius: 10px; display: flex; flex-direction: column; }
.idx-lbl { font-size: 0.8rem; color: var(--text-muted); margin-bottom: 5px; }
.idx-v { font-family: var(--font-serif); font-size: 1.6rem; font-weight: 800; color: var(--text-main); }
.idx-eval { font-size: 0.75rem; margin-top: 5px; font-weight: bold; }

.text-success { color: var(--success-color); }
.text-warning { color: var(--warning-color); }
.text-danger { color: #ff4d4f; }
.text-center { text-align: center; }
.mt-2 { margin-top: 10px; }
.ml-2 { margin-left: 10px; }
.text-sm { font-size: 0.8rem; }
.flex-row { display: flex; }

.wobble-item { flex: 1; border-right: 1px solid rgba(0,0,0,0.1); }
.wobble-item:last-child { border-right: none; }
.wobble-item .val { font-size: 1.2rem; font-weight: 800; font-family: var(--font-serif); }
.wobble-item .lbl { font-size: 0.8rem; color: var(--text-muted); }
</style>
