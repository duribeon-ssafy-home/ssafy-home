<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getProperty, getPropertyRisk } from '@/api/propertyApi'
import { createPropertyReport } from '@/api/reportApi'
import { roomTypeLabels } from '@/data/mockProperties'
import { reportReasons } from '@/data/reportReasons'
import { useFavorites } from '@/composables/useFavorites'
import { useAuthStore } from '@/stores/auth'
import { useCompareStore } from '@/stores/compare'
const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})

const router = useRouter()
const authStore = useAuthStore()
const { loadFavorites, toggleFavorite: toggle, isFavorited } = useFavorites()
const compareStore = useCompareStore()

const property = ref(null)
const risk = ref(null)
const isLoading = ref(false)
const isError = ref(false)
const isReportModalOpen = ref(false)
const reportReason = ref(reportReasons[0]?.value || '')
const reportContent = ref('')
const isSubmittingReport = ref(false)
const reportMessage = ref('')
const reportErrorMessage = ref('')

const riskMeta = {
  SAFE:    { label: '안전', class: 'risk--safe' },
  CAUTION: { label: '주의', class: 'risk--caution' },
  DANGER:  { label: '위험', class: 'risk--danger' },
  UNKNOWN: { label: '분석 불가', class: 'risk--unknown' },
}
const isToggling = ref(false)

const isFavorite = computed(() => property.value ? isFavorited(property.value.propertyId) : false)

async function fetchProperty() {
  isLoading.value = true
  isError.value = false
  try {
    const [propertyData, riskData] = await Promise.allSettled([
      getProperty(props.id),
      getPropertyRisk(props.id),
    ])
    if (propertyData.status === 'fulfilled') property.value = propertyData.value
    else isError.value = true
    if (riskData.status === 'fulfilled') risk.value = riskData.value
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchProperty()
  loadFavorites()
})
watch(() => props.id, fetchProperty)

async function handleFavorite() {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (isToggling.value || !property.value) return
  isToggling.value = true
  try {
    await toggle(property.value.propertyId)
  } finally {
    isToggling.value = false
  }
}

function openReportModal() {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }

  reportReason.value = reportReasons[0]?.value || ''
  reportContent.value = ''
  reportMessage.value = ''
  reportErrorMessage.value = ''
  isReportModalOpen.value = true
}

function closeReportModal() {
  if (isSubmittingReport.value) return
  isReportModalOpen.value = false
}

async function submitReport() {
  if (!property.value || isSubmittingReport.value || !reportReason.value) {
    return
  }

  isSubmittingReport.value = true
  reportMessage.value = ''
  reportErrorMessage.value = ''

  try {
    await createPropertyReport(property.value.propertyId, {
      reason: reportReason.value,
      content: reportContent.value.trim() || null,
    })
    reportMessage.value = '신고가 접수되었습니다.'
    isReportModalOpen.value = false
    await refreshRisk()
  } catch (error) {
    reportErrorMessage.value = getReportErrorMessage(error)
  } finally {
    isSubmittingReport.value = false
  }
}

async function refreshRisk() {
  try {
    risk.value = await getPropertyRisk(props.id)
  } catch {
    // 신고 접수 자체는 완료되었으므로 위험도 재조회 실패는 화면 흐름을 막지 않는다.
  }
}

const imageUrl = computed(() => property.value?.images?.[0]?.imageUrl)
const priceLabel = computed(() => {
  if (!property.value) return ''
  if (property.value.rentType === 'JEONSE') {
    return `전세 ${formatMoneyManwon(property.value.deposit)}`
  }
  return `월세 ${formatMoneyManwon(property.value.monthlyRent)} / 보증금 ${formatMoneyManwon(
    property.value.deposit,
  )}`
})

function formatMoneyManwon(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? `${amount.toLocaleString('ko-KR')}만` : '-'
}

function getReportErrorMessage(error) {
  const message = error.response?.data?.message
  const errorCode = error.response?.data?.errorCode

  if (errorCode === 'REPORT_ALREADY_EXISTS') {
    return '이미 신고한 매물입니다.'
  }

  return message || '신고 접수에 실패했습니다. 잠시 후 다시 시도해 주세요.'
}
</script>

