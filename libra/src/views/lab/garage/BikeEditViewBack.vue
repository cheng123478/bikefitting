<template>
  <div class="bike-edit-page">
    <div class="container lab-container">

      <!-- ================= 1. 头部工具栏 (严格同步 MyGarageView) ================= -->
      <div class="toolbar-row" v-scroll-reveal>
        <div class="text-group">
          <h1 class="page-title">{{ isEdit ? '单车校准' : '新增单车' }}</h1>
          <p class="page-subtitle">BIKE CALIBRATION / 精密几何与设定录入</p>
        </div>

        <div class="tabs-group">
          <div class="header-actions">
            <button class="btn btn-outline" @click="$router.back()">
              <i class="fas fa-chevron-left"></i> 返回车库
            </button>
          </div>
        </div>
      </div>

      <!-- ================= 2. 核心编辑区域 ================= -->
      <div class="edit-content">

        <!-- 左侧：录入表单 -->
        <main class="form-section glass-card" v-scroll-reveal>

          <!-- 模块 01：基础身份 -->
          <div class="form-group">
            <h4 class="group-title">01 基础身份</h4>
            <div class="input-grid">

              <!-- 图片上传：带 /api 前缀回显 -->
              <div class="zen-form-item full-width">
                <label class="input-label">单车视觉快照</label>
                <div class="image-uploader" @click="triggerUpload" :class="{ 'has-img': formData.imageUrl }">
                  <img v-if="formData.imageUrl" :src="formData.imageUrl.startsWith('http') ? formData.imageUrl : '/api' + formData.imageUrl" class="preview-img" />
                  <div v-else class="upload-placeholder">
                    <i class="fas fa-camera-retro"></i>
                    <span>上传实车美照</span>
                    <p>建议使用白底或侧面无背景照片</p>
                  </div>
                  <input type="file" ref="fileInput" hidden @change="handleFileChange" accept="image/*" />
                  <div class="upload-mask" v-if="uploading">
                    <i class="fas fa-circle-notch fa-spin"></i>
                  </div>
                </div>
              </div>

              <div class="zen-form-item" @click="activeField = 'type'">
                <label class="input-label">单车类型</label>
                <div class="type-selector">
                  <button v-for="type in bikeTypes" :key="type.code"
                          :class="{active: formData.bikeType === type.code}"
                          @click="formData.bikeType = type.code" type="button">
                    {{ type.name }}
                  </button>
                </div>
              </div>

              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">品牌型号</label>
                  <input v-model="formData.brandModel" class="zen-input" placeholder="例如: Giant TCR Adv" @focus="activeField = 'brand'">
                </div>
                <div class="zen-form-item">
                  <label class="input-label">车架尺码</label>
                  <input v-model="formData.frameSize" class="zen-input" placeholder="52 / M / 540" @focus="activeField = 'size'">
                </div>
              </div>

              <div class="zen-form-item">
                <label class="input-label">单车备注</label>
                <input v-model="formData.remark" class="zen-input" placeholder="例如：我的主力爬坡单车" @focus="activeField = 'remark'">
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 模块 02：车架几何 (DNA) -->
          <div class="form-group">
            <h4 class="group-title">02 车架几何 (Frame Geometry)</h4>
            <p class="group-desc">数据来源：厂商官网几何表。这是 Fitting 方案的基础。</p>
            <div class="input-grid">
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">Stack (堆高 - M) *</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.frameStack" class="zen-input" @focus="activeField = 'frameStack'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">Reach (前伸量 - L) *</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.frameReach" class="zen-input" @focus="activeField = 'frameReach'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-3">
                <div class="zen-form-item">
                  <label class="input-label">立管角度 (STA - B)</label>
                  <div class="input-with-unit">
                    <input type="number" step="0.1" v-model.number="formData.seatTubeAngle" class="zen-input" @focus="activeField = 'seatTubeAngle'">
                    <span class="unit">°</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">头管角度 (HTA - E)</label>
                  <div class="input-with-unit">
                    <input type="number" step="0.1" v-model.number="formData.headTubeAngle" class="zen-input" @focus="activeField = 'headTubeAngle'">
                    <span class="unit">°</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">五通下沉 (J)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.bbDrop" class="zen-input" @focus="activeField = 'bbDrop'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">轴距 (H)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.wheelBase" class="zen-input" @focus="activeField = 'wheelBase'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">跨高 (SH / Q)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.standOverHeight" class="zen-input" @focus="activeField = 'standoverHeight'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 模块 03：实车设定 (The Setup) -->
          <div class="form-group">
            <h4 class="group-title">03 实车设定 (Current Setup)</h4>
            <p class="group-desc">使用卷尺测量目前车辆的真实调整值。</p>
            <div class="input-grid">
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">当前座高 (A)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleHeight" class="zen-input" @focus="activeField = 'saddleHeight'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">座垫后移量 (Setback - C)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleSetback" class="zen-input" @focus="activeField = 'saddleSetback'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-3">
                <div class="zen-form-item">
                  <label class="input-label">把立长度</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.stemLength" class="zen-input" @focus="activeField = 'stemLength'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">把立角度</label>
                  <div class="input-with-unit">
                    <input type="number" step="1" v-model.number="formData.stemAngle" class="zen-input" @focus="activeField = 'stemAngle'">
                    <span class="unit">°</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">垫圈高度</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.spacersHeight" class="zen-input" @focus="activeField = 'spacersHeight'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">把横宽度 (N)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.handlebarWidth" class="zen-input" @focus="activeField = 'handlebarWidth'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">曲柄长度 (P)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.crankLength" class="zen-input" @focus="activeField = 'crankLength'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="form-actions-footer">
            <div class="footer-left">
              <button v-if="isEdit" type="button" class="btn-retire-text" @click="handleRetire">
                <i class="fas fa-archive"></i> 退役这辆单车
              </button>
            </div>
            <div class="footer-right">
              <button class="btn btn-primary btn-xl btn-glow" :disabled="submitting" @click="submitForm">
                <i class="fas" :class="submitting ? 'fa-circle-notch fa-spin' : 'fa-check'"></i>
                {{ submitting ? '数据同步中...' : '保存并同步至车库' }}
              </button>
            </div>
          </div>
        </main>

        <!-- ================= 3. 右侧：动态可视化指引 (Sticky) ================= -->
        <aside class="guide-section">
          <div class="visualizer-container glass-card">
            <BikeVisualizer :activePart="activeField" :bikeType="formData.bikeType" />
          </div>

          <Transition name="fade-slide" mode="out-in">
            <div class="field-info-box glass-card" :key="activeField">
              <h4>{{ activeFieldInfo.title }}</h4>
              <p>{{ activeFieldInfo.desc }}</p>
              <div class="fittor-tip" v-if="activeFieldInfo.tip">
                <strong>Fittor 建议：</strong> {{ activeFieldInfo.tip }}
              </div>
            </div>
          </Transition>
        </aside>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user.js';
