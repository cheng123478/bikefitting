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

          <!-- 模块 01：基础信息 -->
          <div class="form-group">
            <h4 class="group-title">01 基础信息 (Basic Info)</h4>
            <div class="input-grid">
              <div class="identity-section" :class="{ 'locked-section': !isManualMode && (isGeometryLocked || isEdit) }">
                <!-- 品牌识别与搜索入口 -->
                <div class="zen-form-item full-width autocomplete-container">
                  <div class="label-row">
                    <label class="input-label">品牌 (中/英文搜索) *</label>
                    <button v-if="formData.brand && !isEdit" type="button" class="btn-reset" :class="{ 'force-clickable': isGeometryLocked }" @click.stop.prevent="resetSearch">重新检索型号</button>
                  </div>
                  <div class="search-input-wrapper">
                    <input
                        v-model="formData.brand"
                        class="zen-input"
                        :class="{ 'is-linked': formData.bikeModelId && !isManualMode }"
                        placeholder="输入品牌，如: Giant / 捷安特"
                        :disabled="!isManualMode && (isEdit || isGeometryLocked)"
                        @input="onSearchInput"
                        @focus="showDropdown = true; activeField = 'brand'"
                        @blur="closeDropdown"
                    >
                    <div class="search-status-icon">
                      <i v-if="isSearching" class="fas fa-circle-notch fa-spin"></i>
                      <i v-else-if="formData.bikeModelId" class="fas fa-link text-primary"></i>
                      <i v-else class="fas fa-search"></i>
                    </div>

                    <!-- 搜索结果下拉 -->
                    <transition name="fade-slide-y">
                      <div class="search-dropdown glass-card" v-if="showDropdown && formData.brand && !isEdit && !isManualMode">
                        <!-- 顶部固定：手动录入入口 -->
                        <div class="dropdown-header">
                          <span class="header-text">未找到型号？</span>
                          <button class="manual-entry-btn" @mousedown="enableManualEntry">手动录入</button>
                        </div>
                        <div class="dropdown-divider"></div>
                        <!-- 可滚动区域 -->
                        <div class="dropdown-scroll-area">
                          <div v-if="modelSuggestions.length === 0 && !isSearching" class="dropdown-empty">
                            未在库中找到相关型号
                          </div>
                          <div v-for="item in modelSuggestions" :key="item.model" class="suggestion-item" @mousedown="selectModelGroup(item)">
                            <div class="item-main">
                              <span class="s-brand">{{ item.brand }}</span>
                              <span class="s-model">{{ item.model }}</span>
                            </div>
                            <span class="s-cn" v-if="item.brandCn">{{ item.brandCn }}</span>
                          </div>
                        </div>
                      </div>
                    </transition>
                  </div>
                </div>

                <!-- 步骤 2：品牌和型号（选择型号后显示） -->
                <template v-if="currentStep !== 'search' || isManualMode">
                  <div class="grid-2">
                    <div class="zen-form-item">
                      <label class="input-label">中文品牌</label>
                      <input v-model="formData.brandCn" class="zen-input" :disabled="!isManualMode" @focus="activeField = 'brandCn'">
                    </div>
                    <div class="zen-form-item">
                      <label class="input-label">具体型号 *</label>
                      <input v-model="formData.model" class="zen-input" :disabled="!isManualMode" @focus="activeField = 'model'">
                    </div>
                  </div>
                </template>

                <!-- 步骤 3：年份选择（自动模式下，选择型号后显示） -->
                <template v-if="!isManualMode && currentStep !== 'search'">
                  <!-- 年份选择药丸 -->
                  <div class="zen-form-item full-width year-selection-highlight" v-if="availableYears.length > 0 && !formData.modelYear">
                    <label class="input-label">
                      <i class="fas fa-hand-point-right highlight-icon"></i> 选择生产年份
                    </label>
                    <div class="pill-group">
                      <button v-for="y in availableYears" :key="y"
                              class="pill-btn" :class="{ active: formData.modelYear === y }"
                              @click="onYearSelect(y)" type="button">
                        {{ y }}
                      </button>
                    </div>
                  </div>

                  <!-- 年份已选择后显示锁定状态 -->
                  <div class="zen-form-item full-width" v-if="formData.modelYear">
                    <label class="input-label">生产年份</label>
                    <div class="locked-field">
                      <span class="locked-value">{{ formData.modelYear }}</span>
                      <span class="lock-icon"><i class="fas fa-lock"></i></span>
                    </div>
                  </div>
                </template>

                <!-- 步骤 4：尺码选择（自动模式下，选择年份后显示） -->
                <template v-if="!isManualMode && formData.modelYear">
                  <!-- 尺码选择药丸 -->
                  <div class="zen-form-item full-width size-selection-highlight" v-if="availableSizes.length > 0 && !formData.size">
                    <label class="input-label">
                      <i class="fas fa-hand-point-right highlight-icon"></i> 选择车架尺码
                    </label>
                    <div class="pill-group">
                      <button v-for="s in availableSizes" :key="s"
                              class="pill-btn" :class="{ active: formData.size === s }"
                              @click="onSizeSelect(s)" type="button">
                        {{ s }}
                      </button>
                    </div>
                  </div>

                  <!-- 尺码已选择后显示锁定状态 -->
                  <div class="zen-form-item full-width" v-if="formData.size">
                    <label class="input-label">车架尺码</label>
                    <div class="locked-field">
                      <span class="locked-value">{{ formData.size }}</span>
                      <span class="lock-icon"><i class="fas fa-lock"></i></span>
                    </div>
                  </div>
                </template>

                <!-- 手动模式下的年份和尺码输入 -->
                <template v-if="isManualMode">
                  <div class="grid-2">
                    <div class="zen-form-item">
                      <label class="input-label">年份</label>
                      <input v-model="formData.modelYear" class="zen-input" placeholder="2024" @focus="activeField = 'modelYear'">
                    </div>
                    <div class="zen-form-item">
                      <label class="input-label">尺码</label>
                      <input v-model="formData.size" class="zen-input" placeholder="例如: 52/M" @focus="activeField = 'size'">
                    </div>
                  </div>
                </template>

                <!-- 步骤 5：其他基础信息（自动模式下选择尺码后显示，手动模式下始终显示） -->
                <template v-if="isManualMode || currentStep === 'complete'">
                  <div class="zen-form-item" @click="activeField = 'type'">
                    <label class="input-label">单车类型</label>
                    <div class="type-selector" :class="{ disabled: !isManualMode && isGeometryLocked }">
                      <button v-for="type in bikeTypes" :key="type.code"
                              :class="{active: formData.bikeType === type.code}"
                              @click="formData.bikeType = type.code" type="button">
                        {{ type.name }}
                      </button>
                    </div>
                  </div>
                </template>
              </div>

              <!-- 单车备注和图片上传：不在锁定区域内，始终可编辑 -->
              <template v-if="isManualMode || currentStep === 'complete'">
                <div class="zen-form-item">
                  <label class="input-label">单车备注</label>
                  <input v-model="formData.remark" class="zen-input" placeholder="例如：我的主力爬坡单车" @focus="activeField = 'remark'">
                </div>

                <!-- 图片上传 -->
                <div class="zen-form-item full-width">
                  <label class="input-label">单车视觉快照</label>
                  <div class="image-uploader" @click="triggerUpload" :class="{ 'has-img': formData.imageUrl }" @mouseenter="activeField = 'imageUrl'">
                    <img v-if="formData.imageUrl" :src="formData.imageUrl.startsWith('http') ? formData.imageUrl : '/api' + formData.imageUrl" class="preview-img" />
                    <div v-else class="upload-placeholder">
                      <i class="fas fa-camera-retro"></i>
                      <span>上传实车美照</span>
                      <p>建议使用白底或侧面无背景照片</p>
                    </div>
                    <input type="file" ref="fileInput" hidden @change="handleFileChange" accept="image/*">
                    <div class="upload-mask" v-if="uploading">
                      <i class="fas fa-circle-notch fa-spin"></i>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- 模块 02：车架几何（自动模式下选择尺码后显示，手动模式下始终显示） -->
          <template v-if="isManualMode || currentStep === 'complete'">
            <div class="divider"></div>
            <div class="form-group" :class="{ 'locked-section': !isManualMode && isGeometryLocked }">
              <div class="group-header">
                <h4 class="group-title">02 车架几何 (Frame Geometry)</h4>
                <div v-if="!isManualMode && isGeometryLocked" class="lock-tag"><i class="fas fa-shield-alt"></i> 数据已锁定</div>
              </div>
              <p class="group-desc" v-if="!isManualMode">数据来源：厂商官网几何表。这是 Fitting 方案的基础。</p>
              <div class="input-grid">
                <div class="grid-2">
                  <div class="zen-form-item">
                    <label class="input-label">堆高 * </label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.stack" class="zen-input" :disabled="!isManualMode && isGeometryLocked" @focus="activeField = 'stack'">
                      <span class="unit">mm</span>
                    </div>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">前伸量 * </label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.reach" class="zen-input" :disabled="!isManualMode && isGeometryLocked" @focus="activeField = 'reach'">
                      <span class="unit">mm</span>
                    </div>
                  </div>
                </div>
                <div class="grid-3">
                  <div class="zen-form-item">
                    <label class="input-label">立管角度 * </label>
                    <div class="input-with-unit">
                      <input type="number" step="0.1" v-model.number="formData.seatTubeAngle" class="zen-input" :disabled="!isManualMode && isGeometryLocked" @focus="activeField = 'seatTubeAngle'">
                      <span class="unit">°</span>
                    </div>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">头管角度</label>
                    <div class="input-with-unit">
                      <input type="number" step="0.1" v-model.number="formData.headTubeAngle" class="zen-input" :disabled="!isManualMode && isGeometryLocked" @focus="activeField = 'headTubeAngle'">
                      <span class="unit">°</span>
                    </div>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">五通下沉</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.bbDrop" class="zen-input" :disabled="!isManualMode && isGeometryLocked" @focus="activeField = 'bbDrop'">
                      <span class="unit">mm</span>
                    </div>
                  </div>
                </div>
                <div class="grid-2">
                  <div class="zen-form-item">
                    <label class="input-label">轴距</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.wheelBase" class="zen-input" :disabled="!isManualMode && isGeometryLocked" @focus="activeField = 'wheelBase'">
                      <span class="unit">mm</span>
                    </div>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">跨高</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.standOverHeight" class="zen-input" :disabled="!isManualMode && isGeometryLocked" @focus="activeField = 'standOverHeight'">
                      <span class="unit">mm</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 模块 03：实车设定（自动模式下选择尺码后显示，手动模式下始终显示） -->
          <template v-if="isManualMode || currentStep === 'complete'">
            <div class="divider"></div>
            <div class="form-group">
              <h4 class="group-title">03 实车设定 (Current Setup)</h4>
              <p class="group-desc">可选填写目前车辆的真实调整值；不填写也能生成理想推荐，填写后用于当前车辆对比。输入框中的示例仅提示单位和量级。</p>
              <div class="input-grid">
                <div class="grid-3">
                  <div class="zen-form-item">
                    <label class="input-label">当前座高</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.saddleHeight" class="zen-input" :class="{ 'has-warning': fieldWarnings.saddleHeight }" placeholder="例如：720" @focus="activeField = 'saddleHeight'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.saddleHeight" class="field-warning">{{ fieldWarnings.saddleHeight }}</p>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">座垫后移</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.saddleSetback" class="zen-input" :class="{ 'has-warning': fieldWarnings.saddleSetback }" placeholder="例如：55" @focus="activeField = 'saddleSetback'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.saddleSetback" class="field-warning">{{ fieldWarnings.saddleSetback }}</p>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">座垫角度</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.saddleTilt" class="zen-input" :class="{ 'has-warning': fieldWarnings.saddleTilt }" placeholder="例如：-2" @focus="activeField = 'saddleTilt'">
                      <span class="unit">°</span>
                    </div>
                    <p v-if="fieldWarnings.saddleTilt" class="field-warning">{{ fieldWarnings.saddleTilt }}</p>
                  </div>
                </div>
                <div class="grid-3">
                  <div class="zen-form-item">
                    <label class="input-label">把立长度</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.stemLength" class="zen-input" :class="{ 'has-warning': fieldWarnings.stemLength }" placeholder="例如：90" @focus="activeField = 'stemLength'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.stemLength" class="field-warning">{{ fieldWarnings.stemLength }}</p>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">把立角度</label>
                    <div class="input-with-unit">
                      <input type="number" step="1" v-model.number="formData.stemAngle" class="zen-input" :class="{ 'has-warning': fieldWarnings.stemAngle }" placeholder="例如：-6" @focus="activeField = 'stemAngle'">
                      <span class="unit">°</span>
                    </div>
                    <p v-if="fieldWarnings.stemAngle" class="field-warning">{{ fieldWarnings.stemAngle }}</p>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">垫圈高度</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.spacersHeight" class="zen-input" :class="{ 'has-warning': fieldWarnings.spacersHeight }" placeholder="例如：20" @focus="activeField = 'spacersHeight'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.spacersHeight" class="field-warning">{{ fieldWarnings.spacersHeight }}</p>
                  </div>
                </div>
                <div class="grid-3">
                  <div class="zen-form-item">
                    <label class="input-label">把横宽度</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.handlebarWidth" class="zen-input" :class="{ 'has-warning': fieldWarnings.handlebarWidth }" placeholder="例如：400" @focus="activeField = 'handlebarWidth'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.handlebarWidth" class="field-warning">{{ fieldWarnings.handlebarWidth }}</p>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">曲柄长度</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.crankLength" class="zen-input" :class="{ 'has-warning': fieldWarnings.crankLength }" placeholder="例如：170" @focus="activeField = 'crankLength'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.crankLength" class="field-warning">{{ fieldWarnings.crankLength }}</p>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">车把落差</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.handlebarDrop" class="zen-input" :class="{ 'has-warning': fieldWarnings.handlebarDrop }" placeholder="例如：70" @focus="activeField = 'handlebarDrop'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.handlebarDrop" class="field-warning">{{ fieldWarnings.handlebarDrop }}</p>
                  </div>
                </div>
                <div class="grid-2">
                  <div class="zen-form-item">
                    <label class="input-label">座垫宽度</label>
                    <div class="input-with-unit">
                      <input type="number" v-model.number="formData.saddleWidth" class="zen-input" :class="{ 'has-warning': fieldWarnings.saddleWidth }" placeholder="例如：143" @focus="activeField = 'saddleWidth'">
                      <span class="unit">mm</span>
                    </div>
                    <p v-if="fieldWarnings.saddleWidth" class="field-warning">{{ fieldWarnings.saddleWidth }}</p>
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">踏板类型</label>
                    <select v-model="formData.pedalType" class="zen-input" @focus="activeField = 'pedalType'">
                      <option :value="null">请选择踏板类型</option>
                      <option v-for="opt in pedalOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 底部操作栏 -->
          <div class="form-actions-footer" v-if="isManualMode || currentStep === 'complete'">
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
        <aside class="guide-section" v-if="isManualMode || currentStep !== 'search'">
          <div class="visualizer-container glass-card">
            <BikeVisualizer :activePart="activeField" :bikeType="formData.bikeType" :pedalType="formData.pedalType" />
          </div>

          <Transition name="fade-slide" mode="out-in">
            <div class="field-info-box glass-card" :key="activeField">
              <h4>{{ activeFieldInfo.title }}</h4>
              <p>{{ activeFieldInfo.desc }}</p>
              <div class="fittor-tip" v-if="activeFieldInfo.tip">
                <strong>Fitter 建议：</strong> {{ activeFieldInfo.tip }}
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
import { searchModelNames, getBikeModelTree } from '@/api/bikeModel.js';
import { showToast, showConfirm } from '@/utils/toast.js';
import BikeVisualizer from '@/views/lab/components/BikeVisualizer.vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isEdit = computed(() => !!route.query.id);
const submitting = ref(false);
const activeField = ref('brand');
const isGeometryLocked = ref(false);
const isManualMode = ref(false);

