<template>
  <div class="analyzer-page">

    <!-- 1. 顶部导航栏 -->
    <header class="analyzer-header">
      <div class="header-left">
        <span class="en-tag">VISION ENGINE</span>
        <div class="status-badge" :class="{ processing: isProcessing || isRecording, ready: isReady && !isProcessing }">
          <span class="dot"></span>
          {{ statusText }}
        </div>
      </div>
      <button class="btn-close-circle" @click="stopAndExit" title="退出分析">
        <i class="fas fa-times"></i>
      </button>
    </header>

    <!-- 2. 核心工作台 -->
    <div class="workspace">
      <div class="video-stage glass-card">

        <!-- 隐形视频源：增加 timeupdate 同步进度 -->
        <video
            ref="videoRef"
            class="input-video-hidden"
            playsinline webkit-playsinline muted
            @ended="handleVideoEnd"
            @loadeddata="onVideoDataLoaded"
            @timeupdate="onTimeUpdate"
        ></video>

        <!-- 输出画布 -->
        <canvas ref="canvasRef" class="output-canvas"></canvas>
        <input type="file" ref="fileInput" accept="video/*" style="display: none" @change="handleFileSelected"/>

        <!-- ================= A. 上传模式的引导 ================= -->
        <div class="stage-overlay upload-overlay" v-if="mode === 'upload' && !isFileLoaded">
          <div class="upload-box" @click="triggerFileUpload">
            <div class="upload-icon-wrapper"><i class="fas fa-cloud-upload-alt"></i></div>
            <h3>导入骑行视频</h3>
            <p class="sub-text">支持 MP4/MOV，建议 1080P/60fps</p>
            <button class="btn-primary-sm">选择本地视频</button>
          </div>
        </div>

        <!-- ================= B. 摄像头模式的 AR 引导 ================= -->
        <Transition name="fade">
          <div class="stage-overlay ar-overlay" v-if="mode === 'camera' && showGuide && !isRecording">
            <svg viewBox="0 0 200 150" class="ghost-bike">
              <path
                  d="M40,110 A25,25 0 1,1 90,110 M140,110 A25,25 0 1,1 190,110 M90,110 L140,110 L115,40 L65,40 L90,110 M115,40 L115,20 M140,110 L115,40"
                  fill="none" stroke="rgba(212, 175, 55, 0.6)" stroke-width="1.5" stroke-dasharray="4,4"/>
              <circle cx="90" cy="110" r="2" fill="#D4AF37"/>
            </svg>

            <div class="guide-panel">
              <div class="leveler" :class="{ 'level-ok': isLevel, 'level-bad': !isLevel }">
                <i class="fas fa-mobile-alt"></i> <span>设备倾角: {{ tiltAngle }}°</span>
              </div>
              <p class="tip-text" v-if="isLevel">请对齐单车轮廓，点击下方开始采集</p>
              <p class="tip-text warning" v-else>请调整手机直至完全垂直地面 (0°)</p>
            </div>
          </div>
        </Transition>

        <!-- ================= C. 悬浮数据面板 ================= -->
        <div class="hud-metrics" v-if="isReady || isFileLoaded">
          <!-- 侧面模式 -->
          <template v-if="viewType === 'side'">
            <div class="metric-card" :class="{ 'in-range': metrics.sideMaxKnee >= 140 && metrics.sideMaxKnee <= 150 }">
              <div class="card-header">
                <label>膝角 (伸展度)</label>
                <div class="target-tag">目标: 140°-150°</div>
              </div>
              <div class="dual-vals">
                <div class="val-item">
                  <span class="l">实时</span>
                  <span class="v small">{{ metrics.kneeAngle }}°</span>
                </div>
                <div class="val-item highlight">
                  <span class="l">极限</span>
                  <span class="v">{{ metrics.sideMaxKnee || '--' }}°</span>
                </div>
              </div>
              <div class="desc">腿部伸展角度 (BDC)</div>
            </div>

            <div class="metric-card" :class="{ 'in-range': metrics.sideMinHip >= 45 && metrics.sideMinHip <= 55 }">
              <div class="card-header">
                <label>髋角 (核心)</label>
                <div class="target-tag">目标: 45°-55°</div>
              </div>
              <div class="dual-vals">
                <div class="val-item">
                  <span class="l">实时</span>
                  <span class="v small">{{ metrics.hipAngle }}°</span>
                </div>
                <div class="val-item highlight">
                  <span class="l">极限</span>
                  <span class="v">{{ metrics.sideMinHip < 999 ? metrics.sideMinHip : '--' }}°</span>
                </div>
              </div>
              <div class="desc">躯干与大腿夹角 (TDC)</div>
            </div>

            <div class="metric-card" :class="{ 'in-range': metrics.torsoAngle >= 40 && metrics.torsoAngle <= 50 }">
              <div class="card-header">
                <label>躯干俯角 (Torso Pitch)</label>
                <div class="target-tag">目标: 40°–50°</div>
              </div>
              <div class="val-wrapper">
                <span class="val">{{ metrics.torsoAngle }}°</span>
                <span class="insight">{{ metrics.torsoAngle < 30 ? '极度放松骠' : metrics.torsoAngle < 40 ? '较为流线型' : metrics.torsoAngle <= 50 ? '均衡 ✓' : '相对直立' }}</span>
              </div>
              <div class="desc">和水平面的夹角：0°=完全俯伏（TT），90°=直立</div>
            </div>

            <div class="metric-card" :class="{ 'in-range': metrics.reachAngle >= 85 && metrics.reachAngle <= 95 }">
              <div class="card-header">
                <label>伸展角度 (Reach)</label>
                <div class="target-tag">目标: 85°-95°</div>
              </div>
              <div class="val-wrapper">
                <span class="val">{{ metrics.reachAngle }}°</span>
                <span class="insight">手臂延伸合理性</span>
              </div>
              <div class="desc">躯干与手臂夹角</div>
            </div>

            <div class="metric-card" :class="{ 'in-range': metrics.ankleAtBdc >= 90 && metrics.ankleAtBdc <= 100 }">
              <div class="card-header">
                <label>过悬/下坠测量 (Ankling / BDC)</label>
                <div class="target-tag">目标: 90°-100°</div>
              </div>
              <div class="val-wrapper">
                <span class="val">{{ metrics.ankleAtBdc || '--' }}°</span>
                <span class="insight">触底瞬间跖屈评估</span>
              </div>
              <div class="desc">脚尖下压 / 脚跟下坠侦测 (垫高不合理表现)</div>
            </div>

            <div class="metric-card" :class="{ 'in-range': Math.abs(metrics.kopsOffset) < 10 }">
              <div class="card-header">
                <label>膝曲柄同心轴位 (KOPS)</label>
                <div class="target-tag">理想偏移 &lt; 10%</div>
              </div>
              <div class="val-wrapper">
                <span class="val" :class="{ warning: Math.abs(metrics.kopsOffset) > 15 }">
                  {{ metrics.kopsOffset > 0 ? '偏前' : '退后' }} {{ Math.abs(metrics.kopsOffset) || 0 }}%
                </span>
              </div>
              <div class="desc">3点钟时膝盖偏移量（以胫骨长度标准化，跨设备可比）</div>
            </div>
          </template>

          <!-- 正面模式 -->
          <template v-else-if="viewType === 'front'">
            <div class="metric-card" :class="{ 'in-range': metrics.frontMaxLWobble < 10 && metrics.frontMaxRWobble < 10 }">
              <div class="card-header">
                <label>膝盖追踪 (偏离率)</label>
                <div class="target-tag">目标 < 10%</div>
              </div>
              <div class="dual-vals">
                <div class="val-item">
                  <span class="l">左膝最大</span>
                  <span class="v small">{{ metrics.frontMaxLWobble }}%</span>
                </div>
                <div class="val-item">
                  <span class="l">右膝最大</span>
                  <span class="v small">{{ metrics.frontMaxRWobble }}%</span>
                </div>
              </div>
              <div class="desc">膝偏离/骨盆宽比值 (结合极值波动角综合判定)</div>
            </div>

            <div class="metric-card" :class="{ 'in-range': Math.abs(metrics.frontMaxHipTilt) < 2.5 && Math.abs(metrics.frontMaxShoulderTilt) < 2.5 }">
              <div class="card-header">
                <label>躯干稳度</label>
                <div class="target-tag">目标 < 2.5°</div>
              </div>
              <div class="val-wrapper">
                <span class="val">H:{{ Math.abs(metrics.frontMaxHipTilt) }} / S:{{ Math.abs(metrics.frontMaxShoulderTilt) }}</span>
                <span class="insight">反映核心稳定性</span>
              </div>
              <div class="desc">H: 骨盆侧倾峰值 | S: 肩膀侧倾峰值</div>
            </div>

            <div class="metric-card" :class="{ 'in-range': metrics.frontMaxLFlare < 15 && metrics.frontMaxRFlare < 15 }">
              <div class="card-header">
                <label>手肘外张 (Elbow Flare)</label>
                <div class="target-tag">目标 < 15px</div>
              </div>
              <div class="dual-vals">
                <div class="val-item">
                  <span class="l">左肘最大</span>
                  <span class="v small">{{ metrics.frontMaxLFlare || 0 }}px</span>
                </div>
                <div class="val-item">
                  <span class="l">右肘最大</span>
                  <span class="v small">{{ metrics.frontMaxRFlare || 0 }}px</span>
                </div>
              </div>
              <div class="desc">距离过大提示车把过宽或未主动收肘</div>
            </div>
          </template>

          <!-- 背面模式 -->
          <template v-else-if="viewType === 'back'">
            <div class="metric-card">
              <label>骨盆摇摆 (Pelvis Sway)</label>
              <div class="val" :class="{ warning: metrics.pelvisSway > 20 }">
                {{ metrics.pelvisSway }}px
              </div>
            </div>
            <div class="metric-card">
              <label>高低肩 (Shoulder Tilt)</label>
              <div class="val" :class="{ warning: metrics.shoulderTilt > 3 }">
                {{ metrics.shoulderTilt }}°
              </div>
            </div>
            <div class="metric-card">
              <label>脊柱侧偏 (Spine Tilt)</label>
              <div class="val" :class="{ warning: metrics.spineTilt > 5 }">
                {{ metrics.spineTilt }}°
              </div>
            </div>
            <div class="metric-card duo">
              <label>骨盆下坠极值 (Hip Drop)</label>
              <div class="val">
                左: {{ metrics.leftHipDrop }}% / 右: {{ metrics.rightHipDrop }}%
              </div>
              <div class="desc">占骨盆宽度比例 (>10%需注意)</div>
            </div>
          </template>
        </div>

        <!-- ================= D. 播放控制条 (仅上传模式显示) ================= -->
        <div class="playback-controls glass-card" v-if="mode === 'upload' && isFileLoaded">
          <button class="btn-play" @click="togglePlayPause">
            <i class="fas" :class="isPlaying ? 'fa-pause' : 'fa-play'"></i>
          </button>

          <div class="scrubber-wrapper">
            <input
                type="range"
                class="scrubber"
                min="0"
                :max="videoDuration || 100"
                step="0.01"
                v-model="currentTime"
                @input="handleSeek"
            />
            <div class="time-display">{{ formatTime(currentTime) }} / {{ formatTime(videoDuration) }}</div>
          </div>
        </div>

        <!-- ================= E. 底部操作控制栏 ================= -->
        <div class="stage-controls" v-if="isReady || isFileLoaded">

          <!-- 摄像头模式：录制按钮 -->
          <template v-if="mode === 'camera'">
            <button class="btn-record" @click="toggleRecording" :class="{ recording: isRecording }"
                    :disabled="!isLevel && !isRecording">
              <div class="record-inner"></div>
              <svg class="progress-ring" v-if="isRecording">
                <circle class="ring-bg" cx="40" cy="40" r="36"/>
                <circle class="ring-val" cx="40" cy="40" r="36" :style="{ strokeDashoffset: ringOffset }"/>
              </svg>
            </button>
            <p class="hint">
              <span v-if="!isRecording">点击开始 30s 智能采集</span>
              <span v-else-if="recordingTime < MIN_DURATION" style="color: #ff4d4f">
                数据收集中... (至少 {{ MIN_DURATION }}s)
              </span>
              <span v-else>
                采集剩余 {{ MAX_RECORD_TIME - recordingTime }}s (可点击结束)
              </span>
            </p>
          </template>

          <!-- 上传模式 -->
          <template v-if="mode === 'upload' && isFileLoaded">
            <!-- 分析中：进度条 -->
            <div class="analysis-progress-box" v-if="!isAnalysisComplete">
              <div class="bar-header">
                <span>Aries 引擎逐帧解析中（0.2倍速）...</span>
                <span>{{ analyzeProgress }}%</span>
              </div>
              <div class="bar-bg">
                <div class="bar-fill" :style="{ width: analyzeProgress + '%' }"></div>
              </div>
            </div>

            <!-- 分析完毕：操作按钮 -->
            <div class="analysis-complete-box" v-else>
              <button class="btn-text-reupload" @click="triggerFileUpload">
                <i class="fas fa-redo"></i> 重新选择
              </button>
              <button class="btn-save-result" @click="finishSession">
                <i class="fas fa-check"></i> 确认并保存数据
              </button>
            </div>
          </template>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, onUnmounted, computed} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {Camera} from '@mediapipe/camera_utils';
