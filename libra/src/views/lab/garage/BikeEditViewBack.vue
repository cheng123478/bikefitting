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

          <!-- 妯″潡 01锛氬熀纭€韬唤 -->
          <div class="form-group">
            <h4 class="group-title">01 鍩虹韬唤</h4>
            <div class="input-grid">

              <!-- 鍥剧墖涓婁紶锛氬甫 /api 鍓嶇紑鍥炴樉 -->
              <div class="zen-form-item full-width">
                <label class="input-label">鍗曡溅瑙嗚蹇収</label>
                <div class="image-uploader" @click="triggerUpload" :class="{ 'has-img': formData.imageUrl }">
                  <img v-if="formData.imageUrl" :src="formData.imageUrl.startsWith('http') ? formData.imageUrl : '/api' + formData.imageUrl" class="preview-img" />
                  <div v-else class="upload-placeholder">
                    <i class="fas fa-camera-retro"></i>
                    <span>涓婁紶瀹炶溅缇庣収</span>
                    <p>寤鸿浣跨敤鐧藉簳鎴栦晶闈㈡棤鑳屾櫙鐓х墖</p>
                  </div>
                  <input type="file" ref="fileInput" hidden @change="handleFileChange" accept="image/*" />
                  <div class="upload-mask" v-if="uploading">
                    <i class="fas fa-circle-notch fa-spin"></i>
                  </div>
                </div>
              </div>

              <div class="zen-form-item" @click="activeField = 'type'">
                <label class="input-label">鍗曡溅绫诲瀷</label>
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
                  <label class="input-label">鍝佺墝鍨嬪彿</label>
                  <input v-model="formData.brandModel" class="zen-input" placeholder="渚嬪: Giant TCR Adv" @focus="activeField = 'brand'">
                </div>
                <div class="zen-form-item">
                  <label class="input-label">杞︽灦灏虹爜</label>
                  <input v-model="formData.frameSize" class="zen-input" placeholder="52 / M / 540" @focus="activeField = 'size'">
                </div>
              </div>

              <div class="zen-form-item">
                <label class="input-label">鍗曡溅澶囨敞</label>
                <input v-model="formData.remark" class="zen-input" placeholder="渚嬪锛氭垜鐨勪富鍔涚埇鍧″崟杞? @focus="activeField = 'remark'">
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 妯″潡 02锛氳溅鏋跺嚑浣?(DNA) -->
          <div class="form-group">
            <h4 class="group-title">02 杞︽灦鍑犱綍 (Frame Geometry)</h4>
            <p class="group-desc">鏁版嵁鏉ユ簮锛氬巶鍟嗗畼缃戝嚑浣曡〃銆傝繖鏄?Fitting 鏂规鐨勫熀纭€銆?/p>
            <div class="input-grid">
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">Stack (鍫嗛珮 - M) *</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.frameStack" class="zen-input" @focus="activeField = 'frameStack'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">Reach (鍓嶄几閲?- L) *</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.frameReach" class="zen-input" @focus="activeField = 'frameReach'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-3">
                <div class="zen-form-item">
                  <label class="input-label">绔嬬瑙掑害 (STA - B)</label>
                  <div class="input-with-unit">
                    <input type="number" step="0.1" v-model.number="formData.seatTubeAngle" class="zen-input" @focus="activeField = 'seatTubeAngle'">
                    <span class="unit">掳</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">澶寸瑙掑害 (HTA - E)</label>
                  <div class="input-with-unit">
                    <input type="number" step="0.1" v-model.number="formData.headTubeAngle" class="zen-input" @focus="activeField = 'headTubeAngle'">
                    <span class="unit">掳</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">浜旈€氫笅娌?(J)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.bbDrop" class="zen-input" @focus="activeField = 'bbDrop'">
                    <span class="unit">mm</span>
                  </div>
                </div>
              </div>
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">杞磋窛 (H)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.wheelBase" class="zen-input" @focus="activeField = 'wheelBase'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">璺ㄩ珮 (SH / Q)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.standOverHeight" class="zen-input" @focus="activeField = 'standoverHeight'">
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
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">褰撳墠搴ч珮 (A)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleHeight" class="zen-input" @focus="activeField = 'saddleHeight'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">搴у灚鍚庣Щ閲?(Setback - C)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.saddleSetback" class="zen-input" @focus="activeField = 'saddleSetback'">
                    <span class="unit">mm</span>
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
              <div class="grid-2">
                <div class="zen-form-item">
                  <label class="input-label">鎶婃í瀹藉害 (N)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.handlebarWidth" class="zen-input" @focus="activeField = 'handlebarWidth'">
                    <span class="unit">mm</span>
                  </div>
                </div>
                <div class="zen-form-item">
                  <label class="input-label">鏇叉焺闀垮害 (P)</label>
                  <div class="input-with-unit">
                    <input type="number" v-model.number="formData.crankLength" class="zen-input" @focus="activeField = 'crankLength'">
                    <span class="unit">mm</span>
                  </div>
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
import { showToast, showConfirm } from '@/utils/toast.js';
import BikeVisualizer from '@/views/lab/components/BikeVisualizer.vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isEdit = computed(() => !!route.query.id);
const submitting = ref(false);
const activeField = ref('brand');

