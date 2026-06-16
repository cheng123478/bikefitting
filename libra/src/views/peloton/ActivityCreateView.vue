<template>
  <div class="activity-edit-page">
    <div class="container">
      <header class="edit-header">
        <button class="back-btn" @click="handleBack">
          <i class="fas fa-arrow-left"></i> 返回
        </button>
      </header>

      <div class="edit-card">
        <!-- 装饰元素 -->
        <div class="card-decor"></div>
        
        <!-- 标题区 -->
        <div class="card-title-area">
          <div class="title-icon">
            <i class="fas fa-calendar-plus"></i>
          </div>
          <h1 class="page-title">发布活动</h1>
          <p class="page-subtitle">组织一场骑行活动，召集车队伙伴一起出发</p>
        </div>

        <form class="edit-form" @submit.prevent="handleSubmit">
          <!-- 活动标题 -->
          <div class="form-group">
            <label class="form-label">活动标题</label>
            <div class="input-wrap">
              <i class="input-icon fas fa-heading"></i>
              <input v-model="form.title" type="text" class="form-input" placeholder="简明扼要地描述活动主题" maxlength="50" />
              <span class="char-count">{{ form.title.length }}/50</span>
            </div>
          </div>

          <!-- 活动类型 -->
          <div class="form-group">
            <label class="form-label">活动类型</label>
            <div class="type-selector">
              <button
                v-for="t in activityTypes"
                :key="t.value"
                type="button"
                class="type-btn"
                :class="{ active: form.activityType === t.value }"
                @click="form.activityType = t.value"
              >
                <i :class="t.icon"></i> {{ t.label }}
              </button>
            </div>
          </div>

          <!-- 活动日期 & 时间 -->
          <div class="form-row">
            <div class="form-group flex-1">
              <label class="form-label">活动日期</label>
              <div class="input-wrap">
                <i class="input-icon far fa-calendar-alt"></i>
                <input v-model="form.eventDate" type="date" class="form-input" />
              </div>
            </div>
            <div class="form-group flex-1">
              <label class="form-label">开始时间</label>
              <div class="input-wrap">
                <i class="input-icon far fa-clock"></i>
                <input v-model="form.eventTime" type="time" class="form-input" />
              </div>
            </div>
          </div>

          <!-- 活动地点 -->
          <div class="form-group">
            <label class="form-label">活动地点</label>
            <div class="input-wrap">
              <i class="input-icon fas fa-map-marker-alt"></i>
              <input v-model="form.location" type="text" class="form-input" placeholder="集合地点或活动区域" maxlength="100" />
              <span class="char-count">{{ form.location.length }}/100</span>
            </div>
          </div>

          <!-- 骑行路线 -->
          <div class="form-group">
            <label class="form-label">骑行路线</label>
            <div class="input-wrap">
              <i class="input-icon fas fa-route"></i>
              <input v-model="form.route" type="text" class="form-input" placeholder="如：滨江绿道 → 龙腾大道（可选）" maxlength="200" />
              <span class="char-count">{{ form.route.length }}/200</span>
            </div>
          </div>

          <!-- 难度 & 里程 -->
          <div class="form-row">
            <div class="form-group flex-1">
              <label class="form-label">难度等级</label>
              <select v-model="form.difficulty" class="form-input form-select">
                <option value="">请选择</option>
                <option value="easy">⭐ 休闲骑</option>
                <option value="medium">⭐⭐ 进阶骑</option>
                <option value="hard">⭐⭐⭐ 挑战骑</option>
              </select>
            </div>
            <div class="form-group flex-1">
              <label class="form-label">预计里程 (km)</label>
              <div class="input-wrap">
                <i class="input-icon fas fa-road"></i>
                <input v-model.number="form.distance" type="number" class="form-input" placeholder="如：50" min="0" step="1" />
              </div>
            </div>
          </div>

          <!-- 人数上限 -->
          <div class="form-group">
            <label class="form-label">人数上限</label>
            <div class="input-wrap">
              <i class="input-icon fas fa-users"></i>
              <input v-model.number="form.maxParticipants" type="number" class="form-input" placeholder="不填则不限人数" min="1" />
            </div>
          </div>

          <!-- 活动描述 -->
          <div class="form-group">
            <label class="form-label">活动描述</label>
            <div class="textarea-wrap">
              <textarea v-model="form.description" class="form-textarea" placeholder="详细说明活动内容、注意事项、装备要求等..." rows="5" maxlength="2000"></textarea>
              <span class="char-count">{{ form.description.length }}/2000</span>
            </div>
          </div>

          <!-- 封面图片 -->
          <div class="form-group">
            <label class="form-label">封面图片</label>
            <div class="image-upload-area" @click="triggerUpload">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                class="file-input-hidden"
                @change="handleImageChange"
              />
              <template v-if="coverPreview">
                <img :src="coverPreview" class="cover-preview" alt="封面预览" />
                <div class="cover-overlay">
                  <i class="fas fa-sync-alt"></i> 更换图片
                </div>
              </template>
              <template v-else>
                <div class="upload-placeholder">
                  <i class="fas fa-cloud-upload-alt"></i>
                  <span>点击上传封面图</span>
                  <span class="upload-hint">建议尺寸 1200×630，支持 JPG/PNG</span>
                </div>
              </template>
            </div>
          </div>

          <!-- 预览区 -->
          <div v-if="form.title || form.eventDate || form.location" class="preview-section">
            <div class="preview-label">
              <i class="fas fa-eye"></i> 活动卡片预览
            </div>
            <div class="preview-card">
              <div class="preview-card-header">
                <div class="preview-type-badge">{{ activityTypeLabel }}</div>
                <span class="preview-date">{{ previewDateText }}</span>
              </div>
              <div class="preview-card-body">
                <h4 class="preview-card-title">{{ form.title || '活动标题' }}</h4>
                <div class="preview-card-meta" v-if="form.location">
                  <i class="fas fa-map-marker-alt"></i> {{ form.location }}
                </div>
                <p class="preview-card-desc">{{ form.description || '活动描述...' }}</p>
                <div class="preview-card-stats" v-if="form.difficulty || form.distance || form.maxParticipants">
                  <span v-if="form.difficulty" class="preview-stat">{{ difficultyLabel }}</span>
                  <span v-if="form.distance" class="preview-stat">{{ form.distance }} km</span>
                  <span v-if="form.maxParticipants" class="preview-stat">{{ form.maxParticipants }}人上限</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="handleBack">取消</button>
            <button type="submit" class="btn-submit" :disabled="!isFormValid || submitting">
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-paper-plane"></i>
              <span>发布活动</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from '@/utils/toast'
