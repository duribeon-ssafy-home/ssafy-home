<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { compareWithAi } from '@/api/aiApi'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  propertyIds: {
    type: Array,
    required: true,
  },
  width: {
    type: Number,
    default: 420,
  },
})

const emit = defineEmits(['close', 'resize'])
const router = useRouter()

const presets = [
  '내 생활패턴 기준으로 어디가 제일 나아?',
  '전세사기 위험이 낮은 순서로 알려줘',
  '보증금이 싼데 위험 점수가 높은 매물을 설명해줘',
]

const question = ref('')
const history = ref([]) // { question, result, error }[]
const isLoading = ref(false)
const isResizing = ref(false)

const hasCompareContext = computed(() => props.propertyIds.length >= 2)
const canSubmit = computed(() => question.value.trim().length > 0)

function getAnswerParts(result) {
  if (!result?.answer) return []

  const highlights = [
    createHighlight(result.recommendedTitle, 'property'),
    createHighlight(
      result.recommendedTotalScore != null ? `${result.recommendedTotalScore}점` : null,
      scoreTone(result.recommendedTotalScore),
    ),
    createHighlight(result.recommendedRiskLabelText, riskTone(result.recommendedRiskLabelText)),
    createHighlight(result.lifestyleTypeName, 'lifestyle'),
  ].filter(Boolean)

  return splitHighlights(result.answer, highlights)
}

watch(
  () => [props.open, hasCompareContext.value],
  ([isOpen, hasContext]) => {
    if (isOpen && hasContext && !question.value) {
      question.value = presets[0]
    }
  },
)

function selectPreset(preset) {
  question.value = preset
}

async function submit() {
  if (!canSubmit.value || isLoading.value) return

  const submittedQuestion = question.value.trim()
  question.value = ''
  isLoading.value = true

  try {
    const result = await compareWithAi({
      propertyIds: props.propertyIds,
      question: submittedQuestion,
    })
    history.value.push({ question: submittedQuestion, result, error: null })
  } catch {
    history.value.push({ question: submittedQuestion, result: null, error: '분석 결과를 불러오지 못했어요. 잠시 후 다시 시도해주세요.' })
  } finally {
    isLoading.value = false
  }
}

async function goSurvey() {
  emit('close')
  await router.push({ name: 'survey' })
}

function createHighlight(text, tone) {
  if (!text) return null
  return { text, tone }
}

function splitHighlights(text, highlights) {
  const uniqueHighlights = highlights
    .filter((item, index, array) => array.findIndex((candidate) => candidate.text === item.text) === index)
    .sort((a, b) => b.text.length - a.text.length)
  const parts = []
  let remaining = text

  while (remaining.length > 0) {
    const match = uniqueHighlights
      .map((item) => ({ ...item, index: remaining.indexOf(item.text) }))
      .filter((item) => item.index >= 0)
      .sort((a, b) => a.index - b.index || b.text.length - a.text.length)[0]

    if (!match) {
      parts.push({ text: remaining, tone: null })
      break
    }

    if (match.index > 0) {
      parts.push({ text: remaining.slice(0, match.index), tone: null })
    }

    parts.push({ text: match.text, tone: match.tone })
    remaining = remaining.slice(match.index + match.text.length)
  }

  return parts
}

function riskTone(label) {
  if (label === '안전') return 'risk-safe'
  if (label === '주의') return 'risk-caution'
  if (label === '위험') return 'risk-danger'
  return 'risk-unknown'
}

function scoreTone(score) {
  if (score == null) return 'score-mid'
  if (score >= 75) return 'score-high'
  if (score >= 50) return 'score-mid'
  return 'score-low'
}

function startResize(event) {
  if (event.button !== 0) return

  isResizing.value = true
  document.body.classList.add('ai-panel-resizing')
  window.addEventListener('mousemove', resize)
  window.addEventListener('mouseup', stopResize)
}

function resize(event) {
  if (!isResizing.value) return
  emit('resize', window.innerWidth - event.clientX)
}

function stopResize() {
  isResizing.value = false
  document.body.classList.remove('ai-panel-resizing')
  window.removeEventListener('mousemove', resize)
  window.removeEventListener('mouseup', stopResize)
}

onBeforeUnmount(() => {
  stopResize()
})
</script>