import {usePoseEstimation} from '@/api/poseestimation';
import {showToast} from '@/utils/toast';
import {useDynamicSessionStore} from '@/stores/dynamicSession';

const router = useRouter();
const route = useRoute();
const sessionStore = useDynamicSessionStore();

// --- 基础状态 ---
const videoRef = ref(null);
const canvasRef = ref(null);
const fileInput = ref(null);

const mode = computed(() => route.query.mode || 'camera');
const viewType = computed(() => route.query.view || 'side');
const bikeId = computed(() => route.query.bikeId || null);

const showGuide = ref(true);
const isFileLoaded = ref(false);
const isProcessing = ref(false);
const analyzeProgress = ref(0);
const bestSnapshots = ref({bdc: null, tdc: null});

// 【新增】：用于“锁死”第一次分析完毕后的数据，防止回放时数据被污染
const finalRawData = ref(null);

// --- 播放器与分析状态 ---
const isPlaying = ref(false);
const currentTime = ref(0);
const videoDuration = ref(0);
const isAnalysisComplete = ref(false);

// --- 录制状态 (Camera 专属) ---
const isRecording = ref(false);
const recordingTime = ref(0);
let recordTimer = null;
const MAX_RECORD_TIME = 30;
const MIN_DURATION = 1; // 最短有效采集时长（秒），保证至少完成数个完整踩踏周期
const ANALYSIS_SPEED = 0.2;
const tiltAngle = ref(0);
const isLevel = computed(() => Math.abs(tiltAngle.value) < 3);

