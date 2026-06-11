<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminReports } from '@/api/adminReportApi'

const reportStatuses = [
  { label: '전체', value: '' },
  { label: '대기', value: 'PENDING' },
  { label: '주의', value: 'CAUTION' },
  { label: '위험', value: 'DANGER' },
  { label: '숨김', value: 'HIDDEN' },
  { label: '처리 완료', value: 'RESOLVED' },
]
const tableHeaders = ['신고 대상', '사유', '신고자', '처리 상태', '접수일', '관리']

const featureAreas = [
  {
    title: '신고 목록',
    description: '접수된 신고를 전체 또는 처리 상태별로 확인합니다.',
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

const reasonLabels = {
  FAKE_LISTING: '허위 매물',
  PRICE_MISMATCH: '가격 불일치',
  PHOTO_MISMATCH: '사진 불일치',
  NO_CONTACT: '연락 불가',
  FRAUD_SUSPECTED: '사기 의심',
  ETC: '기타',
}

const statusLabels = {
  PENDING: '대기',
  CAUTION: '주의',
  DANGER: '위험',
  HIDDEN: '숨김',
  RESOLVED: '처리 완료',
}

const statusToneClass = {
  PENDING: 'report-status--pending',
  CAUTION: 'report-status--caution',
  DANGER: 'report-status--danger',
  HIDDEN: 'report-status--hidden',
  RESOLVED: 'report-status--resolved',
}

const reports = ref([])
const activeStatus = ref('')
const isLoading = ref(false)
const errorMessage = ref('')
const hasLoaded = ref(false)

const statusChipLabel = computed(() => {
  if (isLoading.value) {
    return '조회 중'
  }

  if (errorMessage.value) {
    return '조회 실패'
  }

  if (!hasLoaded.value) {
    return '조회 대기'
  }

  return `총 ${reports.value.length.toLocaleString('ko-KR')}건`
})

const activeStatusLabel = computed(() => getStatusFilterLabel(activeStatus.value))
const hasReports = computed(() => reports.value.length > 0)
const emptyTitle = computed(() =>
  activeStatus.value ? '해당 상태의 신고가 없습니다.' : '표시할 신고 데이터가 없습니다.',
)
const emptyDescription = computed(() =>
  activeStatus.value
    ? `${activeStatusLabel.value} 상태로 접수된 신고가 없습니다.`
    : '신고가 접수되면 이 표에서 대상 매물, 신고자, 처리 상태를 확인할 수 있습니다.',
)

onMounted(() => {
  loadReports()
})

async function loadReports(status = activeStatus.value) {
  const normalizedStatus = status.trim()

  isLoading.value = true
  errorMessage.value = ''

  try {
    reports.value = await getAdminReports(normalizedStatus ? { status: normalizedStatus } : {})
    activeStatus.value = normalizedStatus
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error)
    reports.value = []
  } finally {
    hasLoaded.value = true
    isLoading.value = false
  }
}

function selectStatus(status) {
  if (isLoading.value || status === activeStatus.value) {
    return
  }

  loadReports(status)
}

function getStatusFilterLabel(status) {
  return reportStatuses.find((item) => item.value === status)?.label || '전체'
}

function getReasonLabel(reason) {
  return reasonLabels[reason] || reason || '-'
}

function getStatusLabel(status) {
  return statusLabels[status] || status || '-'
}

function getStatusClass(status) {
  return statusToneClass[status] || ''
}

function getReporterName(report) {
  return report.reporterNickname || '닉네임 없음'
}

function formatDate(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date)
}