<template>
  <Transition name="drawer-push">
    <aside
      v-if="open"
      class="ai-panel"
      :style="{ width: `${width}px` }"
      aria-label="AI 비교 상담 패널"
    >
      <button
        class="ai-panel__resize"
        type="button"
        aria-label="AI 상담 패널 너비 조절"
        @mousedown="startResize"
      />

      <div class="ai-panel__content">
        <header class="ai-panel__header">
          <div>
            <p>AI Compare</p>
            <h2>AI 비교 상담</h2>
          </div>
          <button class="ai-panel__close" type="button" aria-label="닫기" @click="emit('close')">×</button>
        </header>

        <section class="ai-panel__section">
          <label for="ai-compare-question">질문</label>
          <textarea
            id="ai-compare-question"
            v-model="question"
            rows="4"
            maxlength="500"
            :placeholder="hasCompareContext ? '비교 매물에 대해 궁금한 점을 입력하세요.' : '부동산 계약이나 용어에 대해 궁금한 점을 입력하세요.'"
          />
          <button class="analyze-button" type="button" :disabled="!canSubmit || isLoading" @click="submit">
            {{ isLoading ? '답변 생성 중...' : hasCompareContext ? '분석하기' : '질문하기' }}
          </button>
        </section>

        <Transition name="preset-rise">
          <section v-if="hasCompareContext" class="ai-panel__section preset-section">
            <h3>추천 질문</h3>
            <div class="preset-list">
              <button
                v-for="preset in presets"
                :key="preset"
                type="button"
                :class="{ active: question === preset }"
                @click="selectPreset(preset)"
              >
                {{ preset }}
              </button>
            </div>
          </section>
        </Transition>

        <p v-if="propertyIds.length > 0 && propertyIds.length < 2" class="notice">
          매물 2개 이상을 선택하면 비교 분석까지 받을 수 있어요.
        </p>

        <div v-if="isLoading" class="chat-loading">AI가 분석 중입니다...</div>

        <div class="chat-history">
          <div v-for="(item, index) in history" :key="index" class="chat-entry">
            <div class="chat-question">{{ item.question }}</div>

            <p v-if="item.error" class="error-message">{{ item.error }}</p>

            <section v-if="item.result" class="ai-result">
              <div v-if="Array.isArray(item.result.propertyAnalyses)" class="result-summary">
                <div class="summary-card summary-card--wide">
                  <span>추천 매물</span>
                  <strong>{{ item.result.recommendedTitle }}</strong>
                  <small>#{{ item.result.recommendedPropertyId }}</small>
                </div>
                <div class="summary-card" :class="`summary-card--${scoreTone(item.result.recommendedTotalScore)}`">
                  <span>총점</span>
                  <strong>{{ item.result.recommendedTotalScore }}점</strong>
                </div>
                <div class="summary-card" :class="`summary-card--${riskTone(item.result.recommendedRiskLabelText)}`">
                  <span>위험도</span>
                  <strong>{{ item.result.recommendedRiskLabelText }}</strong>
                </div>
              </div>

              <p class="answer">
                <span
                  v-for="(part, pIdx) in getAnswerParts(item.result)"
                  :key="`${part.text}-${pIdx}`"
                  :class="part.tone ? ['answer-highlight', `answer-highlight--${part.tone}`] : null"
                >{{ part.text }}</span>
              </p>

              <div v-if="item.result.requiresLifestyleSurvey" class="survey-guide">
                <p>{{ item.result.surveyGuideMessage }}</p>
                <button type="button" @click="goSurvey">설문하러 가기</button>
              </div>

              <div v-if="Array.isArray(item.result.propertyAnalyses)" class="analysis-list">
                <article v-for="analysis in item.result.propertyAnalyses" :key="analysis.propertyId" class="analysis-item">
                  <div class="analysis-item__head">
                    <div>
                      <span>#{{ analysis.propertyId }}</span>
                      <h3>{{ analysis.title }}</h3>
                    </div>
                    <strong>{{ analysis.totalScore }}점</strong>
                  </div>

                  <div class="score-grid">
                    <span>위험도 {{ analysis.riskLabelText }} · {{ analysis.riskScore }}점</span>
                    <span>생활 적합도 {{ analysis.lifestyleFitScore != null ? `${analysis.lifestyleFitScore}점` : '설문 필요' }}</span>
                    <span>비용 조건 {{ analysis.costScore }}점</span>
                  </div>

                  <div class="reason-columns">
                    <div>
                      <h4>좋은 점</h4>
                      <ul>
                        <li v-for="pro in analysis.pros" :key="pro">{{ pro }}</li>
                      </ul>
                    </div>
                    <div>
                      <h4>주의할 점</h4>
                      <ul>
                        <li v-for="con in analysis.cons" :key="con">{{ con }}</li>
                      </ul>
                    </div>
                  </div>
                </article>
              </div>

              <details v-if="item.result.sources?.length" class="sources">
                <summary>근거 보기</summary>
                <ul>
                  <li v-for="source in item.result.sources" :key="`${source.type}-${source.id}-${source.title}`">
                    {{ source.type }} · {{ source.id }} · {{ source.title }}
                  </li>
                </ul>
              </details>

              <ul v-if="item.result.warnings?.length" class="warning-list">
                <li v-for="warning in item.result.warnings" :key="warning">{{ warning }}</li>
              </ul>
            </section>
          </div>
        </div>
      </div>
    </aside>
  </Transition>
