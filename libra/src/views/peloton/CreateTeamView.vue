<template>
  <div class="create-team-page">
    <div class="container">
      <!-- 顶部返回 + 标题 -->
      <header class="create-header">
        <button class="back-btn" @click="handleBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <div class="header-text">
          <h1 class="page-title serif-title">组建你的车队</h1>
          <p class="header-sub">召集志同道合的骑手，一起破风前行</p>
        </div>
      </header>

      <form class="create-form" @submit.prevent="handleSubmit">
        <!-- 视觉预览区：封面 + 头像 + 名称实时预览 -->
        <div class="preview-banner">
          <div class="cover-upload" @click="triggerCover">
            <input
              ref="coverInput"
              type="file"
              accept="image/*"
              class="file-input-hidden"
              @change="e => handleImageUpload(e, 'cover')"
            />
            <template v-if="coverPreview">
              <img :src="coverPreview" class="cover-img" alt="封面预览" />
              <div class="cover-overlay">
                <i class="fas fa-camera"></i>
                <span>更换封面</span>
              </div>
            </template>
            <template v-else>
              <div class="cover-empty">
                <div class="cover-empty-icon">
                  <i class="fas fa-mountain-sun"></i>
                </div>
                <span>点击上传封面图</span>
              </div>
            </template>
            <div v-if="uploading.cover" class="upload-progress">
              <div class="progress-bar"><div class="progress-fill"></div></div>
            </div>
          </div>

          <!-- 头像浮层 -->
          <div class="avatar-float" @click="triggerAvatar">
            <input
              ref="avatarInput"
              type="file"
              accept="image/*"
              class="file-input-hidden"
              @change="e => handleImageUpload(e, 'avatar')"
            />
            <div class="avatar-ring">
              <img v-if="avatarPreview" :src="avatarPreview" class="avatar-img" alt="头像预览" />
              <i v-else class="fas fa-camera avatar-icon"></i>
              <div v-if="uploading.avatar" class="avatar-loading">
                <i class="fas fa-spinner fa-spin"></i>
              </div>
            </div>
            <span class="avatar-edit-hint"><i class="fas fa-pen"></i></span>
          </div>
        </div>

        <!-- 表单主体 -->
        <div class="form-body">
          <!-- 基本信息卡片 -->
          <section class="form-card">
            <div class="card-label">
              <span class="label-dot"></span>
              基本信息
            </div>

            <div class="field-group">
              <label class="field-label">车队名称 <span class="required">*</span></label>
              <input
                v-model="form.name"
                type="text"
                class="field-input"
                placeholder="起个响亮的名字，让世界记住你们"
                maxlength="50"
              />
              <span class="field-counter">{{ form.name.length }}/50</span>
            </div>

            <div class="field-group">
              <label class="field-label">所在城市</label>
              <div class="input-with-icon">
                <i class="fas fa-map-marker-alt input-icon"></i>
                <input
                  v-model="form.location"
                  type="text"
                  class="field-input field-input-icon"
                  placeholder="上海、北京、成都..."
                  maxlength="50"
                />
              </div>
            </div>

            <div class="field-group">
              <label class="field-label">车队宣言</label>
              <textarea
                v-model="form.description"
                class="field-textarea"
                placeholder="你们的骑行理念、目标或招新宣言..."
                rows="4"
                maxlength="500"
              ></textarea>
              <span class="field-counter">{{ form.description.length }}/500</span>
            </div>
          </section>

          <!-- 标签卡片 -->
          <section class="form-card">
            <div class="card-label">
              <span class="label-dot"></span>
              车队标签
              <span class="label-hint">选择最能代表车队的标签</span>
            </div>

            <div class="tag-grid">
              <button
                v-for="tag in presetTags"
                :key="tag.label"
                type="button"
                class="tag-chip"
                :class="{ active: form.tags.includes(tag.label) }"
                @click="toggleTag(tag)"
              >
                <i :class="tag.icon" class="chip-icon"></i>
                <span class="chip-text">{{ tag.label }}</span>
              </button>
            </div>
            <p v-if="form.tags.length >= 5" class="tag-limit-hint">
              <i class="fas fa-info-circle"></i> 最多选择 5 个标签
            </p>
          </section>

          <!-- 提交 -->
          <div class="form-actions">
            <button type="button" class="btn-ghost" @click="handleBack">取消</button>
            <button type="submit" class="btn-primary" :disabled="!isFormValid || submitting">
              <template v-if="submitting">
                <i class="fas fa-spinner fa-spin"></i> 创建中...
              </template>
              <template v-else>
                <i class="fas fa-paper-plane"></i> 创建车队
              </template>
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { createTeam, uploadTeamImage } from '@/api/team';
import { showToast } from '@/utils/toast';

const router = useRouter();
const submitting = ref(false);

