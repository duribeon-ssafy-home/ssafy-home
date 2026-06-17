import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const MAX = 4

export const useCompareStore = defineStore('compare', () => {
  const ids = ref([])

  const count = computed(() => ids.value.length)
  const isFull = computed(() => ids.value.length >= MAX)

  function has(propertyId) {
    return ids.value.includes(propertyId)
  }

  function toggle(propertyId) {
    if (has(propertyId)) {
      ids.value = ids.value.filter((id) => id !== propertyId)
    } else if (!isFull.value) {
      ids.value = [...ids.value, propertyId]
    }
  }

  function clear() {
    ids.value = []
  }

  return { ids, count, isFull, has, toggle, clear }
})