// 步骤状态：'search' -> 'brand_selected' -> 'year_selected' -> 'complete'
const currentStep = ref('search');

const bikeTypes = [
  { name: '公路车', code: 1 }, { name: '山地车', code: 2 },
  { name: '全地形', code: 3 }, { name: '城市车', code: 4 }
];

const pedalOptions = [
  { label: '平踏 + 运动鞋', value: 1 },
  { label: '公路锁踏 + 锁鞋', value: 2 },
  { label: '山地锁踏 + 锁鞋', value: 3 }
];

// --- 搜索与级联状态 ---
const isSearching = ref(false);
const showDropdown = ref(false);
const modelSuggestions = ref([]);
const selectedModelTree = ref([]);
let searchTimer = null;

const formData = reactive({
  id: null,
  userId: userStore.userInfo?.id,
  bikeModelId: null,
  brand: '',
  brandCn: '',
  model: '',
  modelYear: '',
  size: '',
  remark: '',
  bikeType: null,
  stack: null,
  reach: null,
  seatTubeAngle: null,
  headTubeAngle: null,
  bbDrop: null,
  wheelBase: null,
  standOverHeight: null,
  saddleHeight: null,
  saddleSetback: null,
  saddleTilt: null,
  stemLength: null,
  stemAngle: null,
  spacersHeight: null,
  crankLength: null,
  handlebarWidth: null,
  handlebarDrop: null,
  saddleWidth: null,
  pedalType: null,
  imageUrl: ''
});

