import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getMyLatestLifestyleResult } from '@/api/lifestyleApi'
import { getProperties } from '@/api/propertyApi'
import { getRecommendations } from '@/api/recommendationApi'
import { useAuthStore } from '@/stores/auth'
import HomeView from '../HomeView.vue'

vi.mock('@/api/lifestyleApi', () => ({
  getMyLatestLifestyleResult: vi.fn(),
}))

vi.mock('@/api/propertyApi', () => ({
  getProperties: vi.fn(),
}))

vi.mock('@/api/recommendationApi', () => ({
  getRecommendations: vi.fn(),
}))

const lifestyleResult = {
  lifestyleType: 'LIVING_COST_COMPACT',
  typeName: '생활권 중심 실속형',
  filterPreset: {
    facilityScoreMin: 70,
    facilityCountMin: 20,
    monthlyRentMax: 50,
    depositMax: 1000,
    areaMin: null,
    buildYearMin: null,
  },
}

describe('HomeView', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    getProperties.mockResolvedValue([])
    getRecommendations.mockResolvedValue([])
    getMyLatestLifestyleResult.mockRejectedValue({
      response: { data: { errorCode: 'LIFESTYLE_RESULT_NOT_FOUND' } },
    })
  })

  it('비로그인 사용자는 일반 매물 API로 조회한다', async () => {
    mountHome()
    await flushPromises()

    expect(getMyLatestLifestyleResult).not.toHaveBeenCalled()
    expect(getProperties).toHaveBeenCalledWith({})
    expect(getRecommendations).not.toHaveBeenCalled()
  })

  it('로그인 사용자의 생활 성향 결과가 있으면 추천 API와 기본 필터를 사용한다', async () => {
    getMyLatestLifestyleResult.mockResolvedValue(lifestyleResult)

    const { wrapper } = mountHome({ authenticated: true })
    await flushPromises()

    expect(getRecommendations).toHaveBeenCalledWith({
      maxDeposit: 1000,
      maxMonthlyRent: 50,
      roomType: 'ONE_ROOM',
    })
    expect(getProperties).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="deposit-select"]').element.value).toBe('1000')
    expect(wrapper.get('[data-testid="monthly-rent-select"]').element.value).toBe('50')
    expect(wrapper.get('[data-testid="room-type-ONE_ROOM"]').classes()).toContain(
      'filter-chip--active',
    )
    expect(wrapper.get('[data-testid="recommendation-notice"]').text()).toContain(
      '생활권 중심 실속형 기준',
    )
  })

  it('추천 모드에서 사용자가 필터를 바꾸면 추천 API를 다시 호출한다', async () => {
    getMyLatestLifestyleResult.mockResolvedValue(lifestyleResult)

    const { wrapper } = mountHome({ authenticated: true })
    await flushPromises()
    getRecommendations.mockClear()

    await wrapper.get('[data-testid="monthly-rent-select"]').setValue('80')
    await wrapper.get('form.filter-bar').trigger('submit')
    await flushPromises()

    expect(getRecommendations).toHaveBeenCalledWith({
      maxDeposit: 1000,
      maxMonthlyRent: 80,
      roomType: 'ONE_ROOM',
    })
  })

  it('로그인 사용자라도 생활 성향 결과가 없으면 일반 매물 API로 조회한다', async () => {
    mountHome({ authenticated: true })
    await flushPromises()

    expect(getMyLatestLifestyleResult).toHaveBeenCalled()
    expect(getProperties).toHaveBeenCalledWith({})
    expect(getRecommendations).not.toHaveBeenCalled()
  })
})

function mountHome(options = {}) {
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

  const wrapper = mount(HomeView, {
    global: {
      plugins: [pinia],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a href="#"><slot /></a>',
        },
        PropertyCard: {
          props: ['property'],
          template: '<article>{{ property.title }}</article>',
        },
      },
    },
  })

  return { wrapper, authStore }
}
