import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import { getRecommendations } from '../recommendationApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('recommendationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('추천 매물 목록 응답의 content를 반환한다', async () => {
    const properties = [{ propertyId: 1, title: '추천 매물' }]
    const params = { maxMonthlyRent: 50, roomType: 'ONE_ROOM' }
    api.get.mockResolvedValue({ data: { data: { content: properties } } })

    await expect(getRecommendations(params)).resolves.toBe(properties)

    expect(api.get).toHaveBeenCalledWith('/recommendations', { params })
  })
})