// --- Hook ---
const {
  initPose,
  sendFrame,
  isReady,
  metrics,
  keypoints,
  dualKeypoints,
  startCollection,
  stopCollection,
  setSnapshotCallback,
  closePose
} = usePoseEstimation(viewType);

let camera = null;
let animationFrameId = null;

const statusText = computed(() => {
  if (!isReady.value) return '初始化 AI 模型...';
  if (mode.value === 'upload') {
    if (isAnalysisComplete.value) return '分析完毕，可回放预览';
    return isProcessing.value ? '深度分析中' : '等待导入视频';
  }
  return isRecording.value ? '数据采集中' : '实时预览就绪';
});

const ringOffset = computed(() => {
  const circumference = 2 * Math.PI * 36;
  return circumference - (recordingTime.value / MAX_RECORD_TIME) * circumference;
});

// ==========================================
// 格式化时间
// ==========================================
const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00';
  const m = Math.floor(seconds / 60).toString().padStart(2, '0');
  const s = Math.floor(seconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};

// ==========================================
// 播放器拖拽逻辑
// ==========================================
const onTimeUpdate = () => {
  if (videoRef.value) {
    currentTime.value = videoRef.value.currentTime;
    if (isProcessing.value && videoDuration.value > 0) {
      analyzeProgress.value = Math.min(100, Math.round((currentTime.value / videoDuration.value) * 100));
    }
  }
};

const handleSeek = async () => {
  if (videoRef.value) {
    videoRef.value.currentTime = currentTime.value;
    if (!isPlaying.value && isReady.value) {
      await sendFrame(videoRef.value);
      forceDrawSingleFrame();
    }
  }
};

