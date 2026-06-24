import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const AUTH_RECOVERY_SKIP_PATHS = ['/auth/login', '/auth/signup', '/auth/refresh', '/auth/logout']
const PUBLIC_GET_PATHS = ['/lifestyle/questions', '/locations', '/properties']
const PUBLIC_POST_PATHS = ['/lifestyle/results/preview']

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

let isRefreshing = false
let pendingRequests = []

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)

  if (token && shouldAttachAccessToken(config)) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { response, config } = error

    if (!response || !config) {
      return Promise.reject(error)
    }

    if (response.status === 401 && shouldHandleAuthError(config)) {
      if (shouldRefresh(config)) {
        return retryWithRefreshedToken(config)
      }

      await clearAuthAndRedirectToLogin()
    }

    if (response.status === 403 && shouldHandleAuthError(config)) {
      await redirectToForbidden(config.url)
    }

    return Promise.reject(error)
  },
)

function shouldRefresh(config) {
  return (
    !config._retry &&
    Boolean(localStorage.getItem(REFRESH_TOKEN_KEY))
  )
}

function shouldAttachAccessToken(config) {
  return !shouldSkipAuthRecovery(config.url) && !isPublicRequest(config)
}

function shouldHandleAuthError(config) {
  return !shouldSkipAuthRecovery(config.url) && !isPublicRequest(config)
}

function shouldSkipAuthRecovery(url = '') {
  const requestPath = normalizeRequestPath(url)
  return AUTH_RECOVERY_SKIP_PATHS.includes(requestPath)
}

function isPublicRequest(config) {
  const requestPath = normalizeRequestPath(config.url)
  const method = (config.method || 'get').toLowerCase()

  if (method !== 'get') {
    return method === 'post' && PUBLIC_POST_PATHS.includes(requestPath)
  }

  return PUBLIC_GET_PATHS.includes(requestPath) ||
    (/^\/properties\/[^/]+$/.test(requestPath) &&
      !requestPath.startsWith('/properties/me'))
}

function normalizeRequestPath(url = '') {
  if (/^https?:\/\//.test(url)) {
    const parsedUrl = new URL(url)
    const basePath = new URL(API_BASE_URL).pathname
    return stripApiBasePath(parsedUrl.pathname, basePath)
  }

  const [path] = url.split('?')
  return path.startsWith('/') ? path : `/${path}`
}

function stripApiBasePath(path, basePath) {
  if (basePath !== '/' && path.startsWith(basePath)) {
    return path.slice(basePath.length) || '/'
  }

  return path
}

async function retryWithRefreshedToken(config) {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingRequests.push({ resolve, reject })
    }).then((token) => {
      config.headers.Authorization = `Bearer ${token}`
      return api(config)
    })
  }

  config._retry = true
  isRefreshing = true

  try {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    const response = await refreshClient.post('/auth/refresh', { refreshToken })
    const tokenResponse = response.data.data

    localStorage.setItem(ACCESS_TOKEN_KEY, tokenResponse.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken)

    await syncAuthStoreTokens(tokenResponse)
    resolvePendingRequests(tokenResponse.accessToken)

    config.headers.Authorization = `Bearer ${tokenResponse.accessToken}`
    return api(config)
  } catch (refreshError) {
    rejectPendingRequests(refreshError)
    await clearAuthAndRedirectToLogin()
    return Promise.reject(refreshError)
  } finally {
    isRefreshing = false
  }
}

function resolvePendingRequests(token) {
  pendingRequests.forEach(({ resolve }) => resolve(token))
  pendingRequests = []
}

function rejectPendingRequests(error) {
  pendingRequests.forEach(({ reject }) => reject(error))
  pendingRequests = []
}

async function syncAuthStoreTokens(tokenResponse) {
  try {
    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()
    authStore.setSession({
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      user: authStore.user,
    })
  } catch {
    // Pinia가 아직 준비되지 않은 초기 요청에서는 localStorage 갱신만으로 충분하다.
  }
}

async function clearAuthAndRedirectToLogin() {
  try {
    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()
    await authStore.logout({ revoke: false })
  } catch {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem('authUser')
  }

  await redirectToLogin()
}

async function redirectToLogin() {
  const currentPath = `${window.location.pathname}${window.location.search}`

  if (currentPath.startsWith('/login')) {
    return
  }

  await pushRoute({
    name: 'login',
    query: { redirect: currentPath },
  })
}

async function redirectToForbidden(from) {
  if (window.location.pathname === '/forbidden') {
    return
  }

  await pushRoute({
    name: 'forbidden',
    query: from ? { from } : {},
  })
}

async function pushRoute(route) {
  try {
    const { default: router } = await import('@/router')
    await router.push(route)
  } catch {
    const target = route.name === 'forbidden' ? '/forbidden' : '/login'
    window.location.assign(target)
  }
}

export default api
