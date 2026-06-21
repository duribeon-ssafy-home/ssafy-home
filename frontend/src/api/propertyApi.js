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

export async function getPropertyRisk(propertyId) {
  const response = await api.get(`/properties/${propertyId}/risk`)
  return response.data.data
}

export async function getMyProperties() {
  const response = await api.get('/properties/me')
  return response.data.data
}

export async function getMyProperty(propertyId) {
  const response = await api.get(`/properties/me/${propertyId}`)
  return response.data.data
}

export async function createProperty(data) {
  const response = await api.post('/properties', data)
  return response.data.data
}

export async function updateProperty(id, data) {
  const response = await api.patch(`/properties/${id}`, data)
  return response.data.data
}

export async function deleteProperty(id) {
  await api.delete(`/properties/${id}`)
}

export async function uploadPropertyImages(propertyId, files) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  const response = await api.post(`/properties/${propertyId}/images`, formData)
  return response.data.data
}

export async function deletePropertyImage(propertyId, imageId) {
  await api.delete(`/properties/${propertyId}/images/${imageId}`)
}
