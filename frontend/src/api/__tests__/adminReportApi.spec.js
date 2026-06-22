import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import { getAdminReport, getAdminReports, updateAdminReport } from '../adminReportApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
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

  it('관리자 신고 상세 조회 응답의 data를 반환한다', async () => {
    const report = { reportId: 3, status: 'PENDING' }
    api.get.mockResolvedValue({ data: { data: report } })

    await expect(getAdminReport(3)).resolves.toBe(report)

    expect(api.get).toHaveBeenCalledWith('/admin/reports/3')
  })

  it('관리자 신고 상태 변경 응답의 data를 반환한다', async () => {
    const report = { reportId: 3, status: 'HIDDEN' }
    api.patch.mockResolvedValue({ data: { data: report } })

    await expect(updateAdminReport(3, { status: 'HIDDEN' })).resolves.toBe(report)

    expect(api.patch).toHaveBeenCalledWith('/admin/reports/3', { status: 'HIDDEN' })
  })
})
