import { describe, expect, it, vi } from 'vitest'
import {
  LIFESTYLE_RESULT_STORAGE_KEY,
  createLifestyleAnswerPayload,
  createLifestyleResultSnapshot,
  readLifestyleResultSnapshot,
  writeLifestyleResultSnapshot,
} from '../lifestyleResultSession'

function createMemoryStorage(initialValue = null) {
  let value = initialValue

  return {
    getItem: vi.fn(() => value),
    setItem: vi.fn((key, nextValue) => {
      value = nextValue
    }),
    removeItem: vi.fn(() => {
      value = null
    }),
  }
}

describe('lifestyleResultSession', () => {
  it('질문과 답변 맵을 백엔드 요청 payload로 변환한다', () => {
    const questions = [{ questionId: 1 }, { questionId: 2 }, { questionId: 3 }]
    const answerMap = {
      1: 'A',
      2: 'B',
      3: 'A',
    }

    expect(createLifestyleAnswerPayload(questions, answerMap)).toEqual({
      answers: [
        { questionId: 1, selectedOption: 'A' },
        { questionId: 2, selectedOption: 'B' },
        { questionId: 3, selectedOption: 'A' },
      ],
    })
  })

  it('예산 입력값이 있으면 payload에 함께 담는다', () => {
    const questions = [{ questionId: 1 }, { questionId: 2 }]
    const answerMap = {
      1: 'A',
      2: 'B',
    }

    expect(
      createLifestyleAnswerPayload(questions, answerMap, {
        monthlyRentMax: 65,
        depositMax: 2000,
      }),
    ).toEqual({
      answers: [
        { questionId: 1, selectedOption: 'A' },
        { questionId: 2, selectedOption: 'B' },
      ],
      monthlyRentMax: 65,
      depositMax: 2000,
    })
  })

  it('결과와 답변을 저장 상태가 포함된 스냅샷으로 만든다', () => {
    const result = {
      lifestyleType: 'LIVING_COST_HOME_BALANCED',
      filterPreset: { monthlyRentMax: 50 },
    }
    const answers = [{ questionId: 1, selectedOption: 'A' }]
    const snapshot = createLifestyleResultSnapshot(result, answers, {
      saved: false,
      budget: { monthlyRentMax: 50 },
    })

    expect(snapshot).toMatchObject({
      lifestyleType: 'LIVING_COST_HOME_BALANCED',
      filterPreset: { monthlyRentMax: 50 },
      answers,
      budget: { monthlyRentMax: 50 },
      saved: false,
      savedAt: null,
    })
  })

  it('스냅샷을 저장하고 다시 읽는다', () => {
    const storage = createMemoryStorage()
    const snapshot = {
      lifestyleType: 'LOCATION_FLEXIBLE_COMPACT',
      answers: [],
      saved: true,
    }

    writeLifestyleResultSnapshot(snapshot, storage)

    expect(storage.setItem).toHaveBeenCalledWith(
      LIFESTYLE_RESULT_STORAGE_KEY,
      JSON.stringify(snapshot),
    )
    expect(readLifestyleResultSnapshot(storage)).toEqual(snapshot)
  })

  it('깨진 스냅샷은 제거하고 null을 반환한다', () => {
    const storage = createMemoryStorage('{')

    expect(readLifestyleResultSnapshot(storage)).toBeNull()
    expect(storage.removeItem).toHaveBeenCalledWith(LIFESTYLE_RESULT_STORAGE_KEY)
  })
})
