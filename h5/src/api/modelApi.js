import request from './request'

// ── 模型广场 ─────────────────────────────────────────────────
export function fetchMarketModels(category) {
  const params = category && category !== '全部' ? { category } : {}
  return request.post('/canvas/models/list', null, { params })
}

// ── 用户模型库 ───────────────────────────────────────────────
export function fetchLibrary() {
  return request.post('/canvas/models/library/list')
}

export function addFromMarket(modelId) {
  return request.post('/canvas/models/library/add-market', null, { params: { modelId } })
}

export function addCustomModel(data) {
  return request.post('/canvas/models/library/add-custom', data)
}

export function updateCustomModel(libraryId, data) {
  return request.post('/canvas/models/library/update-custom', data, { params: { libraryId } })
}

export function toggleModelEnabled(libraryId) {
  return request.post('/canvas/models/library/toggle-enabled', null, { params: { libraryId } })
}

export function removeFromLibrary(libraryId) {
  return request.post('/canvas/models/library/remove', null, { params: { libraryId } })
}
