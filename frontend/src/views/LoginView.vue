<script setup>
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PasswordField from '@/components/PasswordField.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)

async function submitLogin() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await authStore.loginWithPassword({
      email: email.value,
      password: password.value,
    })
    router.push(resolveRedirect())
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '로그인에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

function resolveRedirect() {
  const redirect = route.query.redirect

  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }

  return { name: 'home' }
}
</script>

<template>
  <main class="page auth-page">
    <section class="auth-shell">
      <div class="auth-visual">
        <div>
          <p>SSAFY HOME</p>
          <h1>내 조건에 맞는 집을 더 빠르게 찾으세요</h1>
        </div>
      </div>

      <form class="auth-form" @submit.prevent="submitLogin">
        <p class="eyebrow">Welcome Back</p>
        <h2>로그인</h2>
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
        <PasswordField id="login-password" v-model="password" required />
        <p v-if="errorMessage" class="form-message form-message--error" role="alert">
          {{ errorMessage }}
        </p>
        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '로그인 중...' : '로그인' }}
        </button>
        <RouterLink :to="{ name: 'signup' }">회원가입으로 이동</RouterLink>
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
    url('https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80')
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

.form-message {
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 800;
  padding: 11px 12px;
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
