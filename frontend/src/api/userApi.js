import api from '@/api/axios'

export async function getMyProfile() {
  const response = await api.get('/users/me')
  return response.data.data
}

export async function updateMyProfile(payload) {
  const response = await api.patch('/users/me', payload)
  return response.data.data
}

export async function updateMyStatus(payload) {
  const response = await api.patch('/users/me/status', payload)
  return response.data.data
}

export async function changeMyPassword(payload) {
  const response = await api.patch('/users/me/password', payload)
  return response.data.data
}

export async function updateMyRole(payload) {
  const response = await api.patch('/users/me/role', payload)
  return response.data.data
}
