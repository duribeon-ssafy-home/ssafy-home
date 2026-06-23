import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FilterBar from '../FilterBar.vue'

describe('FilterBar', () => {
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
})
