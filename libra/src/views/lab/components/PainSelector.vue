<template>
  <div class="pain-selector">
    <div v-if="loading" class="loading-state">
      <i class="fas fa-spinner fa-spin"></i> 加载痛感选项...
    </div>
    <div v-else class="pain-options-grid">
      <label
          v-for="opt in options"
          :key="opt.code"
          class="pain-option-label"
          :class="{ active: modelValue.includes(opt.code) }"
      >
        <input
            type="checkbox"
            :value="opt.code"
            :checked="modelValue.includes(opt.code)"
            @change="toggleSelection(opt.code)"
            hidden
        >
        <i class="fas fa-check-circle check-icon"></i>
        <span class="opt-text">{{ opt.description }}</span>
      </label>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import {getPainOptions} from '@/api/lab.js';

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue']);

const options = ref([]);
const loading = ref(true);

const fetchOptions = async () => {
  try {
    const res = await getPainOptions();
    options.value = res.data || res;
  } catch (e) {
    console.error("Failed to fetch pain options", e);
  } finally {
    loading.value = false;
  }
};

const toggleSelection = (code) => {
  const newValue = [...props.modelValue];
  const index = newValue.indexOf(code);
  if (index > -1) {
    newValue.splice(index, 1);
  } else {
    newValue.push(code);
  }
  emit('update:modelValue', newValue);
};

onMounted(fetchOptions);
</script>

<style scoped>
.pain-options-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.pain-option-label {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.pain-option-label:hover {
  border-color: var(--primary-color);
  background: rgba(92, 110, 88, 0.02);
}

.pain-option-label.active {
  border-color: var(--primary-color);
  background: rgba(92, 110, 88, 0.05);
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(0, 0, 0, 0.05));
  border-width: 1px;
}

.check-icon {
  font-size: 1.1rem;
  color: #cbd5e1;
  transition: 0.3s;
}

.pain-option-label.active .check-icon {
  color: var(--primary-color);
}

.opt-text {
  font-size: 0.9rem;
  color: var(--text-main);
  font-weight: 500;
  line-height: 1.2;
}

.loading-state {
  padding: 20px;
  text-align: center;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .pain-options-grid {
    grid-template-columns: 1fr;
  }
}
</style>
