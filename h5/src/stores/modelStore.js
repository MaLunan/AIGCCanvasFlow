// 模型中心 Store：管理模型广场列表和用户个人模型库，支持乐观更新
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  fetchMarketModels,
  fetchLibrary,
  addFromMarket,
  addCustomModel,
  updateCustomModel,
  toggleModelEnabled,
  removeFromLibrary,
} from '../api/modelApi'

export const useModelStore = defineStore('model', () => {
  // ── 模型广场（公开模型列表）──────────────────────────────────────────────
  const marketModels  = ref([])   // 广场模型列表
  const marketLoading = ref(false)
  const marketError   = ref(null)

  /** 加载广场模型，可按 category 过滤 */
  async function loadMarket(category) {
    marketLoading.value = true
    marketError.value   = null
    try {
      const data = await fetchMarketModels(category)
      marketModels.value = data ?? []
    } catch (e) {
      marketError.value = e?.message ?? '加载失败'
    } finally {
      marketLoading.value = false
    }
  }

  // ── 用户模型库（收藏/自定义模型）────────────────────────────────────────
  const libraryModels  = ref([])   // 用户模型库列表
  const libraryLoading = ref(false)
  const libraryError   = ref(null)

  /** 加载当前用户的模型库（需要登录） */
  async function loadLibrary() {
    libraryLoading.value = true
    libraryError.value   = null
    try {
      const data = await fetchLibrary()
      libraryModels.value = data ?? []
    } catch (e) {
      libraryError.value = e?.message ?? '加载失败'
    } finally {
      libraryLoading.value = false
    }
  }

  /**
   * 广场中已添加到库的 modelKey 集合（用于本地乐观更新 inLibrary 状态）
   * 过滤掉自定义模型（isCustom）和无 key 的条目
   */
  const libraryModelKeys = computed(() =>
    new Set(libraryModels.value.filter(m => !m.isCustom && m.modelKey).map(m => m.modelKey))
  )

  /** 判断广场模型是否已在用户库中（用于广场列表显示"已添加"状态）*/
  function isInLibrary(modelKey) {
    return libraryModelKeys.value.has(modelKey)
  }

  // ── 广场模型 → 用户库 ───────────────────────────────────────────────────
  /** 将广场模型添加到用户库，乐观更新库列表和广场 inLibrary 标记 */
  async function addMarketToLibrary(model) {
    const newEntry = await addFromMarket(model.modelKey)
    libraryModels.value.unshift(newEntry)   // 新条目插入列表头部

    // 同步更新广场列表的 inLibrary 标记，避免重新请求广场数据
    const found = marketModels.value.find(m => m.modelKey === model.modelKey)
    if (found) found.inLibrary = true
    return newEntry
  }

  // ── 自定义模型管理 ──────────────────────────────────────────────────────
  /** 添加自定义模型（用户自填 API Key/接口地址），新条目插入库列表头部 */
  async function addCustom(form) {
    const entry = await addCustomModel(form)
    libraryModels.value.unshift(entry)
    return entry
  }

  /** 更新自定义模型配置，原地更新库列表 */
  async function updateCustom(libraryId, form) {
    const updated = await updateCustomModel(libraryId, form)
    const idx = libraryModels.value.findIndex(m => m.id === libraryId)
    if (idx !== -1) libraryModels.value[idx] = updated  // 原地替换，保持列表顺序
    return updated
  }

  /** 切换模型启用/禁用状态（乐观更新，无需重新加载列表）*/
  async function toggleEnabled(libraryId) {
    await toggleModelEnabled(libraryId)
    // 接口成功后本地取反，避免重新请求
    const m = libraryModels.value.find(m => m.id === libraryId)
    if (m) m.enabled = !m.enabled
  }

  /** 从用户库移除模型，并同步更新广场 inLibrary 标记 */
  async function removeFromLib(libraryId) {
    // 先记录模型信息，用于后续同步广场标记
    const model = libraryModels.value.find(m => m.id === libraryId)
    await removeFromLibrary(libraryId)
    // 从库列表移除
    libraryModels.value = libraryModels.value.filter(m => m.id !== libraryId)

    // 同步更新广场 inLibrary（非自定义模型才有对应广场条目）
    if (model && !model.isCustom && model.modelId) {
      const found = marketModels.value.find(m => m.id === model.modelId)
      if (found) found.inLibrary = false
    }
  }

  return {
    marketModels, marketLoading, marketError, loadMarket,
    libraryModels, libraryLoading, libraryError, loadLibrary,
    libraryModelKeys, isInLibrary,
    addMarketToLibrary, addCustom, updateCustom, toggleEnabled, removeFromLib,
  }
})
