<script setup>
const tableHeaders = ['회원', '이메일', '역할', '상태', '가입일', '관리']

const featureAreas = [
  '회원 목록',
  '회원 검색',
  '회원 상세',
  '상태 변경',
  '역할 변경',
]
</script>

<template>
  <main class="page admin-list-page">
    <section class="section-container admin-list-page__inner">
      <div class="page-heading">
        <div>
          <p class="eyebrow">Admin Users</p>
          <h1>회원 관리</h1>
          <p>회원 조회, 검색, 상세 확인, 상태 변경, 역할 변경 기능을 연결할 관리자 화면입니다.</p>
        </div>
        <RouterLink class="back-link" :to="{ name: 'admin-dashboard' }">대시보드로 이동</RouterLink>
      </div>

      <section class="toolbar-panel" aria-label="회원 검색 준비 영역">
        <label>
          회원 검색
          <input type="search" placeholder="이름, 이메일, 닉네임" disabled />
        </label>
        <button type="button" disabled>검색 준비 중</button>
      </section>

      <section class="list-layout">
        <div class="table-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Users</p>
              <h2>회원 목록</h2>
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
                      <strong>표시할 회원 데이터가 아직 없습니다.</strong>
                      <p>관리자 회원 API를 연결하면 검색 결과와 페이지네이션이 이 표에 표시됩니다.</p>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <aside class="scope-panel" aria-label="회원 관리 후속 범위">
          <div class="panel-heading">
            <p class="eyebrow">Next Scope</p>
            <h2>후속 기능</h2>
          </div>

          <ul>
            <li v-for="area in featureAreas" :key="area">
              <span aria-hidden="true"></span>
              {{ area }}
            </li>
          </ul>
        </aside>
      </section>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.admin-list-page {
  min-height: calc(100vh - var(--header-height));
  padding: 48px 0 76px;
  background:
    linear-gradient(135deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0.56) 54%),
    var(--color-bg);
}

.admin-list-page__inner {
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

.toolbar-panel,
.table-panel,
.scope-panel {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-card);
}

.toolbar-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
  padding: 18px;

  label {
    display: grid;
    gap: 8px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 900;
  }

  input {
    width: 100%;
    height: 44px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface-muted);
    color: var(--color-muted);
    padding: 0 13px;
  }

  button {
    min-height: 44px;
    border: 0;
    border-radius: var(--radius-sm);
    background: var(--color-subtle);
    color: var(--color-surface);
    font-weight: 900;
    padding: 0 15px;
  }
}

.list-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.28fr);
  gap: 18px;
  align-items: start;
}

.table-panel,
.scope-panel {
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

.scope-panel {
  position: sticky;
  top: calc(var(--header-height) + 18px);

  ul {
    display: grid;
    gap: 10px;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  li {
    display: grid;
    grid-template-columns: 10px minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface-muted);
    color: var(--color-heading);
    font-weight: 900;
    padding: 13px;
  }

  span {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--color-primary);
  }
}

@media (max-width: 920px) {
  .list-layout {
    grid-template-columns: 1fr;
  }

  .scope-panel {
    position: static;
  }
}

@media (max-width: 680px) {
  .page-heading,
  .panel-heading,
  .toolbar-panel {
    align-items: flex-start;
    grid-template-columns: 1fr;
  }

  .page-heading {
    flex-direction: column;

    h1 {
      font-size: 34px;
    }
  }

  .table-panel,
  .scope-panel {
    padding: 20px;
  }
}
</style>
