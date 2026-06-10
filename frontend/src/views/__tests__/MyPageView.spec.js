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
    computed: {
      routeName() {
        return typeof this.to === 'object' && this.to !== null ? this.to.name : this.to
      },
    },
    template: '<a href="#" :data-route-name="routeName"><slot /></a>',
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

const adminProfile = {
  ...buyerProfile,
  id: 9,
  email: 'admin@example.com',
  name: '관리자',
  nickname: '운영자',
  phoneNumber: '01011112222',
  role: 'ADMIN',
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
    const agentEntry = wrapper.find('[data-testid="agent-entry-button"]')

    expect(agentEntry.exists()).toBe(true)
    expect(agentEntry.attributes('data-route-name')).toBe('agent-properties')
  })

  it('ADMIN은 관리자 페이지 버튼이 admin-dashboard 라우트로 연결된다', async () => {
    getMyProfile.mockResolvedValue({ ...adminProfile })
    const { wrapper } = mountMyPage(adminProfile)
    await flushPromises()

    const adminEntry = wrapper.find('[data-testid="admin-entry-button"]')

    expect(adminEntry.exists()).toBe(true)
    expect(adminEntry.attributes('data-route-name')).toBe('admin-dashboard')
    expect(wrapper.find('[data-testid="agent-entry-button"]').exists()).toBe(false)
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

function mountMyPage(profile = buyerProfile) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()

  authStore.setSession({
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    user: { ...profile },
  })

  const wrapper = mount(MyPageView, {
    global: {
      plugins: [pinia],
    },
  })

  return { wrapper, authStore }
}
