import api from '@/api/axios'

export async function login(payload) {
  const response = await api.post('/auth/login', payload)
  return response.data.data
}

export async function signup(payload) {
  const response = await api.post('/auth/signup', payload)
  return response.data.data
}

export async function getMe() {
  const response = await api.get('/auth/me')
  return response.data.data
}

export async function refreshTokens(payload) {
  const response = await api.post('/auth/refresh', payload)
  return response.data.data
}

export async function logout(payload) {
  const response = await api.post('/auth/logout', payload)
  return response.data.data
}

export async function requestTemporaryPassword(payload) {
  const response = await api.post('/auth/password/forgot', payload)
  return response.data.data
}
