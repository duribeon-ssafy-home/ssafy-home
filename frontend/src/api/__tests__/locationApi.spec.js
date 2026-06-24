import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import { searchLocations } from '../locationApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('locationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('지역 자동완성 후보 응답의 data를 반환한다', async () => {
    const locations = [{ code: '2638010300', fullName: '부산광역시 사하구 하단동' }]
    api.get.mockResolvedValue({ data: { data: locations } })

    await expect(searchLocations('하단')).resolves.toBe(locations)

    expect(api.get).toHaveBeenCalledWith('/locations', { params: { keyword: '하단' } })
  })
})
