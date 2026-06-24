import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { searchLocations } from '@/api/locationApi'
import FilterBar from '../FilterBar.vue'

vi.mock('@/api/locationApi', () => ({
  searchLocations: vi.fn(),
}))

describe('FilterBar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useRealTimers()
  })

  it('보증금과 월세를 직접 입력해 검색할 수 있다', async () => {
    const wrapper = mount(FilterBar)

    await wrapper.get('[data-testid="deposit-select"]').setValue('1500')
    await wrapper.get('[data-testid="monthly-rent-select"]').setValue('65')
    await wrapper.get('form.filter-bar').trigger('submit')

    expect(wrapper.emitted('search')?.[0]?.[0]).toMatchObject({
      deposit: 1500,
      monthlyRent: 65,
    })
  })

  it('드롭다운 주요 금액에서 보증금과 월세를 선택할 수 있다', async () => {
    const wrapper = mount(FilterBar)

    await wrapper.get('button[aria-label="보증금 주요 금액 선택"]').trigger('click')
    await wrapper.findAll('.amount-menu button').find((button) => button.text() === '1,000만').trigger('click')
    await wrapper.get('button[aria-label="월세 주요 금액 선택"]').trigger('click')
    await wrapper.findAll('.amount-menu button').find((button) => button.text() === '50만').trigger('click')
    await wrapper.get('form.filter-bar').trigger('submit')

    expect(wrapper.get('[data-testid="deposit-select"]').element.value).toBe('1000')
    expect(wrapper.get('[data-testid="monthly-rent-select"]').element.value).toBe('50')
    expect(wrapper.emitted('search')?.[0]?.[0]).toMatchObject({
      deposit: '1000',
      monthlyRent: '50',
    })
  })

  it('지역 자동완성 후보를 선택해 검색할 수 있다', async () => {
    vi.useFakeTimers()
    searchLocations.mockResolvedValue([
      {
        code: '2638010300',
        sido: '부산광역시',
        gugun: '사하구',
        dong: '하단동',
        fullName: '부산광역시 사하구 하단동',
      },
    ])

    const wrapper = mount(FilterBar)

    await wrapper.get('[data-testid="location-input"]').setValue('하단')
    expect(wrapper.get('[data-testid="location-suggestions"]').text()).toContain(
      '지역 후보를 찾고 있습니다',
    )
    await vi.runAllTimersAsync()
    await wrapper.vm.$nextTick()

    expect(searchLocations).toHaveBeenCalledWith('하단')
    await wrapper.get('[data-testid="location-suggestions"] button').trigger('mousedown')
    await wrapper.get('form.filter-bar').trigger('submit')

    expect(wrapper.get('[data-testid="location-input"]').element.value).toBe('부산광역시 사하구 하단동')
    expect(wrapper.emitted('search')?.[0]?.[0]).toMatchObject({
      location: '부산광역시 사하구 하단동',
      locationParts: {
        sido: '부산광역시',
        gugun: '사하구',
        dong: '하단동',
      },
    })
  })

  it('지역 자동완성 후보를 화살표 키와 엔터로 선택할 수 있다', async () => {
    vi.useFakeTimers()
    Element.prototype.scrollIntoView = vi.fn()
    searchLocations.mockResolvedValue([
      {
        code: '4611016100',
        sido: '전라남도',
        gugun: '목포시',
        dong: '달동',
        fullName: '전라남도 목포시 달동',
      },
      {
        code: '4611015800',
        sido: '전라남도',
        gugun: '목포시',
        dong: '상동',
        fullName: '전라남도 목포시 상동',
      },
    ])

    const wrapper = mount(FilterBar)
    const input = wrapper.get('[data-testid="location-input"]')

    await input.setValue('목포')
    await vi.runAllTimersAsync()
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[data-testid="location-suggestions"]').text()).not.toContain('4611016100')

    await input.trigger('keydown', { key: 'ArrowDown' })
    await input.trigger('keydown', { key: 'Enter' })

    expect(wrapper.get('[data-testid="location-input"]').element.value).toBe('전라남도 목포시 상동')
  })
})