<template>
  <main class="page detail-page">
    <section class="section-container">
      <RouterLink class="back-link" :to="{ name: 'home' }">매물 찾기로 돌아가기</RouterLink>

      <div v-if="isLoading" class="placeholder">
        <strong>매물 정보를 불러오는 중입니다...</strong>
      </div>

      <div v-else-if="isError" class="placeholder">
        <strong>매물을 불러올 수 없습니다</strong>
        <p>잠시 후 다시 시도해 주세요.</p>
      </div>

      <div v-else-if="property" class="detail-layout">
        <div class="gallery-shell">
          <img v-if="imageUrl" :src="imageUrl" :alt="property.title" />
          <div v-else class="gallery-placeholder">
            <span>등록된 이미지가 없습니다</span>
          </div>
        </div>

        <aside class="info-panel">
          <p class="eyebrow">Property Detail</p>
          <h1>{{ property.title }}</h1>
          <strong>{{ priceLabel }}</strong>
          <p>{{ property.roadAddress }}</p>

          <dl>
            <div>
              <dt>방 타입</dt>
              <dd>{{ roomTypeLabels[property.roomType] }}</dd>
            </div>
            <div>
              <dt>면적</dt>
              <dd>{{ Number(property.area).toFixed(1) }}m2</dd>
            </div>
            <div>
              <dt>층수</dt>
              <dd>{{ property.floor }}층</dd>
            </div>
            <div>
              <dt>연식</dt>
              <dd>{{ property.buildYear }}년</dd>
            </div>
          </dl>

          <div v-if="risk" class="risk-panel" :class="riskMeta[risk.label]?.class">
            <div class="risk-header">
              <span class="risk-badge">{{ riskMeta[risk.label]?.label }}</span>
              <span v-if="risk.label !== 'UNKNOWN'" class="risk-score">위험 점수 {{ risk.score }}점</span>
            </div>
            <dl class="risk-detail">
              <template v-if="property.rentType === 'JEONSE'">
                <div v-if="risk.marketPriceAvg">
                  <dt>주변 전세금 평균</dt>
                  <dd>{{ Number(risk.marketPriceAvg).toLocaleString('ko-KR') }}만원</dd>
                </div>
              </template>
              <template v-else>
                <div v-if="risk.avgMonthlyRent != null">
                  <dt>주변 평균 월세</dt>
                  <dd>{{ Number(risk.avgMonthlyRent).toLocaleString('ko-KR') }}만원</dd>
                </div>
                <div v-if="risk.avgDeposit != null">
                  <dt>주변 평균 보증금</dt>
                  <dd>{{ Number(risk.avgDeposit).toLocaleString('ko-KR') }}만원</dd>
                </div>
              </template>
              <div v-if="risk.priceGapRate != null">
                <dt>시세 대비 차이</dt>
                <dd>{{ risk.priceGapRate > 0 ? '+' : '' }}{{ risk.priceGapRate.toFixed(1) }}%</dd>
              </div>
              <div>
                <dt>신고 건수</dt>
                <dd>{{ risk.reportCount }}건</dd>
              </div>
              <div>
                <dt>소유자 확인</dt>
                <dd>{{ risk.ownerVerified ? '확인됨' : '미확인' }}</dd>
              </div>
            </dl>
            <p v-if="risk.label === 'UNKNOWN'" class="risk-notice">
              비교 가능한 주변 매물이 부족해 분석이 어렵습니다.
            </p>
          </div>

          <div class="panel-actions">
            <button
              type="button"
              :class="{ active: isFavorite }"
              :disabled="isToggling"
              @click="handleFavorite"
            >
              {{ isFavorite ? '♥ 찜 해제' : '♡ 찜하기' }}
            </button>
            <button
              class="ghost"
              type="button"
              :class="{ 'compare-active': compareStore.has(property.propertyId) }"
              :disabled="compareStore.isFull && !compareStore.has(property.propertyId)"
              @click="compareStore.toggle(property)"
            >
              {{ compareStore.has(property.propertyId) ? '✓ 비교함에서 제거' : '+ 비교 추가' }}
            </button>
            <button
              class="danger-ghost"
              type="button"
              @click="openReportModal"
            >
              의심 매물 신고
            </button>
          </div>
          <p v-if="reportMessage" class="report-feedback report-feedback--success">
            {{ reportMessage }}
          </p>
          <p v-if="reportErrorMessage && !isReportModalOpen" class="report-feedback report-feedback--error">
            {{ reportErrorMessage }}
          </p>
        </aside>
      </div>

      <div v-else class="placeholder">
        <strong>존재하지 않는 매물입니다</strong>
        <p>목록에서 다른 매물을 선택해 주세요.</p>
      </div>

      <div
        v-if="isReportModalOpen"
        class="modal-backdrop"
        role="presentation"
        @click.self="closeReportModal"
      >
        <section class="report-modal" role="dialog" aria-modal="true" aria-labelledby="report-modal-title">
          <div class="report-modal__header">
            <div>
              <p class="eyebrow">Report</p>
              <h2 id="report-modal-title">의심 매물 신고</h2>
            </div>
            <button
              class="modal-close"
              type="button"
              aria-label="신고 모달 닫기"
              :disabled="isSubmittingReport"
              @click="closeReportModal"
            >
              ×
            </button>
          </div>

          <form class="report-form" @submit.prevent="submitReport">
            <fieldset>
              <legend>신고 사유</legend>
              <label
                v-for="reason in reportReasons"
                :key="reason.value"
                class="reason-option"
                :class="{ 'reason-option--active': reportReason === reason.value }"
              >
                <input v-model="reportReason" type="radio" name="reportReason" :value="reason.value" />
                <span>{{ reason.label }}</span>
              </label>
            </fieldset>

            <label class="content-field">
              <span>상세 내용</span>
              <textarea
                v-model="reportContent"
                maxlength="2000"
                rows="6"
                placeholder="의심되는 내용을 구체적으로 적어주세요."
              />
            </label>
            <p class="content-count">{{ reportContent.length.toLocaleString('ko-KR') }} / 2,000</p>
            <p v-if="reportErrorMessage" class="report-feedback report-feedback--error">
              {{ reportErrorMessage }}
            </p>

            <div class="modal-actions">
              <button type="button" class="ghost" :disabled="isSubmittingReport" @click="closeReportModal">
                취소
              </button>
              <button type="submit" :disabled="isSubmittingReport || !reportReason">
                {{ isSubmittingReport ? '접수 중...' : '신고 접수' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.detail-page {
  padding: 44px 0 76px;
}

.back-link {
  display: inline-flex;
  margin-bottom: 18px;
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 900;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.8fr);
  gap: 24px;
  align-items: start;
}

.gallery-shell,
.info-panel,
.placeholder {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.gallery-shell {
  position: relative;
  overflow: hidden;
  aspect-ratio: 16 / 10;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.gallery-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-soft);
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 700;
}

.info-panel {
  display: grid;
  gap: 16px;
  padding: 26px;

  h1 {
    color: var(--color-heading);
    font-size: 26px;
    font-weight: 900;
    line-height: 1.28;
  }

  strong {
    color: var(--color-primary-dark);
    font-size: 24px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-weight: 700;
  }

  dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    padding-top: 8px;
  }

  dt {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }

  dd {
    margin-top: 4px;
    color: var(--color-heading);
    font-weight: 900;
  }
}

