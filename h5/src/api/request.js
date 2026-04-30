import axios from 'axios'

const request = axios.create({
  baseURL: '/',
  timeout: 15000,
})

// ─── 请求拦截器 ───────────────────────────────────────────────────────────────
request.interceptors.request.use((config) => {
  // TODO: 接入真实登录后从 token 中解析 userId
  config.headers['X-User-Id'] = '1'
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
    const msg =
      error.response?.data?.message ||
      error.message ||
      '网络异常，请稍后重试'
    return Promise.reject(new Error(msg))
  },
)

export default request
