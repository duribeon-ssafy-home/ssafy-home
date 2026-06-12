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

  it('시설 5개+ 선택 후 제출 시 facilityCountMin: 5를 emit한다', async () => {
    const wrapper = mount(DetailedFilterBar)
    await wrapper.find('[data-testid="facility-5"]').trigger('click')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('search')?.[0]?.[0].facilityCountMin).toBe(5)
  })
})
