import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getMyLatestLifestyleResult } from '@/api/lifestyleApi'
import { getProperties } from '@/api/propertyApi'
import { getRecommendations } from '@/api/recommendationApi'
import { useAuthStore } from '@/stores/auth'
import FilterBar from '@/components/FilterBar.vue'
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

vi.mock('@/composables/useFavorites', () => ({
  useFavorites: () => ({
    loadFavorites: vi.fn(),
  }),
}))

const lifestyleResult = {
  lifestyleType: 'LIVING_COST_COMPACT',
  typeName: '알뜰이',
  filterPreset: {
    facilityScoreMin: 70,
    facilityCountMin: 20,
    monthlyRentMax: 50,
    depositMax: 1000,
    areaMin: null,
    buildYearMin: null,
    preferredSido: '부산광역시',
    preferredGugun: '사하구',
    preferredDong: '하단동',
  },
}

describe('HomeView', () => {
  beforeEach(() => {
    localStorage.clear()
    document.body.innerHTML = ''
    vi.clearAllMocks()
    Element.prototype.scrollIntoView = vi.fn()
    getProperties.mockResolvedValue(createPage())
    getRecommendations.mockResolvedValue(createPage())
    getMyLatestLifestyleResult.mockRejectedValue({
      response: { data: { errorCode: 'LIFESTYLE_RESULT_NOT_FOUND' } },
    })
  })

  it('비로그인 사용자는 조건 없이 전체 매물을 조회한다', async () => {
    mountHome()
    await flushPromises()

    expect(getMyLatestLifestyleResult).not.toHaveBeenCalled()
    expect(getProperties).toHaveBeenCalledWith({
      page: 0,
      size: 6,
      sort: 'createdAt,desc',
    })
    expect(getRecommendations).not.toHaveBeenCalled()
  })

  it('로그인 사용자의 생활 성향 결과가 있으면 저장된 선호 조건으로 추천 API를 조회한다', async () => {
    getMyLatestLifestyleResult.mockResolvedValue(lifestyleResult)

    const { wrapper } = mountHome({ authenticated: true })
    await flushPromises()

    expect(getRecommendations).toHaveBeenCalledWith({
      sido: '부산광역시',
      gugun: '사하구',
      dong: '하단동',
      facilityCountMin: 20,
      maxMonthlyRent: 50,
      maxDeposit: 1000,
      page: 0,
      size: 6,
    })
    expect(getProperties).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="location-input"]').element.value).toBe(
      '부산광역시 사하구 하단동',
    )
    expect(wrapper.get('[data-testid="deposit-select"]').element.value).toBe('1000')
    expect(wrapper.get('[data-testid="monthly-rent-select"]').element.value).toBe('50')
    expect(wrapper.get('[data-testid="room-type-ALL"]').classes()).toContain('filter-chip--active')
    expect(wrapper.get('[data-testid="recommendation-notice"]').text()).toContain('알뜰이 기준')
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
      sido: '부산광역시',
      gugun: '사하구',
      dong: '하단동',
      maxMonthlyRent: 80,
      maxDeposit: 1000,
      page: 0,
      size: 6,
    })
  })

  it('추천 API가 실패하면 같은 조건으로 일반 매물 API를 조회한다', async () => {
    getMyLatestLifestyleResult.mockResolvedValue(lifestyleResult)
    getRecommendations.mockRejectedValue(new Error('recommendation failed'))

    mountHome({ authenticated: true })
    await flushPromises()

    expect(getRecommendations).toHaveBeenCalledWith({
      sido: '부산광역시',
      gugun: '사하구',
      dong: '하단동',
      facilityCountMin: 20,
      maxMonthlyRent: 50,
      maxDeposit: 1000,
      page: 0,
      size: 6,
    })
    expect(getProperties).toHaveBeenCalledWith({
      sido: '부산광역시',
      gugun: '사하구',
      dong: '하단동',
      facilityCountMin: 20,
      maxMonthlyRent: 50,
      maxDeposit: 1000,
      page: 0,
      size: 6,
      sort: 'createdAt,desc',
    })
  })

  it('검색 폼을 제출하면 검색 후 추천 매물 영역으로 부드럽게 이동한다', async () => {
    const { wrapper } = mountHome()
    await flushPromises()
    Element.prototype.scrollIntoView.mockClear()

    await wrapper.get('[data-testid="location-input"]').setValue('부산광역시')
    await wrapper.get('form.filter-bar').trigger('submit')
    await flushPromises()

    expect(getProperties).toHaveBeenLastCalledWith({
      sido: '부산광역시',
      page: 0,
      size: 6,
      sort: 'createdAt,desc',
    })
    expect(Element.prototype.scrollIntoView).toHaveBeenCalledWith({
      behavior: 'smooth',
      block: 'start',
    })
  })

  it('전체 주소를 직접 입력하면 시도, 구군, 동으로 분리해 검색한다', async () => {
    const { wrapper } = mountHome()
    await flushPromises()
    getProperties.mockClear()

    await wrapper.get('[data-testid="location-input"]').setValue('부산광역시 사하구 하단동')
    await wrapper.get('form.filter-bar').trigger('submit')
    await flushPromises()

    expect(getProperties).toHaveBeenCalledWith({
      sido: '부산광역시',
      gugun: '사하구',
      dong: '하단동',
      page: 0,
      size: 6,
      sort: 'createdAt,desc',
    })
  })

  it('자동완성으로 선택한 지역 값으로 검색한다', async () => {
    const { wrapper } = mountHome()
    await flushPromises()
    getProperties.mockClear()

    wrapper.findComponent(FilterBar).vm.$emit('search', {
      location: '부산광역시 사하구 하단동',
      locationParts: {
        sido: '부산광역시',
        gugun: '사하구',
        dong: '하단동',
      },
      deposit: '',
      monthlyRent: '',
      roomType: 'ALL',
    })
    await flushPromises()

    expect(getProperties).toHaveBeenCalledWith({
      sido: '부산광역시',
      gugun: '사하구',
      dong: '하단동',
      page: 0,
      size: 6,
      sort: 'createdAt,desc',
    })
  })

  it('로그인 사용자라도 생활 성향 결과가 없으면 일반 매물 API로 조회한다', async () => {
    mountHome({ authenticated: true })
    await flushPromises()

    expect(getMyLatestLifestyleResult).toHaveBeenCalled()
    expect(getProperties).toHaveBeenCalledWith({
      page: 0,
      size: 6,
      sort: 'createdAt,desc',
    })
    expect(getRecommendations).not.toHaveBeenCalled()
  })

  it('매물 살펴보기 버튼을 누르면 추천 매물 영역으로 부드럽게 이동한다', async () => {
    const { wrapper } = mountHome()
    await flushPromises()

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '매물 살펴보기')
      .trigger('click')

    expect(Element.prototype.scrollIntoView).toHaveBeenCalledWith({
      behavior: 'smooth',
      block: 'start',
    })
  })
})

function createPage(content = []) {
  return {
    content,
    totalPages: 0,
    totalElements: content.length,
    currentPage: 0,
  }
}

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
    attachTo: document.body,
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
