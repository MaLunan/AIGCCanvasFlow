import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/authApi'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('access_token') || '')
  const userId = ref(localStorage.getItem('user_id') || '')
  const username = ref(localStorage.getItem('username') || '')

  const isLoggedIn = computed(() => !!token.value)

  async function login(credentials) {
    const res = await authApi.login(credentials)
    token.value = res.accessToken
    userId.value = String(res.userId)
    username.value = res.username
    localStorage.setItem('access_token', res.accessToken)
    localStorage.setItem('user_id', String(res.userId))
    localStorage.setItem('username', res.username)
  }

  async function register(data) {
    await authApi.register(data)
  }

  function logout() {
    authApi.logout().catch(() => {})
    token.value = ''
    userId.value = ''
    username.value = ''
    localStorage.removeItem('access_token')
    localStorage.removeItem('user_id')
    localStorage.removeItem('username')
  }

  return { token, userId, username, isLoggedIn, login, register, logout }
})
