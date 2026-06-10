import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { getMyLatestLifestyleResult } from '@/api/lifestyleApi'
import { getMyProfile, updateMyProfile, updateMyRole, updateMyStatus } from '@/api/userApi'
import { useAuthStore } from '@/stores/auth'
import MyPageView from '../MyPageView.vue'

const { routerPush } = vi.hoisted(() => ({
  routerPush: vi.fn(),
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a href="#"><slot /></a>',
  },
  useRouter: () => ({
    push: routerPush,
  }),
}))

vi.mock('@/api/userApi', () => ({
  getMyProfile: vi.fn(),
  updateMyProfile: vi.fn(),
  updateMyRole: vi.fn(),
  updateMyStatus: vi.fn(),
}))

vi.mock('@/api/lifestyleApi', () => ({
  getMyLatestLifestyleResult: vi.fn(),
}))

const buyerProfile = {
  id: 1,
  email: 'buyer@example.com',
  name: '홍길동',
  nickname: '길동',
  phoneNumber: '',
  role: 'BUYER',
  status: 'ACTIVE',
}

describe('MyPageView', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    routerPush.mockReset()
    getMyProfile.mockResolvedValue({ ...buyerProfile })
    getMyLatestLifestyleResult.mockRejectedValue({
      response: { data: { errorCode: 'LIFESTYLE_RESULT_NOT_FOUND' } },
    })
  })

  it('프로필 수정 후 화면 상태와 authStore.user를 동기화한다', async () => {
    const { wrapper, authStore } = mountMyPage()
    await flushPromises()

    await wrapper.find('[data-testid="edit-profile-button"]').trigger('click')
    await wrapper.find('[data-testid="profile-name-input"]').setValue('김싸피')
    await wrapper.find('[data-testid="profile-nickname-input"]').setValue('싸피홈')
    await wrapper.find('[data-testid="profile-phone-input"]').setValue('01012345678')

    const updatedProfile = {
      ...buyerProfile,
      name: '김싸피',
      nickname: '싸피홈',
      phoneNumber: '01012345678',
    }
    updateMyProfile.mockResolvedValue(updatedProfile)

    await wrapper.find('form.profile-form').trigger('submit')
    await flushPromises()

    expect(updateMyProfile).toHaveBeenCalledWith({
      name: '김싸피',
      nickname: '싸피홈',
      phoneNumber: '01012345678',
    })
    expect(authStore.user.name).toBe('김싸피')
    expect(authStore.user.nickname).toBe('싸피홈')
    expect(wrapper.text()).toContain('프로필 정보가 저장되었습니다.')
  })

  it('BUYER가 전화번호를 입력하면 AGENT로 전환한다', async () => {
    const { wrapper, authStore } = mountMyPage()
    await flushPromises()

    updateMyRole.mockResolvedValue({
      id: buyerProfile.id,
      role: 'AGENT',
      phoneNumber: '01099998888',
    })

    await wrapper.find('[data-testid="role-phone-input"]').setValue('01099998888')
    await wrapper.find('form.role-upgrade').trigger('submit')
    await flushPromises()
    await nextTick()

    expect(updateMyRole).toHaveBeenCalledWith({
      role: 'AGENT',
      phoneNumber: '01099998888',
    })
    expect(authStore.user.role).toBe('AGENT')
    expect(wrapper.find('[data-testid="agent-entry-button"]').exists()).toBe(true)
  })

  it('계정 비활성화 성공 시 세션을 정리하고 로그인 화면으로 이동한다', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { wrapper, authStore } = mountMyPage()
    await flushPromises()

    updateMyStatus.mockResolvedValue({
      id: buyerProfile.id,
      status: 'INACTIVE',
    })

    await wrapper.find('[data-testid="inactive-account-button"]').trigger('click')
    await flushPromises()

    expect(confirmSpy).toHaveBeenCalled()
    expect(updateMyStatus).toHaveBeenCalledWith({ status: 'INACTIVE' })
    expect(authStore.isAuthenticated).toBe(false)
    expect(routerPush).toHaveBeenCalledWith({
      name: 'login',
      query: { reason: 'inactive' },
    })

    confirmSpy.mockRestore()
  })
})

function mountMyPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()

  authStore.setSession({
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    user: { ...buyerProfile },
  })

  const wrapper = mount(MyPageView, {
    global: {
      plugins: [pinia],
    },
  })

  return { wrapper, authStore }
}