const togglePlayPause = () => {
  if (!videoRef.value) return;
  if (videoRef.value.paused) {
    videoRef.value.play();
    isPlaying.value = true;
    drawLoop();
  } else {
    videoRef.value.pause();
    isPlaying.value = false;
  }
};

const forceDrawSingleFrame = () => {
  if (!canvasRef.value || !videoRef.value) return;
  const ctx = canvasRef.value.getContext('2d');
  ctx.drawImage(videoRef.value, 0, 0, canvasRef.value.width, canvasRef.value.height);
  if (keypoints.value && keypoints.value.knee) {
    drawSkeleton(ctx, canvasRef.value.width, canvasRef.value.height);
  }
};

// ==========================================
// 循环绘图与骨架渲染
// ==========================================
const drawLoop = () => {
  if (!canvasRef.value || !videoRef.value) return;
  if (mode.value === 'upload' && videoRef.value.ended) return;

  const ctx = canvasRef.value.getContext('2d');
  const width = videoRef.value.videoWidth;
  const height = videoRef.value.videoHeight;

  if (canvasRef.value.width !== width) canvasRef.value.width = width;
  if (canvasRef.value.height !== height) canvasRef.value.height = height;

  ctx.drawImage(videoRef.value, 0, 0, width, height);

  if (isReady.value && keypoints.value && keypoints.value.knee) {
    drawSkeleton(ctx, width, height);
  }

  if (mode.value === 'upload') {
    animationFrameId = requestAnimationFrame(async () => {
      await sendFrame(videoRef.value);
      drawLoop();
    });
  }
};

const drawSkeleton = (ctx, w, h) => {
  if (viewType.value === 'front' || viewType.value === 'back') {
    const dual = dualKeypoints.value;
    ctx.lineWidth = 5;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#5C6E58';

    const drawSide = (pts) => {
      ctx.beginPath();
      if (pts.shoulder && pts.hip) { ctx.moveTo(pts.shoulder.x * w, pts.shoulder.y * h); ctx.lineTo(pts.hip.x * w, pts.hip.y * h); }
      if (pts.hip && pts.knee) ctx.lineTo(pts.knee.x * w, pts.knee.y * h);
      if (pts.knee && pts.ankle) ctx.lineTo(pts.ankle.x * w, pts.ankle.y * h);
      if (pts.shoulder && pts.elbow) { ctx.moveTo(pts.shoulder.x * w, pts.shoulder.y * h); ctx.lineTo(pts.elbow.x * w, pts.elbow.y * h); }
      if (pts.elbow && pts.wrist) ctx.lineTo(pts.wrist.x * w, pts.wrist.y * h);
      ctx.stroke();
    };

    drawSide(dual.left);
    drawSide(dual.right);

    // 连接左右
    ctx.beginPath();
    ctx.setLineDash([5, 5]);
    if (dual.left.shoulder && dual.right.shoulder) {
      ctx.moveTo(dual.left.shoulder.x * w, dual.left.shoulder.y * h);
      ctx.lineTo(dual.right.shoulder.x * w, dual.right.shoulder.y * h);
    }
    if (dual.left.hip && dual.right.hip) {
      ctx.moveTo(dual.left.hip.x * w, dual.left.hip.y * h);
      ctx.lineTo(dual.right.hip.x * w, dual.right.hip.y * h);
    }
    ctx.stroke();
    ctx.setLineDash([]);

    // 绘制圆点
    ctx.fillStyle = '#D4AF37';
    [...Object.values(dual.left), ...Object.values(dual.right)].forEach(p => {
      if (p && p.v > 0.5) {
        ctx.beginPath();
        ctx.arc(p.x * w, p.y * h, 5, 0, 2 * Math.PI);
        ctx.fill();
      }
    });

    if (viewType.value === 'back') {
      ctx.beginPath();
      ctx.setLineDash([2, 5]);
      ctx.strokeStyle = '#D4AF37'; // 金色代表脊柱线
      if (dual.left.shoulder && dual.right.shoulder && dual.left.hip && dual.right.hip) {
        const sMidX = (dual.left.shoulder.x + dual.right.shoulder.x) / 2 * w;
        const sMidY = (dual.left.shoulder.y + dual.right.shoulder.y) / 2 * h;
        const hMidX = (dual.left.hip.x + dual.right.hip.x) / 2 * w;
        const hMidY = (dual.left.hip.y + dual.right.hip.y) / 2 * h;
        ctx.moveTo(sMidX, sMidY);
        ctx.lineTo(hMidX, hMidY);
      }
      ctx.stroke();
      ctx.setLineDash([]);
    }

    return;
  }

  // --- 原有的侧面绘图逻辑 ---
  const pts = keypoints.value;
  const joints = [pts.ear, pts.shoulder, pts.elbow, pts.wrist,
    pts.hip, pts.knee, pts.ankle, pts.heel, pts.toe];

  ctx.lineWidth = 5;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.strokeStyle = '#5C6E58';

  ctx.beginPath();
  if (pts.shoulder && pts.hip) {
    ctx.moveTo(pts.shoulder.x * w, pts.shoulder.y * h);
    ctx.lineTo(pts.hip.x * w, pts.hip.y * h);
  }
  if (pts.hip && pts.knee) ctx.lineTo(pts.knee.x * w, pts.knee.y * h);
  if (pts.knee && pts.ankle) ctx.lineTo(pts.ankle.x * w, pts.ankle.y * h);

  if (pts.ankle && pts.heel && pts.toe) {
    ctx.moveTo(pts.ankle.x * w, pts.ankle.y * h);
    ctx.lineTo(pts.heel.x * w, pts.heel.y * h);
    ctx.lineTo(pts.toe.x * w, pts.toe.y * h);
    ctx.lineTo(pts.ankle.x * w, pts.ankle.y * h);
  }
  if (pts.shoulder && pts.elbow) {
    ctx.moveTo(pts.shoulder.x * w, pts.shoulder.y * h);
    ctx.lineTo(pts.elbow.x * w, pts.elbow.y * h);
  }
  if (pts.elbow && pts.wrist) ctx.lineTo(pts.wrist.x * w, pts.wrist.y * h);
  if (pts.shoulder && pts.ear) {
    ctx.moveTo(pts.shoulder.x * w, pts.shoulder.y * h);
    ctx.lineTo(pts.ear.x * w, pts.ear.y * h);
  }
  ctx.stroke();

  ctx.fillStyle = '#D4AF37';
  ctx.strokeStyle = '#FFFFFF';
  ctx.lineWidth = 2;

  joints.forEach(p => {
    if (p && (p.v === undefined || p.v > 0.5)) {
      ctx.beginPath();
      ctx.arc(p.x * w, p.y * h, 6, 0, 2 * Math.PI);
      ctx.fill();
      ctx.stroke();
    }
  });
};

