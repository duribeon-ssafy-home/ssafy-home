import { ref } from 'vue'
import { getFavorites, addFavorite, removeFavorite } from '@/api/favoriteApi'
import { useAuthStore } from '@/stores/auth'

const favoriteIds = ref(new Set())

export function useFavorites() {
  const authStore = useAuthStore()

  async function loadFavorites() {
    if (!authStore.isAuthenticated) {
      favoriteIds.value = new Set()
      return
    }
    try {
      const favorites = await getFavorites()
      favoriteIds.value = new Set(favorites.map((f) => f.property.propertyId))
    } catch {
      favoriteIds.value = new Set()
    }
  }

  async function toggleFavorite(propertyId) {
    if (!authStore.isAuthenticated) return false

    const next = new Set(favoriteIds.value)
    if (next.has(propertyId)) {
      await removeFavorite(propertyId)
      next.delete(propertyId)
    } else {
      await addFavorite(propertyId)
      next.add(propertyId)
    }
    favoriteIds.value = next
    return true
  }

  function isFavorited(propertyId) {
    return favoriteIds.value.has(propertyId)
  }

  return { loadFavorites, toggleFavorite, isFavorited }
}
