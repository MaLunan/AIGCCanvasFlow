// 项目列表 Store：维护用户项目的本地缓存，所有操作与后端保持同步
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { projectApi } from '../api/projectApi'

export const useProjectStore = defineStore('projects', () => {
  const projects = ref([])      // 项目列表缓存（不含 canvasData，节省内存）
  const loading  = ref(false)   // 加载状态，控制骨架屏/loading 动画

  // ─── 列表 ─────────────────────────────────────────────────────────────────

  /** 从服务端加载项目列表（无 canvasData） */
  async function fetchProjects() {
    loading.value = true
    try {
      const page = await projectApi.list()
      // 后端返回分页对象，取 records 字段；后端无数据时容错为空数组
      projects.value = page?.records ?? []
    } catch (e) {
      console.error('[projectStore] fetchProjects failed', e)
    } finally {
      loading.value = false
    }
  }

  // ─── 单条 ─────────────────────────────────────────────────────────────────

  /**
   * 从本地缓存按 id 查找（同步）
   * id 可能是 number（来自后端）或 string（来自路由参数），统一转 string 比较
   */
  function getProject(id) {
    return projects.value.find((p) => String(p.id) === String(id)) ?? null
  }

  /**
   * 从服务端获取单个项目（含 canvasData），并更新本地缓存
   * 打开画布时调用，确保拿到最新完整数据
   */
  async function fetchProject(id) {
    const project = await projectApi.get(id)
    const idx = projects.value.findIndex((p) => String(p.id) === String(id))
    if (idx >= 0) projects.value[idx] = project     // 已有则更新
    else projects.value = [project, ...projects.value]  // 未有则插入头部
    return project
  }

  // ─── CRUD ─────────────────────────────────────────────────────────────────

  /** 创建新项目，插入缓存头部（最新项目排最前），返回项目对象 */
  async function createProject(name = '未命名项目') {
    const project = await projectApi.create(name)
    projects.value = [project, ...projects.value]
    return project
  }

  /** 重命名项目，更新本地缓存对应条目 */
  async function renameProject(id, name) {
    const updated = await projectApi.rename(id, name)
    const idx = projects.value.findIndex((p) => String(p.id) === String(id))
    // 合并更新（保留本地缓存中 canvasData 等其他字段）
    if (idx >= 0) projects.value[idx] = { ...projects.value[idx], ...updated }
  }

  /** 保存画布数据，同步更新本地缓存中的 canvasData / frameCount */
  async function saveCanvas(id, nodes, edges) {
    await projectApi.saveCanvas(id, { nodes, edges })
    const idx = projects.value.findIndex((p) => String(p.id) === String(id))
    if (idx >= 0) {
      projects.value[idx] = {
        ...projects.value[idx],
        canvasData: { nodes, edges },
        frameCount: nodes.length,   // 用节点数近似表示画布规模
      }
    }
  }

  /** 删除项目，从本地缓存移除对应条目 */
  async function deleteProject(id) {
    await projectApi.delete(id)
    projects.value = projects.value.filter((p) => String(p.id) !== String(id))
  }

  return {
    projects,
    loading,
    fetchProjects,
    fetchProject,
    getProject,
    createProject,
    renameProject,
    saveCanvas,
    deleteProject,
  }
})
