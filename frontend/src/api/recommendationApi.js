import api from '@/api/axios'

export async function getRecommendations(params = {}) {
  const response = await api.get('/recommendations', { params })
  const page = response.data.data
  return {
    content: page.content,
    totalPages: page.totalPages,
    totalElements: page.totalElements,
    currentPage: page.number,
  }
}