// ==========================================
// 上传模式流程控制 (重磅修复区域)
// ==========================================
const triggerFileUpload = () => {
  fileInput.value.value = null; // 重置防止选同文件不触发
  fileInput.value.click();
};

const handleFileSelected = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  videoRef.value.src = URL.createObjectURL(file);

  // 重置所有状态
  isAnalysisComplete.value = false;
  isProcessing.value = false;
  analyzeProgress.value = 0;
  finalRawData.value = null;
  videoRef.value.load();
};

const onVideoDataLoaded = () => {
  if (mode.value === 'upload') {
    videoDuration.value = videoRef.value.duration || 0;

    if (videoDuration.value < MIN_DURATION) {
      showToast(`视频时长不足 ${MIN_DURATION} 秒，无法进行有效分析`, 'error');
      fileInput.value.value = null;
      return;
    }

    isFileLoaded.value = true;

    // 开始深度分析
    isProcessing.value = true;
    isPlaying.value = true;
    startCollection(); // Hook 开启记录模式

    videoRef.value.playbackRate = ANALYSIS_SPEED;
    videoRef.value.play();
    drawLoop();
  }
};

const handleVideoEnd = () => {
  if (mode.value === 'upload') {
    isPlaying.value = false;
    cancelAnimationFrame(animationFrameId);

    // 【核心修复】：如果是第一次播放结束（即分析结束）
    if (isProcessing.value) {
      isProcessing.value = false;
      isAnalysisComplete.value = true;

      // 1. 恢复正常播放速度，方便用户预览回放
      videoRef.value.playbackRate = ANALYSIS_SPEED;

      // 2. 立即下达停止采集指令，并固化数据！
      // 这样用户再次点击播放时，AI 只画线，不再污染数据数组。
      finalRawData.value = stopCollection();

      showToast('AI 解析完成，您可以回放预览或保存数据', 'success');
    }
  }
};