import { saveUserBike, getBikeDetail, uploadBikePic, retireUserBike } from '@/api/userBike.js';
import { showToast, showConfirm } from '@/utils/toast.js';
import BikeVisualizer from '@/components/lab/BikeVisualizer.vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isEdit = computed(() => !!route.query.id);
const submitting = ref(false);
const activeField = ref('brand');

const bikeTypes = [
  { name: '公路车', code: 1 }, { name: '山地车', code: 2 },
  { name: '全地形', code: 3 }, { name: 'TT/计时', code: 4 }, { name: '城市车', code: 5 }
];

const formData = reactive({
  id: null,
  userId: userStore.userInfo?.id,
  brandModel: '',
  frameSize: '',
  remark: '',
  bikeType: 1,
  frameStack: null,
  frameReach: null,
  seatTubeAngle: null,
  headTubeAngle: null,
  topTubeLength: null,
  bbDrop: null,
  wheelBase: null,
  standOverHeight: null,
  saddleHeight: null,
  saddleSetback: null,
  stemLength: null,
  stemAngle: null,
  spacersHeight: null,
  crankLength: null,
  handlebarWidth: null,
  handlebarDrop: null,
  imageUrl: ''
});

const fieldGuides = {
  'brand': { title: '品牌与型号', desc: '记录您的单车身份，方便快速识别。' },
  'remark': { title: '单车备注', desc: '为单车起个昵称（如：雨天备用车）。' },
  'frameStack': { title: 'Stack (堆高 - M)', desc: '五通中心到头管顶部中心的垂直距离。', tip: '决定骑行姿态“高低”的最核心指标。' },
  'frameReach': { title: 'Reach (前伸量 - L)', desc: '五通中心到头管顶部中心的水平距离。', tip: '衡量车架长短最客观的指标。' },
  'seatTubeAngle': { title: '立管角度 (STA - B)', desc: '立管与水平线的夹角。角度越陡(>74°)，重心越靠前。' },
  'headTubeAngle': { title: '头管角度 (HTA - E)', desc: '头管与水平线的夹角，直接影响操控灵敏度。' },
  'saddleHeight': { title: '当前座高 (A)', desc: '五通中心沿立管中心线至座垫顶面的直线距离。' },
  'saddleSetback': { title: '座垫后移量 (C)', desc: '座垫鼻尖相对于五通中心垂线的水平位移。', tip: '建议结合您的股胫比进行微调。' },
  'bbDrop': { title: '五通下沉 (J)', desc: '前后轴连线与五通中心的垂直落差。', tip: '决定了车辆的重心稳定性和座垫离地间距。' },
  'wheelBase': { title: '轴距 (H)', desc: '前后轮轴心之间的水平距离。' },
  'standOverHeight': { title: '跨高 (Q/SH)', desc: '上管中心点到地面的高度，涉及上下车的安全性。' },
  'headTubeLength': { title: '头管长度 (D)', desc: '头管自身的物理长度。' },
  'stemLength': { title: '把立长度', desc: '把立中心盖到头管盖中心的直线距离。' },
  'spacersHeight': { title: '垫圈高度', desc: '把立下方所有垫圈的累计高度。' },
  'crankLength': { title: '曲柄长度 (P)', desc: '五通中心到脚踏轴心的距离。' }
};

