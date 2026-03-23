<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="isOpen" class="modal-overlay is-visible" @click.self="close">
        <div class="bike-modal-content">

          <div class="modal-header">
            <h3 class="serif title">{{ isEdit ? '编辑车辆档案' : '添加爱车' }}</h3>
            <button class="close-btn" @click="close"><i class="fas fa-times"></i></button>
          </div>

          <form @submit.prevent="submit" class="bike-form">
            <!-- 1. 图片上传 -->
            <div class="upload-area" @click="$refs.fileInput.click()" :style="bgStyle">
              <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*" hidden>
              <div v-if="!form.image" class="placeholder">
                <i class="fas fa-camera"></i>
                <span>上传照片</span>
              </div>
            </div>

            <!-- 2. 基础信息 -->
            <div class="section-label">基础信息</div>
            <div class="grid-2">
              <div class="input-wrapper">
                <label>品牌</label>
                <input type="text" v-model="form.brand" class="zen-input" placeholder="如：Specialized" required>
              </div>
              <div class="input-wrapper">
                <label>型号</label>
                <input type="text" v-model="form.model" class="zen-input" placeholder="如：Tarmac SL7" required>
              </div>
            </div>

            <div class="grid-2">
              <div class="input-wrapper">
                <label>类型</label>
                <div class="select-wrapper">
                  <select v-model="form.type" class="zen-select">
                    <option value="公路车">公路车 (Road)</option>
                    <option value="山地车">山地车 (MTB)</option>
                    <option value="折叠车">折叠车 (Folding)</option>
                    <option value="铁三车">铁三车 (TT)</option>
                    <option value="Gravel">Gravel</option>
                  </select>
                  <i class="fas fa-chevron-down arrow"></i>
                </div>
              </div>
              <div class="input-wrapper">
                <label>尺码</label>
                <input type="text" v-model="form.size" class="zen-input" placeholder="如：54 / M" required>
              </div>
            </div>

            <!-- 3. 进阶参数开关 -->
            <div class="advanced-toggle" @click="showAdvanced = !showAdvanced">
              <span><i class="fas fa-cogs"></i> Fitting 几何参数 (选填)</span>
              <i class="fas fa-chevron-right arrow-icon" :class="{ rotated: showAdvanced }"></i>
            </div>

            <!-- 4. 进阶参数 -->
            <Transition name="slide-down">
              <div v-if="showAdvanced" class="advanced-section">
                <div class="hint-text">准确填写以下配件规格，有助于 AI 提供更精准的调整建议。</div>

                <div class="grid-2">
                  <div class="input-wrapper">
                    <label>曲柄长度</label>
                    <div class="input-group suffix-group">
                      <input type="number" v-model="form.specs.crankLength" class="zen-input" placeholder="170">
                      <span class="suffix">mm</span>
                    </div>
                  </div>
                  <div class="input-wrapper">
                    <label>把立长度</label>
                    <div class="input-group suffix-group">
                      <input type="number" v-model="form.specs.stemLength" class="zen-input" placeholder="100">
                      <span class="suffix">mm</span>
                    </div>
                  </div>
                </div>

                <div class="grid-2">
                  <div class="input-wrapper">
                    <label>车把宽度</label>
                    <div class="input-group suffix-group">
                      <input type="number" v-model="form.specs.barWidth" class="zen-input" placeholder="400">
                      <span class="suffix">mm</span>
                    </div>
                  </div>
                  <div class="input-wrapper">
                    <label>当前座高</label>
                    <div class="input-group suffix-group">
                      <input type="number" v-model="form.specs.saddleHeight" class="zen-input" placeholder="五通到座垫面">
                      <span class="suffix">mm</span>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>

            <!-- 5. 设为默认 -->
            <div class="switch-row" @click="form.isDefault = !form.isDefault">
              <div class="switch-label">
                <span class="main-text">设为默认车辆</span>
              </div>
              <div class="zen-switch" :class="{ checked: form.isDefault }">
                <div class="slider"></div>
              </div>
            </div>

            <div class="form-footer">
              <button type="button" class="btn btn-outline" @click="close">取消</button>
              <button type="submit" class="btn btn-primary">保存车辆</button>
            </div>
          </form>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue';

const props = defineProps({ isOpen: Boolean, initialData: Object });
const emit = defineEmits(['close', 'submit']);

const isEdit = computed(() => !!props.initialData);
const showAdvanced = ref(false);
const fileInput = ref(null);