import { publishActivity, uploadTeamImage } from '@/api/team'

const router = useRouter()
const route = useRoute()

const fileInput = ref(null)
const submitting = ref(false)
const coverPreview = ref('')
const uploadedCoverUrl = ref('')

const activityTypes = [
  { value: 'ride', label: '骑行', icon: 'fas fa-bicycle' },
  { value: 'race', label: '比赛', icon: 'fas fa-flag-checkered' },
  { value: 'training', label: '训练', icon: 'fas fa-dumbbell' },
  { value: 'social', label: '聚会', icon: 'fas fa-coffee' }
]

const form = reactive({
  title: '',
  activityType: 'ride',
  eventDate: '',
  eventTime: '',
  location: '',
  route: '',
  difficulty: '',
  distance: null,
  maxParticipants: null,
  description: ''
})

const isFormValid = computed(() => {
  return form.title.trim() && form.eventDate && form.location.trim() && form.description.trim()
})

const activityTypeLabel = computed(() => {
  const t = activityTypes.find(a => a.value === form.activityType)
  return t ? t.label : '活动'
})

const difficultyLabel = computed(() => {
  const map = { easy: '⭐ 休闲', medium: '⭐⭐ 进阶', hard: '⭐⭐⭐ 挑战' }
  return map[form.difficulty] || ''
})

const previewDateText = computed(() => {
  let text = form.eventDate || ''
  if (form.eventTime) text += ' ' + form.eventTime
  return text || '待定'
})

const handleBack = () => {
  router.push(`/peloton/${route.params.id}`)
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleImageChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  
  // 本地预览
  const reader = new FileReader()
  reader.onload = () => {
    coverPreview.value = reader.result
  }
  reader.readAsDataURL(file)

  // 上传到 COS
  try {
    const fd = new FormData()
    fd.append('file', file)
    const url = await uploadTeamImage(fd)
    uploadedCoverUrl.value = url
  } catch {
    showToast('图片上传失败，请重试', 'error')
    coverPreview.value = ''
    uploadedCoverUrl.value = ''
  }
}

