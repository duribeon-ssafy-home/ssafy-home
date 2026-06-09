<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import PasswordField from '@/components/PasswordField.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const name = ref('')
const nickname = ref('')
const email = ref('')
const password = ref('')
const phoneNumber = ref('')
const role = ref('BUYER')
const errorMessage = ref('')
const isSubmitting = ref(false)

const isAgent = computed(() => role.value === 'AGENT')

async function submitSignup() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await authStore.signupAndLogin({
      name: name.value,
      nickname: nickname.value,
      email: email.value,
      password: password.value,
      phoneNumber: phoneNumber.value || null,
      role: role.value,
    })
    router.push(resolveRedirect())
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '회원가입에 실패했습니다.'
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
          <h1>나에게 맞는 주거 조건을 저장해두세요</h1>
        </div>
      </div>

      <form class="auth-form" @submit.prevent="submitSignup">
        <p class="eyebrow">Create Account</p>
        <h2>회원가입</h2>
        <label>
          이름
          <input v-model="name" autocomplete="name" required type="text" placeholder="이름" />
        </label>
        <label>
          닉네임
          <input v-model="nickname" required type="text" placeholder="닉네임" />
        </label>
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
        <PasswordField
          id="signup-password"
          v-model="password"
          autocomplete="new-password"
          required
        />
        <label>
          역할
          <select v-model="role">
            <option value="BUYER">일반 사용자</option>
            <option value="AGENT">중개인</option>
          </select>
        </label>
        <label>
          전화번호
          <input
            v-model="phoneNumber"
            autocomplete="tel"
            :required="isAgent"
            type="tel"
            placeholder="010-0000-0000"
          />
        </label>
        <p class="form-note">
          중개인으로 가입하면 전화번호가 필수입니다.
        </p>
        <p v-if="errorMessage" class="form-message form-message--error" role="alert">
          {{ errorMessage }}
        </p>
        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '가입 및 로그인 중...' : '회원가입' }}
        </button>
        <RouterLink :to="{ name: 'login' }">로그인으로 이동</RouterLink>
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
  align-items: start;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.auth-visual {
  width: 100%;
  height: 780px;
  min-height: 780px;
  display: grid;
  align-items: end;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.1) 0%, rgba(15, 23, 42, 0.62) 100%),
    url('https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80')
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
  gap: 14px;
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

  input,
  select {
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

.form-note {
  margin-top: -4px;
  color: var(--color-subtle);
  font-size: 12px;
  font-weight: 800;
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
    height: 280px;
    min-height: 280px;
  }
}
</style>