// 计算属性：从模型树中提取年份
const availableYears = computed(() => {
  if (!selectedModelTree.value.length) return [];
  const years = selectedModelTree.value.map(v => v.modelYear);
  return [...new Set(years)].filter(Boolean).sort((a, b) => b - a);
});

// 计算属性：根据选中年份提取尺寸
const availableSizes = computed(() => {
  if (!formData.modelYear || !selectedModelTree.value.length) return [];
  return selectedModelTree.value
      .filter(v => v.modelYear === formData.modelYear)
      .map(v => v.size);
});

const fieldGuides = {
  // --- 01 基础身份 (Identity) ---
  'brand': { title: '品牌 (Brand English)', desc: '品牌全球通用名。', tip: '输入英文名称更易匹配官方数据库。' },
  'brandCn': { title: '品牌 (Brand 中文)', desc: '品牌的中文译名。' },
  'model': { title: '型号 (Model)', desc: '具体的系列与型号。', tip: '例如 TCR Advanced, Tarmac SL8 等。' },
  'modelYear': { title: '款型年份 (Model Year)', desc: '车辆的设计年份。', tip: '同一型号不同年份的几何可能存在显著差异。' },
  'size': { title: '官方尺码 (Size)', desc: '车架标签上的尺码。', tip: '如 52, S, 540 等。' },
  'bikeType': { title: '单车类型 (Bike Type)', desc: '车辆的分类属性。', tip: '不同类型的车有完全不同的理想角度范围。' },
  'remark': { title: '备注 (Remark)', desc: '您的个性化记录。' },
  'imageUrl': { title: '单车视觉快照', desc: '车辆的真实照片。', tip: '这将作为档案封面，并辅助进行视觉对齐校验。' },

  // --- 02 车架几何 (DNA / Geometry) ---
  'stack': { title: '堆高 (Stack)', desc: '五通中心到头管顶部中心的垂直距离。', tip: '决定骑行姿态“高低”的基准，Stack 越高姿态越直立。' },
  'reach': { title: '前伸量 (Reach)', desc: '五通中心到头管顶部中心的水平距离。', tip: '衡量车架长短最客观的指标，不受立管角度干扰。' },
  'seatTubeAngle': { title: '立管角度 (STA)', desc: '立管中心线与水平线的夹角。', tip: '直接影响座垫前后的可调范围及重心分布。' },
  'headTubeAngle': { title: '头管角度 (HTA)', desc: '头管中心线与水平线的夹角。', tip: '角度越小（斜）操控越稳，角度越大（直）转向越灵敏。' },
  'bbDrop': { title: '五通下沉量 (BB Drop)', desc: '前后轴连线与五通中心的垂直落差。', tip: '值越大重心越低，高速过弯更稳。' },
  'wheelBase': { title: '轴距', desc: '前后轮轴心之间的水平距离。', tip: '长轴距利于平稳巡航，短轴距利于灵活爬坡。' },
  'standOverHeight': { title: '跨高', desc: '车架上管中心点到地面的垂直距离。', tip: '关乎停车落脚时的安全性，建议与身体跨高保持 2-5cm 余量。'},

  // --- 03 实车设定 (The Setup) ---
  'saddleHeight': { title: '座垫高度 (Saddle Height)', desc: '五通中心沿立管至座垫顶面中心点的距离。', tip: 'Fitting 中最重要的参数，直接决定踩踏效率和膝盖健康。' },
  'saddleSetback': { title: '座垫后移量 (Saddle Setback)', desc: '座垫中心相对于五通中心垂直线的水平位移。', tip: '建议使用铅垂线从坐垫鼻头垂直向下对准五通轴心，测量其水平距离。' },
  'saddleTilt': { title: '座垫倾角 (Saddle Tilt)', desc: '座垫中心线与水平线的夹角。', tip: '决定了会阴压力。' },
  'stemLength': { title: '把立长度 (Stem Length)', desc: '把立中心盖到车把夹持位中心点的距离。', tip: '调整上肢拉伸感（Reach）最直接的零件。' },
  'stemAngle': { title: '把立角度 (Stem Angle)', desc: '把立相对水平线的安装角度。', tip: '下倾填负数，如 -6；上扬填正数，如 +6。' },
  'spacersHeight': { title: '垫圈高度 (Spacers Height)', desc: '把立下方所有垫圈的总高度。', tip: '在不更换把立的情况下调节车头高低的唯一手段。' },
  'handlebarWidth': { title: '把横宽度 (Handlebar Width)', desc: '左右手变头/握把中心点之间的距离。', tip: '通常与肩宽对齐，更窄利于气动，更宽利于呼吸与操控。' },
  'crankLength': { title: '曲柄长度 (Crank Length)', desc: '五通中心到脚踏轴心的中心距离。', tip: '影响踩踏杠杆比及深蹲压力，小个子或高转频选手建议缩短。' },
  'handlebarDrop': { title: '车把落差', desc: '座垫顶面与车把顶面之间的垂直高度差。',  tip: '体现了骑行的激进程度，数值越大躯干角度越低。' },
  'saddleWidth': { title: '座垫宽度', desc: '座垫最宽处的横向距离。', tip: '需根据您的坐骨宽度来选择。' },
  'pedalType': { title: '踏板类型', desc: '使用的脚踏与鞋具组合。', tip: '锁鞋会增加有效的“腿长”，对座高有显著影响。' }
};