</template>

<style lang="scss" scoped>
.ai-panel {
  position: sticky;
  top: 0;
  z-index: 60;
  display: flex;
  flex: 0 0 auto;
  min-width: 0;
  max-width: 700px;
  height: 100vh;
  background: var(--color-surface);
  border-left: 1px solid var(--color-border);
  box-shadow: -14px 0 30px rgba(15, 23, 42, 0.08);
}

.ai-panel__content {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.ai-panel__resize {
  position: sticky;
  top: 0;
  align-self: flex-start;
  flex: 0 0 10px;
  margin-left: -6px;
  width: 10px;
  height: 100vh;
  cursor: col-resize;
  background: transparent;
  z-index: 2;

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 4px;
    width: 2px;
    height: 56px;
    border-radius: 999px;
    background: var(--color-border-strong);
    transform: translateY(-50%);
    transition: background-color var(--transition-fast);
  }

  &:hover::after {
    background: var(--color-primary);
  }
}

.ai-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;

  p {
    color: var(--color-primary);
    font-size: 12px;
    font-weight: 900;
    margin-bottom: 4px;
  }

  h2 {
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }
}

.ai-panel__close {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-xs);
  background: var(--color-bg-soft);
  color: var(--color-heading);
  font-size: 22px;
  cursor: pointer;
}

.ai-panel__section {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;

  h3,
  label {
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 900;
  }

  textarea {
    width: 100%;
    resize: vertical;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    padding: 12px;
    color: var(--color-text);
    font: inherit;
    line-height: 1.5;
  }
}

.preset-list {
  display: grid;
  gap: 8px;

  button {
    min-height: 42px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    color: var(--color-heading);
    padding: 10px 12px;
    text-align: left;
    font-size: 13px;
    font-weight: 800;
    cursor: pointer;

    &.active,
    &:hover {
      border-color: var(--color-primary);
      background: var(--color-primary-soft);
      color: var(--color-primary-dark);
    }
  }
}

.preset-rise-enter-active,
.preset-rise-leave-active {
  transition:
    opacity var(--transition-base),
    transform var(--transition-base);
}

.preset-rise-enter-from,
.preset-rise-leave-to {
  opacity: 0;
  transform: translateY(14px);
}

.analyze-button {
  height: 44px;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: white;
  font-weight: 900;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
}

.notice,
.error-message {
  border-radius: var(--radius-sm);
  padding: 12px;
  font-size: 13px;
  font-weight: 800;
}