const handleSubmit = async () => {
  if (!isFormValid.value) {
    showToast('请完善必填信息', 'warning')
    return
  }
  submitting.value = true
  try {
    const teamId = route.params.id
    const data = {
      title: form.title.trim(),
      activityType: form.activityType,
      eventDate: form.eventDate,
      eventTime: form.eventTime || undefined,
      location: form.location.trim(),
      route: form.route.trim() || undefined,
      difficulty: form.difficulty || undefined,
      distance: form.distance || undefined,
      maxParticipants: form.maxParticipants || undefined,
      description: form.description.trim(),
      coverImage: uploadedCoverUrl.value || undefined
    }
    await publishActivity(teamId, data)
    showToast('活动发布成功', 'success')
    router.push(`/peloton/${route.params.id}`)
  } catch (err) {
    showToast(err.message || '发布失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.activity-edit-page {
  padding: 100px 0 80px;
  min-height: 100vh;
  background: linear-gradient(180deg, rgba(92,110,88,0.03) 0%, transparent 40%);
}

.edit-header {
  margin-bottom: 32px;
}
.back-btn {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 18px; background: #fff;
  border: 1px solid rgba(0,0,0,0.06); border-radius: 10px;
  font-size: 0.88rem; font-weight: 600; color: var(--text-body);
  cursor: pointer; transition: all 0.25s ease;
}
.back-btn:hover { border-color: var(--primary-color); color: var(--primary-color) }

.edit-card {
  position: relative;
  max-width: 720px; margin: 0 auto;
  background: #fff; border-radius: 24px;
  padding: 48px 48px 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06), 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
}
.card-decor {
  position: absolute; top: -60px; right: -60px;
  width: 180px; height: 180px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-color) 0%, rgba(92,110,88,0.3) 100%);
  opacity: 0.08; pointer-events: none;
}

.card-title-area {
  text-align: center; margin-bottom: 40px;
}
.title-icon {
  width: 56px; height: 56px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(92,110,88,0.1), rgba(92,110,88,0.05));
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
}
.title-icon i { font-size: 1.3rem; color: var(--primary-color) }
.page-title {
  font-size: 1.6rem; font-weight: 800; color: var(--text-main); margin: 0 0 8px 0;
}
.page-subtitle {
  font-size: 0.85rem; color: var(--text-muted); margin: 0;
}

.edit-form {
  display: flex; flex-direction: column; gap: 24px;
}

.form-group {
  display: flex; flex-direction: column; gap: 8px;
}
.form-label {
  font-size: 0.82rem; font-weight: 700; color: var(--text-main);
  letter-spacing: 0.03em;
}
.form-row { display: flex; gap: 16px; }
.flex-1 { flex: 1; }

.input-wrap {
  position: relative; display: flex; align-items: center;
}
.input-icon {
  position: absolute; left: 16px;
  font-size: 0.78rem; color: var(--text-muted); opacity: 0.5;
  pointer-events: none;
}
.form-input {
  width: 100%; padding: 14px 60px 14px 40px;
  border: 1.5px solid rgba(0,0,0,0.07); border-radius: 12px;
  font-size: 0.95rem; color: var(--text-main);
  background: rgba(0,0,0,0.01); transition: all 0.25s ease;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: var(--primary-color);
  background: #fff;
  outline: none;
  box-shadow: 0 0 0 4px rgba(92,110,88,0.08);
}
.form-input::placeholder { color: rgba(0,0,0,0.22) }
.form-select {
  appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23999' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 16px center;
  padding-right: 40px;
}

.textarea-wrap {
  position: relative;
}
.form-textarea {
  width: 100%; padding: 16px 18px 36px;
  border: 1.5px solid rgba(0,0,0,0.07); border-radius: 12px;
  font-size: 0.95rem; color: var(--text-main);
  background: rgba(0,0,0,0.01); transition: all 0.25s ease;
  resize: vertical; line-height: 1.75; font-family: inherit;
  box-sizing: border-box;
}
.form-textarea:focus {
  border-color: var(--primary-color);
  background: #fff;
  outline: none;
  box-shadow: 0 0 0 4px rgba(92,110,88,0.08);
}
.form-textarea::placeholder { color: rgba(0,0,0,0.22) }

.char-count {
  position: absolute; right: 14px; bottom: 12px;
  font-size: 0.7rem; color: var(--text-muted); opacity: 0.6;
}

