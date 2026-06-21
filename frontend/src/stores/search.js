import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSearchStore = defineStore('search', () => {
  const params = ref({})
  const locationText = ref('')

  function setSearch(p, text = '') {
    params.value = { ...p }
    locationText.value = text
  }

  return { params, locationText, setSearch }
})