const activeFieldInfo = computed(() => fieldGuides[activeField.value] || fieldGuides['brand']);

onMounted(async () => {
  if (isEdit.value) {
    const res = await getBikeDetail(route.query.id);
    Object.assign(formData, res.data || res);
  }
});

const fileInput = ref(null);
const uploading = ref(false);
const triggerUpload = () => fileInput.value.click();

const handleFileChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const fd = new FormData();
  fd.append('file', file);
  uploading.value = true;
  try {
    const res = await uploadBikePic(fd);
    formData.imageUrl = res.data || res;
    showToast('快照已捕捉', 'success');
  } catch (error) {
    showToast('上传失败', 'error');
  } finally {
    uploading.value = false;
  }
};

const handleRetire = async () => {
  const isConfirmed = await showConfirm(`确定要将这辆战车退役吗？历史记录将被封存。`, '战车退役');
  if (isConfirmed) {
    try {
      await retireUserBikeInfo(formData.id);
      showToast('战车已移至荣誉档案', 'success');
      router.push('/lab/garage');
    } catch (e) {
      showToast('退役失败', 'error');
    }
  }
};

const submitForm = async () => {
  if (!formData.frameStack || !formData.frameReach) {
    showToast('Stack 和 Reach 为核心基准，不可为空', 'warning');
    return;
  }
  submitting.value = true;
  try {
    await saveUserBikeInfo(formData);
    showToast('档案同步成功', 'success');
    router.push('/lab/garage');
  } catch (error) {
    showToast('保存异常', 'error');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
/* 1. 基础对齐 */
.bike-edit-page { padding-top: var(--header-height); min-height: 100vh; background: transparent; }
.lab-container { padding-top: 40px; padding-bottom: 80px; }

/* 2. 头部工具栏 (严格同步 GarageView) */
.toolbar-row {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: var(--space-xl); padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}
.page-title { font-family: var(--font-serif); font-size: 2.2rem; color: var(--text-main); line-height: 1.1; }
.page-subtitle { font-size: 1rem; color: var(--text-muted); font-family: var(--font-serif); font-style: italic; }
.header-actions { display: flex; align-items: center; gap: 20px; }

/* 3. 布局 */
.edit-content {
  display: grid;
  /* 修改点 1：调整比例，给右侧更多空间 */
  /* 左侧表单占 55%，右侧展台占 45%，间距拉大到 50px */
  grid-template-columns: 1.2fr 1fr;
  gap: 50px;
  align-items: start;
}

/* 左侧表单 */
.form-section {
  padding: 45px;
  background: var(--bg-surface);
  border-radius: var(--radius-md);
  border: var(--border-glass);
}

.group-title { font-size: 1.1rem; color: var(--primary-color); font-weight: 700; border-left: 3px solid var(--primary-color); padding-left: 12px; margin-bottom: 20px; }
.group-desc { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 25px; margin-top: -10px; }

.input-grid { display: grid; gap: 24px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; }

.zen-form-item { display: flex; flex-direction: column; gap: 8px; }
.input-label { font-size: 0.75rem; font-weight: 800; color: var(--text-muted); letter-spacing: 0.05em; text-transform: uppercase; }

.zen-input {
  width: 100%; padding: 12px 16px; border: 1px solid #EDEDED; background: #FAFAFA;
  border-radius: var(--radius-sm); outline: none; transition: 0.3s;
}
.zen-input:focus { border-color: var(--primary-color); background: white; box-shadow: 0 0 0 4px rgba(92, 110, 88, 0.05); }
.zen-input::-webkit-outer-spin-button, .zen-input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
.zen-input[type=number] { -moz-appearance: textfield; }

.input-with-unit { position: relative; }
.unit { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); font-size: 0.75rem; color: #AAA; font-weight: 700; }

.type-selector { display: flex; gap: 8px; flex-wrap: wrap; }
.type-selector button { padding: 8px 14px; border-radius: 4px; border: 1px solid #EEE; background: white; font-size: 0.8rem; cursor: pointer; transition: 0.3s; color: var(--text-muted); }
.type-selector button.active { background: var(--primary-color); color: white; border-color: var(--primary-color); }

/* 4. 图片上传 */
.image-uploader {
  height: 220px; width: 100%; display: flex; align-items: center; justify-content: center;
  cursor: pointer; position: relative; overflow: hidden; border: 1.5px dashed #DDD;
  background: #F9F9F9; border-radius: var(--radius-sm); transition: 0.3s;
}
.image-uploader:hover { border-color: var(--primary-color); background: white; }
.upload-placeholder { text-align: center; color: #AAA; }
.upload-placeholder i { font-size: 2rem; margin-bottom: 10px; display: block; opacity: 0.4; }
.preview-img { width: 100%; height: 100%; object-fit: contain; padding: 15px; }
.upload-mask { position: absolute; inset: 0; background: rgba(255,255,255,0.7); display: flex; align-items: center; justify-content: center; color: var(--primary-color); }

.full-width { grid-column: 1 / -1; }
.divider { height: 1px; background: rgba(0,0,0,0.05); margin: 40px 0; }

/* 5. 底部操作 */
.form-actions-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 50px; padding-top: 30px; border-top: 1px solid rgba(0,0,0,0.05);
}
.btn-xl { padding: 16px 40px; font-size: 1rem; border-radius: var(--radius-pill); font-weight: 700; }
.btn-glow { box-shadow: 0 8px 25px rgba(92, 110, 88, 0.25); }
.btn-retire-text { background: none; border: none; color: var(--danger-color); font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; opacity: 0.6; transition: 0.3s; }
.btn-retire-text:hover { opacity: 1; text-decoration: underline; }

/* 6. 右侧 Sticky */
.guide-section {
  display: flex;
  flex-direction: column;
  gap: 25px;
  /* Sticky 逻辑保持不变 */
  position: sticky;
  top: calc(var(--header-height) + 30px);
  z-index: 10;
  height: fit-content;
}

.visualizer-container {
  /* 修改点 2：高度大幅增加，让单车图“撑满” */
  height: 460px;
  background: white;
  border: var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  /* 增加一点内阴影，增加深邃感 */
  box-shadow: inset 0 0 20px rgba(0,0,0,0.02);
}
/* 文字说明盒 */
.field-info-box {
  padding: 30px;
  border-left: 6px solid var(--accent-gold); /* 加粗边框 */
  background: white;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  box-shadow: var(--shadow-sm);
}

.field-info-box h4 {
  color: var(--text-main);
  margin-bottom: 12px;
  font-size: 1.2rem; /* 字号微调 */
  font-weight: 700;
}

.field-info-box p {
  font-size: 0.95rem; /* 字号微调，更易读 */
  color: var(--text-muted);
  line-height: 1.7;
}
/* Fittor 建议块 */
.fittor-tip {
  margin-top: 18px;
  padding: 15px;
  background: rgba(212, 175, 55, 0.08);
  border-radius: 6px;
  font-size: 0.9rem;
  color: #967d28; /* 加深一点金色文字，提升对比度 */
  line-height: 1.6;
  font-weight: 500;
  display: flex;
  gap: 8px;
}
.fittor-tip::before {
  content: '💡'; /* 增加一个小图标 */
  font-style: normal;
}
/* 动效 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateX(20px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-20px); }

@media (max-width: 1280px) {
  .edit-content {
    grid-template-columns: 1fr; /* 变为上下结构 */
  }

  .guide-section {
    position: static; /* 取消 Sticky */
    order: -1; /* 关键：在移动端/平板端，让图片显示在表单上方！方便对照 */
  }

  .visualizer-container {
    height: 400px; /* 移动端高度适度减小 */
  }
}
</style>