import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import RangeSlider from '../RangeSlider.vue'

describe('RangeSlider', () => {
  it('min thumb 변경 시 update:modelValue를 emit한다', async () => {
    const wrapper = mount(RangeSlider, {
      props: { min: 0, max: 100, step: 1, modelValue: [20, 80] },
    })
    await wrapper.findAll('input[type=range]')[0].setValue('30')
    expect(wrapper.emitted('update:modelValue')?.[0]?.[0][0]).toBe(30)
    expect(wrapper.emitted('update:modelValue')?.[0]?.[0][1]).toBe(80)
  })

  it('max thumb 변경 시 update:modelValue를 emit한다', async () => {
    const wrapper = mount(RangeSlider, {
      props: { min: 0, max: 100, step: 1, modelValue: [20, 80] },
    })
    await wrapper.findAll('input[type=range]')[1].setValue('70')
    expect(wrapper.emitted('update:modelValue')?.[0]?.[0][1]).toBe(70)
  })

  it('min이 max를 넘으면 max-step으로 클램핑된다', async () => {
    const wrapper = mount(RangeSlider, {
      props: { min: 0, max: 100, step: 1, modelValue: [20, 50] },
    })
    await wrapper.findAll('input[type=range]')[0].setValue('60')
    const emitted = wrapper.emitted('update:modelValue')?.[0]?.[0]
    expect(emitted[0]).toBeLessThanOrEqual(emitted[1])
    expect(emitted[0]).toBe(49) // max(50) - step(1)
  })

  it('max가 min보다 작아지면 min+step으로 클램핑된다', async () => {
    const wrapper = mount(RangeSlider, {
      props: { min: 0, max: 100, step: 1, modelValue: [40, 80] },
    })
    await wrapper.findAll('input[type=range]')[1].setValue('30')
    const emitted = wrapper.emitted('update:modelValue')?.[0]?.[0]
    expect(emitted[1]).toBe(41) // min(40) + step(1)
  })
})
