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
