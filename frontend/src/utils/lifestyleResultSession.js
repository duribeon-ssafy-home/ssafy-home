export const LIFESTYLE_RESULT_STORAGE_KEY = 'lifestyleResult'

export function createLifestyleAnswerPayload(questions, answerMap, budget = {}) {
  const payload = {
    answers: questions.map((question) => ({
      questionId: question.questionId,
      selectedOption: answerMap[question.questionId],
    })),
  }

  if (budget.monthlyRentMax != null) {
    payload.monthlyRentMax = budget.monthlyRentMax
  }

  if (budget.depositMax != null) {
    payload.depositMax = budget.depositMax
  }

  if (budget.preferredSido) {
    payload.preferredSido = budget.preferredSido
  }

  if (budget.preferredGugun) {
    payload.preferredGugun = budget.preferredGugun
  }

  if (budget.preferredDong) {
    payload.preferredDong = budget.preferredDong
  }

  return payload
}

export function createLifestyleResultSnapshot(result, answers = [], options = {}) {
  const { saved = false, budget = {} } = options

  return {
    ...result,
    answers,
    budget,
    saved,
    savedAt: saved ? new Date().toISOString() : null,
  }
}

export function readLifestyleResultSnapshot(storage = window.sessionStorage) {
  const rawResult = storage.getItem(LIFESTYLE_RESULT_STORAGE_KEY)

  if (!rawResult) {
    return null
  }

  try {
    return JSON.parse(rawResult)
  } catch {
    storage.removeItem(LIFESTYLE_RESULT_STORAGE_KEY)
    return null
  }
}

export function writeLifestyleResultSnapshot(snapshot, storage = window.sessionStorage) {
  storage.setItem(LIFESTYLE_RESULT_STORAGE_KEY, JSON.stringify(snapshot))
}
