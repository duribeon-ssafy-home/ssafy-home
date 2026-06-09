<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    default: '비밀번호',
  },
  modelValue: {
    type: String,
    default: '',
  },
  placeholder: {
    type: String,
    default: '비밀번호',
  },
  autocomplete: {
    type: String,
    default: 'current-password',
  },
  required: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

const isPasswordVisible = ref(false)
const isCapsLockOn = ref(false)

const inputType = computed(() => (isPasswordVisible.value ? 'text' : 'password'))
const toggleLabel = computed(() =>
  isPasswordVisible.value ? '비밀번호 숨기기' : '비밀번호 표시하기',
)

function updateValue(event) {
  emit('update:modelValue', event.target.value)
}

function updateCapsLock(event) {
  isCapsLockOn.value = event.getModifierState?.('CapsLock') || false
}

function clearCapsLockNotice() {
  isCapsLockOn.value = false
}

function togglePasswordVisibility() {
  isPasswordVisible.value = !isPasswordVisible.value
}
</script>

<template>
  <div class="password-field">
    <label :for="id">{{ label }}</label>
    <div class="password-control">
      <input
        :id="id"
        :autocomplete="autocomplete"
        :placeholder="placeholder"
        :required="required"
        :type="inputType"
        :value="modelValue"
        @blur="clearCapsLockNotice"
        @input="updateValue"
        @keydown="updateCapsLock"
        @keyup="updateCapsLock"
      />
      <button type="button" :aria-label="toggleLabel" @click="togglePasswordVisibility">
        <svg
          v-if="!isPasswordVisible"
          aria-hidden="true"
          viewBox="0 0 24 24"
          focusable="false"
        >
          <path
            d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"
          />
          <circle cx="12" cy="12" r="3" />
        </svg>
        <svg v-else aria-hidden="true" viewBox="0 0 24 24" focusable="false">
          <path d="m4 4 16 16" />
          <path d="M9.6 5.4A10 10 0 0 1 12 5c6 0 9.5 7 9.5 7a16 16 0 0 1-3.1 4" />
          <path d="M14.1 14.1A3 3 0 0 1 9.9 9.9" />
          <path d="M6.7 6.7C3.9 8.5 2.5 12 2.5 12S6 19 12 19a9.7 9.7 0 0 0 4.1-.9" />
        </svg>
      </button>
    </div>
    <em v-if="isCapsLockOn">CapsLock이 켜져 있습니다</em>
  </div>
</template>

<style lang="scss" scoped>
.password-field {
  display: grid;
  gap: 8px;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 900;
}

.password-control {
  position: relative;

  input {
    width: 100%;
    height: 46px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    color: var(--color-heading);
    padding: 0 48px 0 13px;
    outline: none;
    transition:
      border-color var(--transition-fast),
      box-shadow var(--transition-fast);

    &:focus {
      border-color: var(--color-primary);
      box-shadow: 0 0 0 4px rgba(54, 95, 145, 0.12);
    }
  }

  button {
    position: absolute;
    top: 50%;
    right: 8px;
    transform: translateY(-50%);
    width: 34px;
    height: 34px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-xs);
    background: transparent;
    color: var(--color-muted);
    transition:
      background-color var(--transition-fast),
      color var(--transition-fast);

    &:hover {
      background: var(--color-primary-soft);
      color: var(--color-primary-dark);
    }
  }

  svg {
    width: 18px;
    height: 18px;
    fill: none;
    stroke: currentColor;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 1.9;
  }
}

em {
  color: var(--color-danger);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}
</style>