const bikeTypes = [
  { name: '鍏矾杞?, code: 1 }, { name: '灞卞湴杞?, code: 2 },
  { name: '鍏ㄥ湴褰?, code: 3 }, { name: 'TT/璁℃椂', code: 4 }, { name: '鍩庡競杞?, code: 5 }
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
  'brand': { title: '鍝佺墝涓庡瀷鍙?, desc: '璁板綍鎮ㄧ殑鍗曡溅韬唤锛屾柟渚垮揩閫熻瘑鍒€? },
  'remark': { title: '鍗曡溅澶囨敞', desc: '涓哄崟杞﹁捣涓樀绉帮紙濡傦細闆ㄥぉ澶囩敤杞︼級銆? },
  'frameStack': { title: 'Stack (鍫嗛珮 - M)', desc: '浜旈€氫腑蹇冨埌澶寸椤堕儴涓績鐨勫瀭鐩磋窛绂汇€?, tip: '鍐冲畾楠戣濮挎€佲€滈珮浣庘€濈殑鏈€鏍稿績鎸囨爣銆? },
  'frameReach': { title: 'Reach (鍓嶄几閲?- L)', desc: '浜旈€氫腑蹇冨埌澶寸椤堕儴涓績鐨勬按骞宠窛绂汇€?, tip: '琛￠噺杞︽灦闀跨煭鏈€瀹㈣鐨勬寚鏍囥€? },
  'seatTubeAngle': { title: '绔嬬瑙掑害 (STA - B)', desc: '绔嬬涓庢按骞崇嚎鐨勫す瑙掋€傝搴﹁秺闄?>74掳)锛岄噸蹇冭秺闈犲墠銆? },
  'headTubeAngle': { title: '澶寸瑙掑害 (HTA - E)', desc: '澶寸涓庢按骞崇嚎鐨勫す瑙掞紝鐩存帴褰卞搷鎿嶆帶鐏垫晱搴︺€? },
  'saddleHeight': { title: '褰撳墠搴ч珮 (A)', desc: '浜旈€氫腑蹇冩部绔嬬涓績绾胯嚦搴у灚椤堕潰鐨勭洿绾胯窛绂汇€? },
  'saddleSetback': { title: '搴у灚鍚庣Щ閲?(C)', desc: '搴у灚榧诲皷鐩稿浜庝簲閫氫腑蹇冨瀭绾跨殑姘村钩浣嶇Щ銆?, tip: '寤鸿缁撳悎鎮ㄧ殑鑲¤儷姣旇繘琛屽井璋冦€? },
  'bbDrop': { title: '浜旈€氫笅娌?(J)', desc: '鍓嶅悗杞磋繛绾夸笌浜旈€氫腑蹇冪殑鍨傜洿钀藉樊銆?, tip: '鍐冲畾浜嗚溅杈嗙殑閲嶅績绋冲畾鎬у拰搴у灚绂诲湴闂磋窛銆? },
  'wheelBase': { title: '杞磋窛 (H)', desc: '鍓嶅悗杞酱蹇冧箣闂寸殑姘村钩璺濈銆? },
  'standOverHeight': { title: '璺ㄩ珮 (Q/SH)', desc: '涓婄涓績鐐瑰埌鍦伴潰鐨勯珮搴︼紝娑夊強涓婁笅杞︾殑瀹夊叏鎬с€? },
  'headTubeLength': { title: '澶寸闀垮害 (D)', desc: '澶寸鑷韩鐨勭墿鐞嗛暱搴︺€? },
  'stemLength': { title: '鎶婄珛闀垮害', desc: '鎶婄珛涓績鐩栧埌澶寸鐩栦腑蹇冪殑鐩寸嚎璺濈銆? },
  'spacersHeight': { title: '鍨湀楂樺害', desc: '鎶婄珛涓嬫柟鎵€鏈夊灚鍦堢殑绱楂樺害銆? },
  'crankLength': { title: '鏇叉焺闀垮害 (P)', desc: '浜旈€氫腑蹇冨埌鑴氳笍杞村績鐨勮窛绂汇€? }
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
    showToast('蹇収宸叉崟鎹?, 'success');
  } catch (error) {
    showToast('涓婁紶澶辫触', 'error');
  } finally {
    uploading.value = false;
  }
};

const handleRetire = async () => {
  const isConfirmed = await showConfirm(`纭畾瑕佸皢杩欒締鎴樿溅閫€褰瑰悧锛熷巻鍙茶褰曞皢琚皝瀛樸€俙, '鎴樿溅閫€褰?);
  if (isConfirmed) {
    try {
      await retireUserBikeInfo(formData.id);
      showToast('鎴樿溅宸茬Щ鑷宠崳瑾夋。妗?, 'success');
      router.push('/lab/garage');
    } catch (e) {
      showToast('閫€褰瑰け璐?, 'error');
    }
  }
};

const submitForm = async () => {
  if (!formData.frameStack || !formData.frameReach) {
    showToast('Stack 鍜?Reach 涓烘牳蹇冨熀鍑嗭紝涓嶅彲涓虹┖', 'warning');
    return;
  }
  submitting.value = true;
  try {
    await saveUserBikeInfo(formData);
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