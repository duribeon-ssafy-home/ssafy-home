import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import PasswordField from '../PasswordField.vue'

describe('PasswordField', () => {
  it('비밀번호 입력 필드를 렌더링한다', () => {
    const wrapper = mount(PasswordField, {
      props: {
        id: 'password',
        modelValue: '',
      },
    })

    expect(wrapper.text()).toContain('비밀번호')
    expect(wrapper.find('input').attributes('type')).toBe('password')
    expect(wrapper.find('button').attributes('aria-label')).toBe('비밀번호 표시하기')
  })

  it('비밀번호 보기 버튼을 클릭하면 비밀번호가 표시된다', async () => {
    const wrapper = mount(PasswordField, {
      props: {
        id: 'password',
        modelValue: 'secret-password',
      },
    })

    await wrapper.find('button').trigger('click')

    expect(wrapper.find('input').attributes('type')).toBe('text')
    expect(wrapper.find('button').attributes('aria-label')).toBe('비밀번호 숨기기')
  })

  it('CapsLock이 켜진 상태로 입력하면 안내 문구가 표시된다', async () => {
    const wrapper = mount(PasswordField, {
      props: {
        id: 'password',
        modelValue: '',
      },
    })

    const keydown = new KeyboardEvent('keydown', { bubbles: true })
    Object.defineProperty(keydown, 'getModifierState', {
      value: (key) => key === 'CapsLock',
    })

    wrapper.find('input').element.dispatchEvent(keydown)
    await nextTick()

    expect(wrapper.text()).toContain('CapsLock이 켜져 있습니다')

    await wrapper.find('input').trigger('blur')

    expect(wrapper.text()).not.toContain('CapsLock이 켜져 있습니다')
  })
})
