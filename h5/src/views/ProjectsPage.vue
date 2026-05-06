<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '../stores/projectStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const projectStore = useProjectStore()
const { projects, loading } = storeToRefs(projectStore)

onMounted(() => projectStore.fetchProjects())

// ─── Create ──────────────────────────────────────────────────────────────────
const showCreate = ref(false)
const newName = ref('')
const createError = ref('')
const creating = ref(false)
function openCreate() {
  newName.value = ''
  createError.value = ''
  showCreate.value = true
  setTimeout(() => document.getElementById('new-project-input')?.focus(), 50)
}
async function confirmCreate() {
  const name = newName.value.trim() || '未命名项目'
  const duplicate = projects.value.some((p) => p.name === name)
  if (duplicate) {
    createError.value = `「${name}」已存在，请使用其他名称`
    return
  }
  createError.value = ''
  creating.value = true
  try {
    const project = await projectStore.createProject(name)
    showCreate.value = false
    router.push({ path: '/canvas', query: { projectId: project.id } })
  } catch (e) {
    createError.value = e.message || '创建失败，请稍后重试'
  } finally {
    creating.value = false
  }
}

// ─── Open ─────────────────────────────────────────────────────────────────────
function openProject(project) {
  router.push({ path: '/canvas', query: { projectId: project.id } })
}

// ─── Rename ──────────────────────────────────────────────────────────────────
const renamingId = ref(null)
const renameValue = ref('')
function startRename(project, e) {
  e.stopPropagation()
  renamingId.value = project.id
  renameValue.value = project.name
  setTimeout(() => document.getElementById(`rename-${project.id}`)?.select(), 50)
}
async function commitRename(id) {
  if (renameValue.value.trim()) {
    await projectStore.renameProject(id, renameValue.value.trim())
  }
  renamingId.value = null
}

