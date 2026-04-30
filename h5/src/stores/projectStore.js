import { defineStore } from 'pinia'
import { ref } from 'vue'
import { projectApi } from '../api/projectApi'

export const useProjectStore = defineStore('projects', () => {
  const projects = ref([])
  const loading = ref(false)

  // ─── 列表 ─────────────────────────────────────────────────────────────────

  /** 从服务端加载项目列表（无 canvasData） */
  async function fetchProjects() {
    loading.value = true
    try {
      const page = await projectApi.list()
      projects.value = page?.records ?? []
    } catch (e) {
      console.error('[projectStore] fetchProjects failed', e)
    } finally {
      loading.value = false
    }
  }

  // ─── 单条 ─────────────────────────────────────────────────────────────────

  /** 从本地缓存按 id 查找（同步，id 可以是 number 或 string） */
  function getProject(id) {
    return projects.value.find((p) => String(p.id) === String(id)) ?? null
  }

  /** 从服务端获取单个项目（含 canvasData），并更新本地缓存 */
  async function fetchProject(id) {
    const project = await projectApi.get(id)
    const idx = projects.value.findIndex((p) => String(p.id) === String(id))
    if (idx >= 0) projects.value[idx] = project
    else projects.value = [project, ...projects.value]
    return project
  }

  // ─── CRUD ─────────────────────────────────────────────────────────────────

  /** 创建新项目，插入缓存头部，返回项目对象 */
  async function createProject(name = '未命名项目') {
    const project = await projectApi.create(name)
    projects.value = [project, ...projects.value]
    return project
  }

  /** 重命名，更新缓存 */
  async function renameProject(id, name) {
    const updated = await projectApi.rename(id, name)
    const idx = projects.value.findIndex((p) => String(p.id) === String(id))
    if (idx >= 0) projects.value[idx] = { ...projects.value[idx], ...updated }
  }

  /** 保存画布数据，更新本地缓存中的 canvasData / frameCount */
  async function saveCanvas(id, nodes, edges) {
    await projectApi.saveCanvas(id, { nodes, edges })
    const idx = projects.value.findIndex((p) => String(p.id) === String(id))
    if (idx >= 0) {
      projects.value[idx] = {
        ...projects.value[idx],
        canvasData: { nodes, edges },
        frameCount: nodes.length,
      }
    }
  }

  /** 删除项目，移出缓存 */
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
