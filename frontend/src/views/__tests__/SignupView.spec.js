import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@/stores/auth'
import SignupView from '../SignupView.vue'

const { routerPush } = vi.hoisted(() => ({
  routerPush: vi.fn(),
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a href="#"><slot /></a>',
  },
  useRoute: () => ({
    query: {},
  }),
  useRouter: () => ({
    push: routerPush,
  }),
}))

describe('SignupView', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    routerPush.mockReset()
  })

  it('비밀번호 8자 이상 조건을 실시간으로 표시하고 짧은 비밀번호 제출을 막는다', async () => {
    const wrapper = mountSignup()
    const authStore = useAuthStore()
    vi.spyOn(authStore, 'signupAndLogin')

    await wrapper.find('#signup-password').setValue('short')

    expect(wrapper.get('[data-testid="password-rule"]').classes()).toContain(
      'password-rule--invalid',
    )

    await wrapper.find('form.auth-form').trigger('submit')

    expect(authStore.signupAndLogin).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('비밀번호는 8자 이상 입력해주세요.')

    await wrapper.find('#signup-password').setValue('password123')

    expect(wrapper.get('[data-testid="password-rule"]').classes()).toContain(
      'password-rule--valid',
    )
  })
})

function mountSignup() {
  const pinia = createPinia()
  setActivePinia(pinia)

  return mount(SignupView, {
    global: {
      plugins: [pinia],
    },
  })
}