function getApiErrorMessage(error) {
  return error.response?.data?.message || '신고 목록을 불러오지 못했습니다.'
}
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

      <section class="status-row" aria-label="신고 상태 필터">
        <button
          v-for="status in reportStatuses"
          :key="status.value || 'ALL'"
          type="button"
          :class="{ 'status-filter--active': activeStatus === status.value }"
          :disabled="isLoading"
          @click="selectStatus(status.value)"
        >
          {{ status.label }}
        </button>
      </section>

      <section class="reports-layout">
        <div class="table-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Reports</p>
              <h2>신고 목록</h2>
            </div>
            <span class="status-chip" :class="{ 'status-chip--error': errorMessage }">
              {{ statusChipLabel }}
            </span>
          </div>

          <p v-if="activeStatus && !isLoading" class="result-note">
            <strong>{{ activeStatusLabel }}</strong> 상태 신고
          </p>

          <p v-if="errorMessage" class="form-message form-message--error" role="alert">
            {{ errorMessage }}
          </p>

          <div class="table-wrap" :aria-busy="isLoading" role="status">
            <table>
              <thead>
                <tr>
                  <th v-for="header in tableHeaders" :key="header" scope="col">
                    {{ header }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="isLoading">
                  <td :colspan="tableHeaders.length">
                    <div class="loading-state">
                      <span aria-hidden="true"></span>
                      <p>신고 목록을 불러오는 중입니다.</p>
                    </div>
                  </td>
                </tr>

                <tr v-else-if="errorMessage">
                  <td :colspan="tableHeaders.length">
                    <div class="empty-state">
                      <strong>신고 목록 조회에 실패했습니다.</strong>
                      <p>잠시 후 다시 시도하거나 로그인 상태를 확인해주세요.</p>
                      <button type="button" @click="loadReports()">다시 조회</button>
                    </div>
                  </td>
                </tr>

                <tr v-else-if="!hasReports">
                  <td :colspan="tableHeaders.length">
                    <div class="empty-state">
                      <strong>{{ emptyTitle }}</strong>
                      <p>{{ emptyDescription }}</p>
                    </div>
                  </td>
                </tr>

                <template v-else>
                  <tr v-for="report in reports" :key="report.reportId">
                    <td>
                      <div class="target-cell">
                        <strong>{{ report.propertyTitle || '제목 없음' }}</strong>
                        <span>{{ report.propertyAddress || '-' }}</span>
                      </div>
                    </td>
                    <td>
                      <span class="reason-badge">{{ getReasonLabel(report.reason) }}</span>
                    </td>
                    <td>
                      <div class="reporter-cell">
                        <strong>{{ getReporterName(report) }}</strong>
                        <span>{{ report.reporterEmail || '-' }}</span>
                      </div>
                    </td>
                    <td>
                      <span class="report-status" :class="getStatusClass(report.status)">
                        {{ getStatusLabel(report.status) }}
                      </span>
                    </td>
                    <td>{{ formatDate(report.createdAt) }}</td>
                    <td>
                      <button class="manage-button" type="button" disabled>상세 준비 중</button>
                    </td>
                  </tr>
                </template>
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
    transition:
      background-color var(--transition-fast),
      border-color var(--transition-fast),
      color var(--transition-fast),
      transform var(--transition-fast);

    &:hover:not(:disabled) {
      border-color: var(--color-primary);
      color: var(--color-primary-dark);
      transform: translateY(-1px);
    }
  }
}

.status-filter--active {
  border-color: var(--color-primary) !important;
  background: var(--color-primary-soft) !important;
  color: var(--color-primary-dark) !important;
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
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 12px;
  font-weight: 900;
  padding: 7px 9px;
}

.status-chip--error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.result-note {
  margin: -6px 0 14px;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;

  strong {
    color: var(--color-heading);
  }
}

.form-message {
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 14px;
  padding: 11px 12px;
}

.form-message--error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.table-wrap {
  overflow-x: auto;

  table {
    width: 100%;
    min-width: 860px;
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
    border-bottom: 1px solid rgba(229, 231, 235, 0.76);
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 700;
    padding: 14px 12px;
    vertical-align: middle;
  }
}

.loading-state,
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

.empty-state button,
.manage-button {
  min-height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 12px;
  font-weight: 900;
  padding: 0 11px;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);

  &:hover:not(:disabled) {
    border-color: var(--color-primary);
    color: var(--color-primary-dark);
    transform: translateY(-1px);
  }
}

.manage-button:disabled {
  background: var(--color-surface-muted);
  color: var(--color-subtle);
}

.loading-state {
  span {
    width: 38px;
    height: 38px;
    border: 4px solid var(--color-primary-soft);
    border-top-color: var(--color-primary);
    border-radius: 50%;
    animation: spin 800ms linear infinite;
  }
}

.target-cell,
.reporter-cell {
  display: grid;
  gap: 3px;
  min-width: 0;

  strong {
    color: var(--color-heading);
    font-size: 15px;
    font-weight: 900;
    overflow-wrap: anywhere;
  }

  span {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
    overflow-wrap: anywhere;
  }
}

.reason-badge,
.report-status {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 900;
  padding: 7px 9px;
  white-space: nowrap;
}

.reason-badge {
  background: var(--color-surface-muted);
  color: var(--color-muted);
}

.report-status {
  background: var(--color-surface-muted);
  color: var(--color-muted);
}

.report-status--pending {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.report-status--caution {
  background: var(--color-accent-soft);
  color: #7a5f31;
}

.report-status--danger,
.report-status--hidden {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.report-status--resolved {
  background: #ecfdf3;
  color: #027a48;
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

@keyframes spin {
  to {
    transform: rotate(360deg);
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