.risk-panel {
  border-radius: var(--radius-sm);
  padding: 16px;
  display: grid;
  gap: 12px;
}

.risk--safe    { background: #ecfdf3; }
.risk--caution { background: #fffbeb; }
.risk--danger  { background: #fff1f0; }
.risk--unknown { background: var(--color-surface-muted); }

.risk-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-badge {
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 900;
  padding: 4px 9px;

  .risk--safe &    { background: #027a48; color: #fff; }
  .risk--caution & { background: #b45309; color: #fff; }
  .risk--danger &  { background: var(--color-danger); color: #fff; }
  .risk--unknown & { background: var(--color-muted); color: #fff; }
}

.risk-score {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
}

.risk-detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;

  dt {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }

  dd {
    margin-top: 3px;
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 900;
  }
}

.risk-notice {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.panel-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;

  button {
    height: 46px;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-weight: 900;
  }

  .ghost {
    border: 1px solid var(--color-border);
    background: var(--color-surface);
    color: var(--color-heading);
  }

  .compare-active {
    border-color: var(--color-primary);
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
  }

  .active {
    background: var(--color-danger);
  }

  .danger-ghost {
    grid-column: 1 / -1;
    border: 1px solid #fecaca;
    background: #fff7f7;
    color: var(--color-danger);
  }
}

.report-feedback {
  border-radius: var(--radius-sm);
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 800;
}

.report-feedback--success {
  background: #ecfdf3;
  color: #027a48;
}

.report-feedback--error {
  background: #fff1f0;
  color: var(--color-danger);
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgb(15 23 42 / 58%);
}

.report-modal {
  width: min(560px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: 0 24px 70px rgb(15 23 42 / 28%);
}

.report-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--color-border);
  padding: 22px 24px 18px;

  h2 {
    margin-top: 4px;
    color: var(--color-heading);
    font-size: 22px;
    font-weight: 900;
  }
}

.modal-close {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-xs);
  background: var(--color-bg-soft);
  color: var(--color-heading);
  font-size: 24px;
  font-weight: 700;
}

.report-form {
  display: grid;
  gap: 16px;
  padding: 22px 24px 24px;

  fieldset {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    border: 0;
    padding: 0;
  }

  legend,
  .content-field span {
    grid-column: 1 / -1;
    margin-bottom: 4px;
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 900;
  }
}

.reason-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  color: var(--color-heading);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;

  input {
    accent-color: var(--color-primary);
  }
}

.reason-option--active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.content-field {
  display: grid;
  gap: 8px;

  textarea {
    width: 100%;
    resize: vertical;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    padding: 12px;
    color: var(--color-heading);
    font: inherit;
    line-height: 1.5;
  }
}

.content-count {
  justify-self: end;
  margin-top: -8px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 800;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;

  button {
    min-width: 104px;
    height: 44px;
    border-radius: var(--radius-sm);
    background: var(--color-primary);
    color: var(--color-surface);
    font-weight: 900;
  }

  .ghost {
    border: 1px solid var(--color-border);
    background: var(--color-surface);
    color: var(--color-heading);
  }
}

.placeholder {
  display: grid;
  gap: 8px;
  place-items: center;
  min-height: 280px;
  color: var(--color-muted);
  text-align: center;

  strong {
    color: var(--color-heading);
    font-size: 20px;
    font-weight: 900;
  }
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .report-form fieldset {
    grid-template-columns: 1fr;
  }
}
</style>
