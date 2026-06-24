<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getLifestyleQuestions,
  previewLifestyleResult,
  saveLifestyleResult,
} from '@/api/lifestyleApi'
import { searchLocations } from '@/api/locationApi'
import { fallbackLifestyleQuestions, normalizeLifestyleQuestions } from '@/data/lifestyle'
import { useAuthStore } from '@/stores/auth'
import { resolveLocationParam } from '@/utils/locationFilter'
import {
  createLifestyleAnswerPayload,
  createLifestyleResultSnapshot,
  writeLifestyleResultSnapshot,
} from '@/utils/lifestyleResultSession'

const router = useRouter()
const authStore = useAuthStore()

const questions = ref([])
const currentIndex = ref(0)
const answerMap = ref({})
const selectedAnswer = ref('')
const monthlyRentInput = ref('')
const depositInput = ref('')
const preferredLocationInput = ref('')
const selectedPreferredLocation = ref(null)
const locationSuggestions = ref([])
const isLocationMenuOpen = ref(false)
const isLocationLoading = ref(false)
const locationSearchTimer = ref(null)
const activeLocationIndex = ref(-1)
const locationMenuRef = ref(null)
const isLoading = ref(true)
const isAdvancing = ref(false)
const isSubmittingResult = ref(false)
const noticeMessage = ref('')
const errorMessage = ref('')
let locationRequestId = 0

const currentQuestion = computed(() => questions.value[currentIndex.value])
const isBudgetStep = computed(
  () => questions.value.length > 0 && currentIndex.value >= questions.value.length,
)
const totalSteps = computed(() => (questions.value.length || 6) + 1)
const progress = computed(() => {
  if (!totalSteps.value) {
    return 0
  }

  return (Math.min(currentIndex.value + 1, totalSteps.value) / totalSteps.value) * 100
})
const answeredCount = computed(() => Object.keys(answerMap.value).length)
const canGoPrevious = computed(() => currentIndex.value > 0 && !isAdvancing.value)
const budgetValues = computed(() => {
  const preferredLocation =
    selectedPreferredLocation.value?.fullName === preferredLocationInput.value
      ? selectedPreferredLocation.value
      : resolveLocationParam(preferredLocationInput.value)
  const values = {
    preferredSido: preferredLocation.sido,
    preferredGugun: preferredLocation.gugun,
    preferredDong: preferredLocation.dong,
  }

  const monthlyRentMax = parseBudgetNumber(monthlyRentInput.value)
  const depositMax = parseBudgetNumber(depositInput.value)

  return {
    ...values,
    monthlyRentMax,
    depositMax,
  }
})

onMounted(() => {
  loadQuestions()
})

async function loadQuestions() {
  try {
    const data = await getLifestyleQuestions()

    if (!Array.isArray(data) || !data.length) {
      throw new Error('Lifestyle questions are empty.')
    }

    questions.value = normalizeLifestyleQuestions(data)
  } catch {
    questions.value = normalizeLifestyleQuestions(fallbackLifestyleQuestions)
    noticeMessage.value = '백엔드 연결이 없어 임시 설문 데이터로 화면을 표시 중입니다.'
  } finally {
    isLoading.value = false
  }
}

function selectOption(option) {
  if (!currentQuestion.value || isAdvancing.value || isSubmittingResult.value) {
    return
  }

  errorMessage.value = ''
  selectedAnswer.value = option
  answerMap.value = {
    ...answerMap.value,
    [currentQuestion.value.questionId]: option,
  }
  isAdvancing.value = true

  window.setTimeout(() => {
    if (currentIndex.value === questions.value.length - 1) {
      currentIndex.value += 1
      selectedAnswer.value = ''
      isAdvancing.value = false
      return
    }

    currentIndex.value += 1
    selectedAnswer.value = answerMap.value[currentQuestion.value.questionId] || ''
    isAdvancing.value = false
  }, 360)
}

function goPrevious() {
  if (!canGoPrevious.value) {
    return
  }

  currentIndex.value -= 1
  selectedAnswer.value = currentQuestion.value
    ? answerMap.value[currentQuestion.value.questionId] || ''
    : ''
}

