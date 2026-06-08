<script setup>
import { RouterLink } from 'vue-router'
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

      <form class="auth-form">
        <p class="eyebrow">Create Account</p>
        <h2>회원가입</h2>
        <label>
          이름
          <input type="text" placeholder="이름" />
        </label>
        <label>
          이메일
          <input type="email" placeholder="you@example.com" />
        </label>
        <label>
          비밀번호
          <input type="password" placeholder="비밀번호" />
        </label>
        <label>
          역할
          <select>
            <option>일반 사용자</option>
            <option>중개인</option>
          </select>
        </label>
        <button type="button">회원가입</button>
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
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
}

.auth-visual {
  min-height: 620px;
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
  }

  button {
    height: 48px;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-weight: 900;
  }

  a {
    color: var(--color-primary);
    font-size: 14px;
    font-weight: 900;
  }
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
