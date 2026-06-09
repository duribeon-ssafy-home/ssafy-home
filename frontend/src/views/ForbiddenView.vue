<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="page forbidden-page">
    <section class="section-container">
      <div class="notice-panel">
        <p class="eyebrow">Access Restricted</p>
        <h1>접근 권한이 없습니다</h1>
        <p>
          현재 계정으로는 이 페이지를 볼 수 없습니다. 필요한 권한이 있는 계정으로 로그인하거나
          메인 화면에서 다시 탐색해 주세요.
        </p>
        <div class="notice-actions">
          <RouterLink class="primary-link" :to="{ name: 'home' }">메인으로 이동</RouterLink>
          <RouterLink class="secondary-link" :to="{ name: 'login' }">다른 계정으로 로그인</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.forbidden-page {
  display: grid;
  align-items: center;
  padding: 56px 0 76px;
}

.notice-panel {
  width: min(100%, 640px);
  display: grid;
  gap: 16px;
  margin: 0 auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-panel);
  padding: 42px;
  text-align: center;

  h1 {
    color: var(--color-heading);
    font-size: 34px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.22;
  }

  p:not(.eyebrow) {
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.7;
  }
}

.notice-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 6px;
}

.primary-link,
.secondary-link {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  padding: 0 16px;
  transition:
    border-color var(--transition-fast),
    background-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
  }
}

.primary-link {
  background: var(--color-primary);
  color: var(--color-surface);
}

.secondary-link {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-heading);

  &:hover {
    border-color: var(--color-primary);
  }
}
</style>
