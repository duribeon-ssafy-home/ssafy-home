<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getMyLatestLifestyleResult, saveLifestyleResult } from '@/api/lifestyleApi'
import { createLifestylePresetChips, lifestyleTypeMeta } from '@/data/lifestyle'
import { useAuthStore } from '@/stores/auth'
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

const result = ref(null)
const isLoading = ref(true)
const isSaving = ref(false)
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

    if (replaceQuery && route.query.saveLifestyle) {
      await router.replace({ name: 'result' })
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '설문 결과를 저장하지 못했습니다.'
  } finally {
    isSaving.value = false
  }
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
        자취 성향 테스트를 완료하면 나에게 맞는 주거 유형과 우선 추천 기준을 바로 확인할 수 있습니다.
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
      <div class="section-container status-panel" :class="{ 'status-panel--saved': isSaved }">
        <div>
          <p class="eyebrow">{{ isSaved ? 'Saved Preference' : 'Temporary Preference' }}</p>
          <h2>{{ isSaved ? '저장된 선호 유형입니다' : '현재 결과는 임시 저장 중입니다' }}</h2>
          <p>
            {{
              isSaved
                ? '마이페이지에서 언제든 다시 확인할 수 있습니다.'
                : '로그인하면 이 결과를 내 선호 유형으로 저장하고 나중에 다시 볼 수 있습니다.'
            }}
          </p>
        </div>

        <div class="result-actions">
          <button
            v-if="!isSaved"
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
          <RouterLink v-if="isSaved" class="secondary-link" :to="{ name: 'my-page' }">
            마이페이지에서 보기
          </RouterLink>
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
    letter-spacing: 0;
    line-height: 1.18;
  }

  p:not(.eyebrow) {
    max-width: 620px;
    color: var(--color-muted);
    font-size: 17px;
    font-weight: 700;
    line-height: 1.7;
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
  border-radius: var(--radius-sm);
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
    letter-spacing: 0;
    line-height: 1.12;
  }

  p:not(.eyebrow) {
    max-width: 620px;
    margin-top: 18px;
    color: var(--color-muted);
    font-size: 18px;
    font-weight: 700;
    line-height: 1.7;
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
    line-height: 1.7;
  }
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;

  span {
    border: 1px solid rgba(54, 95, 145, 0.2);
    border-radius: var(--radius-sm);
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
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-panel);
  padding: 24px;

  h2 {
    margin-top: 6px;
    color: var(--color-heading);
    font-size: 22px;
    font-weight: 900;
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
  border-radius: var(--radius-sm);
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
