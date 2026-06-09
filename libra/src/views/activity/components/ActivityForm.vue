<template>
  <form @submit.prevent="submit" class="activity-form">

    <!-- [新增] 关联车队提示条 -->
    <div v-if="teamInfo" class="linked-team-bar">
      <div class="link-icon"><i class="fas fa-shield-alt"></i></div>
      <div class="link-content">
        <span class="label">所属骑士团</span>
        <span class="val">{{ teamInfo.name }}</span>
      </div>
    </div>

    <!-- 1. 封面上传 -->
    <div class="form-section upload-section">
      <div class="upload-box" @click="$refs.fileInput.click()" :style="bgStyle">
        <input type="file" ref="fileInput" @change="handleFileChange" accept="image/*" hidden>
        <div v-if="!form.cover" class="placeholder">
          <i class="fas fa-camera icon-lg"></i>
          <span>点击上传活动封面</span>
          <span class="sub-text">建议尺寸 1200x600</span>
        </div>
        <div v-else class="change-hint">点击更换封面</div>
      </div>
    </div>

    <!-- 2. 基础信息 -->
    <div class="glass-card form-card">
      <h3 class="section-title-sm">基础信息</h3>

      <div class="input-wrapper">
        <label>活动标题</label>
        <div class="input-group">
          <input type="text" v-model="form.title" class="zen-input" placeholder="例如：周末西湖休闲骑" required>
        </div>
      </div>

      <div class="grid-2">
        <div class="input-wrapper">
          <label>集合时间</label>
          <div class="input-group">
            <input type="datetime-local" v-model="form.time" class="zen-input" required>
          </div>
        </div>
        <div class="input-wrapper">
          <label>集合地点</label>
          <div class="input-group icon-input">
            <i class="fas fa-map-marker-alt input-icon"></i>
            <input type="text" v-model="form.location" class="zen-input" placeholder="例如：少年宫广场" required>
          </div>
        </div>
      </div>

      <div class="grid-3">
        <div class="input-wrapper">
          <label>限额人数</label>
          <div class="input-group">
            <input type="number" v-model="form.maxCount" class="zen-input" placeholder="50">
          </div>
        </div>
        <div class="input-wrapper">
          <label>费用 (元)</label>
          <div class="input-group">
            <input type="number" v-model="form.price" class="zen-input" placeholder="0为免费">
          </div>
        </div>
        <div class="input-wrapper">
          <label>难度等级</label>
          <div class="input-group select-group">
            <select v-model="form.difficulty" class="zen-select">
              <option value="L1">L1 休闲 (均速20)</option>
              <option value="L2">L2 进阶 (均速25)</option>
              <option value="L3">L3 挑战 (均速30+)</option>
            </select>
            <i class="fas fa-chevron-down arrow"></i>
          </div>
        </div>
      </div>

      <div class="input-wrapper">
        <label>活动详情</label>
        <textarea
            class="zen-textarea"
            v-model="form.desc"
            rows="5"
            placeholder="介绍一下路线特色、注意事项等..."
            required
        ></textarea>
      </div>
    </div>

    <!-- 3. 行程规划 -->
    <div class="glass-card form-card">
      <div class="header-row">
        <h3 class="section-title-sm">行程规划</h3>
        <button type="button" class="btn btn-sm btn-outline" @click="addNode">
          <i class="fas fa-plus"></i> 添加节点
        </button>
      </div>

      <div class="itinerary-list">
        <div v-for="(node, index) in form.itinerary" :key="index" class="itinerary-item">
          <div class="node-time">
            <input type="time" v-model="node.time" class="simple-input" required>
          </div>
          <div class="node-content">
            <input type="text" v-model="node.title" placeholder="节点名称" class="simple-input title-input" required>
            <input type="text" v-model="node.detail" placeholder="详情描述" class="simple-input desc-input">
          </div>
          <!-- 删除按钮 -->
          <button
              type="button"
              class="btn-icon text-danger"
              @click="removeNode(index)"
              v-if="form.itinerary.length > 1"
          >
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- 4. 提交栏 -->
    <div class="form-actions">
      <button type="button" class="btn btn-outline" @click="$emit('cancel')">取消</button>
      <button type="submit" class="btn btn-primary" :disabled="isLoading">
        {{ isEdit ? '保存修改' : '发布活动' }}
      </button>
    </div>

  </form>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';

const props = defineProps({
  initialData: Object,
  teamInfo: Object,
  isLoading: Boolean
});