async function finishSurvey() {
  isSubmittingResult.value = true
  errorMessage.value = ''

  try {
    await initializeAuthIfPossible()

    const payload = createLifestyleAnswerPayload(
      questions.value,
      answerMap.value,
      budgetValues.value,
    )
    const result = authStore.isAuthenticated
      ? await saveLifestyleResult(payload)
      : await previewLifestyleResult(payload)
    const snapshot = createLifestyleResultSnapshot(result, payload.answers, {
      saved: authStore.isAuthenticated,
      budget: {
        monthlyRentMax: payload.monthlyRentMax,
        depositMax: payload.depositMax,
        preferredSido: payload.preferredSido,
        preferredGugun: payload.preferredGugun,
        preferredDong: payload.preferredDong,
      },
    })

    writeLifestyleResultSnapshot(snapshot)
    await router.push({ name: 'result' })
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      '설문 결과를 분석하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSubmittingResult.value = false
    isAdvancing.value = false
  }
}

function selectPreferredLocation(location) {
  selectedPreferredLocation.value = location
  preferredLocationInput.value = location.fullName
  locationSuggestions.value = []
  isLocationMenuOpen.value = false
  isLocationLoading.value = false
  activeLocationIndex.value = -1
  if (locationSearchTimer.value) {
    window.clearTimeout(locationSearchTimer.value)
  }
  locationRequestId += 1
}

function closeLocationMenuSoon() {
  window.setTimeout(() => {
    isLocationMenuOpen.value = false
  }, 120)
}

function handlePreferredLocationInput(event) {
  const keyword = event.target.value
  if (preferredLocationInput.value !== keyword) {
    preferredLocationInput.value = keyword
  }
  scheduleLocationSearch(keyword)
}

function scheduleLocationSearch(keyword) {
  if (selectedPreferredLocation.value?.fullName === keyword) {
    locationSuggestions.value = []
    isLocationMenuOpen.value = false
    isLocationLoading.value = false
    activeLocationIndex.value = -1
    return
  }

  selectedPreferredLocation.value = null
  activeLocationIndex.value = -1

  if (locationSearchTimer.value) {
    window.clearTimeout(locationSearchTimer.value)
  }

  const normalizedKeyword = String(keyword || '').trim()
  if (normalizedKeyword.length < 2) {
    locationSuggestions.value = []
    isLocationMenuOpen.value = false
    isLocationLoading.value = false
    activeLocationIndex.value = -1
    return
  }

  isLocationMenuOpen.value = true
  isLocationLoading.value = true
  const requestId = ++locationRequestId
  locationSearchTimer.value = window.setTimeout(async () => {
    try {
      const suggestions = await searchLocations(normalizedKeyword)
      if (requestId !== locationRequestId) return
      locationSuggestions.value = suggestions
      isLocationMenuOpen.value = true
      isLocationLoading.value = false
      activeLocationIndex.value = suggestions.length ? 0 : -1
    } catch {
      if (requestId !== locationRequestId) return
      locationSuggestions.value = []
      isLocationMenuOpen.value = true
      isLocationLoading.value = false
      activeLocationIndex.value = -1
    }
  }, 220)
}

function handlePreferredLocationKeydown(event) {
  if (!isLocationMenuOpen.value) return

  const lastIndex = locationSuggestions.value.length - 1
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (lastIndex < 0) return
    activeLocationIndex.value =
      activeLocationIndex.value >= lastIndex ? 0 : activeLocationIndex.value + 1
    scrollActiveLocationIntoView()
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    if (lastIndex < 0) return
    activeLocationIndex.value =
      activeLocationIndex.value <= 0 ? lastIndex : activeLocationIndex.value - 1
    scrollActiveLocationIntoView()
  }

  if (event.key === 'Enter' && activeLocationIndex.value >= 0) {
    event.preventDefault()
    selectPreferredLocation(locationSuggestions.value[activeLocationIndex.value])
  }

  if (event.key === 'Escape') {
    isLocationMenuOpen.value = false
    activeLocationIndex.value = -1
  }
}

async function scrollActiveLocationIntoView() {
  await nextTick()
  locationMenuRef.value
    ?.querySelector(`[data-location-index="${activeLocationIndex.value}"]`)
    ?.scrollIntoView({ block: 'nearest' })
}