/* 类型选择器 */
.type-selector { display: flex; gap: 10px; flex-wrap: wrap; }
.type-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 10px 20px; border-radius: 24px;
  border: 1.5px solid rgba(0,0,0,0.07); background: rgba(0,0,0,0.01);
  font-size: 0.85rem; font-weight: 600; color: var(--text-muted);
  cursor: pointer; transition: all 0.25s ease;
}
.type-btn:hover { border-color: var(--primary-color); color: var(--primary-color); }
.type-btn.active {
  background: rgba(92,110,88,0.08); border-color: var(--primary-color);
  color: var(--primary-color);
}
.type-btn i { font-size: 0.78rem; }

/* 图片上传 */
.image-upload-area {
  position: relative; width: 100%; min-height: 180px;
  border: 2px dashed rgba(0,0,0,0.07); border-radius: 12px;
  overflow: hidden; cursor: pointer; transition: border-color 0.25s ease;
  background: rgba(0,0,0,0.01);
}
.image-upload-area:hover { border-color: var(--primary-color); }
.file-input-hidden { display: none; }
.upload-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; padding: 48px 20px; color: var(--text-muted);
}
.upload-placeholder i { font-size: 2rem; opacity: 0.2; }
.upload-placeholder span { font-size: 0.88rem; }
.upload-hint { font-size: 0.75rem !important; opacity: 0.45; }

.cover-preview { width: 100%; height: 220px; object-fit: cover; display: block; }
.cover-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s ease;
}
.image-upload-area:hover .cover-overlay { opacity: 1; }
.cover-overlay { color: #fff; font-size: 0.9rem; font-weight: 600; }
.cover-overlay i { margin-right: 6px; }

/* 预览区 */
.preview-section {
  padding: 20px 0 8px;
  border-top: 1px dashed rgba(0,0,0,0.06);
}
.preview-label {
  font-size: 0.75rem; font-weight: 600; color: var(--text-muted);
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 12px;
}
.preview-label i { font-size: 0.7rem; }
.preview-card {
  padding: 16px 18px; border-radius: 12px;
  background: rgba(92,110,88,0.04);
  border: 1px solid rgba(92,110,88,0.08);
}
.preview-card-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 10px;
}
.preview-type-badge {
  font-size: 0.7rem; font-weight: 700; color: var(--primary-color);
  background: rgba(92,110,88,0.1); padding: 3px 10px; border-radius: 20px;
}
.preview-date {
  font-size: 0.72rem; color: var(--text-muted);
}
.preview-card-title {
  font-size: 0.92rem; font-weight: 700; color: var(--text-main);
  margin: 0 0 6px 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.preview-card-meta {
  font-size: 0.75rem; color: var(--text-muted);
  display: flex; align-items: center; gap: 4px; margin-bottom: 4px;
}
.preview-card-desc {
  font-size: 0.82rem; color: var(--text-muted); line-height: 1.5;
  margin: 0 0 8px;
  display: -webkit-box; -webkit-line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden;
}
.preview-card-stats { display: flex; gap: 10px; flex-wrap: wrap; }
.preview-stat {
  font-size: 0.7rem; color: var(--text-muted);
  background: rgba(0,0,0,0.04); padding: 2px 8px; border-radius: 10px;
}

/* 按钮区 */
.form-actions {
  display: flex; gap: 14px; justify-content: flex-end;
  padding-top: 24px; border-top: 1px solid rgba(0,0,0,0.05);
}
.btn-cancel {
  padding: 12px 28px; border-radius: 10px; border: none;
  background: rgba(0,0,0,0.04); color: var(--text-muted);
  font-size: 0.92rem; font-weight: 600; cursor: pointer;
  transition: all 0.25s ease;
}
.btn-cancel:hover { background: rgba(0,0,0,0.08) }
.btn-submit {
  padding: 12px 32px; border-radius: 24px; border: none;
  background: var(--primary-color); color: #fff;
  font-size: 0.92rem; font-weight: 600; cursor: pointer;
  display: inline-flex; align-items: center; gap: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(92,110,88,0.25);
}
.btn-submit:hover:not(:disabled) {
  background: var(--primary-dark); transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(92,110,88,0.35);
}
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; transform: none }
.btn-submit i { font-size: 0.82rem }

@media (max-width: 768px) {
  .activity-edit-page { padding: 90px 0 60px }
  .edit-card { padding: 32px 24px 28px; border-radius: 18px }
  .page-title { font-size: 1.3rem }
  .form-row { flex-direction: column; gap: 0 }
  .type-selector { gap: 8px }
  .type-btn { padding: 8px 16px; font-size: 0.82rem }
  .form-actions { flex-direction: column-reverse }
  .btn-cancel, .btn-submit { width: 100%; text-align: center; justify-content: center }
}
</style>