const activeFieldInfo = computed(() => fieldGuides[activeField.value] || fieldGuides['brand']);

const hasInputValue = (value) => value !== null && value !== undefined && value !== '' && !Number.isNaN(value);
const optionalCockpitFields = ['stemLength', 'stemAngle', 'spacersHeight'];
const safetyRangeRules = {
  saddleHeight: { 
    label: '当前座高', 
    min: 450, 
    max: 1100, 
    unit: 'mm',
    safetyMessage: '安全风险：座高超出人类正常使用范围，可能导致膝盖严重损伤或锁鞋脱落'
  },
  saddleSetback: { 
    label: '座垫后移', 
    min: -80, 
    max: 150, 
    unit: 'mm',
    safetyMessage: '安全风险：过度偏移会导致重心失衡，爬坡翘头或下坡失控'
  },
  saddleTilt: { 
    label: '座垫角度', 
    min: -25, 
    max: 15, 
    unit: '°',
    safetyMessage: '安全风险：角度过大会导致会阴部严重压迫或从座垫滑落'
  },
  stemLength: { 
    label: '把立长度', 
    min: 30, 
    max: 220, 
    unit: 'mm',
    safetyMessage: '安全风险：过短导致转向过于灵敏失控，过长导致操控迟钝下坡危险'
  },
  stemAngle: { 
    label: '把立角度', 
    min: -35, 
    max: 35, 
    unit: '°',
    safetyMessage: '安全风险：极端角度会导致头管和前叉受力异常，增加断裂风险'
  },
  spacersHeight: { 
    label: '垫圈高度', 
    min: 0, 
    max: 90, 
    unit: 'mm',
    safetyMessage: '安全风险：垫圈过高会降低车头刚性，前叉舵管易断裂'
  },
  handlebarWidth: { 
    label: '把横宽度', 
    min: 300, 
    max: 950, 
    unit: 'mm',
    safetyMessage: '安全风险：过窄操控不稳，过宽过弯困难易碰撞障碍物'
  },
  crankLength: { 
    label: '曲柄长度', 
    min: 140, 
    max: 195, 
    unit: 'mm',
    safetyMessage: '安全风险：长度不当会导致膝关节过度屈伸，造成永久性损伤'
  },
  handlebarDrop: { 
    label: '车把落差', 
    min: -150, 
    max: 250, 
    unit: 'mm',
    safetyMessage: '安全风险：过大落差会导致重心严重偏移，易翻车或造成颈腰损伤'
  },
  saddleWidth: { 
    label: '座垫宽度', 
    min: 100, 
    max: 220, 
    unit: 'mm',
    safetyMessage: '安全风险：过窄坐骨支撑不足，过宽严重摩擦大腿内侧'
  }
};

