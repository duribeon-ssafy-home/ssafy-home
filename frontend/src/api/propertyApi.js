import api from '@/api/axios'

export async function getProperties(params = {}) {
  const response = await api.get('/properties', { params })
  return response.data.data
}

export async function getProperty(propertyId) {
  const response = await api.get(`/properties/${propertyId}`)
  return response.data.data
}