function parseBudgetNumber(value) {
  const parsed = Number(String(value).replace(/,/g, '').trim())
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

async function initializeAuthIfPossible() {
  if (authStore.accessToken && !authStore.isInitialized) {
    await authStore.initializeAuth()
  }
}
</script>

<template>
  <main class="page survey-page">
    <section class="section-container survey-shell">
      <div class="survey-top">
        <div>
          <p class="eyebrow">자취TI</p>
          <h1>나의 자취 성향을 찾아볼게요</h1>
          <p class="survey-description">
            6개의 선택과 예산 기준으로 생활권, 집 컨디션 선호를 분석해 매물 추천 기준을 만듭니다.
          </p>
        </div>
        <div class="step-counter">
          <strong>{{ Math.min(currentIndex + 1, totalSteps) }}</strong>
          <span>/ {{ totalSteps }}</span>
        </div>
      </div>

      <div class="progress-track" aria-label="설문 진행률">
        <span :style="{ width: `${progress}%` }"></span>
      </div>

      <p v-if="noticeMessage" class="notice">{{ noticeMessage }}</p>
      <p v-if="errorMessage" class="notice notice--error" role="alert">{{ errorMessage }}</p>

      <div v-if="isLoading || isSubmittingResult" class="loading-panel">
        <span></span>
        <p>
          {{ isSubmittingResult ? '설문 결과를 분석하는 중입니다.' : '질문을 불러오는 중입니다.' }}
        </p>
      </div>

      <Transition v-else name="question-slide" mode="out-in">
        <section v-if="isBudgetStep" key="budget" class="question-panel budget-panel">
          <div class="question-copy">
            <span class="category">예산 기준</span>
            <h2>선호 지역과 원하는 가격대가 있나요?</h2>
            <p>
              지역은 기본 검색 조건으로 저장하고, 알고 있는 예산은 추천순 점수에 먼저 반영합니다.
            </p>
          </div>

          <label class="budget-field location-preference-field">
            <span>선호 지역</span>
            <div class="location-control">
              <input
                v-model="preferredLocationInput"
                data-testid="preferred-location-input"
                type="search"
                autocomplete="off"
                placeholder="예: 서울 강남구 역삼동"
                @input="handlePreferredLocationInput"
                @focus="isLocationMenuOpen = preferredLocationInput.trim().length >= 2"
                @keydown="handlePreferredLocationKeydown"
                @blur="closeLocationMenuSoon"
              />
              <div
                v-if="isLocationMenuOpen"
                ref="locationMenuRef"
                class="location-menu"
                data-testid="preferred-location-suggestions"
              >
                <p v-if="isLocationLoading">지역 후보를 찾고 있습니다</p>
                <button
                  v-for="(location, index) in locationSuggestions"
                  :key="location.code"
                  v-show="!isLocationLoading"
                  type="button"
                  :class="{ 'location-menu__item--active': activeLocationIndex === index }"
                  :data-location-index="index"
                  @mousedown.prevent="selectPreferredLocation(location)"
                >
                  <strong>{{ location.fullName }}</strong>
                </button>
                <p v-if="!isLocationLoading && !locationSuggestions.length">
                  입력한 지역명으로 검색
                </p>
              </div>
            </div>
          </label>

          <div class="budget-grid">
            <label class="budget-field">
              <span>월세 상한</span>
              <div>
                <input
                  v-model="monthlyRentInput"
                  type="number"
                  min="1"
                  step="1"
                  inputmode="numeric"
                  placeholder="예: 60"
                />
                <em>만원 이하</em>
              </div>
            </label>

            <label class="budget-field">
              <span>보증금 상한</span>
              <div>
                <input
                  v-model="depositInput"
                  type="number"
                  min="1"
                  step="100"
                  inputmode="numeric"
                  placeholder="예: 1000"
                />
                <em>만원 이하</em>
              </div>
            </label>
          </div>

          <div class="budget-optional-note">
            <strong>예산을 아직 몰라도 괜찮아요.</strong>
            <span>월세나 보증금이 애매하면 빈칸으로 두고 바로 결과를 봐도 됩니다.</span>
          </div>
        </section>

        <section v-else :key="currentQuestion?.questionId" class="question-panel">
          <div class="question-copy">
            <span class="category">{{ currentQuestion.category }}</span>
            <h2>{{ currentQuestion.title }}</h2>
            <p>{{ answeredCount }}개의 선택이 반영되었습니다</p>
          </div>

          <div class="option-grid">
            <button
              class="option-card"
              :class="{
                'option-card--selected': selectedAnswer === 'A',
                'option-card--dimmed': selectedAnswer && selectedAnswer !== 'A',
              }"
              type="button"
              :disabled="isSubmittingResult"
              @click="selectOption('A')"
            >
              <span>A</span>
              <strong>{{ currentQuestion.optionA }}</strong>
            </button>

            <button
              class="option-card"
              :class="{
                'option-card--selected': selectedAnswer === 'B',
                'option-card--dimmed': selectedAnswer && selectedAnswer !== 'B',
              }"
              type="button"
              :disabled="isSubmittingResult"
              @click="selectOption('B')"
            >
              <span>B</span>
              <strong>{{ currentQuestion.optionB }}</strong>
            </button>
          </div>
        </section>
      </Transition>

      <div class="survey-actions">
        <button class="ghost-button" type="button" :disabled="!canGoPrevious" @click="goPrevious">
          이전
        </button>
        <button
          v-if="isBudgetStep"
          class="submit-button"
          type="button"
          :disabled="isSubmittingResult"
          @click="finishSurvey"
        >
          결과 보기
        </button>
        <p v-else>선택하면 다음 질문으로 넘어갑니다</p>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.survey-page {
  padding: 56px 0 76px;
  background:
    linear-gradient(135deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0.6) 52%),
    var(--color-bg);
}