.notice {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.error-message {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.chat-loading {
  padding: 12px;
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.chat-history {
  display: grid;
  gap: 24px;
}

.chat-entry {
  display: grid;
  gap: 12px;
}

.chat-question {
  align-self: end;
  justify-self: end;
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 16px 16px 4px 16px;
  background: var(--color-primary);
  color: white;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: keep-all;
}

.ai-result {
  display: grid;
  gap: 16px;
  padding-top: 6px;
}

.result-summary {
  display: grid;
  grid-template-columns: 1fr 92px 92px;
  gap: 10px;
}

.summary-card {
  display: grid;
  align-content: center;
  gap: 4px;
  min-width: 0;
  min-height: 78px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
  padding: 12px;

  span {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 900;
  }

  strong {
    color: var(--color-heading);
    font-size: 18px;
    line-height: 1.25;
    word-break: keep-all;
    overflow-wrap: anywhere;
  }

  small {
    color: var(--color-subtle);
    font-size: 11px;
    font-weight: 800;
  }
}

.summary-card--wide {
  background: var(--color-primary-soft);
  border-color: rgba(54, 95, 145, 0.2);

  strong {
    color: var(--color-primary-dark);
  }
}

.summary-card--risk-safe {
  background: #ecfdf3;
  border-color: rgba(2, 122, 72, 0.18);

  strong {
    color: #027a48;
  }
}

.summary-card--risk-caution {
  background: #fffbeb;
  border-color: rgba(180, 83, 9, 0.2);

  strong {
    color: #b45309;
  }
}

.summary-card--risk-danger {
  background: var(--color-danger-soft);
  border-color: rgba(180, 35, 24, 0.2);

  strong {
    color: var(--color-danger);
  }
}

.summary-card--risk-unknown {
  background: var(--color-bg-soft);
}

.summary-card--score-high {
  background: #ecfdf3;
  border-color: rgba(2, 122, 72, 0.18);

  strong {
    color: #027a48;
  }
}

.summary-card--score-mid {
  background: var(--color-primary-soft);
  border-color: rgba(54, 95, 145, 0.2);

  strong {
    color: var(--color-primary-dark);
  }
}

.summary-card--score-low {
  background: var(--color-danger-soft);
  border-color: rgba(180, 35, 24, 0.2);

  strong {
    color: var(--color-danger);
  }
}

.answer {
  color: var(--color-text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.7;
  white-space: pre-line;
}

.answer-highlight {
  border-radius: 4px;
  font-weight: 900;
  padding: 1px 4px;
}

.answer-highlight--property,
.answer-highlight--lifestyle {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.answer-highlight--score-high,
.answer-highlight--risk-safe {
  background: #ecfdf3;
  color: #027a48;
}

.answer-highlight--score-mid {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.answer-highlight--score-low,
.answer-highlight--risk-danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.answer-highlight--risk-caution {
  background: #fffbeb;
  color: #b45309;
}

.answer-highlight--risk-unknown {
  background: var(--color-bg-soft);
  color: var(--color-muted);
}

.survey-guide {
  display: grid;
  gap: 10px;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  padding: 14px;

  p {
    color: var(--color-primary-dark);
    font-size: 13px;
    font-weight: 800;
    line-height: 1.6;
  }

  button {
    justify-self: start;
    height: 36px;
    border-radius: var(--radius-xs);
    background: var(--color-primary);
    color: white;
    padding: 0 14px;
    font-size: 13px;
    font-weight: 900;
    cursor: pointer;
  }
}

.analysis-list {
  display: grid;
  gap: 12px;
}

.analysis-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 14px;
}

.analysis-item__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  span {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 900;
  }

  h3 {
    color: var(--color-heading);
    font-size: 15px;
    font-weight: 900;
    line-height: 1.4;
  }

  strong {
    color: var(--color-primary);
    white-space: nowrap;
  }
}

.score-grid {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;

  span {
    border-radius: var(--radius-xs);
    background: var(--color-surface-muted);
    padding: 8px;
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
  }
}

.reason-columns {
  display: grid;
  gap: 12px;

  h4 {
    margin-bottom: 6px;
    color: var(--color-heading);
    font-size: 12px;
    font-weight: 900;
  }

  ul {
    display: grid;
    gap: 5px;
    padding-left: 18px;
  }

  li {
    color: var(--color-text);
    font-size: 12px;
    line-height: 1.5;
  }
}

.sources {
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 700;

  summary {
    cursor: pointer;
    color: var(--color-heading);
    font-weight: 900;
  }

  ul {
    display: grid;
    gap: 5px;
    margin-top: 10px;
    padding-left: 18px;
  }
}

.warning-list {
  display: grid;
  gap: 6px;
  border-top: 1px solid var(--color-border);
  padding: 12px 0 0 18px;

  li {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
    line-height: 1.5;
  }
}

:global(body.ai-panel-resizing) {
  cursor: col-resize;
  user-select: none;
}

.drawer-push-enter-active,
.drawer-push-leave-active {
  transition:
    width var(--transition-base),
    transform var(--transition-base),
    opacity var(--transition-base);
}

.drawer-push-enter-from,
.drawer-push-leave-to {
  opacity: 0;
  transform: translateX(24px);
}

@media (max-width: 720px) {
  .ai-panel__content {
    padding: 18px;
  }

  .ai-panel__resize {
    margin-left: -23px;
  }

  .ai-panel__header {
    h2 {
      font-size: 20px;
    }
  }

  .result-summary {
    grid-template-columns: 1fr;
  }

  .summary-card {
    min-height: 64px;
  }
}
</style>
