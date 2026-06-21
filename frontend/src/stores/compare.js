import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const MAX = 4

export const useCompareStore = defineStore('compare', () => {
  const items = ref([]) // { propertyId, title, imageUrl }[]

  const ids = computed(() => items.value.map((i) => i.propertyId))
  const count = computed(() => items.value.length)
  const isFull = computed(() => items.value.length >= MAX)

  function has(propertyId) {
    return items.value.some((i) => i.propertyId === propertyId)
  }

  function toggle(property) {
    const id = typeof property === 'object' ? property.propertyId : property
    if (has(id)) {
      items.value = items.value.filter((i) => i.propertyId !== id)
    } else if (!isFull.value) {
      const snapshot =
        typeof property === 'object'
          ? { propertyId: id, title: property.title, imageUrl: property.images?.[0]?.imageUrl ?? null }
          : { propertyId: id, title: null, imageUrl: null }
      items.value = [...items.value, snapshot]
    }
  }

  function remove(propertyId) {
    items.value = items.value.filter((i) => i.propertyId !== propertyId)
  }

  function clear() {
    items.value = []
  }

  return { items, ids, count, isFull, has, toggle, remove, clear }
})