.survey-shell {
  display: grid;
  gap: 24px;
}

.survey-top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;

  h1 {
    margin-top: 10px;
    color: var(--color-heading);
    font-size: 42px;
    font-weight: 900;
    line-height: var(--lh-tight);
  }
}

.survey-description {
  max-width: 640px;
  margin-top: 12px;
  color: var(--color-muted);
  font-size: 15px;
  font-weight: 700;
  line-height: var(--lh-relaxed);
}

.step-counter {
  min-width: 96px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  color: var(--color-muted);
  padding: 12px 14px;
  text-align: center;

  strong {
    color: var(--color-primary);
    font-size: 24px;
    font-weight: 900;
  }

  span {
    font-weight: 800;
  }
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #dbe3ee;

  span {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, var(--color-primary), #6f8fba);
    transition: width 280ms ease;
  }
}

.notice {
  border: 1px solid rgba(138, 122, 99, 0.26);
  border-radius: var(--radius-sm);
  background: var(--color-accent-soft);
  color: #6f5f45;
  font-size: 13px;
  font-weight: 800;
  padding: 12px 14px;
}

.notice--error {
  border-color: rgba(185, 28, 28, 0.2);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.loading-panel,
.question-panel {
  min-height: 440px;
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-panel);
}

.loading-panel {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 16px;
  color: var(--color-muted);
  font-weight: 800;

  span {
    width: 42px;
    height: 42px;
    border: 4px solid var(--color-primary-soft);
    border-top-color: var(--color-primary);
    border-radius: 50%;
    animation: spin 800ms linear infinite;
  }
}

.question-panel {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 34px;
  padding: 34px;
}

.question-copy {
  display: grid;
  gap: 12px;
  max-width: 820px;

  .category {
    width: fit-content;
    border-radius: var(--radius-xs);
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
    font-size: 12px;
    font-weight: 900;
    padding: 7px 10px;
  }

  h2 {
    color: var(--color-heading);
    font-size: 34px;
    font-weight: 900;
    line-height: var(--lh-snug);
  }

  p {
    color: var(--color-muted);
    font-size: 14px;
    font-weight: 700;
  }
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.option-card {
  display: grid;
  align-content: start;
  gap: 18px;
  min-height: 210px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-heading);
  padding: 24px;
  text-align: left;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    opacity var(--transition-fast),
    transform var(--transition-fast);

  span {
    width: 38px;
    height: 38px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-sm);
    background: var(--color-surface-muted);
    color: var(--color-muted);
    font-size: 14px;
    font-weight: 900;
    transition:
      background-color var(--transition-fast),
      color var(--transition-fast);
  }

  strong {
    font-size: 21px;
    font-weight: 900;
    line-height: var(--lh-relaxed);
  }

  &:hover {
    border-color: rgba(54, 95, 145, 0.36);
    box-shadow: var(--shadow-card-hover);
    transform: translateY(-4px);
  }
}

