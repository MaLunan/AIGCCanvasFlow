// 认证相关 API，路由到 aigc-auth 微服务（经网关转发）
import request from './request'

export const authApi = {
  // 用户登录：POST /auth/login，返回 { token, userId, username }
  login: (data) => request.post('/auth/login', data),

  // 用户注册：POST /user/register，由 aigc-user 服务处理
  register: (data) => request.post('/user/register', data),

  // 退出登录：通知服务端吊销 token（后端可选实现黑名单）
  logout: () => request.post('/auth/logout'),
}
