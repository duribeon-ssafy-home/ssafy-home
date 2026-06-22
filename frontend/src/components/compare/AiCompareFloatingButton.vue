<script setup>
defineProps({
  disabled: {
    type: Boolean,
    default: false,
  },
  bottom: {
    type: Number,
    default: 28,
  },
})

defineEmits(['open'])
</script>

<template>
  <button
    class="ai-compare-button"
    type="button"
    :style="{ bottom: `${bottom}px` }"
    :disabled="disabled"
    @click="$emit('open')"
  >
    <span class="ai-compare-button__mark">AI</span>
    <span>AI 비교 상담</span>
  </button>
</template>

<style lang="scss" scoped>
.ai-compare-button {
  position: fixed;
  right: 28px;
  z-index: 120;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-sm);
  background: var(--color-heading);
  color: white;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.2);
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    background-color var(--transition-fast);

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    background: var(--color-primary-dark);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
}

.ai-compare-button__mark {
  display: inline-grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-xs);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 12px;
}

@media (max-width: 720px) {
  .ai-compare-button {
    right: 16px;
  }
}
</style>
