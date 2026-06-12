import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import DetailedFilterBar from '../DetailedFilterBar.vue'

describe('DetailedFilterBar', () => {
  it('기본 제출 시 search 이벤트를 빈 params로 emit한다', async () => {
    const wrapper = mount(DetailedFilterBar)
    await wrapper.find('form').trigger('submit')
    const params = wrapper.emitted('search')?.[0]?.[0]
    expect(params).toBeDefined()
    expect(params.rentType).toBeUndefined()
    expect(params.roomType).toBeUndefined()
  })

  it('전세 버튼 클릭 후 제출 시 rentType: JEONSE를 emit한다', async () => {
    const wrapper = mount(DetailedFilterBar)
    await wrapper.find('[data-testid="rent-JEONSE"]').trigger('click')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('search')?.[0]?.[0].rentType).toBe('JEONSE')
  })

  it('월세 버튼 클릭 후 제출 시 rentType: MONTHLY를 emit한다', async () => {
    const wrapper = mount(DetailedFilterBar)
    await wrapper.find('[data-testid="rent-MONTHLY"]').trigger('click')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('search')?.[0]?.[0].rentType).toBe('MONTHLY')
  })

  it('방 타입 원룸 선택 후 제출 시 roomType: ONE_ROOM을 emit한다', async () => {
    const wrapper = mount(DetailedFilterBar)
    await wrapper.find('[data-testid="room-ONE_ROOM"]').trigger('click')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('search')?.[0]?.[0].roomType).toBe('ONE_ROOM')
  })

  it('보증금 최대값 입력 후 제출 시 maxDeposit을 숫자로 emit한다', async () => {
    const wrapper = mount(DetailedFilterBar)
    await wrapper.find('[data-testid="deposit-max"]').setValue('5000')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('search')?.[0]?.[0].maxDeposit).toBe(5000)
  })

  describe('지역 입력 파싱', () => {
    async function searchWithLocation(location) {
      const wrapper = mount(DetailedFilterBar)
      await wrapper.find('.filter-input').setValue(location)
      await wrapper.find('form').trigger('submit')
      return wrapper.emitted('search')?.[0]?.[0]
    }

    it('동 이름만 입력하면 dong 파라미터로 emit한다', async () => {
      const params = await searchWithLocation('하단동')
      expect(params.dong).toBe('하단동')
      expect(params.sido).toBeUndefined()
      expect(params.gugun).toBeUndefined()
    })

    it('시도 약칭만 입력하면 sido 정식명칭으로 emit한다', async () => {
      const params = await searchWithLocation('부산')
      expect(params.sido).toBe('부산광역시')
      expect(params.gugun).toBeUndefined()
      expect(params.dong).toBeUndefined()
    })

    it('시도 정식명칭만 입력하면 sido 파라미터로 emit한다', async () => {
      const params = await searchWithLocation('부산광역시')
      expect(params.sido).toBe('부산광역시')
      expect(params.dong).toBeUndefined()
    })

    it('구 이름만 입력하면 gugun 파라미터로 emit한다', async () => {
      const params = await searchWithLocation('사하구')
      expect(params.gugun).toBe('사하구')
      expect(params.sido).toBeUndefined()
      expect(params.dong).toBeUndefined()
    })

    it('시도 약칭 + 동 이름을 입력하면 sido와 dong으로 분리해 emit한다', async () => {
      const params = await searchWithLocation('부산 하단동')
      expect(params.sido).toBe('부산광역시')
      expect(params.dong).toBe('하단동')
      expect(params.gugun).toBeUndefined()
    })

    it('시도 정식명칭 + 동 이름을 입력하면 sido와 dong으로 분리해 emit한다', async () => {
      const params = await searchWithLocation('부산광역시 하단동')
      expect(params.sido).toBe('부산광역시')
      expect(params.dong).toBe('하단동')
      expect(params.gugun).toBeUndefined()
    })

    it('구 + 동 이름을 입력하면 gugun과 dong으로 분리해 emit한다', async () => {
      const params = await searchWithLocation('사하구 하단동')
      expect(params.gugun).toBe('사하구')
      expect(params.dong).toBe('하단동')
      expect(params.sido).toBeUndefined()
    })

    it('시도 + 구 + 동을 모두 입력하면 세 파라미터 모두 emit한다', async () => {
      const params = await searchWithLocation('부산 사하구 하단동')
      expect(params.sido).toBe('부산광역시')
      expect(params.gugun).toBe('사하구')
      expect(params.dong).toBe('하단동')
    })

    it('빈 문자열 입력 시 지역 파라미터를 emit하지 않는다', async () => {
      const params = await searchWithLocation('')
      expect(params.sido).toBeUndefined()
      expect(params.gugun).toBeUndefined()
      expect(params.dong).toBeUndefined()
    })
  })
})
