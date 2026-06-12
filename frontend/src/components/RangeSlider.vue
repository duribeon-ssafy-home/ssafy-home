<script setup>
import { computed } from 'vue'

const props = defineProps({
  min: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  step: { type: Number, default: 1 },
  modelValue: { type: Array, default: () => [0, 100] },
})
const emit = defineEmits(['update:modelValue'])

function onMinInput(e) {
  const v = Math.min(Number(e.target.value), props.modelValue[1] - props.step)
  emit('update:modelValue', [Math.max(v, props.min), props.modelValue[1]])
}

function onMaxInput(e) {
  const v = Math.max(Number(e.target.value), props.modelValue[0] + props.step)
  emit('update:modelValue', [props.modelValue[0], Math.min(v, props.max)])
}

const fillStyle = computed(() => {
  const range = props.max - props.min
  if (range === 0) return { left: '0%', right: '0%' }
  const left = ((props.modelValue[0] - props.min) / range) * 100
  const right = ((props.max - props.modelValue[1]) / range) * 100
  return { left: `${left}%`, right: `${right}%` }
})
</script>

<template>
  <div class="range-slider">
    <div class="track">
      <div class="fill" :style="fillStyle" />
    </div>
    <input
      class="thumb"
      type="range"
      :min="min" :max="max" :step="step"
      :value="modelValue[0]"
      @input="onMinInput"
    />
    <input
      class="thumb"
      type="range"
      :min="min" :max="max" :step="step"
      :value="modelValue[1]"
      @input="onMaxInput"
    />
  </div>
</template>

<style lang="scss" scoped>
.range-slider {
  position: relative;
  height: 20px;
  display: flex;
  align-items: center;
}

.track {
  position: absolute;
  left: 0; right: 0;
  height: 4px;
  background: var(--color-border);
  border-radius: 2px;
}

.fill {
  position: absolute;
  top: 0; bottom: 0;
  background: var(--color-primary);
  border-radius: 2px;
}

.thumb {
  position: absolute;
  width: 100%;
  height: 4px;
  background: none;
  pointer-events: none;
  appearance: none;
  -webkit-appearance: none;
  outline: none;

  &::-webkit-slider-thumb {
    appearance: none;
    -webkit-appearance: none;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: var(--color-primary);
    border: 2px solid var(--color-surface);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
    pointer-events: all;
    cursor: pointer;
    transition: transform var(--transition-fast);

    &:hover { transform: scale(1.2); }
  }

  &::-moz-range-thumb {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: var(--color-primary);
    border: 2px solid var(--color-surface);
    pointer-events: all;
    cursor: pointer;
  }
}
</style>
