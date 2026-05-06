import axios from 'axios'

const request = axios.create({
  baseURL: '/',
  timeout: 15000,
})

// ─── 请求拦截器 ───────────────────────────────────────────────────────────────
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  const userId = localStorage.getItem('user_id')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  if (userId) {
    config.headers['X-User-Id'] = userId
  }
  return config
})

// ─── 响应拦截器 ───────────────────────────────────────────────────────────────
request.interceptors.response.use(
  (response) => {
    const { code, data, message } = response.data
    if (code !== 200) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('user_id')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    const msg =
      error.response?.data?.message ||
      error.message ||
      '网络异常，请稍后重试'
    return Promise.reject(new Error(msg))
  },
)

export default request