const validateSafetyField = (field) => {
  const rule = safetyRangeRules[field];
  const value = formData[field];
  if (!rule) return '';
  if (!hasInputValue(value)) {
    return '';
  }
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return `${rule.label}必须为有效数字。`;
  }
  if (numericValue < rule.min || numericValue > rule.max) {
    return rule.safetyMessage;
  }
  return '';
};

const fieldWarnings = computed(() => ({
  saddleHeight: validateSafetyField('saddleHeight'),
  saddleSetback: validateSafetyField('saddleSetback'),
  saddleTilt: validateSafetyField('saddleTilt'),
  stemLength: validateSafetyField('stemLength'),
  stemAngle: validateSafetyField('stemAngle'),
  spacersHeight: validateSafetyField('spacersHeight'),
  handlebarWidth: validateSafetyField('handlebarWidth'),
  crankLength: validateSafetyField('crankLength'),
  handlebarDrop: validateSafetyField('handlebarDrop'),
  saddleWidth: validateSafetyField('saddleWidth')
}));

const getFirstSafetyWarning = () => {
  return Object.keys(safetyRangeRules)
      .map(field => fieldWarnings.value[field])
      .find(Boolean) || '';
};

const normalizeOptionalNumber = (value) => {
  return hasInputValue(value) ? value : null;
};

