<script setup>
const adminModules = [
  {
    title: '회원 관리',
    description: '가입 회원을 확인하고 계정 상태와 역할을 관리합니다.',
    routeName: 'admin-users',
    label: '회원 관리로 이동',
    tasks: ['회원 목록 조회', '계정 상태 변경', '역할 변경'],
  },
  {
    title: '신고 관리',
    description: '접수된 매물 신고를 검토하고 처리 상태를 기록합니다.',
    routeName: 'admin-reports',
    label: '신고 관리로 이동',
    tasks: ['신고 목록 확인', '신고 상세 검토', '처리 상태 변경'],
  },
]
</script>

<template>
  <main class="page admin-page">
    <section class="section-container admin-page__inner">
      <div class="page-heading">
        <div>
          <p class="eyebrow">Admin Console</p>
          <h1>관리자 대시보드</h1>
          <p>회원과 신고 운영 업무를 한 곳에서 시작하는 관리자 전용 화면입니다.</p>
        </div>
        <span class="status-chip">ADMIN 전용</span>
      </div>

      <section class="module-grid" aria-label="관리자 기능">
        <RouterLink
          v-for="module in adminModules"
          :key="module.routeName"
          class="module-card"
          :to="{ name: module.routeName }"
        >
          <span>{{ module.title }}</span>
          <p>{{ module.description }}</p>
          <ul>
            <li v-for="task in module.tasks" :key="task">{{ task }}</li>
          </ul>
          <strong>{{ module.label }}</strong>
        </RouterLink>
      </section>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.admin-page {
  min-height: calc(100vh - var(--header-height));
  padding: 48px 0 76px;
  background:
    linear-gradient(135deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0.56) 54%),
    var(--color-bg);
}

.admin-page__inner {
  display: grid;
  gap: 28px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;

  h1 {
    margin-top: 8px;
    color: var(--color-heading);
    font-size: 40px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.18;
  }

  p:not(.eyebrow) {
    max-width: 720px;
    margin-top: 10px;
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.7;
  }
}

.status-chip {
  flex: 0 0 auto;
  border: 1px solid rgba(54, 95, 145, 0.22);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 13px;
  font-weight: 900;
  padding: 10px 12px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.module-card {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-card);
}

.module-card {
  display: grid;
  gap: 16px;
  min-height: 310px;
  padding: 30px;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    transform var(--transition-fast);

  &:hover {
    border-color: var(--color-primary);
    box-shadow: var(--shadow-card-hover);
    transform: translateY(-2px);
  }

  span {
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.7;
  }

  ul {
    display: grid;
    gap: 10px;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  li {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 800;

    &::before {
      width: 8px;
      height: 8px;
      flex: 0 0 auto;
      border-radius: 50%;
      background: var(--color-primary);
      content: '';
    }
  }

  strong {
    width: fit-content;
    align-self: end;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-size: 14px;
    font-weight: 900;
    padding: 11px 14px;
  }
}

@media (max-width: 860px) {
  .module-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;

    h1 {
      font-size: 34px;
    }
  }

  .module-card {
    min-height: auto;
    padding: 20px;
  }
}
</style>
