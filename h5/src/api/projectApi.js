import request from './request'

export const projectApi = {
  /** 创建项目 */
  create: (name) =>
    request.post('/canvas/projects', { name }),

  /** 分页获取项目列表（只含基本信息，无 canvasData） */
  list: (current = 1, size = 20) =>
    request.get('/canvas/projects', { params: { current, size } }),

  /** 获取项目详情（含 canvasData） */
  get: (id) =>
    request.get(`/canvas/projects/${id}`),

  /** 重命名 / 更新基本信息 */
  rename: (id, name) =>
    request.put(`/canvas/projects/${id}`, { name }),

  /** 保存画布数据（body 为原始 JSON 字符串） */
  saveCanvas: (id, canvasData) =>
    request.post(`/canvas/projects/${id}/canvas`, JSON.stringify(canvasData), {
      headers: { 'Content-Type': 'text/plain' },
    }),

  /** 删除项目 */
  delete: (id) =>
    request.delete(`/canvas/projects/${id}`),
}
