import api from '@/api/axios'

export async function getRecommendations(params = {}) {
  const response = await api.get('/recommendations', { params })
  return response.data.data.content
}
