<template>
  <div class="realtime-page">
    <div class="container fit-card-container">

      <!-- 头部 -->
      <div class="fit-header">
        <router-link to="/fitting" class="back-link">
          <i class="fas fa-chevron-left"></i> <span>结束伴骑</span>
        </router-link>
        <h2 class="section-title" style="margin:0; font-size: 1.8rem;">实时骑行姿态分析</h2>
      </div>

      <!-- 控制栏 -->
      <div class="glass-card control-bar">
        <div class="user-selector">
          <label class="section-label">监测对象：</label>
          <div class="select-wrapper">
            <select v-model="selectedUser" class="zen-select">
              <option value="self">我自己 (Current User)</option>
              <option v-for="user in teamMembers" :key="user.id" :value="user.id">{{ user.name }}</option>
            </select>
            <i class="fas fa-chevron-down select-icon"></i>
          </div>
        </div>
        <div class="action-buttons">
          <button v-if="!isRunning" class="btn btn-primary" @click="handleStart">
            <i class="fas fa-play"></i> 启动分析
          </button>
          <button v-else class="btn btn-outline" @click="handleStop">
            <i class="fas fa-stop"></i> 停止
          </button>
        </div>
      </div>

      <!-- 监测布局 -->
      <div class="monitor-layout">

        <!-- 左侧：视觉反馈 -->
        <div class="glass-card video-frame" ref="videoFrameRef">
          <div class="canvas-wrapper">
            <video ref="videoElement" class="input_video" playsinline style="display:none;"></video>
            <canvas ref="canvasElement" class="output_canvas"></canvas>

            <div v-if="!isRunning" class="placeholder-overlay">
              <div class="pulse-ring"></div>
              <i class="fas fa-bicycle"></i>
              <p>请将设备固定在侧面，距离约 2-3 米</p>
            </div>

            <div v-if="isRunning && feedback.visible" class="hud-status" :class="feedback.type">
              <i :class="feedback.icon"></i> {{ feedback.message }}
            </div>

            <button class="fullscreen-btn" @click="toggleFullscreen" title="切换全屏">
              <i :class="isFullscreen ? 'fas fa-compress' : 'fas fa-expand'"></i>
            </button>
          </div>
        </div>

        <!-- 右侧：数据仪表盘 -->
        <div class="dashboard-column">

          <div class="glass-card data-card">
            <div class="data-header">
              <i class="fas fa-arrows-alt-v"></i><span class="label-text">膝关节伸展</span>
            </div>
            <div class="data-value">{{ currentAngles.knee }}<span class="unit">°</span></div>
            <div class="data-bar">
              <div class="fill" :style="{ width: getProgress(currentAngles.knee, 135, 150) + '%', background: getStatusColor(currentAngles.knee, 135, 150) }"></div>
            </div>
            <p class="data-hint">
              <span v-if="currentAngles.knee === 0">等待数据...</span>
              <span v-else-if="currentAngles.knee < 135" class="text-danger">角度过小：座垫可能过低</span>
              <span v-else-if="currentAngles.knee > 155" class="text-danger">角度过大：座垫可能过高</span>
              <span v-else class="text-primary">区间完美：发力效率最佳</span>
            </p>
          </div>

          <div class="glass-card data-card">
            <div class="data-header">
              <i class="fas fa-user-edit"></i><span class="label-text">躯干角度</span>
            </div>
            <div class="data-value">{{ currentAngles.hip }}<span class="unit">°</span></div>
            <div class="data-bar">
              <div class="fill" :style="{ width: getProgress(currentAngles.hip, 40, 60) + '%', background: getStatusColor(currentAngles.hip, 40, 60) }"></div>
            </div>
            <p class="data-hint">相对于水平面，越低风阻越小</p>
          </div>

          <div class="glass-card data-card">
            <div class="data-header">
              <i class="fas fa-hand-rock"></i><span class="label-text">肘部弯曲</span>
            </div>
            <div class="data-value">{{ currentAngles.elbow }}<span class="unit">°</span></div>
            <div class="data-bar">
              <div class="fill" :style="{ width: getProgress(currentAngles.elbow, 150, 170) + '%', background: getStatusColor(currentAngles.elbow, 150, 170) }"></div>
            </div>
            <p class="data-hint">保持微屈吸震，避免锁死</p>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useCamera } from '@/composables/useCamera';
import { usePoseDetection } from '@/composables/usePoseDetection';

// --- UI State ---
const isFullscreen = ref(false);
const selectedUser = ref('self');
const videoElement = ref(null);
const canvasElement = ref(null);
const videoFrameRef = ref(null);

