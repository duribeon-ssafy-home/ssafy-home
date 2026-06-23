import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getProperty, getPropertyRisk } from '@/api/propertyApi'
import { createPropertyReport } from '@/api/reportApi'
import { useAuthStore } from '@/stores/auth'
import PropertyDetailView from '../PropertyDetailView.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a href="#"><slot /></a>',
  },
  useRouter: () => ({
    push,
    currentRoute: { value: { fullPath: '/properties/10' } },
  }),
}))

vi.mock('@/api/propertyApi', () => ({
  getProperty: vi.fn(),
  getPropertyRisk: vi.fn(),
}))

vi.mock('@/api/reportApi', () => ({
  createPropertyReport: vi.fn(),
}))

vi.mock('@/composables/useFavorites', () => ({
  useFavorites: () => ({
    loadFavorites: vi.fn(),
    toggleFavorite: vi.fn(),
    isFavorited: vi.fn(() => false),
  }),
}))

const property = {
  propertyId: 10,
  title: '강남 역세권 원룸',
  roadAddress: '서울특별시 강남구 역삼동',
  rentType: 'MONTHLY',
  deposit: 1000,
  monthlyRent: 60,
  roomType: 'ONE_ROOM',
  area: 20.5,
  floor: 3,
  buildYear: 2020,
  images: [],
}

const risk = {
  propertyId: 10,
  label: 'CAUTION',
  score: 35,
  reportCount: 0,
  ownerVerified: false,
  avgMonthlyRent: 58,
  avgDeposit: 900,
  priceGapRate: 3.2,
}

describe('PropertyDetailView', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    getProperty.mockResolvedValue({ ...property })
    getPropertyRisk.mockResolvedValue({ ...risk })
    createPropertyReport.mockResolvedValue({ reportId: 1, reason: 'FRAUD_SUSPECTED' })
  })

  it('비로그인 사용자가 신고를 누르면 로그인 화면으로 보낸다', async () => {
    const { wrapper } = mountPropertyDetail()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '의심 매물 신고').trigger('click')

    expect(push).toHaveBeenCalledWith({
      name: 'login',
      query: { redirect: '/properties/10' },
    })
    expect(createPropertyReport).not.toHaveBeenCalled()
  })

  it('신고 사유와 내용을 백엔드 형식에 맞춰 접수한다', async () => {
    getPropertyRisk
      .mockResolvedValueOnce({ ...risk })
      .mockResolvedValueOnce({ ...risk, reportCount: 1 })

    const { wrapper } = mountPropertyDetail({ authenticated: true })
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '의심 매물 신고').trigger('click')
    await wrapper.find('input[value="FRAUD_SUSPECTED"]').setValue()
    await wrapper.find('textarea').setValue('계약 전 요구 금액이 설명과 다릅니다.')
    await wrapper.find('form.report-form').trigger('submit')
    await flushPromises()

    expect(createPropertyReport).toHaveBeenCalledWith(10, {
      reason: 'FRAUD_SUSPECTED',
      content: '계약 전 요구 금액이 설명과 다릅니다.',
    })
    expect(getPropertyRisk).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('신고가 접수되었습니다.')
    expect(wrapper.text()).toContain('신고 건수')
    expect(wrapper.text()).toContain('1건')
  })

  it('중복 신고 오류를 사용자 메시지로 표시한다', async () => {
    createPropertyReport.mockRejectedValue({
      response: { data: { errorCode: 'REPORT_ALREADY_EXISTS', message: '이미 신고한 매물입니다.' } },
    })

    const { wrapper } = mountPropertyDetail({ authenticated: true })
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '의심 매물 신고').trigger('click')
    await wrapper.find('form.report-form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('이미 신고한 매물입니다.')
  })
})

function mountPropertyDetail(options = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const authStore = useAuthStore()
  if (options.authenticated) {
    authStore.setSession({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      user: { id: 1, role: 'BUYER' },
    })
  }

  const wrapper = mount(PropertyDetailView, {
    props: { id: '10' },
    global: {
      plugins: [pinia],
    },
  })

  return { wrapper, authStore }
}
