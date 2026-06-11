<script setup>
const reportStatuses = ['대기', '검토 중', '처리 완료']
const tableHeaders = ['신고 대상', '사유', '신고자', '처리 상태', '접수일', '관리']

const featureAreas = [
  {
    title: '신고 목록',
    description: '접수된 신고를 상태별로 확인하고 우선순위를 판단합니다.',
  },
  {
    title: '신고 상세',
    description: '신고 사유, 대상 매물, 작성자 정보를 한 화면에서 확인합니다.',
  },
  {
    title: '처리 상태 변경',
    description: '검토 중, 반려, 처리 완료 같은 운영 상태 변경 액션을 연결합니다.',
  },
]
</script>

<template>
  <main class="page admin-reports-page">
    <section class="section-container admin-reports-page__inner">
      <div class="page-heading">
        <div>
          <p class="eyebrow">Admin Reports</p>
          <h1>신고 관리</h1>
          <p>매물 신고를 확인하고 처리 상태를 관리하기 위한 관리자 전용 화면입니다.</p>
        </div>
        <RouterLink class="back-link" :to="{ name: 'admin-dashboard' }">대시보드로 이동</RouterLink>
      </div>

      <section class="status-row" aria-label="신고 상태 필터 준비 영역">
        <button v-for="status in reportStatuses" :key="status" type="button" disabled>
          {{ status }}
        </button>
      </section>

      <section class="reports-layout">
        <div class="table-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Reports</p>
              <h2>신고 목록</h2>
            </div>
            <span class="status-chip">API 연동 대기</span>
          </div>

          <div class="table-wrap" role="status">
            <table>
              <thead>
                <tr>
                  <th v-for="header in tableHeaders" :key="header" scope="col">
                    {{ header }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td :colspan="tableHeaders.length">
                    <div class="empty-state">
                      <strong>표시할 신고 데이터가 아직 없습니다.</strong>
                      <p>관리자 신고 API를 연결하면 신고 목록과 처리 액션이 이 표에 표시됩니다.</p>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <aside class="feature-panel" aria-label="신고 관리 후속 기능">
          <div class="panel-heading">
            <p class="eyebrow">Next Scope</p>
            <h2>후속 기능</h2>
          </div>

          <div class="feature-list">
            <article v-for="area in featureAreas" :key="area.title" class="feature-card">
              <span aria-hidden="true"></span>
              <div>
                <h3>{{ area.title }}</h3>
                <p>{{ area.description }}</p>
              </div>
            </article>
          </div>
        </aside>
      </section>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.admin-reports-page {
  min-height: calc(100vh - var(--header-height));
  padding: 48px 0 76px;
  background:
    linear-gradient(135deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0.56) 54%),
    var(--color-bg);
}

.admin-reports-page__inner {
  display: grid;
  gap: 20px;
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

.back-link {
  flex: 0 0 auto;
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 14px;
  font-weight: 900;
  padding: 0 14px;
  transition:
    border-color var(--transition-fast),
    transform var(--transition-fast);

  &:hover {
    border-color: var(--color-primary);
    transform: translateY(-1px);
  }
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  button {
    min-height: 40px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 900;
    padding: 0 13px;
  }
}

.reports-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.34fr);
  gap: 18px;
  align-items: start;
}

.table-panel,
.feature-panel {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-card);
  padding: 24px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;

  h2 {
    margin-top: 6px;
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }
}

.status-chip {
  flex: 0 0 auto;
  border-radius: var(--radius-xs);
  background: var(--color-accent-soft);
  color: #6f5f45;
  font-size: 12px;
  font-weight: 900;
  padding: 7px 9px;
}

.table-wrap {
  overflow-x: auto;

  table {
    width: 100%;
    min-width: 720px;
    border-collapse: collapse;
  }

  th {
    border-bottom: 1px solid var(--color-border);
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 900;
    padding: 13px 12px;
    text-align: left;
  }

  td {
    padding: 0;
  }
}

.empty-state {
  min-height: 240px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--color-muted);
  text-align: center;

  strong {
    color: var(--color-heading);
    font-size: 20px;
    font-weight: 900;
  }

  p {
    max-width: 430px;
    font-weight: 700;
    line-height: 1.7;
  }
}

.feature-panel {
  position: sticky;
  top: calc(var(--header-height) + 18px);
}

.feature-list {
  display: grid;
  gap: 12px;
}

.feature-card {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
  padding: 15px;

  span {
    width: 10px;
    height: 10px;
    margin-top: 6px;
    border-radius: 50%;
    background: var(--color-danger);
  }

  h3 {
    color: var(--color-heading);
    font-size: 16px;
    font-weight: 900;
  }

  p {
    margin-top: 6px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 700;
    line-height: 1.6;
  }
}

@media (max-width: 980px) {
  .reports-layout {
    grid-template-columns: 1fr;
  }

  .feature-panel {
    position: static;
  }
}

@media (max-width: 680px) {
  .page-heading,
  .panel-heading {
    align-items: flex-start;
    flex-direction: column;

    h1 {
      font-size: 34px;
    }
  }

  .table-panel,
  .feature-panel {
    padding: 20px;
  }
}
</style>
