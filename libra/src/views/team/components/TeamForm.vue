<template>
  <form @submit.prevent="submit" class="team-form">

    <!-- 1. 队徽与封面设置 -->
    <div class="form-section brand-section">
      <div class="logo-upload" @click="$refs.logoInput.click()">
        <img v-if="form.logo" :src="form.logo" class="logo-preview">
        <div v-else class="upload-placeholder">
          <i class="fas fa-shield-alt icon-md"></i>
          <span>上传队徽</span>
        </div>
        <input type="file" ref="logoInput" @change="handleLogoChange" accept="image/*" hidden>
      </div>
      <div class="section-hint">
        <h3 class="serif title-style">车队形象</h3>
        <p>建议上传透明背景的 PNG 图片，展示车队精神。</p>
      </div>
    </div>

    <!-- 2. 基础信息 -->
    <div class="glass-card form-card">
      <div class="input-wrapper">
        <label>骑士团名称</label>
        <input type="text" v-model="form.name" class="zen-input title-input-style" placeholder="例如：破风猎人..." required>
      </div>

      <div class="input-wrapper">
        <label>Slogan / 宣言</label>
        <input type="text" v-model="form.slogan" class="zen-input" placeholder="一句话描述你们的骑行哲学">
      </div>

      <div class="grid-2">
        <div class="input-wrapper">
          <label>所在城市</label>
          <div class="input-group icon-input">
            <i class="fas fa-map-marker-alt input-icon"></i>
            <input type="text" v-model="form.city" class="zen-input" placeholder="例如：杭州">
          </div>
        </div>
        <div class="input-wrapper">
          <label>车队等级</label>
          <div class="select-wrapper">
            <select v-model="form.level" class="zen-select">
              <option value="Casual">☕️ 休闲骑游 (Casual)</option>
              <option value="Amateur">🔥 进阶训练 (Amateur)</option>
              <option value="Pro">⚡️ 竞技精英 (Pro)</option>
            </select>
          </div>
        </div>
      </div>

      <div class="input-wrapper">
        <label>简介</label>
        <textarea v-model="form.desc" rows="4" class="zen-textarea" placeholder="介绍车队的历史、常规活动时间等..."></textarea>
      </div>
    </div>

    <!-- 3. 操作栏 -->
    <div class="form-actions">
      <button type="button" class="btn btn-outline" @click="$emit('cancel')">取消</button>
      <button type="submit" class="btn btn-primary" :disabled="isLoading">
        {{ isEdit ? '保存设置' : '创建骑士团' }}
      </button>
    </div>

  </form>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';

const props = defineProps({ initialData: Object, isLoading: Boolean });
const emit = defineEmits(['submit', 'cancel']);
const isEdit = computed(() => !!props.initialData);
const logoInput = ref(null);

const form = reactive({ name: '', slogan: '', city: '', level: 'Casual', desc: '', logo: '' });

onMounted(() => { if (props.initialData) Object.assign(form, JSON.parse(JSON.stringify(props.initialData))); });

const handleLogoChange = (e) => {
  const file = e.target.files[0];
  if (file) { const reader = new FileReader(); reader.onload = (e) => { form.logo = e.target.result; }; reader.readAsDataURL(file); }
};

const submit = () => emit('submit', { ...form });
</script>

<style scoped>
.team-form { max-width: 800px; margin: 0 auto; }

/* 队徽上传区 */
.brand-section { display: flex; align-items: center; gap: 30px; margin-bottom: 30px; }
.logo-upload {
  width: 100px; height: 100px; border-radius: 50%;
  border: 2px dashed rgba(0,0,0,0.1); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; background: var(--bg-surface); transition: 0.3s;
}
.logo-upload:hover { border-color: var(--primary-color); background: #fff; }
.logo-preview { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; color: var(--text-muted); font-size: 0.8rem; gap: 5px; }
.icon-md { font-size: 1.5rem; }
.title-style { margin-bottom: 5px; font-size: 1.4rem; color: var(--text-main); font-family: var(--font-serif); }
.section-hint p { color: var(--text-muted); font-size: 0.9rem; }

.form-card { padding: 30px; margin-bottom: 24px; }

.title-input-style { font-size: 1.1rem; font-weight: 600; font-family: var(--font-serif); }

.form-actions { display: flex; justify-content: flex-end; gap: 15px; }

@media (max-width: 768px) {
  .brand-section { flex-direction: column; text-align: center; }
  .grid-2 { grid-template-columns: 1fr; }
}
</style>