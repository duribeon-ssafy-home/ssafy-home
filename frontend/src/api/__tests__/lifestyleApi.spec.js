import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import {
  getLifestyleQuestions,
  getMyLatestLifestyleResult,
  previewLifestyleResult,
  saveLifestyleResult,
} from '../lifestyleApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('lifestyleApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('생활 성향 질문 조회 응답의 data를 반환한다', async () => {
    const questions = [{ questionId: 1, selectedOption: 'A' }]
    api.get.mockResolvedValue({ data: { data: questions } })

    await expect(getLifestyleQuestions()).resolves.toBe(questions)

    expect(api.get).toHaveBeenCalledWith('/lifestyle/questions')
  })

  it('비로그인 미리보기 요청을 보낸다', async () => {
    const payload = { answers: [{ questionId: 1, selectedOption: 'A' }] }
    const result = { lifestyleType: 'LIVING_COST_HOME_BALANCED' }
    api.post.mockResolvedValue({ data: { data: result } })

    await expect(previewLifestyleResult(payload)).resolves.toBe(result)

    expect(api.post).toHaveBeenCalledWith('/lifestyle/results/preview', payload)
  })

  it('로그인 저장 요청을 보낸다', async () => {
    const payload = { answers: [{ questionId: 1, selectedOption: 'A' }] }
    const result = { lifestyleType: 'LIVING_COST_HOME_BALANCED' }
    api.post.mockResolvedValue({ data: { data: result } })

    await expect(saveLifestyleResult(payload)).resolves.toBe(result)

    expect(api.post).toHaveBeenCalledWith('/lifestyle/results', payload)
  })

  it('내 최근 생활 성향 결과를 조회한다', async () => {
    const result = { lifestyleType: 'LOCATION_FLEXIBLE_COMPACT' }
    api.get.mockResolvedValue({ data: { data: result } })

    await expect(getMyLatestLifestyleResult()).resolves.toBe(result)

    expect(api.get).toHaveBeenCalledWith('/lifestyle/results/me')
  })
})
