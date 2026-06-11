import api from '@/api/axios'

export async function getAdminUsers(params = {}) {
  const response = await api.get('/admin/users', {
    params: normalizeAdminUserParams(params),
  })

  return response.data.data
}

function normalizeAdminUserParams(params) {
  const keyword = typeof params.keyword === 'string' ? params.keyword.trim() : ''

  return keyword ? { keyword } : {}
}
