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
  // ── 广场模型 ──────────────────────────────────────────────
  const marketModels = ref([])
  const marketLoading = ref(false)
  const marketError = ref(null)

  async function loadMarket(category) {
    marketLoading.value = true
    marketError.value = null
    try {
      const data = await fetchMarketModels(category)
      marketModels.value = data ?? []
    } catch (e) {
      marketError.value = e?.message ?? '加载失败'
    } finally {
      marketLoading.value = false
    }
  }

  // ── 用户模型库 ────────────────────────────────────────────
  const libraryModels = ref([])
  const libraryLoading = ref(false)
  const libraryError = ref(null)

  async function loadLibrary() {
    libraryLoading.value = true
    libraryError.value = null
    try {
      const data = await fetchLibrary()
      libraryModels.value = data ?? []
    } catch (e) {
      libraryError.value = e?.message ?? '加载失败'
    } finally {
      libraryLoading.value = false
    }
  }

  // 广场中已在库的 modelId 集合（用于显示 inLibrary 状态）
  // 后端直接返回 inLibrary 字段，所以前端可直接用；
  // 此计算属性用于本地的乐观更新
  const libraryModelIds = computed(() =>
    new Set(libraryModels.value.filter(m => !m.isCustom && m.modelId).map(m => m.modelId))
  )

  function isInLibrary(modelId) {
    return libraryModelIds.value.has(modelId)
  }

  // ── 广场 → 库 ────────────────────────────────────────────
  async function addMarketToLibrary(model) {
    const newEntry = await addFromMarket(model.id)
    libraryModels.value.unshift(newEntry)
    // 同步更新广场列表的 inLibrary 标记
    const found = marketModels.value.find(m => m.id === model.id)
    if (found) found.inLibrary = true
    return newEntry
  }

  // ── 添加自定义模型 ────────────────────────────────────────
  async function addCustom(form) {
    const entry = await addCustomModel(form)
    libraryModels.value.unshift(entry)
    return entry
  }

  // ── 更新自定义模型 ────────────────────────────────────────
  async function updateCustom(libraryId, form) {
    const updated = await updateCustomModel(libraryId, form)
    const idx = libraryModels.value.findIndex(m => m.id === libraryId)
    if (idx !== -1) libraryModels.value[idx] = updated
    return updated
  }

  // ── 切换启用/禁用 ─────────────────────────────────────────
  async function toggleEnabled(libraryId) {
    await toggleModelEnabled(libraryId)
    const m = libraryModels.value.find(m => m.id === libraryId)
    if (m) m.enabled = !m.enabled
  }

  // ── 从库中移除 ────────────────────────────────────────────
  async function removeFromLib(libraryId) {
    const model = libraryModels.value.find(m => m.id === libraryId)
    await removeFromLibrary(libraryId)
    libraryModels.value = libraryModels.value.filter(m => m.id !== libraryId)
    // 同步更新广场 inLibrary
    if (model && !model.isCustom && model.modelId) {
      const found = marketModels.value.find(m => m.id === model.modelId)
      if (found) found.inLibrary = false
    }
  }

  return {
    marketModels, marketLoading, marketError, loadMarket,
    libraryModels, libraryLoading, libraryError, loadLibrary,
    libraryModelIds, isInLibrary,
    addMarketToLibrary, addCustom, updateCustom, toggleEnabled, removeFromLib,
  }
})
