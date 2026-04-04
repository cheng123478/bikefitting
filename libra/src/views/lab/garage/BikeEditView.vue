<template>
  <div class="bike-edit-page">
    <div class="container lab-container">

      <!-- ================= 1. 澶撮儴宸ュ叿鏍?(涓ユ牸鍚屾 MyGarageView) ================= -->
      <div class="toolbar-row" v-scroll-reveal>
        <div class="text-group">
          <h1 class="page-title">{{ isEdit ? '鍗曡溅鏍″噯' : '鏂板鍗曡溅' }}</h1>
          <p class="page-subtitle">BIKE CALIBRATION / 绮惧瘑鍑犱綍涓庤瀹氬綍鍏?/p>
        </div>

        <div class="tabs-group">
          <div class="header-actions">
            <button class="btn btn-outline" @click="$router.back()">
              <i class="fas fa-chevron-left"></i> 杩斿洖杞﹀簱
            </button>
          </div>
        </div>
      </div>

      <!-- ================= 2. 鏍稿績缂栬緫鍖哄煙 ================= -->
      <div class="edit-content">

        <!-- 宸︿晶锛氬綍鍏ヨ〃鍗?-->
        <main class="form-section glass-card" v-scroll-reveal>

          <!-- 妯″潡 01锛氬熀纭€淇℃伅 -->
          <div class="form-group">
            <h4 class="group-title">01 鍩虹淇℃伅 (Basic Info)</h4>
            <div class="input-grid">
              <div class="identity-section" :class="{ 'locked-section': isGeometryLocked || isEdit }">
                <!-- 鍝佺墝璇嗗埆涓庢悳绱㈠叆鍙?-->
                <div class="zen-form-item full-width autocomplete-container">
                <div class="label-row">
                  <label class="input-label">鍝佺墝 (涓?鑻辨枃鎼滅储) *</label>
                  <button v-if="formData.brand && !isEdit" type="button" class="btn-reset" @click="resetSearch">閲嶆柊妫€绱㈠瀷鍙?/button>
                </div>
                  <div class="search-input-wrapper">
                  <input
                      v-model="formData.brand"
                      class="zen-input"
                      :class="{ 'is-linked': formData.bikeModelId && !isManualMode }"
                      placeholder="杈撳叆鍝佺墝锛屽: Giant / 鎹峰畨鐗?
                      :disabled="isEdit || isGeometryLocked"
                      @input="onSearchInput"
                      @focus="showDropdown = true; activeField = 'brand'"
                      @blur="closeDropdown"
                  >
                  <div class="search-status-icon">
                    <i v-if="isSearching" class="fas fa-circle-notch fa-spin"></i>
                    <i v-else-if="formData.bikeModelId" class="fas fa-link text-primary"></i>
                    <i v-else class="fas fa-search"></i>
                  </div>

                  <!-- 鎼滅储缁撴灉涓嬫媺 -->
                  <transition name="fade-slide-y">
                    <div class="search-dropdown glass-card" v-if="showDropdown && formData.brand && !isEdit && !isManualMode">
                      <div v-if="modelSuggestions.length === 0 && !isSearching" class="dropdown-empty">
                        鏈湪搴撲腑鎵惧埌鐩稿叧鍨嬪彿
                        <button class="manual-entry-btn" @mousedown="enableManualEntry">鎵嬪姩褰曞叆</button>
                      </div>
                      <div v-for="item in modelSuggestions" :key="item.model" class="suggestion-item" @mousedown="selectModelGroup(item)">
                        <div class="item-main">
                          <span class="s-brand">{{ item.brand }}</span>
                          <span class="s-model">{{ item.model }}</span>
                        </div>
                        <span class="s-cn" v-if="item.brandCn">{{ item.brandCn }}</span>
                      </div>
                    </div>
                  </transition>
                  <!-- 鎼滅储杈呭姪鎻愮ず锛氬綋鏈夊搧鐗岃緭鍏ヤ絾娌℃湁鍨嬪彿涓斾笉鏄墜鍔ㄦā寮忔椂鏄剧ず -->
                  <div class="manual-entry-helper" v-if="formData.brand && !formData.model && !isManualMode && !showDropdown && !isEdit">
                    鏈壘鍒板瀷鍙凤紵<button type="button" class="btn-text-link" @click="enableManualEntry">鎵嬪姩褰曞叆</button>
                  </div>
                </div>
              </div>

              <!-- 鍏宠仈韬唤淇℃伅锛氫粎鍦?1.鎵嬪姩妯″紡 2.宸查€夊瀷鍙?3.缂栬緫妯″紡 鏃舵樉绀?-->
              <template v-if="isManualMode || (formData.brand && formData.model) || isEdit">
                <div class="grid-2">
                  <div class="zen-form-item">
                    <label class="input-label">涓枃鍝佺墝</label>
                    <input v-model="formData.brandCn" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'brandCn'">
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">鍏蜂綋鍨嬪彿 *</label>
                    <input v-model="formData.model" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'model'">
                  </div>
                </div>

                <!-- 绾ц仈锛氬勾浠介€夋嫨鑽父 -->
                <div class="zen-form-item full-width" v-if="availableYears.length > 0 && !isManualMode && !formData.modelYear">
                  <label class="input-label">閫夋嫨鐢熶骇骞翠唤</label>
                  <div class="pill-group">
                    <button v-for="y in availableYears" :key="y"
                            class="pill-btn" :class="{ active: formData.modelYear === y }"
                            @click="onYearSelect(y)" type="button">
                      {{ y }}
                    </button>
                  </div>
                </div>

                <!-- 绾ц仈锛氬昂瀵搁€夋嫨鑽父 -->
                <div class="zen-form-item full-width" v-if="formData.modelYear && availableSizes.length > 0 && !isManualMode && !formData.size">
                  <label class="input-label">閫夋嫨杞︽灦灏虹爜</label>
                  <div class="pill-group">
                    <button v-for="s in availableSizes" :key="s"
                            class="pill-btn" :class="{ active: formData.size === s }"
                            @click="onSizeSelect(s)" type="button">
                      {{ s }}
                    </button>
                  </div>
                </div>

                <div class="grid-2">
                  <div class="zen-form-item">
                    <label class="input-label">骞翠唤</label>
                    <input v-model="formData.modelYear" class="zen-input" placeholder="2024" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'modelYear'">
                  </div>
                  <div class="zen-form-item">
                    <label class="input-label">灏虹爜</label>
                    <input v-model="formData.size" class="zen-input" placeholder="渚嬪: 52/M" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'size'">
                  </div>
                </div>
              </template>

              <div class="zen-form-item" @click="activeField = 'type'" v-if="isManualMode || (formData.brand && formData.model) || isEdit">
                <label class="input-label">鍗曡溅绫诲瀷</label>
                <div class="type-selector" :class="{ disabled: isGeometryLocked || isEdit }">
                  <button v-for="type in bikeTypes" :key="type.code"
                          :class="{active: formData.bikeType === type.code}"
                          @click="formData.bikeType = type.code" type="button">
                    {{ type.name }}
                  </button>
                </div>
              </div>
              </div>
              <div class="zen-form-item">
                <label class="input-label">鍗曡溅澶囨敞</label>
                <input v-model="formData.remark" class="zen-input" placeholder="渚嬪锛氭垜鐨勪富鍔涚埇鍧″崟杞? @focus="activeField = 'remark'">
              </div>

              <!-- 鍥剧墖涓婁紶锛氬甫 /api 鍓嶇紑鍥炴樉 -->
              <div class="zen-form-item full-width" v-if="isManualMode || (formData.brand && formData.model) || isEdit">
                <label class="input-label">鍗曡溅瑙嗚蹇収</label>
                <div class="image-uploader" @click="triggerUpload" :class="{ 'has-img': formData.imageUrl }" @mouseenter="activeField = 'imageUrl'">
                  <img v-if="formData.imageUrl" :src="formData.imageUrl.startsWith('http') ? formData.imageUrl : '/api' + formData.imageUrl" class="preview-img" />
                  <div v-else class="upload-placeholder">
                    <i class="fas fa-camera-retro"></i>
                    <span>涓婁紶瀹炶溅缇庣収</span>
                    <p>寤鸿浣跨敤鐧藉簳鎴栦晶闈㈡棤鑳屾櫙鐓х墖</p>
                  </div>
                  <input type="file" ref="fileInput" hidden @change="handleFileChange" accept="image/*" :disabled="isGeometryLocked || isEdit" />
                  <div class="upload-mask" v-if="uploading">
                    <i class="fas fa-circle-notch fa-spin"></i>
                  </div>
                </div>
              </div>

            </div>
          </div>

          <div class="divider"></div>

          <!-- 妯″潡 02锛氳溅鏋跺嚑浣?(DNA) -->
          <div class="form-group" :class="{ 'locked-section': isGeometryLocked || isEdit }">
            <div class="group-header">
              <h4 class="group-title">02 杞︽灦鍑犱綍 (Frame Geometry)</h4>
              <div v-if="isGeometryLocked || isEdit" class="lock-tag"><i class="fas fa-shield-alt"></i> 鏁版嵁宸查攣瀹?/div>
            </div>
            <p class="group-desc">鏁版嵁鏉ユ簮锛氬巶鍟嗗畼缃戝嚑浣曡〃銆傝繖鏄?Fitting 鏂规鐨勫熀纭€銆?/p>
            <div class="input-grid">
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">鍫嗛珮 * </label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.stack" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'stack'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">鍓嶄几閲?* </label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.reach" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'reach'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-3">
                <div class="zen-form-item">
                  <label class="input-label">绔嬬瑙掑害 * </label>
                  <div class="input-with-unit">
                    <input type="number" step="0.1" v-model.number="formData.seatTubeAngle" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'seatTubeAngle'">
                    <span class="unit">掳</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">澶寸瑙掑害</label>
                  <div class="input-with-unit">
                    <input type="number" step="0.1" v-model.number="formData.headTubeAngle" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'headTubeAngle'">
                    <span class="unit">掳</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">浜旈€氫笅娌?/label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.bbDrop" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'bbDrop'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">杞磋窛</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.wheelBase" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'wheelBase'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">璺ㄩ珮</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.standOverHeight" class="zen-input" :disabled="isGeometryLocked || isEdit" @focus="activeField = 'standOverHeight'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 妯″潡 03锛氬疄杞﹁瀹?(The Setup) -->
          <div class="form-group">
            <h4 class="group-title">03 瀹炶溅璁惧畾 (Current Setup)</h4>
            <p class="group-desc">浣跨敤鍗峰昂娴嬮噺鐩墠杞﹁締鐨勭湡瀹炶皟鏁村€笺€?/p>
            <div class="input-grid">
              <div class="grid-3">
                <div class="zen-form-item">
                  <label class="input-label">褰撳墠搴ч珮</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleHeight" class="zen-input" @focus="activeField = 'saddleHeight'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">搴у灚鍚庣Щ</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleSetback" class="zen-input" @focus="activeField = 'saddleSetback'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">搴у灚瑙掑害</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleTilt" class="zen-input" @focus="activeField = 'saddleTilt'">
                    <span class="unit">掳</span>
                  </div>
                </div>
              </div>
              <div class="grid-3">
                <div class="zen-form-item">
                  <label class="input-label">鎶婄珛闀垮害</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.stemLength" class="zen-input" @focus="activeField = 'stemLength'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">鎶婄珛瑙掑害</label>
                  <div class="input-with-unit">
                    <input type="number" step="1" v-model.number="formData.stemAngle" class="zen-input" @focus="activeField = 'stemAngle'">
                    <span class="unit">掳</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">鍨湀楂樺害</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.spacersHeight" class="zen-input" @focus="activeField = 'spacersHeight'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-3">
                <div class="zen-form-item">
                  <label class="input-label">鎶婃í瀹藉害</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.handlebarWidth" class="zen-input" @focus="activeField = 'handlebarWidth'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">鏇叉焺闀垮害</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.crankLength" class="zen-input" @focus="activeField = 'crankLength'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">杞︽妸钀藉樊</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.handlebarDrop" class="zen-input" @focus="activeField = 'handlebarDrop'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">搴у灚瀹藉害</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleWidth" class="zen-input" @focus="activeField = 'saddleWidth'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">韪忔澘绫诲瀷</label>
                  <select v-model="formData.pedalType" class="zen-input" @focus="activeField = 'pedalType'">
                    <option :value="null">璇烽€夋嫨韪忔澘绫诲瀷</option>
                    <option v-for="opt in pedalOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                  </select>
                </div>
              </div>
            </div>
          </div>

          <!-- 搴曢儴鎿嶄綔鏍?-->
          <div class="form-actions-footer">
            <div class="footer-left">
              <button v-if="isEdit" type="button" class="btn-retire-text" @click="handleRetire">
                <i class="fas fa-archive"></i> 閫€褰硅繖杈嗗崟杞?
              </button>
            </div>
            <div class="footer-right">
              <button class="btn btn-primary btn-xl btn-glow" :disabled="submitting" @click="submitForm">
                <i class="fas" :class="submitting ? 'fa-circle-notch fa-spin' : 'fa-check'"></i>
                {{ submitting ? '鏁版嵁鍚屾涓?..' : '淇濆瓨骞跺悓姝ヨ嚦杞﹀簱' }}
              </button>
            </div>
          </div>
        </main>

        <!-- ================= 3. 鍙充晶锛氬姩鎬佸彲瑙嗗寲鎸囧紩 (Sticky) ================= -->
        <aside class="guide-section">
          <div class="visualizer-container glass-card">
            <BikeVisualizer :activePart="activeField" :bikeType="formData.bikeType" />
          </div>

          <Transition name="fade-slide" mode="out-in">
            <div class="field-info-box glass-card" :key="activeField">
              <h4>{{ activeFieldInfo.title }}</h4>
              <p>{{ activeFieldInfo.desc }}</p>
              <div class="fittor-tip" v-if="activeFieldInfo.tip">
                <strong>Fittor 寤鸿锛?/strong> {{ activeFieldInfo.tip }}
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

