<template>
  <div class="custom-input-group">
    <label class="input-label">{{ label }}</label>
    <div class="input-container" :class="{ 'is-focused': isFocused, 'has-warning': !!showWarning }">
      <input
          type="number"
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
          @focus="handleFocus"
          @blur="isFocused = false"
          @wheel="$event.target.blur()"
          :placeholder="placeholder"
          class="zen-field"
      />
      <span class="unit-tag">{{ unit }}</span>
    </div>
    <!-- 动态预警内容 -->
    <transition name="fade">
      <p v-if="showWarning" class="input-hint">
        <i class="fas fa-exclamation-triangle"></i> {{ showWarning }}
      </p>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  label: String,
  modelValue: [String, Number],
  unit: String,
  placeholder: String,
  warning: {
    type: String,
    default: ''
  },
  warningThreshold: {
    type: Number,
    default: 300
  }
});

const emit = defineEmits(['update:modelValue', 'focus']);

const isFocused = ref(false);

const handleFocus = () => {
  isFocused.value = true;
  emit('focus');
};

const showWarning = computed(() => {
  if (props.warning) return props.warning;
  if (!props.modelValue || props.modelValue <= 0) return false;
  
  const val = Number(props.modelValue);
  const label = props.label.toLowerCase();

  // 1. 单位检测：如果输入的是身高、跨高、胸高且小于500，提示单位可能为cm
  const mmFields = ['身高', '跨高', '胸高度', '胸高', 'height', 'inseam', 'sternal'];
  const isMMField = mmFields.some(field => label.includes(field));
  
  if (isMMField && val < 500) {
    return '数值过小，请确认单位为毫米(mm)';
  }

  // 2. 异常极大值检测
  if (label.includes('身高') && val > 3000) {
    return '数值异常：身高超过常规范围';
  }

  return false;
});
</script>

<style scoped>
.custom-input-group { margin-bottom: 20px; }
.input-label { display: block; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 8px; font-weight: 500; }

.input-container {
  display: flex; align-items: center; background: #FAFAFA;
  border: 1px solid #E0E0E0; border-radius: var(--radius-sm);
  padding: 0 15px; transition: all 0.3s var(--ease-out-expo);
}

.input-container.is-focused {
  border-color: var(--primary-color); background: #FFF;
  box-shadow: 0 0 0 3px rgba(92, 110, 88, 0.1);
}

.input-container.has-warning {
  border-color: #E2A84D;
  background: linear-gradient(135deg, rgba(226, 168, 77, 0.14), rgba(255, 248, 235, 0.92));
  box-shadow: 0 8px 22px rgba(226, 168, 77, 0.12);
}

.input-container.has-warning:hover {
  border-color: #D49335;
  background: linear-gradient(135deg, rgba(226, 168, 77, 0.2), rgba(255, 250, 240, 1));
  box-shadow: 0 12px 28px rgba(226, 168, 77, 0.18);
  transform: translateY(-1px);
}

.zen-field {
  flex: 1; border: none; background: transparent; padding: 12px 0;
  font-size: 1rem; color: var(--text-main); outline: none;
}

.unit-tag { font-size: 0.85rem; color: var(--text-muted); font-weight: 600; opacity: 0.6; }

.input-hint {
  font-size: 0.75rem;
  color: #9A641C;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
  border-left: 3px solid #E2A84D;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(226, 168, 77, 0.16), rgba(226, 168, 77, 0.04));
  padding: 7px 10px;
  transition: all 0.25s ease;
}

.input-hint:hover {
  background: linear-gradient(90deg, rgba(226, 168, 77, 0.22), rgba(226, 168, 77, 0.07));
  transform: translateX(2px);
}

input::-webkit-outer-spin-button, input::-webkit-inner-spin-button { -webkit-appearance: none; }
</style>
