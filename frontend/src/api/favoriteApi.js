import api from '@/api/axios'

export async function getFavorites() {
  const response = await api.get('/favorites')
  return response.data.data
}

export async function addFavorite(propertyId) {
  const response = await api.post(`/favorites/${propertyId}`)
  return response.data.data
}

export async function removeFavorite(propertyId) {
  const response = await api.delete(`/favorites/${propertyId}`)
  return response.data.data
}
