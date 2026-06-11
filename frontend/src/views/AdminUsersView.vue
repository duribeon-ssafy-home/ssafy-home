<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminUsers } from '@/api/adminUserApi'

const tableHeaders = ['회원', '이메일', '역할', '상태', '가입일', '관리']

const featureAreas = [
  { label: '회원 목록', status: '연동 완료' },
  { label: '회원 검색', status: '연동 완료' },
  { label: '회원 상세', status: '다음 작업' },
  { label: '상태 변경', status: '다음 작업' },
  { label: '역할 변경', status: '다음 작업' },
]

const roleLabels = {
  BUYER: '일반 사용자',
  AGENT: '중개인',
  ADMIN: '관리자',
}

const statusLabels = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
  BANNED: '정지',
  DELETED: '탈퇴',
}

const statusToneClass = {
  ACTIVE: 'user-status--active',
  INACTIVE: 'user-status--inactive',
  BANNED: 'user-status--danger',
  DELETED: 'user-status--danger',
}

const users = ref([])
const searchKeyword = ref('')
const appliedKeyword = ref('')
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

  return `총 ${users.value.length.toLocaleString('ko-KR')}명`
})

const emptyTitle = computed(() =>
  appliedKeyword.value ? '검색 조건에 맞는 회원이 없습니다.' : '표시할 회원 데이터가 없습니다.',
)

const emptyDescription = computed(() =>
  appliedKeyword.value
    ? '이름, 이메일, 닉네임을 다른 키워드로 다시 검색해보세요.'
    : '회원이 가입하면 이 표에서 계정 상태와 역할을 확인할 수 있습니다.',
)

const hasUsers = computed(() => users.value.length > 0)

onMounted(() => {
  loadUsers()
})

async function loadUsers(keyword = appliedKeyword.value) {
  const normalizedKeyword = keyword.trim()

  isLoading.value = true
  errorMessage.value = ''

  try {
    users.value = await getAdminUsers(normalizedKeyword ? { keyword: normalizedKeyword } : {})
    appliedKeyword.value = normalizedKeyword
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error)
    users.value = []
  } finally {
    hasLoaded.value = true
    isLoading.value = false
  }
}

function submitSearch() {
  loadUsers(searchKeyword.value)
}

function clearSearch() {
  searchKeyword.value = ''
  loadUsers('')
}

function getDisplayName(user) {
  return user.nickname || user.name || '이름 없음'
}

function getRoleLabel(role) {
  return roleLabels[role] || role || '-'
}

function getStatusLabel(status) {
  return statusLabels[status] || status || '-'
}

function getStatusClass(status) {
  return statusToneClass[status] || ''
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
  return error.response?.data?.message || '회원 목록을 불러오지 못했습니다.'
}
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

      <form class="toolbar-panel" aria-label="회원 검색" @submit.prevent="submitSearch">
        <label>
          회원 검색
          <input
            v-model="searchKeyword"
            type="search"
            placeholder="이름, 이메일, 닉네임"
            :disabled="isLoading"
          />
        </label>
        <div class="toolbar-actions">
          <button class="search-button" type="submit" :disabled="isLoading">
            {{ isLoading ? '조회 중' : '검색' }}
          </button>
          <button
            class="reset-button"
            type="button"
            :disabled="isLoading || (!searchKeyword && !appliedKeyword)"
            @click="clearSearch"
          >
            초기화
          </button>
        </div>
      </form>

      <section class="list-layout">
        <div class="table-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Users</p>
              <h2>회원 목록</h2>
            </div>
            <span class="status-chip" :class="{ 'status-chip--error': errorMessage }">
              {{ statusChipLabel }}
            </span>
          </div>

          <p v-if="appliedKeyword && !isLoading" class="result-note">
            <strong>{{ appliedKeyword }}</strong> 검색 결과
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
                      <p>회원 목록을 불러오는 중입니다.</p>
                    </div>
                  </td>
                </tr>

                <tr v-else-if="errorMessage">
                  <td :colspan="tableHeaders.length">
                    <div class="empty-state">
                      <strong>회원 목록 조회에 실패했습니다.</strong>
                      <p>잠시 후 다시 시도하거나 로그인 상태를 확인해주세요.</p>
                      <button type="button" @click="loadUsers()">다시 조회</button>
                    </div>
                  </td>
                </tr>

                <tr v-else-if="!hasUsers">
                  <td :colspan="tableHeaders.length">
                    <div class="empty-state">
                      <strong>{{ emptyTitle }}</strong>
                      <p>{{ emptyDescription }}</p>
                    </div>
                  </td>
                </tr>

                <template v-else>
                  <tr v-for="user in users" :key="user.id">
                    <td>
                      <div class="member-cell">
                        <strong>{{ getDisplayName(user) }}</strong>
                        <span>ID {{ user.id }}</span>
                      </div>
                    </td>
                    <td>{{ user.email || '-' }}</td>
                    <td>
                      <span class="role-badge">{{ getRoleLabel(user.role) }}</span>
                    </td>
                    <td>
                      <span class="user-status" :class="getStatusClass(user.status)">
                        {{ getStatusLabel(user.status) }}
                      </span>
                    </td>
                    <td>{{ formatDate(user.createdAt) }}</td>
                    <td>
                      <button class="manage-button" type="button" disabled>상세 준비 중</button>
                    </td>
                  </tr>
                </template>
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
            <li v-for="area in featureAreas" :key="area.label">
              <span aria-hidden="true"></span>
              <div>
                <strong>{{ area.label }}</strong>
                <small>{{ area.status }}</small>
              </div>
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
    background: var(--color-surface);
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
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.search-button,
.reset-button,
.manage-button,
.empty-state button {
  min-height: 44px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  padding: 0 15px;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast);
}

.search-button {
  background: var(--color-primary);
  color: var(--color-surface);

  &:hover:not(:disabled) {
    background: var(--color-primary-dark);
    transform: translateY(-1px);
  }
}

.reset-button,
.manage-button,
.empty-state button {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-heading);

  &:hover:not(:disabled) {
    border-color: var(--color-primary);
    transform: translateY(-1px);
  }
}

.search-button:disabled,
.reset-button:disabled,
.manage-button:disabled {
  background: var(--color-surface-muted);
  color: var(--color-subtle);
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
    min-width: 780px;
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

.member-cell {
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
  }
}

.role-badge,
.user-status {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 900;
  padding: 7px 9px;
  white-space: nowrap;
}

.role-badge {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.user-status {
  background: var(--color-surface-muted);
  color: var(--color-muted);
}

.user-status--active {
  background: #ecfdf3;
  color: #027a48;
}

.user-status--inactive {
  background: var(--color-accent-soft);
  color: #7a5f31;
}

.user-status--danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.manage-button {
  min-height: 36px;
  font-size: 12px;
  padding: 0 11px;
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
    align-items: start;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface-muted);
    padding: 13px;
  }

  span {
    width: 10px;
    height: 10px;
    margin-top: 5px;
    border-radius: 50%;
    background: var(--color-primary);
  }

  div {
    display: grid;
    gap: 3px;
  }

  strong {
    color: var(--color-heading);
    font-weight: 900;
  }

  small {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
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

  .toolbar-actions {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
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
