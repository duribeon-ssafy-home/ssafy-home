import api from '@/api/axios'

export async function getProperties(params = {}) {
  const response = await api.get('/properties', { params })
  const page = response.data.data
  return {
    content: page.content,
    totalPages: page.totalPages,
    totalElements: page.totalElements,
    currentPage: page.number,
  }
}

export async function getProperty(propertyId) {
  const response = await api.get(`/properties/${propertyId}`)
  return response.data.data
}
