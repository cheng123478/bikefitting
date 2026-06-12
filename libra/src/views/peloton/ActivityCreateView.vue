<template>
  <div class="activity-create-page">
    <div class="container">
      <header class="create-header">
        <button class="back-btn" @click="handleBack">
          <i class="fas fa-arrow-left"></i> 返回
        </button>
        <h1 class="page-title serif-title">发布活动</h1>
      </header>

      <form class="create-form" @submit.prevent="handleSubmit">
        <!-- 活动标题 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-heading"></i> 活动标题 <span class="required">*</span>
          </label>
          <input
            v-model="form.title"
            type="text"
            class="form-input"
            placeholder="请输入活动标题"
            maxlength="50"
          />
          <span class="form-hint">{{ form.title.length }}/50</span>
        </div>

        <!-- 活动类型 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-tag"></i> 活动类型 <span class="required">*</span>
          </label>
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

        <!-- 活动时间 -->
        <div class="form-row">
          <div class="form-group flex-1">
            <label class="form-label">
              <i class="far fa-calendar-alt"></i> 活动日期 <span class="required">*</span>
            </label>
            <input v-model="form.date" type="date" class="form-input" />
          </div>
          <div class="form-group flex-1">
            <label class="form-label">
              <i class="far fa-clock"></i> 开始时间 <span class="required">*</span>
            </label>
            <input v-model="form.time" type="time" class="form-input" />
          </div>
        </div>

        <!-- 活动地点 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-map-marker-alt"></i> 活动地点 <span class="required">*</span>
          </label>
          <input
            v-model="form.location"
            type="text"
            class="form-input"
            placeholder="请输入活动地点"
          />
        </div>

        <!-- 活动路线 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-route"></i> 骑行路线
          </label>
          <input
            v-model="form.route"
            type="text"
            class="form-input"
            placeholder="如：滨江绿道 → 龙腾大道（可选）"
          />
        </div>

        <!-- 难度与距离 -->
        <div class="form-row">
          <div class="form-group flex-1">
            <label class="form-label">
              <i class="fas fa-tachometer-alt"></i> 难度等级
            </label>
            <select v-model="form.difficulty" class="form-input">
              <option value="">请选择</option>
              <option value="easy">⭐ 休闲骑</option>
              <option value="medium">⭐⭐ 进阶骑</option>
              <option value="hard">⭐⭐⭐ 挑战骑</option>
            </select>
          </div>
          <div class="form-group flex-1">
            <label class="form-label">
              <i class="fas fa-road"></i> 预计里程 (km)
            </label>
            <input
              v-model.number="form.distance"
              type="number"
              class="form-input"
              placeholder="如：50"
              min="0"
              step="1"
            />
          </div>
        </div>

        <!-- 人数限制 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-users"></i> 人数上限
          </label>
          <input
            v-model.number="form.maxParticipants"
            type="number"
            class="form-input"
            placeholder="不填则不限人数"
            min="1"
          />
        </div>

        <!-- 活动描述 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-align-left"></i> 活动描述 <span class="required">*</span>
          </label>
          <textarea
            v-model="form.description"
            class="form-textarea"
            placeholder="详细描述活动内容、注意事项等"
            rows="5"
            maxlength="500"
          ></textarea>
          <span class="form-hint">{{ form.description.length }}/500</span>
        </div>

        <!-- 封面图片 -->
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-image"></i> 封面图片
          </label>
          <div class="image-upload-area" @click="triggerUpload">
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              class="file-input-hidden"
              @change="handleImageChange"
            />
            <template v-if="form.coverPreview">
              <img :src="form.coverPreview" class="cover-preview" alt="封面预览" />
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

        <!-- 提交按钮 -->
        <div class="form-actions">
          <button type="button" class="btn-cancel" @click="handleBack">取消</button>
          <button type="submit" class="btn-submit" :disabled="!isFormValid || submitting">
            <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
            <span v-else>发布活动</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getTeamDetail } from '@/api/team'