const form = reactive({
  id: null, brand: '', model: '', type: '公路车', size: '', image: '', isDefault: false,
  specs: { crankLength: '', stemLength: '', barWidth: '', saddleHeight: '' }
});

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    if (props.initialData) {
      const data = JSON.parse(JSON.stringify(props.initialData));
      if (!data.specs) data.specs = { crankLength: '', stemLength: '', barWidth: '', saddleHeight: '' };
      Object.assign(form, data);
      if (form.specs.crankLength || form.specs.stemLength) showAdvanced.value = true;
    } else {
      Object.assign(form, { id: null, brand: '', model: '', type: '公路车', size: '', image: '', isDefault: false, specs: { crankLength: '', stemLength: '', barWidth: '', saddleHeight: '' } });
      showAdvanced.value = false;
    }
  }
});

const bgStyle = computed(() => form.image ? { backgroundImage: `url(${form.image})` } : {});
const handleFileChange = (e) => {
  const file = e.target.files[0];
  if(file) { const reader = new FileReader(); reader.onload = (e) => { form.image = e.target.result; }; reader.readAsDataURL(file); }
};

const close = () => emit('close');
const submit = () => emit('submit', JSON.parse(JSON.stringify(form)));
</script>

<style scoped>
/* 模态框基础 */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(43, 42, 41, 0.6); backdrop-filter: blur(5px);
  z-index: var(--z-modal); display: flex; justify-content: center; align-items: center;
}

.bike-modal-content {
  background: #fff; width: 500px; max-width: 90%;
  border-radius: var(--radius-md); padding: 30px; position: relative;
  box-shadow: var(--shadow-lg); max-height: 85vh; overflow-y: auto;
}

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.title { font-size: 1.5rem; color: var(--text-main); font-family: var(--font-serif); }
.close-btn { background: transparent; border: none; font-size: 1.2rem; color: var(--text-muted); cursor: pointer; }
.close-btn:hover { color: var(--text-main); }

/* 上传区 */
.upload-area {
  height: 150px; background-color: var(--bg-page); border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  border: 2px dashed rgba(0,0,0,0.1); cursor: pointer; margin-bottom: 25px;
  background-size: cover; background-position: center; transition: 0.3s;
}
.upload-area:hover { border-color: var(--primary-color); background-color: rgba(92,110,88,0.05); }
.placeholder { display: flex; flex-direction: column; align-items: center; color: var(--text-muted); gap: 8px; }
.placeholder i { font-size: 1.5rem; }

/* 标题标签 */
.section-label {
  font-size: 0.8rem; font-weight: 700; color: var(--text-muted); letter-spacing: 1px;
  margin-bottom: 15px; text-transform: uppercase;
}

/* 输入框 */
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.input-wrapper { margin-bottom: 15px; }
.input-wrapper label { display: block; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 6px; }

.zen-input, .zen-select {
  width: 100%; padding: 10px 12px; border: 1px solid #E0E0E0; background: #FAFAFA;
  border-radius: var(--radius-sm); outline: none; font-family: var(--font-sans); color: var(--text-main); font-size: 0.95rem;
}
.zen-input:focus, .zen-select:focus { border-color: var(--primary-color); background: #fff; }

.select-wrapper { position: relative; }
.zen-select { appearance: none; cursor: pointer; }
.select-wrapper .arrow { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; font-size: 0.8rem; }

.suffix-group { position: relative; }
.suffix-group .zen-input { padding-right: 35px; }
.suffix { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); font-size: 0.8rem; color: var(--text-muted); }

/* 进阶选项 */
.advanced-toggle {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 15px; background: rgba(92, 110, 88, 0.05);
  border-radius: var(--radius-sm); cursor: pointer; margin-bottom: 15px;
  color: var(--primary-color); font-weight: 600; font-size: 0.9rem; transition: 0.2s;
}
.advanced-toggle:hover { background: rgba(92, 110, 88, 0.1); }
.arrow-icon { transition: transform 0.3s; }
.arrow-icon.rotated { transform: rotate(90deg); }

.advanced-section { margin-bottom: 20px; }
.hint-text { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 15px; }

/* 开关与按钮 */
.switch-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; cursor: pointer; margin-top: 10px; }
.switch-label .main-text { font-weight: 600; color: var(--text-main); font-size: 0.95rem; }
.zen-switch { width: 44px; height: 24px; background: #e0e0e0; border-radius: 20px; position: relative; transition: 0.3s; }
.zen-switch .slider { width: 20px; height: 20px; background: white; border-radius: 50%; position: absolute; top: 2px; left: 2px; transition: 0.3s; box-shadow: 0 2px 5px rgba(0,0,0,0.2); }
.zen-switch.checked { background: var(--primary-color); }
.zen-switch.checked .slider { left: 22px; }

.form-footer { display: flex; justify-content: flex-end; gap: 15px; margin-top: 25px; }

/* 动画 */
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.3s ease; overflow: hidden; max-height: 300px; opacity: 1; }
.slide-down-enter-from, .slide-down-leave-to { max-height: 0; opacity: 0; }
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.3s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>