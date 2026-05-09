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

  /** 上传资产文件（图片/视频），返回 { url, ... } */
  uploadAsset: (file, type = 'other') => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)
    // 不手动设 Content-Type，让 axios 自动附加 multipart boundary
    // 视频文件较大，超时设为 5 分钟
    return request.post('/canvas/assets', formData, { timeout: 300000 })
  },
}
