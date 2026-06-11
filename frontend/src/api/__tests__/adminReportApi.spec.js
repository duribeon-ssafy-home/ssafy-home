import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import { getAdminReports } from '../adminReportApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('adminReportApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('관리자 신고 목록 조회 응답의 data를 반환한다', async () => {
    const reports = [{ reportId: 1, status: 'PENDING' }]
    api.get.mockResolvedValue({ data: { data: reports } })

    await expect(getAdminReports()).resolves.toBe(reports)

    expect(api.get).toHaveBeenCalledWith('/admin/reports', { params: {} })
  })

  it('신고 상태 status를 trim해서 전달한다', async () => {
    const reports = [{ reportId: 2, status: 'RESOLVED' }]
    api.get.mockResolvedValue({ data: { data: reports } })

    await expect(getAdminReports({ status: '  RESOLVED  ' })).resolves.toBe(reports)

    expect(api.get).toHaveBeenCalledWith('/admin/reports', { params: { status: 'RESOLVED' } })
  })
})
