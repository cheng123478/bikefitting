<template>
  <div class="skeleton-player glass-card">
    <div class="player-header">
      <h4 class="title"><i class="fas fa-running"></i> {{ title }} 骨架轨迹动画</h4>
      <div class="player-controls">
        <button class="btn-ctrl" @click="togglePlay" :title="isPlaying ? '暂停' : '播放'">
          <i class="fas" :class="isPlaying ? 'fa-pause' : 'fa-play'"></i>
        </button>
        <button class="btn-ctrl" @click="resetPlay" title="重播">
          <i class="fas fa-redo"></i>
        </button>
        <button v-if="!isRecording && !recordedUrl" class="btn-ctrl share-btn" @click="startRecording" title="导出动画视频">
          <i class="fas fa-video"></i> 提取视频
        </button>
        <button v-if="isRecording" class="btn-ctrl recording-btn pulse" title="正在生成...">
          <i class="fas fa-circle"></i> 录制中
        </button>
        <a v-if="recordedUrl" :href="recordedUrl" :download="downloadFilename" class="btn-ctrl download-btn" title="下载动画视频">
          <i class="fas fa-download"></i> 下载视频 (.webm)
        </a>
      </div>
    </div>
    
    <div class="canvas-wrapper">
      <canvas ref="canvasRef" width="640" height="480"></canvas>
      <div v-if="isBuffering" class="buffer-overlay">
        <i class="fas fa-spinner fa-spin"></i> 建立帧序列
      </div>
    </div>
    
    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue';

const props = defineProps({
  viewType: {
    type: String,
    required: true
  },
  motionData: {
    type: Object,
    default: null
  },
  metrics: {
    type: Array,
    default: () => []
  }
});

const canvasRef = ref(null);
const isPlaying = ref(false);
const isBuffering = ref(true);
const progressPercent = ref(0);

// Record related
const isRecording = ref(false);
const recordedUrl = ref(null);
const downloadFilename = computed(() => `skeleton_${props.viewType.toLowerCase()}_${Date.now()}.webm`);

let frames = [];
let currentIndex = 0;
let animationFrameId = null;
let lastDrawTime = 0;

let mediaRecorder = null;
let recordedChunks = [];

const title = computed(() => {
  if (props.viewType.toUpperCase() === 'SIDE') return '侧面';
  if (props.viewType.toUpperCase() === 'FRONT') return '正面';
  if (props.viewType.toUpperCase() === 'BACK') return '背面';
  return props.viewType;
});

const parseData = () => {
  isBuffering.value = true;
  if (!props.motionData || !props.motionData.dataJson) {
    frames = [];
    isBuffering.value = false;
    return;
  }
  try {
    const raw = typeof props.motionData.dataJson === 'string' 
      ? JSON.parse(props.motionData.dataJson) 
      : props.motionData.dataJson;
    frames = raw || [];
    currentIndex = 0;
  } catch (e) {
    console.error('Failed to parse motion data JSON', e);
    frames = [];
  }
  isBuffering.value = false;
};

const drawPoint = (ctx, point, color = '#E0A96D') => {
  if (!point) return;
  ctx.beginPath();
  // Map normalized coordinates 0-1 to canvas size
  const x = point.x * 640;
  const y = point.y * 480;
  ctx.arc(x, y, 6, 0, 2 * Math.PI);
  ctx.fillStyle = color;
  ctx.fill();
};

const drawLine = (ctx, p1, p2, color = 'rgba(255, 255, 255, 0.7)') => {
  if (!p1 || !p2) return;
  ctx.beginPath();
  ctx.moveTo(p1.x * 640, p1.y * 480);
  ctx.lineTo(p2.x * 640, p2.y * 480);
  ctx.strokeStyle = color;
  ctx.lineWidth = 3;
  ctx.stroke();
};