import { showToast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const fileInput = ref(null)
const submitting = ref(false)
const teamName = ref('')

const activityTypes = [
  { value: 'ride', label: '骑行', icon: 'fas fa-bicycle' },
  { value: 'race', label: '比赛', icon: 'fas fa-flag-checkered' },
  { value: 'training', label: '训练', icon: 'fas fa-dumbbell' },
  { value: 'social', label: '聚会', icon: 'fas fa-coffee' }
]

const form = reactive({
  title: '',
  activityType: 'ride',
  date: '',
  time: '',
  location: '',
  route: '',
  difficulty: '',
  distance: null,
  maxParticipants: null,
  description: '',
  coverImage: null,
  coverPreview: ''
})

const isFormValid = computed(() => {
  return form.title.trim() && form.date && form.time && form.location.trim() && form.description.trim()
})

const handleBack = () => {
  router.push(`/peloton/${route.params.id}`)
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleImageChange = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  form.coverImage = file
  const reader = new FileReader()
  reader.onload = () => {
    form.coverPreview = reader.result
  }
  reader.readAsDataURL(file)
}

const handleSubmit = async () => {
  if (!isFormValid.value) {
    showToast('请完善必填信息', 'warning')
    return
  }
  submitting.value = true
  try {
    // TODO: 接入真实 API
    await new Promise(resolve => setTimeout(resolve, 800))
    showToast('活动发布成功！', 'success')
    router.push(`/peloton/${route.params.id}`)
  } catch (err) {
    showToast('发布失败，请稍后重试', 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const teamData = await getTeamDetail(route.params.id)
    teamName.value = teamData.name || ''
  } catch (e) {
    // 忽略
  }
})
</script>

<style scoped>
.activity-create-page { padding: 120px 0 100px; min-height: 100vh }

.create-header { margin-bottom: 36px; display: flex; align-items: center; gap: 20px }
.back-btn {
  display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px;
  background: #fff; border: 1px solid rgba(0,0,0,0.08); border-radius: 8px;
  font-size: 0.9rem; font-weight: 600; color: var(--text-body); cursor: pointer; transition: all 0.3s
}
.back-btn:hover { border-color: var(--primary-color); color: var(--primary-color); background: rgba(92,110,88,0.05) }
.page-title { font-family: var(--font-serif); font-size: 1.75rem; font-weight: 800; color: var(--text-main); margin: 0 }

.create-form { max-width: 720px; margin: 0 auto }
.form-group { margin-bottom: 24px }
.form-row { display: flex; gap: 16px }
.flex-1 { flex: 1 }
.form-label {
  display: flex; align-items: center; gap: 8px; font-size: 0.88rem; font-weight: 700;
  color: var(--text-main); margin-bottom: 8px
}
.form-label i { font-size: 0.82rem; color: var(--primary-color); opacity: 0.7 }
.required { color: #e74c3c; font-weight: 400 }

.form-input, .form-textarea, select.form-input {
  width: 100%; padding: 12px 16px; border: 1px solid rgba(0,0,0,0.1); border-radius: 10px;
  font-size: 0.9rem; color: var(--text-main); background: #fff; outline: none;
  transition: border-color 0.25s ease, box-shadow 0.25s ease; font-family: inherit; box-sizing: border-box
}
.form-input:focus, .form-textarea:focus { border-color: var(--primary-color); box-shadow: 0 0 0 3px rgba(92,110,88,0.08) }
.form-input::placeholder, .form-textarea::placeholder { color: var(--text-muted); opacity: 0.5 }
.form-textarea { resize: vertical; min-height: 120px }
.form-hint { display: block; text-align: right; font-size: 0.75rem; color: var(--text-muted); margin-top: 4px }

/* 类型选择器 */
.type-selector { display: flex; gap: 10px; flex-wrap: wrap }
.type-btn {
  display: flex; align-items: center; gap: 7px; padding: 10px 20px; border-radius: 24px;
  border: 1px solid rgba(0,0,0,0.08); background: #fff; font-size: 0.85rem;
  font-weight: 600; color: var(--text-muted); cursor: pointer; transition: all 0.25s ease
}
.type-btn:hover { border-color: var(--primary-color); color: var(--primary-color) }
.type-btn.active { background: rgba(92,110,88,0.08); border-color: var(--primary-color); color: var(--primary-color) }
.type-btn i { font-size: 0.78rem }

/* 图片上传 */
.image-upload-area {
  position: relative; width: 100%; min-height: 200px; border: 2px dashed rgba(0,0,0,0.1);
  border-radius: 12px; overflow: hidden; cursor: pointer; transition: border-color 0.25s ease; background: rgba(0,0,0,0.01)
}
.image-upload-area:hover { border-color: var(--primary-color) }
.file-input-hidden { display: none }
.upload-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; padding: 48px 20px; color: var(--text-muted)
}
.upload-placeholder i { font-size: 2.2rem; opacity: 0.25 }
.upload-placeholder span { font-size: 0.88rem }
.upload-hint { font-size: 0.75rem !important; opacity: 0.45 }

.cover-preview { width: 100%; height: 240px; object-fit: cover; display: block }
.cover-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.35); display: flex;
  align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s ease
}
.image-upload-area:hover .cover-overlay { opacity: 1 }
.cover-overlay i { margin-right: 6px }
.cover-overlay { color: #fff; font-size: 0.9rem; font-weight: 600 }

/* 提交按钮 */
.form-actions { display: flex; gap: 14px; justify-content: flex-end; margin-top: 40px; padding-top: 28px; border-top: 1px solid rgba(0,0,0,0.06) }
.btn-cancel {
  padding: 12px 32px; border-radius: 10px; border: 1px solid rgba(0,0,0,0.1); background: #fff;
  font-size: 0.9rem; font-weight: 600; color: var(--text-muted); cursor: pointer; transition: all 0.25s ease
}
.btn-cancel:hover { border-color: var(--text-muted); color: var(--text-body) }
.btn-submit {
  padding: 12px 36px; border-radius: 10px; border: none; background: var(--primary-color);
  font-size: 0.9rem; font-weight: 700; color: #fff; cursor: pointer; transition: all 0.25s ease; box-shadow: 0 2px 8px rgba(92,110,88,0.25)
}
.btn-submit:hover:not(:disabled) { background: var(--primary-dark); transform: translateY(-1px); box-shadow: 0 4px 16px rgba(92,110,88,0.35) }
.btn-submit:disabled { opacity: 0.45; cursor: not-allowed }

@media (max-width: 768px) {
  .activity-create-page { padding: 100px 0 60px }
  .page-title { font-size: 1.35rem }
  .form-row { flex-direction: column; gap: 0 }
  .type-selector { gap: 8px }
  .type-btn { padding: 8px 16px; font-size: 0.82rem }
}
</style>
