// 模型中心 API：模型广场（公开列表）+ 用户模型库（个人收藏/自定义）
import request from './request'

// ── 模型广场 ─────────────────────────────────────────────────
/**
 * 获取模型广场列表，支持按分类过滤
 * category 为 '全部' 或空时不传参数，返回所有模型
 */
export function fetchMarketModels(category) {
  const params = category && category !== '全部' ? { category } : {}
  return request.post('/canvas/models/list', null, { params })
}

// ── 用户模型库 ───────────────────────────────────────────────
/** 获取当前用户的模型库列表（包含自定义模型和从广场添加的模型） */
export function fetchLibrary() {
  return request.post('/canvas/models/library/list')
}

/**
 * 将广场模型添加到用户模型库
 * @param {string} modelKey 广场模型的唯一标识 key
 */
export function addFromMarket(modelKey) {
  return request.post('/canvas/models/library/add-market', null, { params: { modelKey } })
}

/**
 * 添加用户自定义模型（填写 API Key、接口地址等）
 * @param {object} data 自定义模型配置信息
 */
export function addCustomModel(data) {
  return request.post('/canvas/models/library/add-custom', data)
}

/**
 * 更新用户自定义模型信息
 * @param {number} libraryId 用户模型库记录 ID
 * @param {object} data 更新的字段
 */
export function updateCustomModel(libraryId, data) {
  return request.post('/canvas/models/library/update-custom', data, { params: { libraryId } })
}

/**
 * 切换模型启用/禁用状态
 * 禁用后画布中该模型节点无法调用
 */
export function toggleModelEnabled(libraryId) {
  return request.post('/canvas/models/library/toggle-enabled', null, { params: { libraryId } })
}

/** 从用户模型库中移除模型（不删除广场原始数据） */
export function removeFromLibrary(libraryId) {
  return request.post('/canvas/models/library/remove', null, { params: { libraryId } })
}