const emit = defineEmits(['submit', 'cancel']);
const isEdit = computed(() => !!props.initialData);
const fileInput = ref(null);

const form = reactive({
  title: '', time: '', location: '', maxCount: 20, price: 0,
  difficulty: 'L1', desc: '', cover: '',
  itinerary: [{time: '19:00', title: '集合', detail: ''}, {time: '21:00', title: '结束', detail: ''}]
});

onMounted(() => {
  if (props.initialData) Object.assign(form, JSON.parse(JSON.stringify(props.initialData)));
});

const bgStyle = computed(() => form.cover ? {backgroundImage: `url(${form.cover})`} : {});

const handleFileChange = (e) => {
  const file = e.target.files[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = (e) => { form.cover = e.target.result; };
    reader.readAsDataURL(file);
  }
};

const addNode = () => form.itinerary.push({time: '', title: '', detail: ''});
const removeNode = (index) => form.itinerary.splice(index, 1);
const submit = () => emit('submit', {...form});
</script>

<style scoped>
.activity-form { max-width: 800px; margin: 0 auto; }
.form-card { padding: var(--space-lg); margin-bottom: var(--space-md); }

/* 标题样式适配 */
.section-title-sm {
  font-family: var(--font-serif); font-size: 1.4rem; color: var(--text-main);
  margin-bottom: var(--space-md); border-bottom: 1px solid rgba(0,0,0,0.05); padding-bottom: 10px;
}

/* 关联车队 */
.linked-team-bar {
  display: flex; align-items: center; gap: 12px;
  background: rgba(92, 110, 88, 0.08); border: 1px solid var(--primary-color);
  padding: 12px 20px; border-radius: var(--radius-sm); margin-bottom: var(--space-md);
}
.link-icon { color: var(--primary-color); font-size: 1.2rem; }
.link-content .label { font-size: 0.75rem; color: var(--text-muted); }
.link-content .val { font-size: 1rem; font-weight: 600; color: var(--text-main); }

/* 封面上传 */
.upload-box {
  height: 240px; background-color: var(--bg-surface);
  border-radius: var(--radius-md); border: 2px dashed rgba(0,0,0,0.1);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; background-size: cover; background-position: center; position: relative;
  transition: 0.3s; margin-bottom: var(--space-md);
}
.upload-box:hover { border-color: var(--primary-color); background-color: rgba(92,110,88,0.05); }
.placeholder { display: flex; flex-direction: column; align-items: center; color: var(--text-muted); gap: 10px; }
.icon-lg { font-size: 2rem; }
.sub-text { font-size: 0.8rem; opacity: 0.7; }
.change-hint {
  position: absolute; bottom: 10px; right: 10px;
  background: rgba(0,0,0,0.6); color: #fff; padding: 4px 12px;
  border-radius: 20px; font-size: 0.8rem; backdrop-filter: blur(4px);
}

/* 输入设置 Override */
.input-wrapper { margin-bottom: var(--space-md); }

.select-group { position: relative; }
.select-group .arrow { position: absolute; right: 15px; top: 50%; transform: translateY(-50%); pointer-events: none; font-size: 0.8rem; color: var(--text-muted); }

/* 行程列表 */
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.itinerary-item {
  display: flex; gap: 15px; margin-bottom: 15px; align-items: center;
  background: var(--bg-page); padding: 12px; border-radius: var(--radius-sm);
}
.simple-input {
  border: 1px solid #ddd; background: #fff; padding: 8px 12px; border-radius: 6px;
  outline: none; font-family: var(--font-sans); color: var(--text-main); width: 100%; font-size: 0.9rem;
}
.simple-input:focus { border-color: var(--primary-color); }
.node-time { width: 110px; }
.node-content { flex: 1; display: flex; gap: 10px; }
.title-input { flex: 1; font-weight: 600; }
.desc-input { flex: 2; }
.btn-icon { background: transparent; border: none; cursor: pointer; color: var(--text-muted); transition: 0.2s; font-size: 1rem; }
.text-danger:hover { color: var(--danger-color); }

.form-actions { display: flex; justify-content: flex-end; gap: 15px; margin-top: 40px; }

@media (max-width: 768px) {
  .grid-2, .grid-3 { grid-template-columns: 1fr; gap: 15px; }
  .itinerary-item { flex-direction: column; align-items: stretch; }
  .node-time { width: 100%; }
  .node-content { flex-direction: column; }
}
</style>