const buildBikePayload = () => {
  const payload = { ...formData };
  optionalCockpitFields.forEach(field => {
    payload[field] = normalizeOptionalNumber(payload[field]);
  });
  return payload;
};

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getBikeDetail(route.query.id);
      const data = res.data || res;
      console.log('Bike detail loaded:', data);

      // 1. 基础信息平铺 (Top level fields)
      Object.assign(formData, {
        id: data.id,
        userId: data.userId,
        remark: data.remark,
        imageUrl: data.imageUrl,
        brand: data.brand,
        brandCn: data.brandCn,
        model: data.model,
        modelYear: data.modelYear,
        bikeType: data.bikeType
      });

      // 2. 几何信息平铺 (From bikeModelVO)
      if (data.bikeModelVO) {
        formData.bikeModelId = data.bikeModelVO.id;
        formData.size = data.bikeModelVO.size;
        // 核心几何
        const geo = data.bikeModelVO;
        const geoFields = ['stack', 'reach', 'seatTubeAngle', 'headTubeAngle', 'bbDrop', 'wheelBase', 'standOverHeight'];
        geoFields.forEach(f => {
          if (geo[f] !== undefined) formData[f] = geo[f];
        });
      }

      // 3. 实车设定平铺 (From currentSetupVO)
      if (data.currentSetupVO) {
        const setup = data.currentSetupVO;
        const setupFields = [
          'crankLength', 'stemLength', 'handlebarWidth', 'stemAngle', 'spacersHeight',
          'handlebarDrop', 'saddleWidth', 'saddleHeight', 'saddleSetback', 'saddleTilt', 'pedalType'
        ];
        setupFields.forEach(f => {
          if (setup[f] !== undefined) formData[f] = setup[f];
        });
      }

      // 编辑模式下直接进入完成状态
      // 手动录入(sourceType=2)的车型保持可编辑，官方数据才锁定几何
      const isManualSource = data.bikeModelVO?.sourceType === 2;
      isManualMode.value = isManualSource;
      currentStep.value = 'complete';
      isGeometryLocked.value = !!formData.bikeModelId && !isManualSource;
    } catch (e) {
      console.error('Failed to load bike detail', e);
      showToast('获取单车详情失败', 'error');
    }
  }
});

const onSearchInput = () => {
  if (isEdit.value || isManualMode.value || formData.bikeModelId || currentStep.value !== 'search') return;
  showDropdown.value = !!formData.brand;
  if (searchTimer) clearTimeout(searchTimer);
  isSearching.value = true;
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchModelNames(formData.brand);
      modelSuggestions.value = res.data || res;
    } finally { isSearching.value = false; }
  }, 400);
};

const selectModelGroup = async (item) => {
  formData.brand = item.brand;
  formData.brandCn = item.brandCn;
  formData.model = item.model;
  showDropdown.value = false;

  try {
    const res = await getBikeModelTree(item.brand, item.model);
    selectedModelTree.value = res.data || res;
    formData.modelYear = '';
    formData.size = '';
    isGeometryLocked.value = false;
    // 进入品牌型号已选择状态
    currentStep.value = 'brand_selected';
  } catch (e) {
    showToast('无法从数据库调取型号变体', 'error');
  }
};

const onYearSelect = (year) => {
  formData.modelYear = year;
  formData.size = '';
  isGeometryLocked.value = false;
  // 进入年份已选择状态
  currentStep.value = 'year_selected';
};

