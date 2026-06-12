<template>
  <div class="notice-edit-page">
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
            <i class="fas fa-bullhorn"></i>
          </div>
          <h1 class="page-title">{{ isEdit ? '编辑公告' : '发布公告' }}</h1>
          <p class="page-subtitle">{{ isEdit ? '修改公告内容，通知全体车队成员' : '向全体车队成员发布重要通知' }}</p>
        </div>

        <div v-if="loading" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i>
          <p>加载公告信息...</p>
        </div>

        <form v-else class="edit-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label">公告标题</label>
            <div class="input-wrap">
              <i class="input-icon fas fa-heading"></i>
              <input v-model="form.title" type="text" class="form-input" placeholder="简明扼要地描述公告主题" maxlength="50" />
              <span class="char-count">{{ form.title.length }}/50</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">公告内容</label>
            <div class="textarea-wrap">
              <textarea v-model="form.content" class="form-textarea" placeholder="详细说明公告内容，例如：时间、地点、注意事项等..." rows="10" maxlength="1000"></textarea>
              <span class="char-count">{{ form.content.length }}/1000</span>
            </div>
          </div>

          <!-- 预览区 -->
          <div v-if="form.title || form.content" class="preview-section">
            <div class="preview-label">
              <i class="fas fa-eye"></i> 实时预览
            </div>
            <div class="preview-card">
              <div class="preview-dot"></div>
              <div class="preview-content">
                <div class="preview-title">{{ form.title || '公告标题' }}</div>
                <div class="preview-date">{{ today }}</div>
                <p class="preview-text">{{ form.content || '公告内容...' }}</p>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="handleBack">取消</button>
            <button type="submit" class="btn-submit" :disabled="!isFormValid || submitting">
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <i v-else :class="isEdit ? 'fas fa-save' : 'fas fa-paper-plane'"></i>
              <span>{{ isEdit ? '保存修改' : '发布公告' }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const submitting = ref(false)
const loading = ref(false)

const isEdit = computed(() => !!route.params.noticeId)

const today = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const form = reactive({
  title: '',
  content: ''
})

const isFormValid = computed(() => {
  return form.title.trim() && form.content.trim()
})

const handleBack = () => {
  router.push(`/peloton/${route.params.id}`)
}

const handleSubmit = async () => {
  if (!isFormValid.value) return
  submitting.value = true
  try {
    // TODO: 调用后端API
    await new Promise(resolve => setTimeout(resolve, 800))
    showToast(isEdit.value ? '公告修改成功' : '公告发布成功', 'success')
    router.push(`/peloton/${route.params.id}`)
  } catch (err) {
    showToast(isEdit.value ? '修改失败，请重试' : '发布失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    loading.value = true
    // TODO: 从后端加载公告数据
    setTimeout(() => {
      form.title = '示例公告标题'
      form.content = '示例公告内容'
      loading.value = false
    }, 500)
  }
})
</script>

<style scoped>
.notice-edit-page {
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
  display: flex; flex-direction: column; gap: 28px;
}

.form-group {
  display: flex; flex-direction: column; gap: 8px;
}
.form-label {
  font-size: 0.82rem; font-weight: 700; color: var(--text-main);
  letter-spacing: 0.03em;
}

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
.preview-label i { font-size: 0.7rem }
.preview-card {
  display: flex; gap: 14px;
  padding: 16px 18px; border-radius: 12px;
  background: rgba(92,110,88,0.04);
  border: 1px solid rgba(92,110,88,0.08);
}
.preview-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--primary-color); flex-shrink: 0;
  margin-top: 6px; opacity: 0.6;
}
.preview-content { flex: 1; min-width: 0 }
.preview-title {
  font-size: 0.92rem; font-weight: 700; color: var(--text-main);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.preview-date {
  font-size: 0.72rem; color: var(--text-muted); margin: 4px 0 8px;
}
.preview-text {
  font-size: 0.84rem; color: var(--text-muted); line-height: 1.6;
  margin: 0; display: -webkit-box; -webkit-line-clamp: 3;
  -webkit-box-orient: vertical; overflow: hidden;
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
  padding: 12px 32px; border-radius: 10px; border: none;
  background: var(--primary-color); color: #fff;
  font-size: 0.92rem; font-weight: 700; cursor: pointer;
  display: inline-flex; align-items: center; gap: 8px;
  transition: all 0.3s cubic-bezier(0.34,1.56,0.64,1);
}
.btn-submit:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(92,110,88,0.3);
}
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; transform: none }
.btn-submit i { font-size: 0.82rem }

.loading-state { text-align: center; padding: 40px 0; color: var(--text-muted) }
.loading-state i { font-size: 1.8rem; color: var(--primary-color); display: block; margin-bottom: 12px }

@media (max-width: 768px) {
  .notice-edit-page { padding: 90px 0 60px }
  .edit-card { padding: 32px 24px 28px; border-radius: 18px }
  .page-title { font-size: 1.3rem }
  .form-actions { flex-direction: column-reverse }
  .btn-cancel, .btn-submit { width: 100%; text-align: center; justify-content: center }
}
</style>
