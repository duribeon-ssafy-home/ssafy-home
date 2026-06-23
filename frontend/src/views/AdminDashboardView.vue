<script setup>
const adminModules = [
  {
    title: '회원 관리',
    description: '회원 목록, 검색, 상세 확인, 상태와 역할 변경 작업을 관리합니다.',
    routeName: 'admin-users',
    label: '회원 관리로 이동',
  },
  {
    title: '신고 관리',
    description: '매물 신고 목록과 상세 내용을 확인하고 처리 상태를 업데이트합니다.',
    routeName: 'admin-reports',
    label: '신고 관리로 이동',
  },
]

const operationGuides = [
  {
    title: '회원 운영',
    description: '계정 상태를 관리하고 일반 사용자와 중개인 역할을 조정합니다.',
  },
  {
    title: '신고 검토',
    description: '접수된 신고 내용을 확인하고 운영 검토 상태를 기록합니다.',
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
          <strong>{{ module.label }}</strong>
        </RouterLink>
      </section>

      <section class="dashboard-layout">
        <article class="readiness-panel">
          <div class="panel-heading">
            <p class="eyebrow">Operations</p>
            <h2>운영 기준</h2>
          </div>

          <div class="readiness-list">
            <div v-for="item in operationGuides" :key="item.title" class="readiness-item">
              <span aria-hidden="true"></span>
              <div>
                <p>{{ item.title }}</p>
                <small>{{ item.description }}</small>
              </div>
            </div>
          </div>
        </article>

        <article class="empty-state" role="status">
          <span class="empty-label">Admin Only</span>
          <h2>운영 작업은 왼쪽 메뉴에서 바로 진행할 수 있습니다.</h2>
          <p>회원 관리와 신고 관리 화면에서 상세 확인, 상태 변경, 역할 변경을 처리하세요.</p>
        </article>
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
  gap: 22px;
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
  gap: 16px;
}

.module-card,
.readiness-panel,
.empty-state {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-card);
}

.module-card {
  display: grid;
  gap: 12px;
  min-height: 178px;
  padding: 24px;
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

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.56fr) minmax(0, 0.44fr);
  gap: 18px;
  align-items: stretch;
}

.readiness-panel,
.empty-state {
  padding: 24px;
}

.panel-heading {
  display: grid;
  gap: 6px;
  margin-bottom: 18px;

  h2 {
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }
}

.readiness-list {
  display: grid;
  gap: 10px;
}

.readiness-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
  padding: 14px;

  span {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--color-accent);
  }

  p {
    color: var(--color-heading);
    font-weight: 900;
  }

  small {
    display: block;
    margin-top: 5px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 700;
    line-height: 1.5;
  }
}

.empty-state {
  min-height: 260px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  text-align: center;

  h2 {
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }

  p {
    max-width: 420px;
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.7;
  }
}

.empty-label {
  border-radius: var(--radius-xs);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 900;
  padding: 7px 9px;
}

@media (max-width: 860px) {
  .module-grid,
  .dashboard-layout {
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

  .module-card,
  .readiness-panel,
  .empty-state {
    padding: 20px;
  }
}
</style>
