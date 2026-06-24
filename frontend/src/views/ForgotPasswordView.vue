<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { requestTemporaryPassword } from '@/api/authApi'

const email = ref('')
const isSubmitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

async function submitRequest() {
  successMessage.value = ''
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await requestTemporaryPassword({
      email: email.value,
    })
    successMessage.value = '입력한 이메일로 임시 비밀번호를 발송했습니다.'
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message || '임시 비밀번호 발급 요청에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="page auth-page">
    <section class="auth-shell">
      <div class="auth-visual">
        <div>
          <p>SSAFY HOME</p>
          <h1>임시 비밀번호로 다시 로그인하세요</h1>
        </div>
      </div>

      <form class="auth-form" @submit.prevent="submitRequest">
        <p class="eyebrow">Password Help</p>
        <h2>비밀번호 찾기</h2>
        <p class="helper-text">
          가입한 이메일을 입력하면 임시 비밀번호를 보내드립니다.
        </p>

        <label>
          이메일
          <input
            v-model="email"
            autocomplete="email"
            required
            type="email"
            placeholder="you@example.com"
          />
        </label>

        <p v-if="successMessage" class="form-message form-message--success" role="status">
          {{ successMessage }}
        </p>
        <p v-if="errorMessage" class="form-message form-message--error" role="alert">
          {{ errorMessage }}
        </p>

        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '발송 중...' : '임시 비밀번호 받기' }}
        </button>
        <RouterLink :to="{ name: 'login' }">로그인으로 돌아가기</RouterLink>
      </form>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.auth-page {
  display: grid;
  place-items: center;
  padding: 48px 20px 72px;
}

.auth-shell {
  width: min(100%, 980px);
  display: grid;
  grid-template-columns: 1fr 0.86fr;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.auth-visual {
  min-height: 560px;
  display: grid;
  align-items: end;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.1) 0%, rgba(15, 23, 42, 0.62) 100%),
    url('https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80')
      center / cover;
  color: var(--color-surface);
  padding: 34px;

  p {
    font-weight: 900;
  }

  h1 {
    max-width: 390px;
    margin-top: 10px;
    font-size: 34px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.2;
  }
}

.auth-form {
  display: grid;
  align-content: center;
  gap: 16px;
  padding: 42px;

  h2 {
    color: var(--color-heading);
    font-size: 32px;
    font-weight: 900;
  }

  label {
    display: grid;
    gap: 8px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 900;
  }

  input {
    height: 46px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    color: var(--color-heading);
    padding: 0 13px;
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
    height: 48px;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-weight: 900;

    &:disabled {
      background: var(--color-subtle);
    }
  }

  a {
    color: var(--color-primary);
    font-size: 14px;
    font-weight: 900;
  }
}

.helper-text {
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.6;
}

.form-message {
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 800;
  padding: 11px 12px;
}

.form-message--success {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.form-message--error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

@media (max-width: 820px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 280px;
  }
}
</style>
