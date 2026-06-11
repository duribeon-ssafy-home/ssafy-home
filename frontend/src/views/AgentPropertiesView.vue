<script setup>
const summaryItems = [
  {
    label: '등록 매물',
    value: '연동 대기',
    caption: '내 매물 목록이 연결될 영역',
  },
  {
    label: '검토 필요',
    value: '준비 중',
    caption: '가격, 상태, 노출 정보를 점검할 영역',
  },
  {
    label: '이미지 관리',
    value: '준비 중',
    caption: '대표 이미지와 정렬을 관리할 영역',
  },
]

const featureAreas = [
  {
    title: '내 매물 목록',
    description: '등록한 매물의 주소, 거래 유형, 가격, 공개 상태를 한 화면에서 확인합니다.',
  },
  {
    title: '매물 등록',
    description: '주소, 가격, 관리비, 상세 설명을 입력하는 등록 폼이 들어올 예정입니다.',
  },
  {
    title: '매물 수정/삭제',
    description: '매물 상태와 상세 정보를 변경하고 필요 시 삭제 처리하는 액션을 배치합니다.',
  },
  {
    title: '이미지 관리',
    description: '이미지 업로드, 대표 이미지 선택, 노출 순서 변경 기능을 연결합니다.',
  },
]

const tableHeaders = ['매물명', '위치', '가격', '상태', '최근 수정']
</script>

<template>
  <main class="page operations-page">
    <section class="section-container operations-page__inner">
      <div class="page-heading">
        <div>
          <p class="eyebrow">Agent Workspace</p>
          <h1>내 매물 관리</h1>
          <p>중개인이 등록한 매물의 목록, 등록, 수정, 이미지 관리를 이어 붙일 관리 화면입니다.</p>
        </div>
        <span class="status-chip">현재 준비 중</span>
      </div>

      <section class="summary-grid" aria-label="내 매물 관리 요약">
        <article v-for="item in summaryItems" :key="item.label" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.caption }}</p>
        </article>
      </section>

      <section class="workspace-layout">
        <div class="table-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Properties</p>
              <h2>내 매물 목록</h2>
            </div>
            <button class="primary-button" type="button" disabled>매물 등록 준비 중</button>
          </div>

          <div class="empty-table" role="status">
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
                      <strong>연동할 매물 데이터가 아직 없습니다.</strong>
                      <p>후속 작업에서 내 매물 API를 연결하면 이 영역에 목록과 페이지네이션이 표시됩니다.</p>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <aside class="feature-panel" aria-label="후속 기능 영역">
          <div class="panel-heading">
            <p class="eyebrow">Next Scope</p>
            <h2>후속 기능 영역</h2>
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
.operations-page {
  min-height: calc(100vh - var(--header-height));
  padding: 48px 0 76px;
  background:
    linear-gradient(135deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0.56) 54%),
    var(--color-bg);
}

.operations-page__inner {
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.summary-card,
.table-panel,
.feature-panel {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-card);
}

.summary-card {
  display: grid;
  gap: 8px;
  padding: 18px;

  span {
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 900;
  }

  strong {
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 700;
    line-height: 1.6;
  }
}

.workspace-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.34fr);
  gap: 18px;
  align-items: start;
}

.table-panel,
.feature-panel {
  padding: 24px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h2 {
    margin-top: 6px;
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }
}

.primary-button {
  min-height: 42px;
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--color-subtle);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 900;
  padding: 0 15px;
}

.empty-table {
  overflow-x: auto;

  table {
    width: 100%;
    min-width: 620px;
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
  min-height: 220px;
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
    max-width: 440px;
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
    background: var(--color-primary);
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
  .summary-grid,
  .workspace-layout {
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
  }

  .page-heading h1 {
    font-size: 34px;
  }

  .table-panel,
  .feature-panel {
    padding: 20px;
  }
}
</style>
