<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getMyLatestLifestyleResult, saveLifestyleResult } from '@/api/lifestyleApi'
import { getRecommendations } from '@/api/recommendationApi'
import PropertyCard from '@/components/PropertyCard.vue'
import { createLifestylePresetChips, lifestyleTypeMeta } from '@/data/lifestyle'
import { useSearchStore } from '@/stores/search'
import { useAuthStore } from '@/stores/auth'
import { buildLocationText, extractPreferredLocation } from '@/utils/locationFilter'
import balancedCharacter from '@/assets/images/lifestyle/balanced.png'
import carefulCharacter from '@/assets/images/lifestyle/careful.png'
import cozyCharacter from '@/assets/images/lifestyle/cozy.png'
import flexibleCharacter from '@/assets/images/lifestyle/flexible.png'
import practicalCharacter from '@/assets/images/lifestyle/practical.png'
import savingCharacter from '@/assets/images/lifestyle/saving.png'
import spaciousCharacter from '@/assets/images/lifestyle/spacious.png'
import thriftyCharacter from '@/assets/images/lifestyle/thrifty.png'
import {
  createLifestyleResultSnapshot,
  readLifestyleResultSnapshot,
  writeLifestyleResultSnapshot,
} from '@/utils/lifestyleResultSession'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const searchStore = useSearchStore()

const result = ref(null)
const recommendedProperties = ref([])
const isLoading = ref(true)
const isSaving = ref(false)
const isLoadingRecommendations = ref(false)
const noticeMessage = ref('')
const errorMessage = ref('')

const hasResult = computed(() => Boolean(result.value?.lifestyleType))
const meta = computed(() =>
  hasResult.value ? lifestyleTypeMeta[result.value.lifestyleType] : null,
)
const isSaved = computed(() => result.value?.saved === true)
const lifestyleCharacters = {
  LIVING_COST_HOME_BALANCED: {
    label: '균형 잡힌 주거 성향 캐릭터',
    image: balancedCharacter,
  },
  LIVING_COST_COMPACT: {
    label: '알뜰한 생활권 성향 캐릭터',
    image: thriftyCharacter,
  },
  LIVING_FLEXIBLE_HOME: {
    label: '포근한 공간 성향 캐릭터',
    image: cozyCharacter,
  },
  LIVING_FLEXIBLE_COMPACT: {
    label: '실용적인 생활 성향 캐릭터',
    image: practicalCharacter,
  },
  LOCATION_FLEXIBLE_COST_HOME: {
    label: '꼼꼼한 조건 확인 성향 캐릭터',
    image: carefulCharacter,
  },
  LOCATION_FLEXIBLE_COST_COMPACT: {
    label: '절약 중심 성향 캐릭터',
    image: savingCharacter,
  },
  LOCATION_FLEXIBLE_HOME: {
    label: '공간 만족 성향 캐릭터',
    image: spaciousCharacter,
  },
  LOCATION_FLEXIBLE_COMPACT: {
    label: '유연한 탐색 성향 캐릭터',
    image: flexibleCharacter,
  },
}
const resultCharacter = computed(() => lifestyleCharacters[result.value?.lifestyleType] || null)
const hasAnswers = computed(
  () => Array.isArray(result.value?.answers) && result.value.answers.length > 0,
)
const presetChips = computed(() =>
  createLifestylePresetChips(result.value?.filterPreset, meta.value?.chips || []),
)
const preferredLocationText = computed(() =>
  buildLocationText(extractPreferredLocation(result.value?.filterPreset)),
)

onMounted(loadResult)

async function loadResult() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    await initializeAuthIfPossible()

    const cachedResult = readLifestyleResultSnapshot()

    if (cachedResult) {
      result.value = cachedResult

      if (
        route.query.saveLifestyle === '1' &&
        !cachedResult.saved &&
        cachedResult.answers?.length
      ) {
        await saveCurrentResult({ replaceQuery: true })
      }

      await loadRecommendedPreview()
      return
    }

    if (authStore.isAuthenticated) {
      await loadSavedResult()
    }
  } finally {
    isLoading.value = false
  }
}

