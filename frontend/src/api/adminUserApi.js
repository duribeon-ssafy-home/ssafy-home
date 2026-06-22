import api from '@/api/axios'

export async function getAdminUsers(params = {}) {
  const response = await api.get('/admin/users', {
    params: normalizeAdminUserParams(params),
  })

  return response.data.data
}

export async function getAdminUser(userId) {
  const response = await api.get(`/admin/users/${userId}`)

  return response.data.data
}

export async function updateAdminUserStatus(userId, payload) {
  const response = await api.patch(`/admin/users/${userId}/status`, payload)

  return response.data.data
}

export async function updateAdminUserRole(userId, payload) {
  const response = await api.patch(`/admin/users/${userId}/role`, normalizeRolePayload(payload))

  return response.data.data
}

function normalizeAdminUserParams(params) {
  const keyword = typeof params.keyword === 'string' ? params.keyword.trim() : ''

  return keyword ? { keyword } : {}
}

function normalizeRolePayload(payload) {
  const phoneNumber = typeof payload.phoneNumber === 'string' ? payload.phoneNumber.trim() : ''

  return {
    role: payload.role,
    ...(payload.role === 'AGENT' && phoneNumber ? { phoneNumber } : {}),
  }
}