// ─── Delete ──────────────────────────────────────────────────────────────────
const confirmDeleteId = ref(null)
function askDelete(id, e) {
  e.stopPropagation()
  confirmDeleteId.value = id
}
async function doDelete() {
  if (confirmDeleteId.value) {
    await projectStore.deleteProject(confirmDeleteId.value)
  }
  confirmDeleteId.value = null
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function formatTime(ts) {
  if (!ts) return ''
  const diff = Date.now() - new Date(ts).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m}分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}小时前`
  const d = Math.floor(h / 24)
  if (d < 7) return `${d}天前`
  return new Date(ts).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="projects-page">
    <!-- Navbar -->
    <nav class="pnav">
      <a class="pnav-logo" href="/">
        <span class="logo-hex">⬡</span>
        <span class="logo-name">CanvasFlow</span>
      </a>
      <div class="pnav-actions">
        <button class="btn-ghost" @click="router.push('/')">首页</button>
        <button class="btn-primary" @click="openCreate">+ 新建项目</button>
      </div>
    </nav>

    <!-- Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">我的项目</h1>
        <p class="page-sub">{{ projects.length }} 个项目</p>
      </div>
      <button class="btn-primary" @click="openCreate">+ 新建项目</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">加载中...</div>

    <!-- Grid -->
    <div v-else-if="projects.length" class="project-grid">
      <div
        v-for="project in projects"
        :key="project.id"
        class="project-card"
        @click="openProject(project)"
      >
        <!-- Thumbnail / placeholder -->
        <div class="card-thumb">
          <div class="card-thumb-inner">
            <span class="card-thumb-icon">⬡</span>
          </div>
        </div>

        <!-- Card info -->
        <div class="card-info">
          <div v-if="renamingId === project.id" class="rename-wrap" @click.stop>
            <input
              :id="`rename-${project.id}`"
              v-model="renameValue"
              class="rename-input"
              @blur="commitRename(project.id)"
              @keydown.enter="commitRename(project.id)"
              @keydown.escape="renamingId = null"
            />
          </div>
          <div v-else class="card-name" :title="project.name">{{ project.name }}</div>

          <div class="card-meta">
            <span>{{ project.frameCount ?? 0 }} 个节点</span>
            <span class="dot">·</span>
            <span>{{ formatTime(project.updateTime) }}</span>
          </div>
        </div>

        <!-- Actions -->
        <div class="card-actions" @click.stop>
          <button class="card-action-btn" title="重命名" @click="startRename(project, $event)">✏️</button>
          <button class="card-action-btn danger" title="删除" @click="askDelete(project.id, $event)">🗑</button>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-icon">⬡</div>
      <p class="empty-title">暂无项目</p>
      <p class="empty-sub">点击「新建项目」开始你的 AI 创作之旅</p>
      <button class="btn-primary lg" @click="openCreate">+ 新建项目</button>
    </div>

    <!-- Create modal -->
    <Teleport to="body">
      <template v-if="showCreate">
        <div class="modal-backdrop" @click="showCreate = false" />
        <div class="modal" @click.stop>
          <h2 class="modal-title">新建项目</h2>
          <input
            id="new-project-input"
            v-model="newName"
            class="modal-input"
            :class="{ 'input-error': createError }"
            placeholder="项目名称"
            @keydown.enter="confirmCreate"
            @keydown.escape="showCreate = false"
            @input="createError = ''"
          />
          <p v-if="createError" class="modal-error">{{ createError }}</p>
          <div class="modal-actions">
            <button class="btn-ghost" @click="showCreate = false">取消</button>
            <button class="btn-primary" :disabled="creating" @click="confirmCreate">
              {{ creating ? '创建中...' : '创建并进入' }}
            </button>
          </div>
        </div>
      </template>
    </Teleport>

    <!-- Delete confirm -->
    <Teleport to="body">
      <template v-if="confirmDeleteId">
        <div class="modal-backdrop" @click="confirmDeleteId = null" />
        <div class="modal" @click.stop>
          <h2 class="modal-title">删除项目</h2>
          <p class="modal-body">删除后无法恢复，确认删除该项目？</p>
          <div class="modal-actions">
            <button class="btn-ghost" @click="confirmDeleteId = null">取消</button>
            <button class="btn-danger" @click="doDelete">删除</button>
          </div>
        </div>
      </template>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.projects-page {
  min-height: 100vh;
  overflow-y: auto;
  background: $bg-base;
  color: $text-primary;
  font-family: $font-family;
}

.pnav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: $navbar-height;
  background: rgba(11, 11, 22, 0.95);
  border-bottom: 1px solid $border-subtle;
  position: sticky;
  top: 0;
  z-index: $z-navbar;

  &-logo {
    display: flex;
    align-items: center;
    gap: 8px;
    text-decoration: none;
    color: $text-primary;
  }

  &-actions { display: flex; gap: 10px; }
}

.logo-hex { font-size: 22px; color: $accent-primary; }
.logo-name { font-size: 16px; font-weight: 700; }

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  max-width: $max-width;
  margin: 0 auto;
  padding: 40px 32px 24px;
}

.page-title { font-size: 28px; font-weight: 700; margin: 0 0 4px; }
.page-sub { font-size: 13px; color: #666680; margin: 0; }

.project-grid {
  max-width: $max-width;
  margin: 0 auto;
  padding: 0 32px 60px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.project-card {
  background: #13132a;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.18s, box-shadow 0.18s, transform 0.18s;
  position: relative;

  &:hover {
    border-color: rgba($accent-primary, 0.4);
    box-shadow: 0 6px 24px rgba($accent-primary, 0.15);
    transform: translateY(-2px);

    .card-actions { opacity: 1; }
  }
}

.card-thumb {
  width: 100%;
  aspect-ratio: 16/9;
  background: #0f0f20;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-thumb-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1a1a3a 0%, #0f0f22 100%);
}

.card-thumb-icon { font-size: 36px; opacity: 0.2; }

.card-info { padding: 12px 12px 8px; }

.card-name {
  font-size: 13px;
  font-weight: 600;
  color: #d0d0f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #555575;
}

.dot { color: $text-faint; }

.card-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}

.card-action-btn {
  background: rgba(17, 17, 36, 0.9);
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  padding: 4px 6px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
  line-height: 1;

  &:hover { background: #1e1e3a; }
  &.danger:hover { background: #3a1010; }
}

.rename-wrap { margin-bottom: 6px; }

.rename-input {
  width: 100%;
  background: #1a1a2e;
  border: 1px solid $accent-primary;
  border-radius: $radius-sm;
  color: #d0d0f0;
  font-size: 13px;
  font-weight: 600;
  padding: 3px 6px;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 40vh;
  font-size: 14px;
  color: $text-dim;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 50vh;
  color: $text-dim;
}

.empty-icon { font-size: 64px; opacity: 0.15; }
.empty-title { font-size: 18px; font-weight: 600; color: #555575; margin: 0; }
.empty-sub { font-size: 13px; color: $text-dim; margin: 0; }

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  z-index: $z-modal;
}

.modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: $z-modal + 1;
  background: #1a1a2e;
  border: 1px solid $border-default;
  border-radius: 14px;
  padding: 28px 32px;
  min-width: 340px;
  box-shadow: $shadow-modal;
}

.modal-title {
  font-size: 17px;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 16px;
}

.modal-body {
  font-size: 13px;
  color: $text-secondary;
  margin: 0 0 20px;
}

.modal-input {
  width: 100%;
  background: #0f0f1e;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: 14px;
  padding: 10px 12px;
  outline: none;
  margin-bottom: 20px;
  box-sizing: border-box;
  font-family: inherit;
  transition: border-color 0.15s;

  &:focus { border-color: $accent-primary; }
  &.input-error { border-color: #ff4d4d; }
}

.modal-error {
  font-size: 12px;
  color: #ff7070;
  margin: -14px 0 16px;
}

.modal-actions { display: flex; gap: 10px; justify-content: flex-end; }

.btn-ghost {
  padding: 8px 16px;
  background: none;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-family: inherit;

  &:hover { background: rgba(255, 255, 255, 0.047); color: #e0e0ff; }
}

.btn-primary {
  padding: 8px 18px;
  background: $accent-primary;
  border: none;
  border-radius: $radius-md;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;

  &:hover:not(:disabled) { background: #7c82ff; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
  &.lg { padding: 12px 28px; font-size: 14px; }
}

.btn-danger {
  padding: 8px 18px;
  background: #ff4d4d;
  border: none;
  border-radius: $radius-md;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;

  &:hover { background: #ff6666; }
}
</style>