async function loadSavedResult() {
  try {
    const savedResult = await getMyLatestLifestyleResult()
    const snapshot = createLifestyleResultSnapshot(savedResult, [], { saved: true })
    result.value = snapshot
    writeLifestyleResultSnapshot(snapshot)
    await loadRecommendedPreview()
  } catch (error) {
    if (error.response?.data?.errorCode !== 'LIFESTYLE_RESULT_NOT_FOUND') {
      errorMessage.value =
        error.response?.data?.message || '저장된 선호 유형을 불러오지 못했습니다.'
    }
  }
}

async function saveCurrentResult(options = {}) {
  const { replaceQuery = false } = options

  if (!hasAnswers.value) {
    errorMessage.value = '저장할 설문 답변이 없습니다. 설문을 다시 진행해 주세요.'
    return
  }

  if (!authStore.isAuthenticated) {
    await router.push({
      name: 'login',
      query: {
        redirect: router.resolve({ name: 'result', query: { saveLifestyle: '1' } }).fullPath,
      },
    })
    return
  }

  isSaving.value = true
  noticeMessage.value = ''
  errorMessage.value = ''

  try {
    const savedResult = await saveLifestyleResult({
      answers: result.value.answers,
      ...result.value.budget,
    })
    const snapshot = createLifestyleResultSnapshot(savedResult, result.value.answers, {
      saved: true,
      budget: result.value.budget || {},
    })

    result.value = snapshot
    writeLifestyleResultSnapshot(snapshot)
    noticeMessage.value = '설문 결과가 내 선호 유형으로 저장되었습니다.'
    await loadRecommendedPreview()

    if (replaceQuery && route.query.saveLifestyle) {
      await router.replace({ name: 'result' })
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '설문 결과를 저장하지 못했습니다.'
  } finally {
    isSaving.value = false
  }
}

function buildRecommendationParams(filterPreset = {}) {
  const params = {}
  const location = extractPreferredLocation(filterPreset)

  if (location.sido) params.sido = location.sido
  if (location.gugun) params.gugun = location.gugun
  if (location.dong) params.dong = location.dong
  if (filterPreset.depositMax != null) params.maxDeposit = filterPreset.depositMax
  if (filterPreset.monthlyRentMax != null) params.maxMonthlyRent = filterPreset.monthlyRentMax
  if (filterPreset.areaMin != null) params.minArea = filterPreset.areaMin
  if (filterPreset.facilityCountMin != null) params.facilityCountMin = filterPreset.facilityCountMin

  return params
}

async function loadRecommendedPreview() {
  if (!isSaved.value || !authStore.isAuthenticated) {
    recommendedProperties.value = []
    return
  }

  isLoadingRecommendations.value = true

  try {
    const page = await getRecommendations({
      ...buildRecommendationParams(result.value?.filterPreset),
      page: 0,
      size: 3,
    })
    recommendedProperties.value = page.content
  } catch {
    recommendedProperties.value = []
  } finally {
    isLoadingRecommendations.value = false
  }
}

async function goToRecommendedProperties() {
  const params = buildRecommendationParams(result.value?.filterPreset)
  searchStore.setSearch(params, preferredLocationText.value)
  await router.push({ name: 'home', query: { lifestyle: '1' } })
}

async function initializeAuthIfPossible() {
  if (authStore.accessToken && !authStore.isInitialized) {
    await authStore.initializeAuth()
  }
}
</script>

<template>
  <main class="page result-page">
    <section v-if="isLoading" class="section-container result-loading">
      <span></span>
      <p>결과를 불러오는 중입니다.</p>
    </section>

    <section v-else-if="!hasResult" class="section-container empty-state">
      <p class="eyebrow">자취TI</p>
      <h1>아직 분석된 자취TI 결과가 없습니다</h1>
      <p>
        자취 성향 테스트를 완료하면 나에게 맞는 주거 유형과 우선 추천 기준을 바로 확인할 수
        있습니다.
      </p>
      <RouterLink class="primary-link" :to="{ name: 'survey' }">자취TI 시작하기</RouterLink>
    </section>

    <section v-else class="result-hero">
      <div class="section-container result-hero__inner">
        <div class="result-copy result-copy--with-character">
          <div class="result-copy__text">
            <p class="eyebrow">자취TI 결과</p>
            <span>당신의 주거 타입은</span>
            <h1>{{ meta.typeName }}</h1>
            <p>{{ meta.headline }}</p>
          </div>

          <img
            v-if="resultCharacter"
            class="result-character"
            :src="resultCharacter.image"
            :alt="resultCharacter.label"
          />
        </div>

        <div class="summary-panel">
          <strong>우선 추천 기준</strong>
          <div class="chip-list">
            <span
              v-for="(chip, index) in presetChips"
              :key="chip"
              :style="{ '--delay': `${index * 70}ms` }"
            >
              {{ chip }}
            </span>
          </div>
          <p>{{ meta.summary }}</p>
        </div>
      </div>
    </section>

    <section v-if="hasResult" class="result-status-section">
      <div
        v-if="isSaved"
        class="section-container status-panel status-panel--saved recommendation-panel"
      >
        <div class="recommendation-panel__heading">
          <p class="eyebrow">Recommended Homes</p>
          <h2>자취TI 기준으로 먼저 볼 만한 매물</h2>
          <p>
            {{ preferredLocationText || '저장된 선호 조건' }} 기준으로 어울리는 매물을 일부만
            보여드려요.
          </p>
        </div>

        <div class="result-actions">
          <button class="primary-link" type="button" @click="goToRecommendedProperties">
            자세히 보기
          </button>
          <RouterLink class="secondary-link" :to="{ name: 'survey' }">자취TI 다시 하기</RouterLink>
          <RouterLink class="secondary-link" :to="{ name: 'my-page' }"
            >마이페이지에서 보기</RouterLink
          >
        </div>

        <div v-if="isLoadingRecommendations" class="recommendation-preview-state">
          추천 매물을 불러오는 중입니다...
        </div>
        <div v-else-if="recommendedProperties.length" class="recommendation-preview-grid">
          <PropertyCard
            v-for="property in recommendedProperties"
            :key="property.propertyId"
            :property="property"
          />
        </div>
        <div v-else class="recommendation-preview-state">
          지금 조건에 맞는 추천 매물이 없습니다. 자세히 보기에서 조건을 조금 넓혀볼 수 있어요.
        </div>

        <p v-if="noticeMessage" class="form-message form-message--success">{{ noticeMessage }}</p>
        <p v-if="errorMessage" class="form-message form-message--error" role="alert">
          {{ errorMessage }}
        </p>
      </div>

      <div v-else class="section-container status-panel">
        <div>
          <p class="eyebrow">Temporary Preference</p>
          <h2>현재 결과는 임시 저장 중입니다</h2>
          <p>로그인하면 이 결과를 내 선호 유형으로 저장하고 나중에 다시 볼 수 있습니다.</p>
        </div>

        <div class="result-actions">
          <button
            class="primary-link"
            type="button"
            :disabled="isSaving"
            @click="saveCurrentResult()"
          >
            {{
              isSaving
                ? '저장 중...'
                : authStore.isAuthenticated
                  ? '이 결과 저장하기'
                  : '로그인하고 결과 저장하기'
            }}
          </button>
          <RouterLink class="secondary-link" :to="{ name: 'survey' }">자취TI 다시 하기</RouterLink>
        </div>

        <p v-if="noticeMessage" class="form-message form-message--success">{{ noticeMessage }}</p>
        <p v-if="errorMessage" class="form-message form-message--error" role="alert">
          {{ errorMessage }}
        </p>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.result-page {
  padding-bottom: 76px;
  background:
    linear-gradient(180deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0) 420px),
    var(--color-bg);
}

