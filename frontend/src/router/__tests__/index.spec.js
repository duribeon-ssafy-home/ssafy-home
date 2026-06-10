import { flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import router from '../index'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/authApi', () => ({
  getMe: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  refreshTokens: vi.fn(),
  signup: vi.fn(),
}))

const baseUser = {
  id: 1,
  email: 'user@example.com',
  name: '테스트 사용자',
  nickname: '테스터',
  phoneNumber: '01012345678',
  status: 'ACTIVE',
}

describe('router role guards', () => {
  beforeEach(async () => {
    localStorage.clear()
    setActivePinia(createPinia())

    const authStore = useAuthStore()
    authStore.clearSession()
    authStore.isInitialized = true

    await router.replace('/')
    await flushPromises()
  })

  it('비로그인 사용자가 보호 라우트에 접근하면 redirect 쿼리와 함께 로그인으로 이동한다', async () => {
    const currentRoute = await navigateTo('/agent/properties')

    expect(currentRoute.name).toBe('login')
    expect(currentRoute.query.redirect).toBe('/agent/properties')
  })

  it('AGENT는 내 매물 관리 라우트에 접근할 수 있다', async () => {
    setAuthRole('AGENT')

    const currentRoute = await navigateTo('/agent/properties')

    expect(currentRoute.name).toBe('agent-properties')
  })

  it('BUYER는 관리자 라우트에 접근하면 forbidden으로 이동한다', async () => {
    setAuthRole('BUYER')

    const currentRoute = await navigateTo('/admin/users')

    expect(currentRoute.name).toBe('forbidden')
    expect(currentRoute.query.from).toBe('/admin/users')
  })

  it('ADMIN은 관리자 라우트에 접근할 수 있지만 중개인 라우트는 접근할 수 없다', async () => {
    setAuthRole('ADMIN')

    const adminRoute = await navigateTo('/admin')
    const agentRoute = await navigateTo('/agent/properties')

    expect(adminRoute.name).toBe('admin-dashboard')
    expect(agentRoute.name).toBe('forbidden')
    expect(agentRoute.query.from).toBe('/agent/properties')
  })
})

function setAuthRole(role) {
  const authStore = useAuthStore()

  authStore.setSession({
    accessToken: `${role.toLowerCase()}-access-token`,
    refreshToken: `${role.toLowerCase()}-refresh-token`,
    user: {
      ...baseUser,
      role,
    },
  })
  authStore.isInitialized = true
}

async function navigateTo(path) {
  await router.push(path)
  await flushPromises()

  return router.currentRoute.value
}
