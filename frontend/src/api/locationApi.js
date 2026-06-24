import api from '@/api/axios'

export async function searchLocations(keyword) {
  const response = await api.get('/locations', { params: { keyword } })
  return response.data.data
}