.result-loading,
.empty-state {
  min-height: 460px;
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 16px;
  padding-top: 72px;
}

.result-loading {
  justify-items: center;
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

.empty-state {
  h1 {
    color: var(--color-heading);
    font-size: 42px;
    font-weight: 900;
    line-height: var(--lh-tight);
  }

  p:not(.eyebrow) {
    max-width: 620px;
    color: var(--color-muted);
    font-size: 17px;
    font-weight: 700;
    line-height: var(--lh-loose);
  }
}

.result-hero {
  padding: 72px 0 46px;
}

.result-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(320px, 0.74fr);
  gap: 28px;
  align-items: stretch;
}

.result-copy,
.summary-panel {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-panel);
}

.result-copy {
  display: grid;
  align-items: center;
  gap: 24px;
  min-height: 370px;
  padding: 42px;
  animation: fadeUp 520ms ease both;
}

.result-copy--with-character {
  grid-template-columns: minmax(0, 1fr) 210px;
}

.result-copy__text {
  display: grid;
  align-content: center;

  span {
    margin-top: 22px;
    color: var(--color-muted);
    font-size: 17px;
    font-weight: 800;
  }

  h1 {
    margin-top: 12px;
    color: var(--color-heading);
    font-size: 52px;
    font-weight: 900;
    line-height: var(--lh-tight);
  }

  p:not(.eyebrow) {
    max-width: 620px;
    margin-top: 18px;
    color: var(--color-muted);
    font-size: 18px;
    font-weight: 700;
    line-height: var(--lh-loose);
  }
}

