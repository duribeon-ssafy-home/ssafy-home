import api from '@/api/axios'

export async function createPropertyReport(propertyId, payload) {
  const response = await api.post(`/properties/${propertyId}/reports`, payload)

  return response.data.data
}