const onSizeSelect = (size) => {
  formData.size = size;
  const match = selectedModelTree.value.find(v => v.modelYear === formData.modelYear && v.size === size);
  if (match) {
    formData.bikeModelId = match.id;
    formData.bikeType = match.bikeType || 1;
    // 自动填充核心几何
    formData.stack = match.stack;
    formData.reach = match.reach;
    formData.seatTubeAngle = match.seatTubeAngle;
    formData.headTubeAngle = match.headTubeAngle;
    formData.bbDrop = match.bbDrop;
    // 自动填充其他几何 (如果库里有)
    if (match.wheelBase) formData.wheelBase = match.wheelBase;
    if (match.standOverHeight) formData.standOverHeight = match.standOverHeight;
    if (match.imageUrl) formData.imageUrl = match.imageUrl;

    isGeometryLocked.value = true;
    // 进入完成状态，展示所有模块
    currentStep.value = 'complete';
    showToast('校准完成：官方数据已锁定', 'success');
  }
};

const enableManualEntry = () => {
  isManualMode.value = true;
  formData.bikeModelId = null;
  isGeometryLocked.value = false;
  showDropdown.value = false;
  selectedModelTree.value = [];
  // 清空所有几何数据，确保手动录入时从零开始
  formData.stack = null;
  formData.reach = null;
  formData.seatTubeAngle = null;
  formData.headTubeAngle = null;
  formData.bbDrop = null;
  formData.wheelBase = null;
  formData.standOverHeight = null;
  formData.bikeType = null;
  formData.imageUrl = '';
  // 进入完成状态，展示所有模块
  currentStep.value = 'complete';
  showToast('请手动输入实车几何数据', 'info');
};

const resetSearch = () => {
  isManualMode.value = false;
  isGeometryLocked.value = false;
  formData.bikeModelId = null;
  formData.brand = '';
  formData.brandCn = '';
  formData.model = '';
  formData.modelYear = '';
  formData.size = '';
  formData.bikeType = null;
  formData.imageUrl = '';
  selectedModelTree.value = [];
  currentStep.value = 'search';
  showToast('搜索已重置', 'info');
};

const closeDropdown = () => setTimeout(() => showDropdown.value = false, 200);

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
  const isConfirmed = await showConfirm(`确定要将这辆单车退役吗？历史记录将被封存。`, '单车退役');
  if (isConfirmed) {
    try {
      await retireUserBike(formData.id);
      showToast('单车已移至荣誉档案', 'success');
      router.push('/lab/garage');
    } catch (e) {
      showToast('退役失败', 'error');
    }
  }
};

