import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import MapPropertyCard from '../MapPropertyCard.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), currentRoute: { value: { fullPath: '/map' } } }),
}))
vi.mock('@/composables/useFavorites', () => ({
  useFavorites: () => ({ isFavorited: () => false, toggleFavorite: vi.fn() }),
}))
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isAuthenticated: true }),
}))

const base = {
  propertyId: 1,
  title: '역삼 원룸',
  sido: '서울특별시', gugun: '강남구', dong: '역삼동',
  rentType: 'MONTHLY', deposit: 500, monthlyRent: 55,
  roomType: 'ONE_ROOM', area: 23.5, floor: 3,
  latitude: 37.5, longitude: 127.0,
  images: [{ imageUrl: 'https://example.com/img.jpg' }],
}

describe('MapPropertyCard', () => {
  it('월세 가격을 올바르게 표시한다', () => {
    const wrapper = mount(MapPropertyCard, { props: { property: base } })
    expect(wrapper.text()).toContain('월세 55만')
    expect(wrapper.text()).toContain('500만')
  })

  it('전세 가격을 올바르게 표시한다', () => {
    const wrapper = mount(MapPropertyCard, {
      props: { property: { ...base, rentType: 'JEONSE', monthlyRent: 0, deposit: 18000 } },
    })
    expect(wrapper.text()).toContain('전세')
    expect(wrapper.text()).toContain('18,000')
  })

  it('카드 클릭 시 select 이벤트를 propertyId와 함께 emit한다', async () => {
    const wrapper = mount(MapPropertyCard, { props: { property: base } })
    await wrapper.find('.map-property-card').trigger('click')
    expect(wrapper.emitted('select')?.[0]).toEqual([1])
  })

  it('selected prop이 true일 때 --selected 클래스가 붙는다', () => {
    const wrapper = mount(MapPropertyCard, { props: { property: base, selected: true } })
    expect(wrapper.find('.map-property-card').classes()).toContain('map-property-card--selected')
  })

  it('방 타입·면적·층수를 표시한다', () => {
    const wrapper = mount(MapPropertyCard, { props: { property: base } })
    expect(wrapper.text()).toContain('원룸')
    expect(wrapper.text()).toContain('23.5')
    expect(wrapper.text()).toContain('3층')
  })
})
