import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  function setAccessToken(token) {
    accessToken.value = token

    if (token) {
      localStorage.setItem('accessToken', token)
      return
    }

    localStorage.removeItem('accessToken')
  }

  function logout() {
    setAccessToken('')
  }

  return {
    accessToken,
    isAuthenticated,
    setAccessToken,
    logout,
  }
})