.result-character {
  width: min(210px, 100%);
  aspect-ratio: 1;
  justify-self: end;
  filter: drop-shadow(0 18px 24px rgba(54, 95, 145, 0.16));
  animation: characterFloat 3.8s ease-in-out infinite;
}

.summary-panel {
  display: grid;
  align-content: center;
  gap: 20px;
  padding: 32px;
  animation: fadeUp 520ms ease 120ms both;

  strong {
    color: var(--color-heading);
    font-size: 18px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-weight: 700;
    line-height: var(--lh-loose);
  }
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;

  span {
    border: 1px solid rgba(54, 95, 145, 0.2);
    border-radius: var(--radius-pill);
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
    font-size: 13px;
    font-weight: 900;
    padding: 9px 11px;
    animation: chipIn 360ms ease both;
    animation-delay: var(--delay);
  }
}

.result-status-section {
  padding-top: 6px;
}

.status-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-panel);
  padding: 24px;

  h2 {
    margin-top: 6px;
    color: var(--color-heading);
    font-size: 22px;
    font-weight: 900;
    line-height: var(--lh-snug);
  }

  p:not(.eyebrow):not(.form-message) {
    margin-top: 8px;
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.6;
  }
}

.status-panel--saved {
  border-color: rgba(54, 95, 145, 0.24);
}

.recommendation-panel__heading,
.recommendation-preview-grid,
.recommendation-preview-state {
  grid-column: 1 / -1;
}

.recommendation-panel {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
}

.recommendation-panel__heading {
  max-width: 720px;
}

.recommendation-preview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.recommendation-preview-state {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 800;
  padding: 18px;
}

.result-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.primary-link,
.secondary-link {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  font-size: 14px;
  font-weight: 900;
  padding: 0 16px;
  white-space: nowrap;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.primary-link {
  background: var(--color-primary);
  color: var(--color-surface);

  &:hover:not(:disabled) {
    background: var(--color-primary-dark);
    box-shadow: 0 12px 24px rgba(54, 95, 145, 0.2);
    transform: translateY(-1px);
  }

  &:disabled {
    background: var(--color-subtle);
  }
}

.secondary-link {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-heading);

  &:hover {
    border-color: var(--color-primary);
    transform: translateY(-1px);
  }
}

.form-message {
  grid-column: 1 / -1;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 800;
  padding: 11px 12px;
}

.form-message--success {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.form-message--error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes chipIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes characterFloat {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-7px);
  }
}

@media (max-width: 940px) {
  .result-hero__inner,
  .status-panel {
    grid-template-columns: 1fr;
  }

  .result-actions {
    justify-content: flex-start;
  }

  .recommendation-preview-grid {
    grid-template-columns: 1fr;
  }

  .result-copy--with-character {
    grid-template-columns: 1fr;
  }

  .result-copy__text h1 {
    font-size: 38px;
  }

  .result-character {
    width: 176px;
    justify-self: start;
  }

  .empty-state h1 {
    font-size: 34px;
  }
}
</style>