// ==========================================
// 数据提交与操作
// ==========================================
const finishSession = () => {
  isRecording.value = false;
  if (recordTimer) clearInterval(recordTimer);

  let rawData;
  if (mode.value === 'upload') {
    rawData = finalRawData.value;
  } else {
    rawData = stopCollection();
  }

  // 修改验证逻辑：根据不同视角判断是否成功获取到数据
  const hasData = rawData?.frameData && rawData.frameData.length > 0;

  if (!hasData) {
    showToast('未检测到有效的骑行动作，请重新采集', 'error');
    return;
  }

  // 辅助：保留1位小数
  const cleanNum = (val, invalidVal = -1) => {
    if (val === undefined || val === null || val === invalidVal) return 0;
    return Number(Number(val).toFixed(1));
  };

  let localData = {};
  const rawTrigger = rawData.triggerMetrics || {};
  const frameData = rawData.frameData;

  if (viewType.value === 'side') {
    const sideBdc = rawTrigger.bdcVal;
    const sideTdc = rawTrigger.tdcVal;

    if (sideBdc === -1 || sideTdc === 999) {
      showToast('未检测到清晰的侧面动作，请确保侧对镜头', 'warning');
      return;
    }

    const timestamps = frameData.map(f => f.ts);
    const kneeAngles = frameData.map(f => f.metrics.kneeAngle);
    const hipAngles = frameData.map(f => f.metrics.hipAngle);
    const ankleAngles = frameData.map(f => f.metrics.ankleAngle);
    const torsoAngles = frameData.map(f => f.metrics.torsoAngle);
    const ankleYCoords = frameData.map(f => f.metrics.ankleY);
    const avgTorso = torsoAngles.length > 0 ? (torsoAngles.reduce((a, b) => a + b, 0) / torsoAngles.length) : 0;

    // 踏频 (Cadence) 流水线粗算 (通过 Y 轴跨过中值线的时间和次数估计)
    // 关键修正：上传模式以 ANALYSIS_SPEED 倍速播放，Date.now() 时间轴是真实视频时长的 1/ANALYSIS_SPEED 倍。
    // 例：60s 视频以 0.2x 速播放耗时 300s（真实时钟），直接用则踏频偏低 5 倍。
    // 修正方式：将 ts 时间差乘以 ANALYSIS_SPEED，还原为真实视频时长。
    let estimatedCadence = 0;
    if (ankleYCoords && ankleYCoords.length > 10 && timestamps.length > 10) {
      const minY = Math.min(...ankleYCoords);
      const maxY = Math.max(...ankleYCoords);
      const midY = (minY + maxY) / 2;
      let crosses = 0;
      for (let i = 1; i < ankleYCoords.length; i++) {
        if (ankleYCoords[i-1] < midY && ankleYCoords[i] >= midY) crosses++;
      }
      // timeScaleFactor：上传模式将墙钟时间还原为真实视频时间；摄像头模式无需修正
      const timeScaleFactor = mode.value === 'upload' ? ANALYSIS_SPEED : 1;
      const durationMs = (timestamps[timestamps.length - 1] - timestamps[0]) * timeScaleFactor;
      const durationMins = durationMs / 60000;
      estimatedCadence = durationMins > 0 ? Math.round(crosses / durationMins) : 0;
    }

    localData = {
      summary: {
        maxKnee: cleanNum(sideBdc, -1),
        minHip: cleanNum(sideTdc, 999),
        avgTorso: cleanNum(avgTorso, 0),
        ankleAtBdc: cleanNum(metrics.value.ankleAtBdc),
        kopsOffset: cleanNum(metrics.value.kopsOffset),
        cadenceRpm: estimatedCadence,
        frameCount: frameData.length
      },
      triggerMetrics: { bdcVal: rawTrigger.bdcVal, tdcVal: rawTrigger.tdcVal },
      frameData: frameData,
      snapshots: {
        bdc: bestSnapshots.value.bdc,
        tdc: bestSnapshots.value.tdc
      }
    };
  } else if (viewType.value === 'front') {
    const leftKneeAngles = frameData.map(f => f.metrics.leftKneeAngle);
    const rightKneeAngles = frameData.map(f => f.metrics.rightKneeAngle);
    const shoulderTilts = frameData.map(f => f.metrics.shoulderTilt);
    const hipTilts = frameData.map(f => f.metrics.hipTilt);
    const leftKneeWobbles = frameData.map(f => f.metrics.leftKneeWobble);
    const rightKneeWobbles = frameData.map(f => f.metrics.rightKneeWobble);
    const leftElbowFlares = frameData.map(f => f.metrics.leftElbowFlare);
    const rightElbowFlares = frameData.map(f => f.metrics.rightElbowFlare);

    const getAvg = (arr) => arr.length > 0 ? (arr.reduce((a, b) => a + b, 0) / arr.length) : 0;
    const getMaxAbs = (arr) => arr.length > 0 ? Math.max(...arr.map(Math.abs)) : 0;
    const getMax = (arr) => arr.length > 0 ? Math.max(...arr) : 0;
    // 算法1：极值波动角 Variance
    const getVariance = (arr) => arr.length > 0 ? (Math.max(...arr) - Math.min(...arr)) : 0;

    localData = {
      summary: {
        avgKnee_L: cleanNum(getAvg(leftKneeAngles)),
        avgKnee_R: cleanNum(getAvg(rightKneeAngles)),
        kneeVariance_L: cleanNum(getVariance(leftKneeAngles)),
        kneeVariance_R: cleanNum(getVariance(rightKneeAngles)),
        maxWobble_L: cleanNum(getMaxAbs(leftKneeWobbles)),
        maxWobble_R: cleanNum(getMaxAbs(rightKneeWobbles)),
        maxWobbleRatio: cleanNum(Math.max(getMaxAbs(leftKneeWobbles), getMaxAbs(rightKneeWobbles))),
        maxWobble: cleanNum(Math.max(getMaxAbs(leftKneeWobbles), getMaxAbs(rightKneeWobbles))),
        maxHipTilt: cleanNum(getMaxAbs(hipTilts)),
        maxShoulderTilt: cleanNum(getMaxAbs(shoulderTilts)),
        maxElbowFlare_L: cleanNum(getMax(leftElbowFlares || [])),
        maxElbowFlare_R: cleanNum(getMax(rightElbowFlares || [])),
        maxElbowFlare: cleanNum(Math.max(getMax(leftElbowFlares || []), getMax(rightElbowFlares || []))),
        frameCount: frameData.length
      },
      triggerMetrics: {
        max_wobbleVal: rawTrigger.max_wobbleVal,
        max_tiltVal: rawTrigger.max_tiltVal,
        k_extVal: rawTrigger.k_extVal
      },
      frameData: frameData,
      snapshots: {
        k_ext: bestSnapshots.value.k_ext,
        max_wobble: bestSnapshots.value.max_wobble,
        max_tilt: bestSnapshots.value.max_tilt
      }
    };
  } else if (viewType.value === 'back') {
    const pelvisSways = frameData.map(f => f.metrics.pelvisSway);
    const spineTilts = frameData.map(f => f.metrics.spineTilt);
    const leftHipDrops = frameData.map(f => f.metrics.leftHipDrop);
    const rightHipDrops = frameData.map(f => f.metrics.rightHipDrop);
    const shoulderTilts = frameData.map(f => f.metrics.shoulderTilt);

    const getMaxAbs = (arr) => arr.length > 0 ? Math.max(...arr.map(Math.abs)) : 0;
    const getAvg = (arr) => arr.length > 0 ? (arr.reduce((a, b) => a + b, 0) / arr.length) : 0;
    const getVariance = (arr) => arr.length > 0 ? (Math.max(...arr) - Math.min(...arr)) : 0;

    localData = {
      summary: {
        maxPelvisSway: cleanNum(getMaxAbs(pelvisSways)),
        maxSpineTilt: cleanNum(getMaxAbs(spineTilts)),
        spineRockingVar: cleanNum(getVariance(spineTilts)),
        maxHipDrop_L: cleanNum(getMaxAbs(leftHipDrops)),
        maxHipDrop_R: cleanNum(getMaxAbs(rightHipDrops)),
        maxShoulderTilt: cleanNum(getMaxAbs(shoulderTilts || [])),
        frameCount: frameData.length
      },
      triggerMetrics: { max_swayVal: rawTrigger.max_swayVal, max_dropVal: rawTrigger.max_dropVal },
      frameData: frameData,
      snapshots: {
        max_sway: bestSnapshots.value.max_sway,
        max_drop: bestSnapshots.value.max_drop
      }
    };
  }

  const viewNameMap = { side: '侧面', front: '正面', back: '背面' };
  sessionStore.saveCapture(viewType.value, localData);
  showToast(`${viewNameMap[viewType.value] || ''}分析结果已暂存`, 'success');

  setTimeout(() => {
    router.push({
      path: '/lab/dynamic',
      query: { bikeId: bikeId.value }
    });
  }, 300);
};