const bikeTypes = [
  { name: '鍏矾杞?, code: 1 }, { name: '灞卞湴杞?, code: 2 },
  { name: '鍏ㄥ湴褰?, code: 3 }, { name: '鍩庡競杞?, code: 4 }
];

const pedalOptions = [
  { label: '骞宠笍 + 杩愬姩闉?, value: 1 },
  { label: '鍏矾閿佽笍 + 閿侀瀷', value: 2 },
  { label: '灞卞湴閿佽笍 + 閿侀瀷', value: 3 }
];

// --- 鎼滅储涓庣骇鑱旂姸鎬?---
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

// 璁＄畻灞炴€э細浠庢ā鍨嬫爲涓彁鍙栧勾浠?
const availableYears = computed(() => {
  if (!selectedModelTree.value.length) return [];
  const years = selectedModelTree.value.map(v => v.modelYear);
  return [...new Set(years)].filter(Boolean).sort((a, b) => b - a);
});

// 璁＄畻灞炴€э細鏍规嵁閫変腑骞翠唤鎻愬彇灏哄
const availableSizes = computed(() => {
  if (!formData.modelYear || !selectedModelTree.value.length) return [];
  return selectedModelTree.value
      .filter(v => v.modelYear === formData.modelYear)
      .map(v => v.size);
});

const fieldGuides = {
  // --- 01 鍩虹韬唤 (Identity) ---
  'brand': { title: '鍝佺墝 (Brand English)', desc: '鍝佺墝鍏ㄧ悆閫氱敤鍚嶃€?, tip: '杈撳叆鑻辨枃鍚嶇О鏇存槗鍖归厤瀹樻柟鏁版嵁搴撱€? },
  'brandCn': { title: '鍝佺墝 (Brand 涓枃)', desc: '鍝佺墝鐨勪腑鏂囪瘧鍚嶃€? },
  'model': { title: '鍨嬪彿 (Model)', desc: '鍏蜂綋鐨勭郴鍒椾笌鍨嬪彿銆?, tip: '渚嬪 TCR Advanced, Tarmac SL8 绛夈€? },
  'modelYear': { title: '娆惧瀷骞翠唤 (Model Year)', desc: '杞﹁締鐨勮璁″勾浠姐€?, tip: '鍚屼竴鍨嬪彿涓嶅悓骞翠唤鐨勫嚑浣曞彲鑳藉瓨鍦ㄦ樉钁楀樊寮傘€? },
  'size': { title: '瀹樻柟灏虹爜 (Size)', desc: '杞︽灦鏍囩涓婄殑灏虹爜銆?, tip: '濡?52, S, 540 绛夈€? },
  'bikeType': { title: '鍗曡溅绫诲瀷 (Bike Type)', desc: '杞﹁締鐨勫垎绫诲睘鎬с€?, tip: '涓嶅悓绫诲瀷鐨勮溅鏈夊畬鍏ㄤ笉鍚岀殑鐞嗘兂瑙掑害鑼冨洿銆? },
  'remark': { title: '澶囨敞 (Remark)', desc: '鎮ㄧ殑涓€у寲璁板綍銆? },
  'imageUrl': { title: '鍗曡溅瑙嗚蹇収', desc: '杞﹁締鐨勭湡瀹炵収鐗囥€?, tip: '杩欏皢浣滀负妗ｆ灏侀潰锛屽苟杈呭姪杩涜瑙嗚瀵归綈鏍￠獙銆? },

  // --- 02 杞︽灦鍑犱綍 (DNA / Geometry) ---
  'stack': { title: '鍫嗛珮 (Stack)', desc: '浜旈€氫腑蹇冨埌澶寸椤堕儴涓績鐨勫瀭鐩磋窛绂汇€?, tip: '鍐冲畾楠戣濮挎€佲€滈珮浣庘€濈殑鍩哄噯锛孲tack 瓒婇珮濮挎€佽秺鐩寸珛銆? },
  'reach': { title: '鍓嶄几閲?(Reach)', desc: '浜旈€氫腑蹇冨埌澶寸椤堕儴涓績鐨勬按骞宠窛绂汇€?, tip: '琛￠噺杞︽灦闀跨煭鏈€瀹㈣鐨勬寚鏍囷紝涓嶅彈绔嬬瑙掑害骞叉壈銆? },
  'seatTubeAngle': { title: '绔嬬瑙掑害 (STA)', desc: '绔嬬涓績绾夸笌姘村钩绾跨殑澶硅銆?, tip: '鐩存帴褰卞搷搴у灚鍓嶅悗鐨勫彲璋冭寖鍥村強閲嶅績鍒嗗竷銆? },
  'headTubeAngle': { title: '澶寸瑙掑害 (HTA)', desc: '澶寸涓績绾夸笌姘村钩绾跨殑澶硅銆?, tip: '瑙掑害瓒婂皬锛堟枩锛夋搷鎺ц秺绋筹紝瑙掑害瓒婂ぇ锛堢洿锛夎浆鍚戣秺鐏垫晱銆? },
  'bbDrop': { title: '浜旈€氫笅娌夐噺 (BB Drop)', desc: '鍓嶅悗杞磋繛绾夸笌浜旈€氫腑蹇冪殑鍨傜洿钀藉樊銆?, tip: '鍊艰秺澶ч噸蹇冭秺浣庯紝楂橀€熻繃寮洿绋炽€? },
  'wheelBase': { title: '杞磋窛', desc: '鍓嶅悗杞酱蹇冧箣闂寸殑姘村钩璺濈銆?, tip: '闀胯酱璺濆埄浜庡钩绋冲贰鑸紝鐭酱璺濆埄浜庣伒娲荤埇鍧°€? },
  'standOverHeight': { title: '璺ㄩ珮', desc: '杞︽灦涓婄涓績鐐瑰埌鍦伴潰鐨勫瀭鐩磋窛绂汇€?, tip: '鍏充箮鍋滆溅钀借剼鏃剁殑瀹夊叏鎬э紝寤鸿涓庤韩浣撹法楂樹繚鎸?2-5cm 浣欓噺銆?},

  // --- 03 瀹炶溅璁惧畾 (The Setup) ---
  'saddleHeight': { title: '搴у灚楂樺害 (Saddle Height)', desc: '浜旈€氫腑蹇冩部绔嬬鑷冲骇鍨《闈腑蹇冪偣鐨勮窛绂汇€?, tip: 'Fitting 涓渶閲嶈鐨勫弬鏁帮紝鐩存帴鍐冲畾韪╄笍鏁堢巼鍜岃啙鐩栧仴搴枫€? },
  'saddleSetback': { title: '搴у灚鍚庣Щ閲?(Saddle Setback)', desc: '搴у灚榧诲皷鐩稿浜庝簲閫氫腑蹇冨瀭鐩寸嚎鐨勬按骞充綅绉汇€?, tip: '寤鸿浣跨敤閾呭瀭绾夸粠鍧愬灚榧诲ご鍨傜洿鍚戜笅瀵瑰噯浜旈€氳酱蹇冿紝娴嬮噺鍏舵按骞宠窛绂汇€? },
  'saddleTilt': { title: '搴у灚鍊捐 (Saddle Tilt)', desc: '搴у灚涓績绾夸笌鍨傜洿绾跨殑澶硅銆?, tip: '鍐冲畾浜嗕細闃村帇鍔涖€? },
  'stemLength': { title: '鎶婄珛闀垮害 (Stem Length)', desc: '鎶婄珛涓績鐩栧埌杞︽妸澶规寔浣嶄腑蹇冪偣鐨勮窛绂汇€?, tip: '璋冩暣涓婅偄鎷変几鎰燂紙Reach锛夋渶鐩存帴鐨勯浂浠躲€? },
  'stemAngle': { title: '鎶婄珛瑙掑害 (Stem Angle)', desc: '鎶婄珛涓績绾夸笌鍨傜洿鏂瑰悜鐨勫す瑙掋€?, tip: '閫氳繃缈昏浆鎶婄珛鍙樉钁楁敼鍙樿溅澶撮珮搴︺€? },
  'spacersHeight': { title: '鍨湀楂樺害 (Spacers Height)', desc: '鎶婄珛涓嬫柟鎵€鏈夊灚鍦堢殑鎬婚珮搴︺€?, tip: '鍦ㄤ笉鏇存崲鎶婄珛鐨勬儏鍐典笅璋冭妭杞﹀ご楂樹綆鐨勫敮涓€鎵嬫銆? },
  'handlebarWidth': { title: '鎶婃í瀹藉害 (Handlebar Width)', desc: '宸﹀彸鎵嬪彉澶?鎻℃妸涓績鐐逛箣闂寸殑璺濈銆?, tip: '閫氬父涓庤偐瀹藉榻愶紝鏇寸獎鍒╀簬姘斿姩锛屾洿瀹藉埄浜庡懠鍚镐笌鎿嶆帶銆? },
  'crankLength': { title: '鏇叉焺闀垮害 (Crank Length)', desc: '浜旈€氫腑蹇冨埌鑴氳笍杞村績鐨勪腑蹇冭窛绂汇€?, tip: '褰卞搷韪╄笍鏉犳潌姣斿強娣辫共鍘嬪姏锛屽皬涓瓙鎴栭珮杞閫夋墜寤鸿缂╃煭銆? },
  'handlebarDrop': { title: '杞︽妸钀藉樊', desc: '搴у灚椤堕潰涓庤溅鎶婇《闈箣闂寸殑鍨傜洿楂樺害宸€?,  tip: '浣撶幇浜嗛獞琛岀殑婵€杩涚▼搴︼紝鏁板€艰秺澶ц函骞茶搴﹁秺浣庛€? },
  'saddleWidth': { title: '搴у灚瀹藉害', desc: '搴у灚鏈€瀹藉鐨勬í鍚戣窛绂汇€?, tip: '闇€鏍规嵁鎮ㄧ殑鍧愰瀹藉害鏉ラ€夋嫨銆? },
  'pedalType': { title: '韪忔澘绫诲瀷', desc: '浣跨敤鐨勮剼韪忎笌闉嬪叿缁勫悎銆?, tip: '閿侀瀷浼氬鍔犳湁鏁堢殑鈥滆吙闀库€濓紝瀵瑰骇楂樻湁鏄捐憲褰卞搷銆? }
};

const activeFieldInfo = computed(() => fieldGuides[activeField.value] || fieldGuides['brand']);

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getBikeDetail(route.query.id);
      const data = res.data || res;
      console.log('Bike detail loaded:', data);
      
      // 1. 鍩虹淇℃伅骞抽摵 (Top level fields)
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

      // 2. 鍑犱綍淇℃伅骞抽摵 (From bikeModelVO)
      if (data.bikeModelVO) {
        formData.bikeModelId = data.bikeModelVO.id;
        formData.size = data.bikeModelVO.size;
        // 鏍稿績鍑犱綍
        const geo = data.bikeModelVO;
        const geoFields = ['stack', 'reach', 'seatTubeAngle', 'headTubeAngle', 'bbDrop', 'wheelBase', 'standOverHeight'];
        geoFields.forEach(f => {
          if (geo[f] !== undefined) formData[f] = geo[f];
        });
      }

      // 3. 瀹炶溅璁惧畾骞抽摵 (From currentSetupVO)
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

      isGeometryLocked.value = !!formData.bikeModelId;
    } catch (e) {
      console.error('Failed to load bike detail', e);
      showToast('鑾峰彇鍗曡溅璇︽儏澶辫触', 'error');
    }
  }
});

