import { describe, it, expect, vi, beforeEach } from 'vitest'
import { chatWithAi } from '../aiApi'
import api from '../axios'

vi.mock('../axios', () => ({
  default: { post: vi.fn() },
}))

describe('chatWithAi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('질문과 매물 IDs를 POST하고 answer를 반환한다', async () => {
    api.post.mockResolvedValue({
      data: { success: true, data: { answer: '전세사기 예방법입니다.' } },
    })

    const result = await chatWithAi({ question: '전세사기 피하려면?', propertyIds: [1, 2] })

    expect(api.post).toHaveBeenCalledWith('/ai/chat', {
      question: '전세사기 피하려면?',
      propertyIds: [1, 2],
    })
    expect(result).toBe('전세사기 예방법입니다.')
  })

  it('propertyIds 없이 호출하면 빈 배열을 전송한다', async () => {
    api.post.mockResolvedValue({
      data: { success: true, data: { answer: '답변' } },
    })

    await chatWithAi({ question: '질문' })

    expect(api.post).toHaveBeenCalledWith('/ai/chat', {
      question: '질문',
      propertyIds: [],
    })
  })
})