// ==========================================
// 摄像头模式事件
// ==========================================
const toggleRecording = () => {
  if (isRecording.value) {
    if (recordingTime.value < MIN_DURATION) {
      showToast(`采集时间过短，请继续骑行至少 ${MIN_DURATION} 秒`, 'warning');
      return;
    }
    finishSession();
  } else {
    if (!isLevel.value) {
      showToast('请调整手机至垂直', 'warning');
      return;
    }
    isRecording.value = true;
    showGuide.value = false;
    recordingTime.value = 0;
    startCollection(); // Hook 开启记录模式

    recordTimer = setInterval(() => {
      recordingTime.value++;
      if (recordingTime.value >= MAX_RECORD_TIME) finishSession();
    }, 1000);
  }
};

const stopAndExit = () => {
  if (camera) camera.stop();
  if (videoRef.value) videoRef.value.pause();
  router.push({
    path: '/lab/dynamic',
    query: {bikeId: bikeId.value}
  });
};

const handleOrientation = (e) => {
  tiltAngle.value = Math.round(e.gamma || 0);
};

// ==========================================
// 生命周期
// ==========================================
onMounted(async () => {
  await initPose();

  setSnapshotCallback((type) => {
    if (canvasRef.value) bestSnapshots.value[type] = canvasRef.value.toDataURL('image/jpeg', 0.6);
  });

  if (mode.value === 'camera') {
    if (videoRef.value) {
      camera = new Camera(videoRef.value, {
        onFrame: async () => {
          await sendFrame(videoRef.value);
          const ctx = canvasRef.value.getContext('2d');
          ctx.drawImage(videoRef.value, 0, 0, canvasRef.value.width, canvasRef.value.height);
          if (isReady.value && keypoints.value && keypoints.value.knee) {
            drawSkeleton(ctx, canvasRef.value.width, canvasRef.value.height);
          }
        },
        width: 1280, height: 720
      });
      camera.start();
    }
    window.addEventListener('deviceorientation', handleOrientation);
  }
});

onUnmounted(() => {
  if (camera) camera.stop();
  if (recordTimer) clearInterval(recordTimer);
  if (animationFrameId) cancelAnimationFrame(animationFrameId);
  window.removeEventListener('deviceorientation', handleOrientation);

  // 核心修复: 强制清理内存与模型上下文
  closePose();
  if (videoRef.value) {
    videoRef.value.pause();
    videoRef.value.src = "";
    videoRef.value.load();
  }
});
</script>

<style scoped>
/* ====== 1. 基础容器与导航 ====== */
.analyzer-page {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 15, 12, 0.98);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
}

.analyzer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.header-left .en-tag {
  font-size: 0.75rem;
  color: var(--accent-gold);
  letter-spacing: 4px;
  font-weight: 800;
  display: block;
  margin-bottom: 5px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  padding: 4px 12px;
  border-radius: 20px;
}

.status-badge.ready .dot {
  background: #52c41a;
  box-shadow: 0 0 8px #52c41a;
}

.status-badge.processing .dot {
  background: var(--accent-gold);
  animation: pulse 1s infinite;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #666;
}

.btn-close-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: #fff;
  cursor: pointer;
  transition: 0.3s;
  font-size: 1.2rem;
}

.btn-close-circle:hover {
  background: #ff4d4f;
  border-color: #ff4d4f;
  transform: rotate(90deg);
}

