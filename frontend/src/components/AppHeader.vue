<script setup>
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const roleLabels = {
  BUYER: '일반 사용자',
  AGENT: '중개인',
  ADMIN: '관리자',
}

const displayName = computed(() => authStore.user?.nickname || authStore.user?.email || '사용자')
const roleLabel = computed(() => roleLabels[authStore.role] || authStore.role || '사용자')

async function handleAuthAction() {
  await authStore.logout()
  await router.push({ name: 'home' })
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <RouterLink class="brand" :to="{ name: 'home' }" aria-label="SSAFY HOME 메인으로 이동">
        <span>SSAFY</span>
        <strong>HOME</strong>
      </RouterLink>

      <nav class="nav" aria-label="주요 메뉴">
        <RouterLink :to="{ name: 'home' }">매물 찾기</RouterLink>
        <RouterLink :to="{ name: 'survey' }">라이프스타일 추천</RouterLink>
        <RouterLink :to="{ name: 'favorites' }">찜 목록</RouterLink>
      </nav>

      <div class="actions" aria-label="사용자 메뉴">
        <div v-if="authStore.isAuthenticated" class="user-menu">
          <div class="user-summary">
            <p>
              <strong>{{ displayName }}</strong>
              <span> 님 환영합니다!</span>
            </p>
            <span class="role-badge">{{ roleLabel }}</span>
          </div>
          <button class="auth-button" type="button" @click="handleAuthAction">
            로그아웃
          </button>
        </div>

        <div v-else class="auth-links">
          <RouterLink class="auth-button auth-button--ghost" :to="{ name: 'login' }">로그인</RouterLink>
          <RouterLink class="auth-button auth-button--primary" :to="{ name: 'signup' }">
            회원가입
          </RouterLink>
        </div>
      </div>
    </div>
  </header>
</template>

<style lang="scss" scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: var(--header-height);
  border-bottom: 1px solid rgba(229, 231, 235, 0.78);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(18px);
}

.app-header__inner {
  width: min(100% - 40px, var(--content-width));
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
}

.brand {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  color: var(--color-heading);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0;
  white-space: nowrap;

  span {
    font-weight: 800;
  }

  strong {
    color: var(--color-primary);
    font-weight: 900;
  }
}

.nav {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;

  a {
    border-radius: var(--radius-sm);
    color: var(--color-muted);
    font-size: 14px;
    font-weight: 700;
    padding: 10px 13px;
    transition:
      background-color var(--transition-fast),
      color var(--transition-fast);
    white-space: nowrap;
  }

  a:hover,
  a.router-link-active {
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
  }
}

.actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 190px;
}

.auth-links,
.user-menu {
  display: flex;
  align-items: center;
}

.auth-links {
  gap: 8px;
}

.user-menu {
  gap: 14px;
}

.user-summary {
  display: grid;
  justify-items: end;
  gap: 3px;
  min-width: 142px;

  p {
    max-width: 190px;
    overflow: hidden;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 900;
  }

  .role-badge {
    width: fit-content;
    border-radius: var(--radius-xs);
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
    font-size: 11px;
    font-weight: 900;
    padding: 2px 7px;
  }
}

.auth-button {
  min-width: 74px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 14px;
  font-weight: 800;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    transform var(--transition-fast);

  &:hover {
    border-color: var(--color-primary);
    box-shadow: 0 8px 18px rgba(54, 95, 145, 0.14);
    transform: translateY(-1px);
  }
}

.auth-button--primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-surface);
}

.auth-button--ghost {
  background: var(--color-surface);
}

@media (max-width: 760px) {
  .app-header {
    height: auto;
  }

  .app-header__inner {
    width: min(100% - 28px, var(--content-width));
    min-height: var(--header-height);
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px 0;
  }

  .nav {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .actions {
    min-width: 0;
  }

  .user-summary {
    min-width: 0;
    justify-items: start;

    p {
      max-width: 120px;
    }
  }
}
</style>