.option-card--selected {
  border-color: var(--color-primary);
  box-shadow: 0 18px 44px rgba(54, 95, 145, 0.18);
  transform: translateY(-4px) scale(1.01);

  span {
    background: var(--color-primary);
    color: var(--color-surface);
  }
}

.option-card--dimmed {
  opacity: 0.52;
}

.budget-panel {
  grid-template-rows: auto auto auto 1fr;
}

.budget-optional-note {
  display: grid;
  gap: 6px;
  border: 1px solid rgba(54, 95, 145, 0.2);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  padding: 18px 20px;

  strong {
    color: var(--color-primary-dark);
    font-size: 17px;
    font-weight: 900;
  }

  span {
    color: var(--color-muted);
    font-size: 14px;
    font-weight: 800;
    line-height: var(--lh-relaxed);
  }
}

.budget-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.location-preference-field {
  > .location-control {
    grid-template-columns: minmax(0, 1fr);
  }
}

.location-control {
  position: relative;
}

.location-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 120;
  display: grid;
  grid-template-columns: 1fr;
  width: 100%;
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.14);
  overscroll-behavior: contain;

  button {
    display: flex;
    align-items: center;
    grid-column: 1 / -1;
    min-height: 46px;
    background: var(--color-surface);
    color: var(--color-heading);
    text-align: left;
    padding: 0 13px;
    transition:
      background-color var(--transition-fast),
      color var(--transition-fast);

    &:hover {
      background: var(--color-primary-soft);
      color: var(--color-primary-dark);
    }
  }

  strong {
    font-size: 14px;
    font-weight: 900;
  }

  p {
    margin: 0;
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
    padding: 14px;
  }
}

.location-menu__item--active {
  background: var(--color-primary-soft) !important;
  color: var(--color-primary-dark) !important;
}

.budget-field {
  display: grid;
  gap: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 22px;

  > span {
    color: var(--color-heading);
    font-size: 16px;
    font-weight: 900;
  }

  > div {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: 10px;
  }

  input {
    width: 100%;
    min-width: 0;
    height: 54px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface-muted);
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
    padding: 0 14px;

    &:focus {
      outline: 3px solid rgba(54, 95, 145, 0.16);
      border-color: var(--color-primary);
      background: var(--color-surface);
    }
  }

  em {
    color: var(--color-muted);
    font-style: normal;
    font-weight: 900;
    white-space: nowrap;
  }
}

.survey-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.ghost-button {
  min-width: 84px;
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-heading);
  font-weight: 900;
  transition:
    border-color var(--transition-fast),
    transform var(--transition-fast);

  &:hover:not(:disabled) {
    border-color: var(--color-primary);
    transform: translateY(-1px);
  }

  &:disabled {
    color: var(--color-subtle);
    opacity: 0.58;
  }
}

.submit-button {
  min-width: 120px;
  height: 44px;
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: var(--color-surface);
  font-weight: 900;
  transition:
    background-color var(--transition-fast),
    transform var(--transition-fast);

  &:hover:not(:disabled) {
    background: var(--color-primary-dark);
    transform: translateY(-1px);
  }

  &:disabled {
    background: var(--color-subtle);
  }
}

.question-slide-enter-active,
.question-slide-leave-active {
  transition:
    opacity 260ms ease,
    transform 260ms ease;
}

.question-slide-enter-from {
  opacity: 0;
  transform: translateX(24px);
}

.question-slide-leave-to {
  opacity: 0;
  transform: translateX(-18px);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 820px) {
  .survey-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .survey-top h1 {
    font-size: 32px;
  }

  .question-panel {
    padding: 22px;
  }

  .question-copy h2 {
    font-size: 25px;
  }

  .option-grid {
    grid-template-columns: 1fr;
  }

  .budget-grid {
    grid-template-columns: 1fr;
  }

  .option-card {
    min-height: 160px;
  }
}
</style>