/* ====== 2. 视频分析区 ====== */
.workspace {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.video-stage {
  position: relative;
  width: 100%;
  max-width: 1200px;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.input-video-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.output-canvas {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* ====== 3. 各种遮罩引导层 ====== */
.stage-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 上传引导 */
.upload-overlay {
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  z-index: 10;
  pointer-events: auto;
}

.upload-box {
  padding: 50px;
  text-align: center;
  cursor: pointer;
  transition: 0.3s;
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.05);
}

.upload-box:hover {
  border-color: var(--primary-color);
  background: rgba(92, 110, 88, 0.1);
  transform: translateY(-5px);
}

.upload-icon-wrapper {
  font-size: 3.5rem;
  color: var(--primary-color);
  margin-bottom: 20px;
}

.upload-box h3 {
  color: #fff;
  font-size: 1.5rem;
  margin: 0 0 10px;
  font-family: var(--font-serif);
}

.sub-text {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.9rem;
  margin-bottom: 30px;
}

.btn-primary-sm {
  background: var(--primary-color);
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 30px;
  font-weight: bold;
  cursor: pointer;
}

/* AR引导 */
.ar-overlay {
  background: rgba(0, 0, 0, 0.4);
  pointer-events: none;
}

.ghost-bike {
  width: 50%;
  max-width: 400px;
  opacity: 0.6;
  filter: drop-shadow(0 0 10px var(--accent-gold));
}

.guide-panel {
  margin-top: 30px;
  text-align: center;
}

.leveler {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 30px;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  font-weight: bold;
  transition: 0.3s;
}

.leveler.level-ok {
  border-color: #52c41a;
  color: #52c41a;
}

.tip-text {
  color: #fff;
  margin-top: 15px;
  font-size: 1rem;
  text-shadow: 0 2px 5px #000;
}

.tip-text.warning {
  color: #ff4d4f;
}

/* ====== 4. HUD 面板与控制条 ====== */
.hud-metrics {
  position: absolute;
  right: 25px;
  top: 25px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
  width: 240px;
}

.metric-card {
  background: rgba(15, 15, 12, 0.7);
  backdrop-filter: blur(12px);
  padding: 12px 16px;
  border-radius: 14px;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-left: 4px solid rgba(255, 255, 255, 0.2);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
}

.metric-card.in-range {
  border-left: 8px solid var(--accent-gold);
  box-shadow: inset 0 0 10px rgba(92, 110, 88, 0.2);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.metric-card label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #5C6E58;
}

.target-tag {
  font-size: 0.65rem;
  color: var(--accent-gold);
  background: rgba(212, 175, 55, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.val-wrapper {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 2px;
}

.metric-card .val {
  font-size: 1.8rem;
  font-family: 'Lato', sans-serif;
  font-weight: 900;
  color: #fff;
  line-height: 1;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.1);
}

.dual-vals {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 6px;
}

.val-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  opacity: 0.6;
}

.val-item.highlight {
  opacity: 1;
  margin-top: -2px;
}

.val-item .l {
  font-size: 0.65rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
}

.val-item .v {
  font-size: 1.6rem;
  font-family: 'Lato', sans-serif;
  font-weight: 900;
  color: #fff;
}

.val-item .v.small {
  font-size: 1rem;
  font-weight: 600;
  opacity: 0.8;
}

.insight {
  font-size: 0.7rem;
  color: var(--accent-gold);
  font-weight: 500;
  opacity: 0.9;
}

.desc {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.4);
  line-height: 1.4;
}

.stage-controls {
  position: absolute;
  bottom: 30px;
  left: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: auto;
}

/* 自定义播放控制条 */
.playback-controls {
  position: absolute;
  bottom: 120px;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  max-width: 600px;
  background: rgba(18, 18, 16, 0.8);
  backdrop-filter: blur(15px);
  padding: 15px 25px;
  border-radius: 50px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  pointer-events: auto;
}

.btn-play {
  background: var(--primary-color);
  color: #fff;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  transition: 0.3s;
}

.btn-play:hover {
  background: var(--accent-gold);
  color: #000;
}

.scrubber-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 15px;
}

.scrubber {
  flex: 1;
  -webkit-appearance: none;
  appearance: none;
  height: 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
  outline: none;
  cursor: pointer;
}

.scrubber::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--accent-gold);
  cursor: pointer;
  transition: 0.2s;
}

.scrubber::-webkit-slider-thumb:hover {
  transform: scale(1.3);
}

.time-display {
  font-family: 'Lato', monospace;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.7);
  min-width: 80px;
  text-align: right;
}

/* 摄像头录制按钮 */
.btn-record {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  border: 4px solid #fff;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.3s;
}

.btn-record.disabled {
  opacity: 0.5;
  border-color: #666;
  cursor: not-allowed;
}

.record-inner {
  width: 60px;
  height: 60px;
  background: #ff4d4f;
  border-radius: 50%;
  transition: 0.3s;
}

.btn-record.recording .record-inner {
  transform: scale(0.4);
  border-radius: 8px;
}

.progress-ring {
  position: absolute;
  top: -4px;
  left: -4px;
  width: 76px;
  height: 76px;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: transparent;
}

.ring-val {
  fill: none;
  stroke: var(--primary-color);
  stroke-width: 4;
  transition: stroke-dashoffset 1s linear;
}

.hint-text {
  margin-top: 15px;
  color: #fff;
  font-size: 0.95rem;
  text-shadow: 0 2px 4px #000;
}

/* 上传解析进度 */
.analysis-progress-box {
  width: 60%;
  max-width: 400px;
  background: rgba(0, 0, 0, 0.7);
  padding: 20px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.bar-header {
  display: flex;
  justify-content: space-between;
  color: #fff;
  font-size: 0.85rem;
  margin-bottom: 12px;
}

.bar-bg {
  height: 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: var(--accent-gold);
  transition: width 0.3s;
}

/* 分析完成的确认按钮组 */
.analysis-complete-box {
  display: flex;
  gap: 20px;
}

.btn-save-result {
  background: var(--primary-color);
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 30px;
  font-weight: bold;
  font-size: 1rem;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(92, 110, 88, 0.4);
  transition: 0.3s;
}

.btn-save-result:hover {
  background: var(--primary-dark);
  transform: translateY(-2px);
}

.btn-text-reupload {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 12px 25px;
  border-radius: 30px;
  cursor: pointer;
  transition: 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.btn-text-reupload:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: #fff;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.4s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>