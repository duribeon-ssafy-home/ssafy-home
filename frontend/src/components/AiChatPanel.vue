<script setup>
import { ref } from 'vue'
import { chatWithAi } from '@/api/aiApi'

const props = defineProps({
  propertyIds: {
    type: Array,
    default: () => [],
  },
})

const question = ref('')
const answer = ref('')
const isLoading = ref(false)
const error = ref('')

const SUGGESTED_QUESTIONS = [
  '이 매물 중 전세사기 위험이 가장 낮은 곳은?',
  '내 라이프스타일에 가장 잘 맞는 매물은?',
  '계약 전에 꼭 확인해야 할 사항은?',
  '보증금이 시세 대비 비싼 매물이 있나요?',
]

async function submit() {
  if (!question.value.trim() || isLoading.value) return

  isLoading.value = true
  error.value = ''
  answer.value = ''

  try {
    answer.value = await chatWithAi({
      question: question.value.trim(),
      propertyIds: props.propertyIds,
    })
  } catch (e) {
    error.value = 'AI 답변을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    isLoading.value = false
  }
}

function useSuggested(q) {
  question.value = q
  submit()
}
</script>

<template>
  <section class="ai-panel">
    <h3 class="ai-panel__title">AI 매물 상담</h3>

    <div class="ai-panel__suggestions">
      <button
        v-for="q in SUGGESTED_QUESTIONS"
        :key="q"
        class="ai-panel__chip"
        @click="useSuggested(q)"
      >
        {{ q }}
      </button>
    </div>

    <form class="ai-panel__form" @submit.prevent="submit">
      <input
        v-model="question"
        class="ai-panel__input"
        placeholder="매물 비교, 계약 주의사항, 전세사기 예방 등 무엇이든 질문하세요"
        :disabled="isLoading"
      />
      <button type="submit" class="ai-panel__submit" :disabled="isLoading || !question.trim()">
        {{ isLoading ? '분석 중...' : '질문하기' }}
      </button>
    </form>

    <div v-if="isLoading" class="ai-panel__loading">AI가 분석 중입니다...</div>
    <div v-if="error" class="ai-panel__error">{{ error }}</div>
    <div v-if="answer" class="ai-panel__answer">
      <p class="ai-panel__answer-label">AI 답변</p>
      <p class="ai-panel__answer-text">{{ answer }}</p>
    </div>
  </section>
</template>

<style scoped>
.ai-panel {
  margin-top: 2rem;
  padding: 1.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.ai-panel__title {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 1rem;
  color: #1e293b;
}

.ai-panel__suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.ai-panel__chip {
  padding: 0.35rem 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  background: #fff;
  font-size: 0.8rem;
  cursor: pointer;
  color: #475569;
  transition: background 0.15s;
}

.ai-panel__chip:hover {
  background: #e2e8f0;
}

.ai-panel__form {
  display: flex;
  gap: 0.5rem;
}

.ai-panel__input {
  flex: 1;
  padding: 0.6rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
}

.ai-panel__submit {
  padding: 0.6rem 1.2rem;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}

.ai-panel__submit:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.ai-panel__loading {
  margin-top: 1rem;
  color: #64748b;
  font-size: 0.9rem;
}

.ai-panel__error {
  margin-top: 1rem;
  color: #ef4444;
  font-size: 0.9rem;
}

.ai-panel__answer {
  margin-top: 1rem;
  padding: 1rem;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.ai-panel__answer-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #3b82f6;
  margin-bottom: 0.5rem;
}

.ai-panel__answer-text {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #334155;
  white-space: pre-wrap;
}
</style>