const drawFrame = (index) => {
  const ctx = canvasRef.value?.getContext('2d');
  if (!ctx || !frames[index]) return;
  
  const frame = frames[index];
  const pts = frame.pts;
  
  // Fill background
  // Note: var(--bg-glass) doesn't work in canvas fillStyle, using resolved fallback
  ctx.fillStyle = '#f8f9fa'; 
  ctx.fillRect(0, 0, 640, 480);

  const vt = props.viewType.toUpperCase();

  // Primary Line Color
  const lColor = 'rgba(92, 110, 88, 0.7)'; // #5C6E58 with alpha
  const pColor = '#D4AF37'; // gold

  if (vt === 'SIDE') {
    // connect ear to shoulder, shoulder->hip->knee->ankle->toe/heel
    drawLine(ctx, pts.EAR, pts.SHOULDER, lColor);
    drawLine(ctx, pts.SHOULDER, pts.HIP, lColor);
    drawLine(ctx, pts.SHOULDER, pts.ELBOW, lColor);
    drawLine(ctx, pts.ELBOW, pts.WRIST, lColor);
    drawLine(ctx, pts.HIP, pts.KNEE, lColor);
    drawLine(ctx, pts.KNEE, pts.ANKLE, lColor);
    
    // 绘制脚踝三角形 (Ankle -> Heel -> Toe -> Ankle)
    if (pts.ANKLE && pts.HEEL && pts.TOE) {
      drawLine(ctx, pts.ANKLE, pts.HEEL, lColor);
      drawLine(ctx, pts.HEEL, pts.TOE, lColor);
      drawLine(ctx, pts.TOE, pts.ANKLE, lColor);
    } else {
      drawLine(ctx, pts.ANKLE, pts.TOE, lColor);
    }
    
    // Draw joints
    ['EAR','SHOULDER','ELBOW','WRIST','HIP','KNEE','ANKLE','HEEL','TOE'].forEach(k => {
      drawPoint(ctx, pts[k], pColor);
    });
  } else {
    // FRONT or BACK
    // Connect shoulders, hips, then down arms and legs
    drawLine(ctx, pts.L_SHOULDER, pts.R_SHOULDER, lColor);
    drawLine(ctx, pts.L_HIP, pts.R_HIP, lColor);
    drawLine(ctx, pts.L_SHOULDER, pts.L_HIP, lColor);
    drawLine(ctx, pts.R_SHOULDER, pts.R_HIP, lColor);
    
    // Legs
    if (pts.L_KNEE) drawLine(ctx, pts.L_HIP, pts.L_KNEE, lColor);
    if (pts.L_ANKLE) drawLine(ctx, pts.L_KNEE, pts.L_ANKLE, lColor);
    if (pts.R_KNEE) drawLine(ctx, pts.R_HIP, pts.R_KNEE, lColor);
    if (pts.R_ANKLE) drawLine(ctx, pts.R_KNEE, pts.R_ANKLE, lColor);

    // Spine Line (Back View Only)
    if (vt === 'BACK' && pts.L_SHOULDER && pts.R_SHOULDER && pts.L_HIP && pts.R_HIP) {
      ctx.beginPath();
      ctx.setLineDash([4, 4]);
      const sMidX = (pts.L_SHOULDER.x + pts.R_SHOULDER.x) / 2 * 640;
      const sMidY = (pts.L_SHOULDER.y + pts.R_SHOULDER.y) / 2 * 480;
      const hMidX = (pts.L_HIP.x + pts.R_HIP.x) / 2 * 640;
      const hMidY = (pts.L_HIP.y + pts.R_HIP.y) / 2 * 480;
      ctx.moveTo(sMidX, sMidY);
      ctx.lineTo(hMidX, hMidY);
      ctx.strokeStyle = pColor; // Gold color for spine
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.setLineDash([]);
    }

    // Draw joints
    ['L_SHOULDER','R_SHOULDER','L_HIP','R_HIP','L_KNEE','R_KNEE','L_ANKLE','R_ANKLE'].forEach(k => {
      drawPoint(ctx, pts[k], pColor);
    });
  }

  // --- UI Overlay (Watermark & Metrics) ---
  const pad = 30;
  const panelHeight = 85;
  
  // 1. Sleek semi-transparent bottom panel
  ctx.fillStyle = 'rgba(26, 26, 26, 0.85)'; // Dark premium glass
  ctx.fillRect(0, 480 - panelHeight, 640, panelHeight);
  
  // 2. Watermark & Brand (Bottom Right)
  ctx.textAlign = 'right';
  
  // Brand name "骑迹"
  ctx.fillStyle = '#FFFFFF';
  ctx.font = 'bold 22px sans-serif';
  ctx.fillText('骑迹', 640 - pad, 480 - 40);
  
  // URL
  ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
  ctx.font = '12px monospace';
  ctx.fillText('www.ridmiricle.com', 640 - pad, 480 - 20);
  
  // 3. Adjustment Metrics (Bottom Left)
  const adjustMetrics = props.metrics.filter(m => m.status !== 'OK');
  ctx.textAlign = 'left';
  
  adjustMetrics.forEach((m, i) => {
    if (i >= 3) return; // Maintain clean UI
    const y = 480 - panelHeight + 25 + (i * 20);
    
    // Status indicator dot
    ctx.fillStyle = m.status === 'WARNING' ? '#ff4d4f' : '#D4AF37';
    ctx.beginPath();
    ctx.arc(pad, y - 5, 4, 0, Math.PI * 2);
    ctx.fill();
    
    // Metric text: Label Current -> Target
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '14px sans-serif';
    const unit = props.viewType.toLowerCase() === 'side' ? '°' : '';
    const targetText = m.idealRange && m.idealRange !== '--' ? ` (目标 ${m.idealRange}${unit})` : '';
    ctx.fillText(`${m.label}: ${m.currentValue}${unit}${targetText}`, pad + 15, y);
  });

  // Small Top Time marker
  ctx.fillStyle = 'rgba(100, 100, 100, 0.4)';
  ctx.font = '10px monospace';
  ctx.textAlign = 'left';
  ctx.fillText(`SEQ: ${index + 1}/${frames.length} | TS: ${frame.ts}ms`, 15, 20);
};