const coverInput = ref(null);
const avatarInput = ref(null);
const coverPreview = ref('');
const avatarPreview = ref('');
const uploading = reactive({ cover: false, avatar: false });

const presetTags = [
  { label: '公路', icon: 'fas fa-road' },
  { label: '山地', icon: 'fas fa-mountain' },
  { label: '砾石', icon: 'fas fa-grip-lines' },
  { label: '爬坡', icon: 'fas fa-arrow-up-right-dots' },
  { label: '冲刺', icon: 'fas fa-bolt' },
  { label: '长途', icon: 'fas fa-route' },
  { label: '通勤', icon: 'fas fa-city' },
  { label: '休闲骑', icon: 'fas fa-mug-hot' },
  { label: '竞赛', icon: 'fas fa-trophy' },
  { label: '训练', icon: 'fas fa-dumbbell' },
  { label: '新手友好', icon: 'fas fa-seedling' },
  { label: '夜骑', icon: 'fas fa-moon' },
  { label: '周末', icon: 'fas fa-calendar-day' }
];

const form = reactive({
  name: '',
  location: '',
  description: '',
  tags: [],
  avatar: null,
  coverImage: null
});

const isFormValid = computed(() => form.name.trim().length >= 2);

const toggleTag = (tag) => {
  const idx = form.tags.indexOf(tag.label);
  if (idx >= 0) {
    form.tags.splice(idx, 1);
  } else if (form.tags.length < 5) {
    form.tags.push(tag.label);
  } else {
    showToast('最多选择5个标签', 'warning');
  }
};

const triggerCover = () => coverInput.value?.click();
const triggerAvatar = () => avatarInput.value?.click();

const handleImageUpload = async (e, type) => {
  const file = e.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = () => {
    if (type === 'cover') coverPreview.value = reader.result;
    else avatarPreview.value = reader.result;
  };
  reader.readAsDataURL(file);

  uploading[type] = true;
  try {
    const fd = new FormData();
    fd.append('file', file);
    const url = await uploadTeamImage(fd);
    if (type === 'cover') form.coverImage = url;
    else form.avatar = url;
  } catch {
    showToast(`${type === 'cover' ? '封面' : '头像'}上传失败，请重试`, 'error');
  } finally {
    uploading[type] = false;
  }
};

const handleBack = () => router.push('/peloton');

const handleSubmit = async () => {
  if (!isFormValid.value) {
    showToast('请至少填写车队名称（2字以上）', 'warning');
    return;
  }
  submitting.value = true;
  try {
    const result = await createTeam({
      name: form.name.trim(),
      location: form.location.trim() || null,
      description: form.description.trim() || null,
      tags: form.tags.length > 0 ? form.tags : null,
      avatar: form.avatar,
      coverImage: form.coverImage
    });
    showToast('车队创建成功！', 'success');
    router.push(`/peloton/${result.id}`);
  } catch (err) {
    showToast(err?.message || '创建失败，请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.create-team-page {
  padding: 120px 0 100px;
  min-height: 100vh;
  background: linear-gradient(180deg, #f6f7f5 0%, #fff 40%);
}

/* ── Header ── */
.create-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  max-width: 680px;
  margin-left: auto;
  margin-right: auto;
}
.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,0.08);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  color: var(--text-body);
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.back-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(92,110,88,0.12);
}
.header-text { flex: 1 }
.page-title {
  font-family: var(--font-serif);
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
  line-height: 1.3;
}
.header-sub {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 4px 0 0;
}

/* ── Form Layout ── */
.create-form {
  max-width: 680px;
  margin: 0 auto;
}

/* ── Preview Banner ── */
.preview-banner {
  position: relative;
  margin-bottom: 32px;
}
.cover-upload {
  position: relative;
  width: 100%;
  height: 220px;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  background: linear-gradient(135deg, #e8ebe6 0%, #d4d9d1 100%);
  transition: box-shadow 0.3s ease;
}
.cover-upload:hover {
  box-shadow: 0 8px 30px rgba(92,110,88,0.15);
}
.file-input-hidden { display: none }
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
}
.cover-overlay i { font-size: 1.1rem }
.cover-upload:hover .cover-overlay { opacity: 1 }

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
}
.cover-empty-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(255,255,255,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}
.cover-empty-icon i { font-size: 1.4rem; color: var(--primary-color); opacity: 0.6 }
.cover-empty span { font-size: 0.85rem; font-weight: 500 }

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 16px;
  background: linear-gradient(transparent, rgba(0,0,0,0.3));
}
.progress-bar {
  height: 3px;
  background: rgba(255,255,255,0.3);
  border-radius: 2px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  width: 40%;
  background: #fff;
  border-radius: 2px;
  animation: progress-indeterminate 1.2s ease-in-out infinite;
}
@keyframes progress-indeterminate {
  0% { transform: translateX(-100%); width: 40% }
  100% { transform: translateX(350%); width: 40% }
}

