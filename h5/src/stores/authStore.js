// 认证状态 Store：管理 token、用户信息，并同步持久化到 localStorage
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/authApi'

export const useAuthStore = defineStore('auth', () => {
  // 初始化时从 localStorage 恢复已登录状态（刷新页面不丢失登录）
  const token    = ref(localStorage.getItem('access_token') || '')
  const userId   = ref(localStorage.getItem('user_id') || '')
  const username = ref(localStorage.getItem('username') || '')

  // 计算属性：token 非空即视为已登录
  const isLoggedIn = computed(() => !!token.value)

  /** 登录：调用接口后将凭证同步到内存和 localStorage */
  async function login(credentials) {
    const res = await authApi.login(credentials)
    // 更新响应式状态
    token.value    = res.accessToken
    userId.value   = String(res.userId)   // 统一转字符串，避免与 X-User-Id 头类型不一致
    username.value = res.username
    // 持久化到本地，供 axios 拦截器读取
    localStorage.setItem('access_token', res.accessToken)
    localStorage.setItem('user_id',      String(res.userId))
    localStorage.setItem('username',     res.username)
  }

  /** 注册：调用接口，成功后不自动登录，由调用方决定后续行为 */
  async function register(data) {
    await authApi.register(data)
  }

  /** 退出：清除内存状态和 localStorage，接口失败不影响本地清除 */
  function logout() {
    authApi.logout().catch(() => {})  // 通知服务端吊销 token，失败可忽略
    token.value    = ''
    userId.value   = ''
    username.value = ''
    localStorage.removeItem('access_token')
    localStorage.removeItem('user_id')
    localStorage.removeItem('username')
  }

  return { token, userId, username, isLoggedIn, login, register, logout }
})
