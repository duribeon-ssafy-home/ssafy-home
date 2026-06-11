import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAdminReports } from '@/api/adminReportApi'
import AdminReportsView from '../AdminReportsView.vue'

vi.mock('@/api/adminReportApi', () => ({
  getAdminReports: vi.fn(),
}))

const routerLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a href="#"><slot /></a>',
}

const pendingReport = {
  reportId: 1,
  userId: 2,
  reporterEmail: 'buyer@example.com',
  reporterNickname: '집찾는하나',
  propertyId: 10,
  propertyTitle: '강남 역세권 원룸',
  propertyAddress: '서울특별시 강남구 역삼동',
  reason: 'FAKE_LISTING',
  content: '허위 매물로 의심됩니다.',
  status: 'PENDING',
  createdAt: '2026-06-10T09:00:00',
}

describe('AdminReportsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAdminReports.mockResolvedValue([{ ...pendingReport }])
  })

  it('진입 시 관리자 신고 목록을 조회하고 테이블에 표시한다', async () => {
    const wrapper = mountAdminReportsView()
    await flushPromises()

    expect(getAdminReports).toHaveBeenCalledWith({})
    expect(wrapper.text()).toContain('강남 역세권 원룸')
    expect(wrapper.text()).toContain('허위 매물')
    expect(wrapper.text()).toContain('집찾는하나')
    expect(wrapper.text()).toContain('대기')
    expect(wrapper.text()).toContain('총 1건')
  })

  it('상태 필터를 클릭하면 status로 재조회한다', async () => {
    getAdminReports.mockResolvedValueOnce([]).mockResolvedValueOnce([
      {
        ...pendingReport,
        reportId: 2,
        propertyTitle: '사진 불일치 오피스텔',
        reason: 'PHOTO_MISMATCH',
        status: 'RESOLVED',
      },
    ])

    const wrapper = mountAdminReportsView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '처리 완료').trigger('click')
    await flushPromises()

    expect(getAdminReports).toHaveBeenLastCalledWith({ status: 'RESOLVED' })
    expect(wrapper.text()).toContain('처리 완료 상태 신고')
    expect(wrapper.text()).toContain('사진 불일치 오피스텔')
    expect(wrapper.text()).toContain('사진 불일치')
  })

  it('조회 실패 시 서버 메시지를 표시한다', async () => {
    getAdminReports.mockRejectedValue({
      response: { data: { message: '관리자 권한이 필요합니다.' } },
    })

    const wrapper = mountAdminReportsView()
    await flushPromises()

    expect(wrapper.text()).toContain('관리자 권한이 필요합니다.')
    expect(wrapper.text()).toContain('신고 목록 조회에 실패했습니다.')
  })
})

function mountAdminReportsView() {
  return mount(AdminReportsView, {
    global: {
      stubs: {
        RouterLink: routerLinkStub,
      },
    },
  })
}
