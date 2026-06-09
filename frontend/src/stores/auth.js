import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe, login, logout as requestLogout, refreshTokens, signup } from '@/api/authApi'

const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const AUTH_USER_KEY = 'authUser'

function readStoredUser() {
  const rawUser = localStorage.getItem(AUTH_USER_KEY)

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser)
  } catch {
    localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  const user = ref(readStoredUser())
  const isInitialized = ref(false)

  const isAuthenticated = computed(() => Boolean(accessToken.value))
  const role = computed(() => user.value?.role || '')
  const isBuyer = computed(() => role.value === 'BUYER')
  const isAgent = computed(() => role.value === 'AGENT')
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setSession(session) {
    accessToken.value = session.accessToken || ''
    refreshToken.value = session.refreshToken || ''
    user.value = session.user || user.value || null

    /*
     * 현재 백엔드는 Bearer JWT 기반 SPA 인증을 사용하므로 새로고침 복구를 위해
     * 토큰을 localStorage에 저장한다. 운영 환경에서는 XSS 방어와 토큰 만료 정책을
     * 함께 관리해야 하며, 가능하다면 httpOnly cookie 전략도 검토할 수 있다.
     */
    if (accessToken.value) {
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken.value)
    } else {
      localStorage.removeItem(ACCESS_TOKEN_KEY)
    }

    if (refreshToken.value) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken.value)
    } else {
      localStorage.removeItem(REFRESH_TOKEN_KEY)
    }

    if (user.value) {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user.value))
    } else {
      localStorage.removeItem(AUTH_USER_KEY)
    }
  }

  function setAccessToken(token) {
    setSession({
      accessToken: token,
      refreshToken: refreshToken.value,
      user: user.value,
    })
  }

  function setUser(nextUser) {
    setSession({
      accessToken: accessToken.value,
      refreshToken: refreshToken.value,
      user: nextUser,
    })
  }

  function clearSession() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USER_KEY)
  }

  async function loginWithPassword(payload) {
    const session = await login(payload)
    setSession(session)
    return session
  }

  async function signupAndLogin(payload) {
    await signup(payload)
    return loginWithPassword({
      email: payload.email,
      password: payload.password,
    })
  }

  async function initializeAuth() {
    if (isInitialized.value) {
      return user.value
    }

    if (!accessToken.value) {
      clearSession()
      isInitialized.value = true
      return null
    }

    try {
      const currentUser = await getMe()
      setUser(currentUser)
      return currentUser
    } catch {
      clearSession()
      return null
    } finally {
      isInitialized.value = true
    }
  }

  async function refreshSession() {
    if (!refreshToken.value) {
      throw new Error('Refresh token is missing.')
    }

    const tokenResponse = await refreshTokens({
      refreshToken: refreshToken.value,
    })
    setSession({
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      user: user.value,
    })
    return tokenResponse
  }

  function hasRole(allowedRoles = []) {
    if (!allowedRoles.length) {
      return true
    }

    return allowedRoles.includes(role.value)
  }

  async function logout(options = {}) {
    const { revoke = true } = options
    const tokenToRevoke = refreshToken.value

    if (!revoke || !tokenToRevoke) {
      clearSession()
      return
    }

    try {
      await requestLogout({ refreshToken: tokenToRevoke })
    } catch {
      // 클라이언트 세션은 이미 제거했으므로 서버 토큰 무효화 실패는 화면 흐름을 막지 않는다.
    } finally {
      clearSession()
    }
  }

  return {
    accessToken,
    refreshToken,
    user,
    role,
    isBuyer,
    isAgent,
    isAdmin,
    isInitialized,
    isAuthenticated,
    setAccessToken,
    setSession,
    setUser,
    clearSession,
    loginWithPassword,
    signupAndLogin,
    initializeAuth,
    refreshSession,
    hasRole,
    logout,
  }
})
