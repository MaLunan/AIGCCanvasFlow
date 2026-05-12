// 项目资源 CRUD 及资产上传，路由到 aigc-canvas 微服务
import request from './request'

export const projectApi = {
  /** 创建项目，仅需传项目名 */
  create: (name) =>
    request.post('/canvas/projects', { name }),

  /** 分页获取项目列表（只含基本信息，无 canvasData，节省带宽） */
  list: (current = 1, size = 20) =>
    request.get('/canvas/projects', { params: { current, size } }),

  /** 获取项目详情（含完整 canvasData JSON），用于打开画布时加载 */
  get: (id) =>
    request.get(`/canvas/projects/${id}`),

  /** 重命名 / 更新基本信息 */
  rename: (id, name) =>
    request.put(`/canvas/projects/${id}`, { name }),

  /**
   * 保存画布数据（body 为原始 JSON 字符串）
   * 使用 text/plain 避免 axios 对 JSON 对象的二次序列化
   */
  saveCanvas: (id, canvasData) =>
    request.post(`/canvas/projects/${id}/canvas`, JSON.stringify(canvasData), {
      headers: { 'Content-Type': 'text/plain' },
    }),

  /** 删除项目（物理删除或软删除由后端决定） */
  delete: (id) =>
    request.delete(`/canvas/projects/${id}`),

  /**
   * 上传资产文件（图片/视频），返回 { url, ... }
   * 不手动设 Content-Type，让 axios 自动附加 multipart boundary
   * 视频文件较大，超时设为 5 分钟
   */
  uploadAsset: (file, type = 'other') => {
    const formData = new FormData()
    formData.append('file', file)         // 文件二进制内容
    formData.append('type', type)         // 资产类型：image / video / other
    return request.post('/canvas/assets', formData, { timeout: 300000 })
  },
}