const loop = (timestamp) => {
  if (!isPlaying.value) return;
  
  if (frames.length > 0) {
    drawFrame(currentIndex);
    progressPercent.value = ((currentIndex + 1) / frames.length) * 100;
    
    currentIndex++;
    if (currentIndex >= frames.length) {
      if (isRecording.value) {
        stopRecording();
      }
      currentIndex = 0; // loop back
    }
  }
  
  // Basic throttling roughly 30fps
  setTimeout(() => {
    animationFrameId = requestAnimationFrame(loop);
  }, 33);
};

const togglePlay = () => {
  isPlaying.value = !isPlaying.value;
  if (isPlaying.value) {
    loop();
  } else {
    cancelAnimationFrame(animationFrameId);
  }
};

const resetPlay = () => {
  currentIndex = 0;
  progressPercent.value = 0;
  if (!isPlaying.value) {
    drawFrame(0);
  }
};

// --- Recording Logic ---
const startRecording = () => {
  if (!canvasRef.value || frames.length === 0) return;
  
  // reset to start
  currentIndex = 0;
  recordedChunks = [];
  
  try {
    const stream = canvasRef.value.captureStream(30); // 30 fps
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
    
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) {
        recordedChunks.push(e.data);
      }
    };
    
    mediaRecorder.onstop = () => {
      const blob = new Blob(recordedChunks, { type: 'video/webm' });
      recordedUrl.value = URL.createObjectURL(blob);
      isRecording.value = false;
    };
    
    mediaRecorder.start();
    isRecording.value = true;
    isPlaying.value = true;
    loop(); // start animation
    
  } catch (err) {
    console.error("Recording failed", err);
  }
};

const stopRecording = () => {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop();
  }
};

watch(() => props.motionData, () => {
  parseData();
  resetPlay();
}, { deep: true, immediate: true });

onMounted(() => {
  if (frames.length > 0 && !isPlaying.value) {
    setTimeout(() => drawFrame(0), 100);
  }
});

onUnmounted(() => {
  cancelAnimationFrame(animationFrameId);
  if (recordedUrl.value) {
    URL.revokeObjectURL(recordedUrl.value);
  }
});
</script>

<style scoped>
.skeleton-player {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-surface);
  border: 1px solid rgba(0,0,0,0.05);
  border-radius: 12px;
}

.player-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: rgba(0,0,0,0.02);
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.title {
  font-size: 1rem;
  color: var(--text-main);
  margin: 0;
}
.title i {
  color: var(--primary-color);
  margin-right: 8px;
}

.player-controls {
  display: flex;
  gap: 10px;
}

.btn-ctrl {
  background: #fff;
  border: 1px solid #ddd;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.3s ease;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
}
.btn-ctrl:hover {
  background: var(--bg-body);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

.share-btn {
  background: var(--primary-color);
  color: #fff;
  border-color: var(--primary-color);
}
.share-btn:hover {
  background: var(--primary-dark);
  color: #fff;
}

.download-btn {
  background: var(--success-color);
  color: #fff;
  border-color: var(--success-color);
  text-decoration: none;
}
.download-btn:hover {
  background: #2b8a3e;
  color: #fff;
}

.recording-btn {
  background: #fff0f0;
  color: red;
  border-color: red;
  pointer-events: none;
}

.pulse {
  animation: pulse-red 1.5s infinite;
}
@keyframes pulse-red {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.canvas-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 4/3;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-glass, rgba(245, 245, 245, 0.5));
}

canvas {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.buffer-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(255,255,255,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--primary-color);
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: rgba(0,0,0,0.05);
}

.progress-fill {
  height: 100%;
  background: var(--primary-color);
  transition: width 0.1s linear;
}
</style>
