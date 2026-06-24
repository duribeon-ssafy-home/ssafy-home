import api from '@/api/axios'

export async function getReviews(propertyId, page = 0, size = 20) {
  const response = await api.get(`/properties/${propertyId}/reviews`, { params: { page, size } })
  return response.data.data
}

export async function createReview(propertyId, { nickname, content }) {
  const response = await api.post(`/properties/${propertyId}/reviews`, { nickname, content })
  return response.data.data
}
