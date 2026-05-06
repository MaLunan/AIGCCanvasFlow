import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/user/register', data),
  logout: () => request.post('/auth/logout'),
}
