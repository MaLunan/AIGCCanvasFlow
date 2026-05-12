// 统一 HTTP 请求封装，基于 axios
import axios from 'axios'

// 创建 axios 实例，所有请求共用此配置
const request = axios.create({
  baseURL: '/',       // 由 vite.config.js proxy 转发到对应微服务
  timeout: 15000,     // 默认超时 15 秒（上传/AI 接口可单独覆盖）
})

// ─── 请求拦截器 ───────────────────────────────────────────────────────────────
request.interceptors.request.use((config) => {
  // 从 localStorage 读取登录后缓存的 token 和用户 ID
  const token = localStorage.getItem('access_token')
  const userId = localStorage.getItem('user_id')

  // 如果存在 token，则附加到请求头（JWT Bearer 鉴权）
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  // 后端某些接口通过 X-User-Id 快速获取用户身份，无需解码 JWT
  if (userId) {
    config.headers['X-User-Id'] = userId
  }
  return config
})

// ─── 响应拦截器 ───────────────────────────────────────────────────────────────
request.interceptors.response.use(
  (response) => {
    // 后端统一响应格式：{ code, data, message }
    const { code, data, message } = response.data
    // 业务层非 200 视为失败，抛出携带 message 的错误
    if (code !== 200) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    // 成功时只返回 data 字段，调用方无需层层解构
    return data
  },
  (error) => {
    // HTTP 401 表示 token 失效或未登录，清除本地凭证并跳转登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('user_id')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    // 优先使用后端返回的 message，否则使用 axios 错误信息
    const msg =
      error.response?.data?.message ||
      error.message ||
      '网络异常，请稍后重试'
    return Promise.reject(new Error(msg))
  },
)

export default request
