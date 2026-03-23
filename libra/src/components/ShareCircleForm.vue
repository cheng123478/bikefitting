<template>
  <form @submit.prevent="submit" class="share-form">
    <div v-if="form.activityId" class="linked-activity-bar">
      <div class="link-info">
        <i class="fas fa-link"></i>
        <span class="label">正在为活动发布分享：</span>
        <span class="val">{{ form.activityTitle }}</span>
      </div>
    </div>

    <!-- 1. 媒体上传区 -->
    <div class="form-section">
      <div class="upload-box" @click="$refs.fileInput.click()" :style="bgStyle">
        <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*,video/*" hidden>
        <div v-if="!form.cover" class="placeholder">
          <i class="fas fa-camera icon-lg"></i>
          <span>上传图片或视频</span>
          <span class="sub-text">支持 JPG, PNG, MP4</span>
        </div>
        <div v-else class="change-hint">点击更换封面</div>
      </div>
    </div>

    <!-- 2. 内容编辑区 -->
    <div class="glass-card form-card">
      <div class="input-wrapper">
        <input
            type="text"
            v-model="form.title"
            class="title-input"
            placeholder="填写标题，吸引更多骑友..."
            required
        >
      </div>

      <div class="input-wrapper">
        <textarea
            class="content-input"
            v-model="form.desc"
            rows="6"
            placeholder="分享你的骑行见闻、Fitting 感受或是装备心得..."
            required
        ></textarea>
      </div>

      <div class="divider"></div>

      <!-- 分类选择 -->
      <div class="meta-options">
        <div class="option-row">
          <label class="section-label">选择分类</label>
          <div class="radio-group">
            <label class="radio-pill">
              <input type="radio" v-model="form.category" value="riding">
              <span>骑行生活</span>
            </label>
            <label class="radio-pill">
              <input type="radio" v-model="form.category" value="fitting">
              <span>Fitting</span>
            </label>
          </div>
        </div>

        <!-- Fitting 专属数据 -->
        <Transition name="fade">
          <div v-if="form.category === 'fitting'" class="fitting-data-panel">
            <h4 class="panel-title serif">Fitting 关键设定 (推荐填写)</h4>

            <!-- 第一行：身体数据 -->
            <div class="grid-3 small-gap">
              <div class="input-group">
                <span class="prefix">身高</span>
                <input type="number" v-model="form.data.height" class="minimal-input" placeholder="cm">
              </div>
              <div class="input-group">
                <span class="prefix">跨高</span>
                <input type="number" v-model="form.data.inseam" class="minimal-input" placeholder="cm">
              </div>
              <div class="input-group">
                <span class="prefix">座高</span>
                <input type="number" v-model="form.data.saddleHeight" class="minimal-input" placeholder="mm">
              </div>
            </div>

            <!-- 第二行：车辆几何 -->
            <div class="grid-3 small-gap" style="margin-top:10px;">
              <div class="input-group">
                <span class="prefix">Stack</span>
                <input type="number" v-model="form.data.stack" class="minimal-input" placeholder="mm">
              </div>
              <div class="input-group">
                <span class="prefix">Reach</span>
                <input type="number" v-model="form.data.reach" class="minimal-input" placeholder="mm">
              </div>
              <div class="input-group">
                <span class="prefix">落差</span>
                <input type="number" v-model="form.data.drop" class="minimal-input" placeholder="mm">
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- 3. 底部操作栏 -->
    <div class="form-actions">
      <button type="button" class="btn btn-outline" @click="$emit('cancel')">取消</button>
      <button type="submit" class="btn btn-primary" :disabled="isLoading">
        {{ isEdit ? '保存修改' : '立即发布' }}
      </button>
    </div>

  </form>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';

const props = defineProps({ initialData: Object, activityInfo: Object, isLoading: Boolean });
const emit = defineEmits(['submit', 'cancel']);
const isEdit = computed(() => !!props.initialData);
const fileInput = ref(null);

const form = reactive({ title: '', desc: '', category: 'riding', cover: '', data: { stack: '', reach: '' }, activityId: null, activityTitle: null });

onMounted(() => {
  if (props.initialData) Object.assign(form, JSON.parse(JSON.stringify(props.initialData)));
  else if (props.activityInfo) { form.activityId = props.activityInfo.id; form.activityTitle = props.activityInfo.title; form.category = 'event'; }
});

const bgStyle = computed(() => form.cover ? { backgroundImage: `url(${form.cover})` } : {});
const handleFileChange = (e) => {
  const file = e.target.files[0];
  if (file) { const reader = new FileReader(); reader.onload = (e) => { form.cover = e.target.result; }; reader.readAsDataURL(file); }
};

const submit = () => emit('submit', { ...form });
</script>

<style scoped>
.share-form { max-width: 800px; margin: 0 auto; }
.form-card { padding: var(--space-lg); margin-bottom: 24px; }

/* 上传区 */
.upload-box {
  height: 280px; background-color: var(--bg-surface); border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background-size: cover; background-position: center; position: relative;
  transition: 0.3s; border: 2px dashed rgba(0,0,0,0.1); margin-bottom: 24px;
}
.upload-box:hover { border-color: var(--primary-color); background-color: rgba(92,110,88,0.05); }
.placeholder { display: flex; flex-direction: column; align-items: center; color: var(--text-muted); gap: 10px; }
.icon-lg { font-size: 2rem; }
.sub-text { font-size: 0.8rem; opacity: 0.7; }
.change-hint { position: absolute; bottom: 15px; right: 15px; background: rgba(0,0,0,0.6); color: #fff; padding: 6px 16px; border-radius: 20px; font-size: 0.85rem; backdrop-filter: blur(4px); }

/* 输入控件 */
.title-input {
  width: 100%; border: none; font-size: 1.6rem; font-weight: 600; font-family: var(--font-serif);
  color: var(--text-main); padding: 10px 0; outline: none; background: transparent;
  border-bottom: 1px solid rgba(0,0,0,0.05); transition: 0.3s;
}
.title-input:focus { border-bottom-color: var(--primary-color); }

.content-input {
  width: 100%; border: none; font-size: 1rem; line-height: 1.7; color: var(--text-body);
  padding: 20px 0; outline: none; background: transparent; resize: none; font-family: var(--font-sans);
}

.divider { height: 1px; background: rgba(0,0,0,0.05); margin: 10px 0 25px; }

/* 分类选择 */
.option-row { margin-bottom: 20px; }
.section-label { display: block; font-size: 0.9rem; color: var(--text-muted); margin-bottom: 10px; font-weight: 600; }
.radio-group { display: flex; gap: 15px; flex-wrap: wrap; }
.radio-pill input { display: none; }
.radio-pill span {
  display: inline-block; padding: 8px 24px; background: var(--bg-page); color: var(--text-muted);
  border-radius: var(--radius-pill); font-size: 0.9rem; transition: 0.3s; cursor: pointer;
}
.radio-pill input:checked + span {
  background: rgba(92, 110, 88, 0.1); color: var(--primary-color); font-weight: 600;
}

/* Fitting 面板 */
.fitting-data-panel {
  background: rgba(92, 110, 88, 0.05); padding: 20px; border-radius: var(--radius-sm); margin-top: 15px;
}
.panel-title { font-size: 1rem; margin-bottom: 15px; color: var(--primary-color); font-family: var(--font-serif); font-weight: 600; }
.input-group {
  display: flex; align-items: center; background: #fff; border: 1px solid #eee;
  border-radius: var(--radius-sm); padding: 0 12px; margin-bottom: 0;
}
.input-group .prefix { color: var(--text-muted); font-size: 0.9rem; margin-right: 10px; font-weight: 500; }
.minimal-input { border: none; padding: 10px 0; background: transparent; width: 100%; outline: none; color: var(--text-main); }
.form-actions { display: flex; justify-content: flex-end; gap: 15px; padding-bottom: 40px; }

.small-gap { gap: 10px; }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); }

@media (max-width: 768px) {
  .grid-2 { grid-template-columns: 1fr; gap: 10px; }
  .grid-3 { grid-template-columns: 1fr 1fr; }
}
</style>