const onSearchInput = () => {
  if (isEdit.value || isManualMode.value || formData.bikeModelId) return;
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
  } catch (e) {
    showToast('鏃犳硶浠庢暟鎹簱璋冨彇鍨嬪彿鍙樹綋', 'error');
  }
};

const onYearSelect = (year) => {
  formData.modelYear = year;
  formData.size = '';
  isGeometryLocked.value = false;
};

const onSizeSelect = (size) => {
  formData.size = size;
  const match = selectedModelTree.value.find(v => v.modelYear === formData.modelYear && v.size === size);
  if (match) {
    formData.bikeModelId = match.id;
    formData.bikeType = match.bikeType || 1;
    // 鑷姩濉厖鏍稿績鍑犱綍
    formData.stack = match.stack;
    formData.reach = match.reach;
    formData.seatTubeAngle = match.seatTubeAngle;
    formData.bbDrop = match.bbDrop;
    // 鑷姩濉厖鍏朵粬鍑犱綍 (濡傛灉搴撻噷鏈?
    if (match.wheelBase) formData.wheelBase = match.wheelBase;
    if (match.standOverHeight) formData.standOverHeight = match.standOverHeight;
    if (match.imageUrl) formData.imageUrl = match.imageUrl;

    isGeometryLocked.value = true;
    showToast('鏍″噯瀹屾垚锛氬畼鏂规暟鎹凡閿佸畾', 'success');
  }
};