const submitForm = async () => {
  if (!formData.stack || !formData.reach) {
    showToast('Stack 和 Reach 为核心基准，不可为空', 'warning');
    return;
  }
  const safetyWarning = getFirstSafetyWarning();
  if (safetyWarning) {
    showToast(safetyWarning, 'warning');
    return;
  }
  submitting.value = true;
  try {
    await saveUserBike(buildBikePayload());
    showToast('档案同步成功', 'success');
    router.push('/lab/garage');
  } catch (error) {
    showToast(error.message || '保存异常，请检查车辆信息', 'error');
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
.page-subtitle { font-size: 0.75rem; color: var(--text-muted); font-family: var(--font-serif); font-style: italic; letter-spacing: 2px; text-transform: uppercase; }
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
  min-height: 600px;
}

.group-title { font-size: 1.1rem; color: var(--primary-color); font-weight: 700; border-left: 3px solid var(--primary-color); padding-left: 12px; margin-bottom: 20px; }
.group-desc { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 25px; margin-top: -10px; }

.input-grid { display: grid; gap: 24px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; }

.zen-form-item { display: flex; flex-direction: column; gap: 8px; }
.label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
.input-label { font-size: 0.75rem; font-weight: 800; color: var(--text-muted); letter-spacing: 0.05em; text-transform: uppercase; }

.zen-input {
  width: 100%; padding: 12px 16px; border: 1px solid #EDEDED; background: #FAFAFA;
  border-radius: var(--radius-sm); outline: none; transition: 0.3s;
}
.zen-input:focus { border-color: var(--primary-color); background: white; box-shadow: 0 0 0 4px rgba(92, 110, 88, 0.05); }
.zen-input.has-warning {
  border-color: #E2A84D;
  background: linear-gradient(135deg, rgba(226, 168, 77, 0.14), rgba(255, 248, 235, 0.92));
  box-shadow: 0 8px 22px rgba(226, 168, 77, 0.12);
}
.zen-input::-webkit-outer-spin-button, .zen-input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
.zen-input[type=number] { -moz-appearance: textfield; }

.input-with-unit { position: relative; }
.unit { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); font-size: 0.75rem; color: #AAA; font-weight: 700; }
.field-warning {
  margin: -2px 0 0;
  padding: 7px 10px;
  border-left: 3px solid #E2A84D;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(226, 168, 77, 0.16), rgba(226, 168, 77, 0.04));
  color: #9A641C;
  font-size: 0.74rem;
  line-height: 1.5;
}

.type-selector { display: flex; gap: 8px; flex-wrap: wrap; }
.type-selector button { padding: 8px 14px; border-radius: 4px; border: 1px solid #EEE; background: white; font-size: 0.8rem; cursor: pointer; transition: 0.3s; color: var(--text-muted); }
.type-selector button.active { background: var(--primary-color); color: white; border-color: var(--primary-color); }
.type-selector.disabled { pointer-events: none; opacity: 0.7; }

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

/* --- 搜索与自动完成 --- */
.autocomplete-container { position: relative; }
.search-input-wrapper { position: relative; }
.search-status-icon { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); color: #ccc; }
.search-dropdown {
  position: absolute; top: calc(100% + 5px); left: 0; right: 0; z-index: 100;
  background: white; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  max-height: 400px;
  display: flex;
  flex-direction: column;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f8f8;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.header-text {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.dropdown-divider {
  height: 1px;
  background: #eee;
  flex-shrink: 0;
}

.dropdown-scroll-area {
  overflow-y: auto;
  max-height: 320px;
}
.suggestion-item { padding: 15px 20px; cursor: pointer; border-bottom: 1px solid #F5F5F5; display: flex; justify-content: space-between; align-items: center; }
.suggestion-item:hover { background: #f4f6f3; }
.s-brand { color: var(--primary-color); font-weight: 800; margin-right: 8px; }
.s-model { font-weight: 600; color: var(--text-main); }
.s-cn { font-size: 0.8rem; color: #999; }
.dropdown-empty { padding: 30px; text-align: center; color: #999; font-size: 0.9rem; }
.manual-entry-btn { margin-top: 10px; padding: 6px 16px; background: var(--primary-color); color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.manual-entry-helper { margin-top: 12px; font-size: 0.85rem; color: #999; text-align: left; padding: 0 4px; }
.btn-text-link { background: none; border: none; color: var(--primary-color); font-weight: 700; cursor: pointer; margin-left: 4px; padding: 0; text-decoration: underline; font-size: inherit; }
.btn-text-link:hover { opacity: 0.8; }
.manual-entry-helper a { color: var(--primary-color); font-weight: 600; text-decoration: none; }

/* --- 锁定状态视觉 --- */
.locked-section { opacity: 0.85; pointer-events: none; }
.force-clickable { pointer-events: auto !important; position: relative; z-index: 10; }
.locked-section .zen-input:disabled { background: #f8f8f8; cursor: not-allowed; border-color: #eee; color: #888; }
.group-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.lock-tag { font-size: 0.75rem; color: var(--primary-color); font-weight: 800; background: #ebf0e9; padding: 4px 10px; border-radius: 4px; border: 1px solid var(--primary-color); }

.identity-section { display: grid; gap: 24px; grid-column: 1 / -1; }
.identity-section.locked-section { opacity: 0.9; pointer-events: none; border: 1px dashed rgba(0,0,0,0.05); padding: 15px; margin: -15px; border-radius: 12px; }
.identity-section.locked-section .zen-input { background: #f8f8f8; color: #888; }

/* --- 药丸选择器 Pills --- */
.pill-group { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; margin-bottom: 5px; }
.pill-btn {
  padding: 8px 20px; border-radius: 50px; border: 1.5px solid #EEE;
  background: white; color: var(--text-muted); font-size: 0.85rem;
  font-weight: 600; cursor: pointer; transition: 0.3s;
}
.pill-btn:hover { border-color: var(--primary-color); color: var(--primary-color); }
.pill-btn.active {
  background: var(--primary-color); border-color: var(--primary-color);
  color: white; box-shadow: 0 4px 12px rgba(92, 110, 88, 0.2);
}

.btn-reset { background: none; border: none; color: var(--primary-color); font-size: 0.75rem; font-weight: 700; cursor: pointer; text-decoration: underline; opacity: 0.8; transition: 0.3s; }
.btn-reset:hover { opacity: 1; }

/* 锁定字段样式 */
.locked-field {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f8f8;
  border: 1px solid #eee;
  border-radius: var(--radius-sm);
  color: #888;
}
.locked-value {
  font-weight: 600;
  color: var(--text-main);
}
.lock-icon {
  color: var(--primary-color);
  font-size: 0.9rem;
}

/* 年份/尺码选择高亮引导 */
.year-selection-highlight,
.size-selection-highlight {
  animation: pulse-highlight 2s ease-in-out infinite;
  padding: 16px;
  margin: -8px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, rgba(92, 110, 88, 0.08) 0%, rgba(212, 175, 55, 0.08) 100%);
  border: 2px dashed var(--primary-color);
}

.year-selection-highlight .input-label,
.size-selection-highlight .input-label {
  color: var(--primary-color);
  font-size: 0.85rem;
  font-weight: 700;
}

.highlight-icon {
  margin-right: 8px;
  animation: bounce 1s ease-in-out infinite;
}

@keyframes pulse-highlight {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(92, 110, 88, 0.4);
    border-color: var(--primary-color);
  }
  50% {
    box-shadow: 0 0 20px 5px rgba(92, 110, 88, 0.2);
    border-color: var(--accent-gold);
  }
}

@keyframes bounce {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(5px); }
}

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
/* Fitter 建议块 */
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