const teamMembers = [{ id: 'u1', name: '队友 A' }, { id: 'u2', name: '队友 B' }];

// --- Composable logic ---
const { isRunning, startCamera, stopCamera } = useCamera(videoElement);
const { 
  currentAngles, 
  feedback, 
  initPose, 
  sendFrame, 
  closePose, 
  resetData 
} = usePoseDetection();

// --- Lifecycle & Control ---
const handleStart = () => {
  if (isRunning.value) return;

  // Initialize Pose first
  initPose((results) => {
    drawResults(results);
  });

  // Start camera and feed frames to Pose
  startCamera(async () => {
    if (videoElement.value) {
      await sendFrame(videoElement.value);
    }
  });

  // Prepare canvas dimensions
  if (canvasElement.value) {
    canvasElement.value.width = 1280;
    canvasElement.value.height = 720;
  }
};

const handleStop = () => {
  stopCamera();
  closePose();
  resetData();
};

/**
 * Renders the analysis results onto the canvas.
 * This is the only UI-specific logic remaining in the component.
 */
const drawResults = (results) => {
  if (!canvasElement.value) return;
  const ctx = canvasElement.value.getContext('2d');
  const { width: w, height: h } = canvasElement.value;

  ctx.save();
  ctx.clearRect(0, 0, w, h);
  ctx.drawImage(results.image, 0, 0, w, h);

  if (results.poseLandmarks) {
    const lm = results.poseLandmarks;
    const isLeftView = lm[11].visibility > lm[12].visibility;
    
    // Mapping for skeletal drawing
    const idx = isLeftView ?
        { shoulder: 11, hip: 23, knee: 25, ankle: 27, elbow: 13, wrist: 15 } :
        { shoulder: 12, hip: 24, knee: 26, ankle: 28, elbow: 14, wrist: 16 };

    drawSkeleton(ctx, lm, idx, w, h);
    drawLabels(ctx, lm, idx, w, h);
  }
  ctx.restore();
};

const drawSkeleton = (ctx, lm, idx, w, h) => {
  const points = [
    lm[idx.shoulder], lm[idx.elbow], lm[idx.wrist], // Arm
    lm[idx.shoulder], lm[idx.hip], lm[idx.knee], lm[idx.ankle] // Leg
  ];

  ctx.lineWidth = 4;
  ctx.strokeStyle = '#EAE8E2';
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  
  ctx.beginPath();
  ctx.moveTo(points[0].x * w, points[0].y * h);
  points.forEach(p => ctx.lineTo(p.x * w, p.y * h));
  ctx.stroke();

  // Draw joint markers
  points.forEach(p => {
    ctx.beginPath();
    ctx.arc(p.x * w, p.y * h, 6, 0, 2 * Math.PI);
    ctx.fillStyle = '#8A9A5B';
    ctx.fill();
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 2;
    ctx.stroke();
  });
};

const drawLabels = (ctx, lm, idx, w, h) => {
  ctx.font = 'bold 24px Lato, sans-serif';
  ctx.fillStyle = '#ffffff';
  ctx.shadowColor = 'rgba(0,0,0,0.5)';
  ctx.shadowBlur = 4;

  ctx.fillText(`${currentAngles.knee}°`, lm[idx.knee].x * w + 15, lm[idx.knee].y * h);
  ctx.fillText(`${currentAngles.hip}°`, lm[idx.hip].x * w + 15, lm[idx.hip].y * h);
  
  ctx.shadowBlur = 0;
};

const toggleFullscreen = () => {
  const elem = videoFrameRef.value;
  if (!document.fullscreenElement) {
    elem.requestFullscreen().catch(err => console.error(err));
  } else {
    document.exitFullscreen();
  }
};

onMounted(() => {
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement;
  });
});

onUnmounted(() => {
  handleStop();
  document.removeEventListener('fullscreenchange', () => {});
});

// --- UI Helpers ---
const getProgress = (val, min, max) => !val ? 0 : Math.min(Math.max(((val - min) / (max - min)) * 60 + 20, 5), 100);
const getStatusColor = (val, min, max) => (val >= min && val <= max) ? 'var(--primary-color)' : 'var(--secondary-color)';
</script>

<style scoped>
.realtime-page {
  padding-top: calc(var(--header-height) + 20px);
  padding-bottom: 60px;
  min-height: 100vh;
}

.fit-card-container { max-width: 1200px; }

/* 头部 */
.fit-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: var(--space-md); padding-bottom: 15px; border-bottom: 1px solid rgba(0,0,0,0.05);
}
.back-link {
  color: var(--text-muted); font-size: 0.9rem; display: flex; align-items: center; gap: 8px; transition: 0.3s;
}
.back-link:hover { color: var(--text-main); transform: translateX(-3px); }