const enableManualEntry = () => {
  isManualMode.value = true;
  formData.bikeModelId = null;
  isGeometryLocked.value = false;
  showDropdown.value = false;
  showToast('璇锋墜鍔ㄨ緭鍏ュ疄杞﹀嚑浣曟暟鎹?, 'info');
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
  selectedModelTree.value = [];
  showToast('鎼滅储宸查噸缃?, 'info');
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
    showToast('蹇収宸叉崟鎹?, 'success');
  } catch (error) {
    showToast('涓婁紶澶辫触', 'error');
  } finally {
    uploading.value = false;
  }
};

const handleRetire = async () => {
  const isConfirmed = await showConfirm(`纭畾瑕佸皢杩欒締鍗曡溅閫€褰瑰悧锛熷巻鍙茶褰曞皢琚皝瀛樸€俙, '鍗曡溅閫€褰?);
  if (isConfirmed) {
    try {
      await retireUserBike(formData.id);
      showToast('鍗曡溅宸茬Щ鑷宠崳瑾夋。妗?, 'success');
      router.push('/lab/garage');
    } catch (e) {
      showToast('閫€褰瑰け璐?, 'error');
    }
  }
};

const submitForm = async () => {
  if (!formData.stack || !formData.reach) {
    showToast('Stack 鍜?Reach 涓烘牳蹇冨熀鍑嗭紝涓嶅彲涓虹┖', 'warning');
    return;
  }
  submitting.value = true;
  try {
    await saveUserBike(formData);
    showToast('妗ｆ鍚屾鎴愬姛', 'success');
    router.push('/lab/garage');
  } catch (error) {
    showToast('淇濆瓨寮傚父', 'error');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
/* 1. 鍩虹瀵归綈 */
.bike-edit-page { padding-top: var(--header-height); min-height: 100vh; background: transparent; }
.lab-container { padding-top: 40px; padding-bottom: 80px; }

/* 2. 澶撮儴宸ュ叿鏍?(涓ユ牸鍚屾 GarageView) */
.toolbar-row {
  display: flex; justify-content: space-between; align-items: flex-end;
  margin-bottom: var(--space-xl); padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}
.page-title { font-family: var(--font-serif); font-size: 2.2rem; color: var(--text-main); line-height: 1.1; }
.page-subtitle { font-size: 1rem; color: var(--text-muted); font-family: var(--font-serif); font-style: italic; }
.header-actions { display: flex; align-items: center; gap: 20px; }

/* 3. 甯冨眬 */
.edit-content {
  display: grid;
  /* 淇敼鐐?1锛氳皟鏁存瘮渚嬶紝缁欏彸渚ф洿澶氱┖闂?*/
  /* 宸︿晶琛ㄥ崟鍗?55%锛屽彸渚у睍鍙板崰 45%锛岄棿璺濇媺澶у埌 50px */
  grid-template-columns: 1.2fr 1fr;
  gap: 50px;
  align-items: start;
}

/* 宸︿晶琛ㄥ崟 */
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
.label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
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
.type-selector.disabled { pointer-events: none; opacity: 0.7; }

/* 4. 鍥剧墖涓婁紶 */
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

/* --- 鎼滅储涓庤嚜鍔ㄥ畬鎴?--- */
.autocomplete-container { position: relative; }
.search-input-wrapper { position: relative; }
.search-status-icon { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); color: #ccc; }
.search-dropdown {
  position: absolute; top: calc(100% + 10px); left: 0; right: 0; z-index: 100;
  background: white; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  max-height: 300px; overflow-y: auto;
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

/* --- 閿佸畾鐘舵€佽瑙?--- */
.locked-section { opacity: 0.85; pointer-events: none; }
.locked-section .zen-input:disabled { background: #f8f8f8; cursor: not-allowed; border-color: #eee; color: #888; }
.group-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.lock-tag { font-size: 0.75rem; color: var(--primary-color); font-weight: 800; background: #ebf0e9; padding: 4px 10px; border-radius: 4px; border: 1px solid var(--primary-color); }

.identity-section { display: grid; gap: 24px; grid-column: 1 / -1; }
.identity-section.locked-section { opacity: 0.9; pointer-events: none; border: 1px dashed rgba(0,0,0,0.05); padding: 15px; margin: -15px; border-radius: 12px; }
.identity-section.locked-section .zen-input { background: #f8f8f8; color: #888; }

/* --- 鑽父閫夋嫨鍣?Pills --- */
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

/* 5. 搴曢儴鎿嶄綔 */
.form-actions-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 50px; padding-top: 30px; border-top: 1px solid rgba(0,0,0,0.05);
}
.btn-xl { padding: 16px 40px; font-size: 1rem; border-radius: var(--radius-pill); font-weight: 700; }
.btn-glow { box-shadow: 0 8px 25px rgba(92, 110, 88, 0.25); }
.btn-retire-text { background: none; border: none; color: var(--danger-color); font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; opacity: 0.6; transition: 0.3s; }
.btn-retire-text:hover { opacity: 1; text-decoration: underline; }

/* 6. 鍙充晶 Sticky */
.guide-section {
  display: flex;
  flex-direction: column;
  gap: 25px;
  /* Sticky 閫昏緫淇濇寔涓嶅彉 */
  position: sticky;
  top: calc(var(--header-height) + 30px);
  z-index: 10;
  height: fit-content;
}

.visualizer-container {
  /* 淇敼鐐?2锛氶珮搴﹀ぇ骞呭鍔狅紝璁╁崟杞﹀浘鈥滄拺婊♀€?*/
  height: 460px;
  background: white;
  border: var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  /* 澧炲姞涓€鐐瑰唴闃村奖锛屽鍔犳繁閭冩劅 */
  box-shadow: inset 0 0 20px rgba(0,0,0,0.02);
}
/* 鏂囧瓧璇存槑鐩?*/
.field-info-box {
  padding: 30px;
  border-left: 6px solid var(--accent-gold); /* 鍔犵矖杈规 */
  background: white;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  box-shadow: var(--shadow-sm);
}

.field-info-box h4 {
  color: var(--text-main);
  margin-bottom: 12px;
  font-size: 1.2rem; /* 瀛楀彿寰皟 */
  font-weight: 700;
}

.field-info-box p {
  font-size: 0.95rem; /* 瀛楀彿寰皟锛屾洿鏄撹 */
  color: var(--text-muted);
  line-height: 1.7;
}
/* Fittor 寤鸿鍧?*/
.fittor-tip {
  margin-top: 18px;
  padding: 15px;
  background: rgba(212, 175, 55, 0.08);
  border-radius: 6px;
  font-size: 0.9rem;
  color: #967d28; /* 鍔犳繁涓€鐐归噾鑹叉枃瀛楋紝鎻愬崌瀵规瘮搴?*/
  line-height: 1.6;
  font-weight: 500;
  display: flex;
  gap: 8px;
}
.fittor-tip::before {
  content: '馃挕'; /* 澧炲姞涓€涓皬鍥炬爣 */
  font-style: normal;
}
/* 鍔ㄦ晥 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateX(20px); }
.fade-slide-leave-to { opacity: 0; transform: translateX(-20px); }

@media (max-width: 1280px) {
  .edit-content {
    grid-template-columns: 1fr; /* 鍙樹负涓婁笅缁撴瀯 */
  }

  .guide-section {
    position: static; /* 鍙栨秷 Sticky */
    order: -1; /* 鍏抽敭锛氬湪绉诲姩绔?骞虫澘绔紝璁╁浘鐗囨樉绀哄湪琛ㄥ崟涓婃柟锛佹柟渚垮鐓?*/
  }

  .visualizer-container {
    height: 400px; /* 绉诲姩绔珮搴﹂€傚害鍑忓皬 */
  }
}
</style>