/* ── Avatar Float ── */
.avatar-float {
  position: absolute;
  bottom: -32px;
  left: 32px;
  cursor: pointer;
  z-index: 2;
}
.avatar-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid #fff;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8ebe6, #d4d9d1);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  position: relative;
}
.avatar-float:hover .avatar-ring {
  box-shadow: 0 4px 20px rgba(92,110,88,0.25);
  transform: scale(1.05);
}
.avatar-img { width: 100%; height: 100%; object-fit: cover }
.avatar-icon { font-size: 1.2rem; color: var(--text-muted); opacity: 0.35 }
.avatar-loading {
  position: absolute;
  inset: 0;
  background: rgba(255,255,255,0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  border-radius: 50%;
}
.avatar-edit-hint {
  position: absolute;
  bottom: -2px;
  right: -4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(92,110,88,0.3);
}
.avatar-edit-hint i { font-size: 0.55rem; color: #fff }

/* ── Form Body ── */
.form-body {
  padding-top: 20px;
}

/* ── Form Card ── */
.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 28px 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(0,0,0,0.04);
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
}
.card-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 24px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.label-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  flex-shrink: 0;
}
.label-hint {
  font-weight: 400;
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: none;
  letter-spacing: 0;
  margin-left: auto;
}

/* ── Fields ── */
.field-group { margin-bottom: 22px }
.field-group:last-child { margin-bottom: 0 }
.field-label {
  display: block;
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--text-body);
  margin-bottom: 8px;
}
.required { color: #d9534f; font-weight: 400 }

.input-with-icon { position: relative }
.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.82rem;
  color: var(--primary-color);
  opacity: 0.5;
  pointer-events: none;
}
.field-input, .field-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid rgba(0,0,0,0.06);
  border-radius: 12px;
  font-size: 0.9rem;
  color: var(--text-main);
  background: #fafbf9;
  outline: none;
  transition: all 0.25s ease;
  font-family: inherit;
  box-sizing: border-box;
}
.field-input-icon { padding-left: 40px }
.field-input:focus, .field-textarea:focus {
  border-color: var(--primary-color);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(92,110,88,0.06);
}
.field-input::placeholder, .field-textarea::placeholder {
  color: var(--text-muted);
  opacity: 0.4;
}
.field-textarea {
  resize: vertical;
  min-height: 100px;
  line-height: 1.7;
}
.field-counter {
  display: block;
  text-align: right;
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-top: 5px;
  opacity: 0.6;
}

/* ── Tag Grid ── */
.tag-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.tag-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid rgba(0,0,0,0.06);
  background: linear-gradient(135deg, #fafbf9 0%, #f3f5f1 100%);
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
  box-shadow: 0 1px 2px rgba(0,0,0,0.03), inset 0 1px 0 rgba(255,255,255,0.7);
  position: relative;
}
.tag-chip:hover {
  border-color: rgba(92,110,88,0.25);
  color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(92,110,88,0.1), inset 0 1px 0 rgba(255,255,255,0.7);
}
.tag-chip.active {
  border-color: transparent;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6d816a 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(92,110,88,0.3), inset 0 1px 0 rgba(255,255,255,0.15);
  transform: translateY(-1px);
}
.chip-icon {
  font-size: 0.75rem;
  opacity: 0.55;
  transition: opacity 0.25s ease;
}
.tag-chip:hover .chip-icon { opacity: 0.8 }
.tag-chip.active .chip-icon { opacity: 0.9 }
.tag-limit-hint {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin: 12px 0 0;
}
.tag-limit-hint i { margin-right: 4px; color: var(--primary-color); opacity: 0.5 }

/* ── Actions ── */
.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 32px;
  padding-bottom: 20px;
}
.btn-ghost {
  padding: 13px 32px;
  border-radius: 12px;
  border: 1.5px solid rgba(0,0,0,0.08);
  background: transparent;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.25s ease;
}
.btn-ghost:hover {
  border-color: rgba(0,0,0,0.15);
  color: var(--text-body);
}
.btn-primary {
  padding: 13px 36px;
  border-radius: 12px;
  border: none;
  background: var(--primary-color);
  font-size: 0.88rem;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
  box-shadow: 0 4px 16px rgba(92,110,88,0.25);
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(92,110,88,0.3);
}
.btn-primary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.btn-primary i { font-size: 0.78rem }

/* ── Responsive ── */
@media (max-width: 768px) {
  .create-team-page { padding: 100px 0 60px }
  .page-title { font-size: 1.3rem }
  .cover-upload { height: 170px; border-radius: 12px }
  .avatar-float { bottom: -28px; left: 20px }
  .avatar-ring { width: 60px; height: 60px }
  .form-card { padding: 22px 20px 16px; border-radius: 12px }
  .tag-chip { padding: 8px 14px; font-size: 0.78rem }
  .form-actions { flex-direction: column }
  .form-actions button { width: 100%; justify-content: center }
}
</style>