/* 控制栏 */
.control-bar {
  display: flex; justify-content: space-between; align-items: center; padding: 15px 20px;
  margin-bottom: var(--space-md); border: var(--border-glass);
}
.user-selector { display: flex; align-items: center; gap: 10px; }
.section-label { font-family: var(--font-serif); color: var(--text-main); font-weight: 600; }

.select-wrapper { position: relative; border: 1px solid #eee; border-radius: var(--radius-sm); background: var(--bg-page); min-width: 180px; }
.zen-select {
  border: none; background: transparent; padding: 10px 30px 10px 12px; width: 100%;
  font-family: var(--font-sans); color: var(--text-main); cursor: pointer; outline: none; appearance: none;
}
.select-icon { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; font-size: 0.8rem; }

/* 布局 */
.monitor-layout { display: grid; grid-template-columns: 2fr 1fr; gap: var(--space-md); }

/* 视频区 */
.video-frame {
  position: relative; width: 100%; background: #000; border-radius: var(--radius-md); overflow: hidden;
  aspect-ratio: 16/9; display: flex; align-items: center; justify-content: center;
  border: var(--border-glass); box-shadow: var(--shadow-md);
}
.video-frame:fullscreen { width: 100vw; height: 100vh; border-radius: 0; }
.video-frame:fullscreen .output_canvas { width: auto; height: 100%; max-width: 100%; }

.canvas-wrapper { width: 100%; height: 100%; position: relative; display: flex; justify-content: center; }
.output_canvas { width: 100%; height: 100%; object-fit: contain; display: block; }

.fullscreen-btn {
  position: absolute; bottom: 20px; right: 20px; width: 40px; height: 40px;
  background: rgba(255, 255, 255, 0.2); border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 50%; color: #fff; cursor: pointer; display: flex; justify-content: center; align-items: center;
  transition: 0.3s; z-index: 20; backdrop-filter: blur(8px);
}
.fullscreen-btn:hover { background: var(--primary-color); border-color: var(--primary-color); transform: scale(1.1); }

.placeholder-overlay {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: rgba(255,255,255,0.7); background: #222;
}
.pulse-ring {
  width: 60px; height: 60px; border-radius: 50%; border: 2px solid rgba(255,255,255,0.2);
  animation: pulse 2s infinite; margin-bottom: 15px;
}
@keyframes pulse { 0% { transform: scale(0.8); opacity: 1; } 100% { transform: scale(1.5); opacity: 0; } }

.hud-status {
  position: absolute; top: 20px; left: 50%; transform: translateX(-50%); padding: 8px 20px;
  border-radius: var(--radius-pill); background: rgba(43, 42, 41, 0.7); backdrop-filter: blur(8px);
  color: #fff; font-size: 0.95rem; display: flex; align-items: center; gap: 8px; white-space: nowrap; z-index: 10;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}
.hud-status.warning { background: rgba(193, 140, 126, 0.95); } /* Danger color with opacity */
.hud-status.good { background: rgba(92, 110, 88, 0.95); } /* Primary color with opacity */

/* 仪表盘 */
.dashboard-column { display: flex; flex-direction: column; gap: var(--space-md); }
.data-card {
  padding: 20px; justify-content: center; border: var(--border-glass);
  background: var(--bg-surface);
}
.data-header { display: flex; align-items: center; gap: 8px; color: var(--text-muted); font-size: 0.9rem; margin-bottom: 5px; }
.label-text { font-family: var(--font-serif); }
.data-value {
  font-size: 2.2rem; font-weight: 600; color: var(--text-main); font-family: var(--font-serif);
  line-height: 1; margin-bottom: 10px;
}
.unit { font-size: 1rem; color: var(--text-muted); margin-left: 4px; font-weight: 400; }
.data-bar {
  width: 100%; height: 6px; background: rgba(0,0,0,0.05); border-radius: 3px; overflow: hidden; margin-bottom: 8px;
}
.fill { height: 100%; transition: width 0.3s ease, background 0.3s ease; }
.data-hint { font-size: 0.8rem; color: var(--text-muted); line-height: 1.4; }

/* 响应式 */
@media (max-width: 900px) {
  .realtime-page { padding-top: 100px; }
  .monitor-layout { grid-template-columns: 1fr; }
  .video-frame { aspect-ratio: 4/3; }
  .control-bar { flex-direction: column; gap: 15px; align-items: stretch; }
  .action-buttons button { width: 100%; }
  .dashboard-column { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .data-card { padding: 15px; }
}
@media (max-width: 480px) {
  .dashboard-column { grid-template-columns: 1fr; }
}
</style>