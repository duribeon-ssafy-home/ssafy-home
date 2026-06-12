import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MapView from '../MapView.vue'

vi.mock('@/api/propertyApi', () => ({
  getProperties: vi.fn(),
}))
vi.mock('@/composables/useFavorites', () => ({
  useFavorites: () => ({ loadFavorites: vi.fn(), isFavorited: () => false, toggleFavorite: vi.fn() }),
}))
vi.mock('@/components/KakaoMap.vue', () => ({
  default: { template: '<div class="kakao-map-stub" />', props: ['properties', 'selectedId'], emits: ['select'] },
}))
vi.mock('@/components/MapPropertyCard.vue', () => ({
  default: {
    template: '<article class="map-property-card" @click="$emit(\'select\', property.propertyId)" />',
    props: ['property', 'selected'],
    emits: ['select'],
  },
}))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

global.IntersectionObserver = class {
  constructor() {}
  observe() {}
  disconnect() {}
}

import { getProperties } from '@/api/propertyApi'

const makePage = (overrides = {}) => ({
  content: [
    { propertyId: 1, title: '테스트 매물', sido: '서울특별시', gugun: '강남구', dong: '역삼동',
      rentType: 'MONTHLY', deposit: 500, monthlyRent: 55, roomType: 'ONE_ROOM',
      area: 23.5, floor: 3, latitude: 37.5, longitude: 127.0, images: [] },
  ],
  totalPages: 1,
  totalElements: 1,
  currentPage: 0,
  ...overrides,
})

beforeEach(() => {
  setActivePinia(createPinia())
  getProperties.mockResolvedValue(makePage())
})

describe('MapView', () => {
  it('마운트 시 getProperties를 호출한다', async () => {
    mount(MapView)
    await flushPromises()
    expect(getProperties).toHaveBeenCalled()
  })

  it('매물 카드가 리스트에 렌더링된다', async () => {
    const wrapper = mount(MapView)
    await flushPromises()
    expect(wrapper.findAll('.map-property-card').length).toBeGreaterThan(0)
  })

  it('카드 select 이벤트 시 인포윈도우가 표시된다', async () => {
    const wrapper = mount(MapView)
    await flushPromises()
    await wrapper.find('.map-property-card').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.info-window').exists()).toBe(true)
  })

  it('총 결과 수를 표시한다', async () => {
    const wrapper = mount(MapView)
    await flushPromises()
    expect(wrapper.text()).toContain('총 1개')
  })
})
