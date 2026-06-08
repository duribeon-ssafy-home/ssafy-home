<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLifestyleQuestions } from '@/api/lifestyleApi'
import {
  buildLifestyleResult,
  fallbackLifestyleQuestions,
  normalizeLifestyleQuestions,
} from '@/data/lifestyle'

const router = useRouter()

const questions = ref([])
const currentIndex = ref(0)
const answerMap = ref({})
const selectedAnswer = ref('')
const isLoading = ref(true)
const isAdvancing = ref(false)
const noticeMessage = ref('')

const currentQuestion = computed(() => questions.value[currentIndex.value])
const progress = computed(() => {
  if (!questions.value.length) {
    return 0
  }

  return ((currentIndex.value + 1) / questions.value.length) * 100
})
const answeredCount = computed(() => Object.keys(answerMap.value).length)
const canGoPrevious = computed(() => currentIndex.value > 0 && !isAdvancing.value)

onMounted(() => {
  loadQuestions()
})

async function loadQuestions() {
  try {
    const data = await getLifestyleQuestions()
    questions.value = normalizeLifestyleQuestions(data)
  } catch {
    questions.value = normalizeLifestyleQuestions(fallbackLifestyleQuestions)
    noticeMessage.value = '백엔드 연결이 없어 임시 설문 데이터로 화면을 표시 중입니다.'
  } finally {
    isLoading.value = false
  }
}

function selectOption(option) {
  if (!currentQuestion.value || isAdvancing.value) {
    return
  }

  selectedAnswer.value = option
  answerMap.value = {
    ...answerMap.value,
    [currentQuestion.value.questionId]: option,
  }
  isAdvancing.value = true

  window.setTimeout(() => {
    if (currentIndex.value === questions.value.length - 1) {
      finishSurvey()
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
  selectedAnswer.value = answerMap.value[currentQuestion.value.questionId] || ''
}

function finishSurvey() {
  const result = buildLifestyleResult(questions.value, answerMap.value)
  sessionStorage.setItem('lifestyleResult', JSON.stringify(result))
  isAdvancing.value = false
  router.push({ name: 'result' })
}
</script>

<template>
  <main class="page survey-page">
    <section class="section-container survey-shell">
      <div class="survey-top">
        <div>
          <p class="eyebrow">Lifestyle Survey</p>
          <h1>생활 방식에 맞는 집을 찾아볼게요</h1>
        </div>
        <div class="step-counter">
          <strong>{{ Math.min(currentIndex + 1, questions.length || 1) }}</strong>
          <span>/ {{ questions.length || 6 }}</span>
        </div>
      </div>

      <div class="progress-track" aria-label="설문 진행률">
        <span :style="{ width: `${progress}%` }"></span>
      </div>

      <p v-if="noticeMessage" class="notice">{{ noticeMessage }}</p>

      <div v-if="isLoading" class="loading-panel">
        <span></span>
        <p>질문을 불러오는 중입니다.</p>
      </div>

      <Transition v-else name="question-slide" mode="out-in">
        <section :key="currentQuestion?.questionId" class="question-panel">
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
        <p>선택하면 다음 질문으로 넘어갑니다</p>
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
    letter-spacing: 0;
    line-height: 1.18;
  }
}

.step-counter {
  min-width: 96px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
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

.loading-panel,
.question-panel {
  min-height: 440px;
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
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
    letter-spacing: 0;
    line-height: 1.25;
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
  border-radius: var(--radius-sm);
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
    letter-spacing: 0;
    line-height: 1.42;
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
  border-radius: var(--radius-sm);
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

  .option-card {
    min-height: 160px;
  }
}
</style>
