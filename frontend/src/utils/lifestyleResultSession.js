export const LIFESTYLE_RESULT_STORAGE_KEY = 'lifestyleResult'

export function createLifestyleAnswerPayload(questions, answerMap) {
  return {
    answers: questions.map((question) => ({
      questionId: question.questionId,
      selectedOption: answerMap[question.questionId],
    })),
  }
}

export function createLifestyleResultSnapshot(result, answers = [], options = {}) {
  const { saved = false } = options

  return {
    ...result,
    answers,
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
