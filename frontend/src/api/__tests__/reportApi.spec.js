import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import { createPropertyReport } from '../reportApi'

vi.mock('@/api/axios', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('reportApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('매물 신고 접수 응답의 data를 반환한다', async () => {
    const report = { reportId: 1, reason: 'FAKE_LISTING' }
    const payload = { reason: 'FAKE_LISTING', content: '허위 매물로 의심됩니다.' }
    api.post.mockResolvedValue({ data: { data: report } })

    await expect(createPropertyReport(10, payload)).resolves.toBe(report)

    expect(api.post).toHaveBeenCalledWith('/properties/10/reports', payload)
  })
})
