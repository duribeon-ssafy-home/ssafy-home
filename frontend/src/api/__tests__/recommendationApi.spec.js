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

  it('추천 매물 페이지 응답을 정규화해서 반환한다', async () => {
    const properties = [{ propertyId: 1, title: '추천 매물' }]
    const params = { maxMonthlyRent: 50, roomType: 'ONE_ROOM' }
    const page = {
      content: properties,
      totalPages: 3,
      totalElements: 13,
      number: 1,
    }
    api.get.mockResolvedValue({ data: { data: page } })

    await expect(getRecommendations(params)).resolves.toEqual({
      content: properties,
      totalPages: 3,
      totalElements: 13,
      currentPage: 1,
    })

    expect(api.get).toHaveBeenCalledWith('/recommendations', { params })
  })
})
