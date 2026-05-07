<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useModelStore } from '../stores/modelStore'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'
import AppNavbar from '../components/AppNavbar.vue'

const router = useRouter()
const modelStore = useModelStore()
const { isLoggedIn } = storeToRefs(useAuthStore())
const {
  marketModels, marketLoading, marketError,
  libraryModels, libraryLoading, libraryError,
} = storeToRefs(modelStore)

// ── Tabs ─────────────────────────────────────────────────────────────────────
const activeTab = ref('market')

// ── Market filter ─────────────────────────────────────────────────────────────
const categories = ['全部', '视频', '图像', '音频', '文本']
const marketCategory = ref('全部')
const marketSearch = ref('')

// 切换分类时重新请求（search 只在前端过滤，避免频繁请求）
watch(marketCategory, (cat) => modelStore.loadMarket(cat))

const filteredMarket = computed(() => {
  if (!marketSearch.value) return marketModels.value
  const q = marketSearch.value.toLowerCase()
  return marketModels.value.filter(m =>
    m.name?.toLowerCase().includes(q) ||
    m.provider?.toLowerCase().includes(q) ||
    m.tags?.some(t => t.includes(marketSearch.value))
  )
})

// ── Library filter ────────────────────────────────────────────────────────────
const libCategory = ref('全部')
const filteredLibrary = computed(() => {
  if (libCategory.value === '全部') return libraryModels.value
  return libraryModels.value.filter(m => m.category === libCategory.value)
})

// ── Load data on mount ────────────────────────────────────────────────────────
onMounted(() => {
  modelStore.loadMarket()
  if (isLoggedIn.value) modelStore.loadLibrary()
})

// ── Add to library ────────────────────────────────────────────────────────────
const addingId = ref(null)
const addError = ref('')
async function handleAddToLibrary(model) {
  if (!isLoggedIn.value) { router.push('/login'); return }
  addingId.value = model.modelKey
  addError.value = ''
  try {
    await modelStore.addMarketToLibrary(model)
  } catch (e) {
    addError.value = e?.response?.data?.message ?? '添加失败'
    setTimeout(() => { addError.value = '' }, 3000)
  } finally {
    addingId.value = null
  }
}

// ── Custom model form ─────────────────────────────────────────────────────────
const showCustomForm = ref(false)
const editingLibraryId = ref(null)
const formLoading = ref(false)
const customForm = ref({
  name: '', category: '文本', description: '',
  apiEndpoint: '', apiKey: '', icon: '⚙️', color: '#646cff',
})
const customIcons = ['⚙️', '🤖', '💡', '🧠', '🔮', '⚡', '🌐', '🎯']

function openCustomForm(model = null) {
  if (model) {
    editingLibraryId.value = model.id
    customForm.value = {
      name: model.name ?? '',
      category: model.category ?? '文本',
      description: model.description ?? '',
      apiEndpoint: model.apiEndpoint ?? '',
      apiKey: '',   // 不回显真实 key
      icon: model.icon ?? '⚙️',
      color: model.color ?? '#646cff',
    }
  } else {
    editingLibraryId.value = null
    customForm.value = { name: '', category: '文本', description: '', apiEndpoint: '', apiKey: '', icon: '⚙️', color: '#646cff' }
  }
  showCustomForm.value = true
}

async function submitCustomForm() {
  if (!customForm.value.name.trim()) return
  formLoading.value = true
  try {
    if (editingLibraryId.value) {
      await modelStore.updateCustom(editingLibraryId.value, { ...customForm.value })
    } else {
      await modelStore.addCustom({ ...customForm.value })
      activeTab.value = 'library'
    }
    showCustomForm.value = false
  } catch (e) {
    alert(e?.response?.data?.message ?? '操作失败')
  } finally {
    formLoading.value = false
  }
}

// ── Toggle / Remove ───────────────────────────────────────────────────────────
const togglingId = ref(null)
async function handleToggle(model) {
  togglingId.value = model.id
  try { await modelStore.toggleEnabled(model.id) } finally { togglingId.value = null }
}

const removingId = ref(null)
async function handleRemove(model) {
  removingId.value = model.id
  try { await modelStore.removeFromLib(model.id) } finally { removingId.value = null }
}

// 当切换到模型库 Tab 时懒加载
function switchToLibrary() {
  activeTab.value = 'library'
  if (isLoggedIn.value && libraryModels.value.length === 0 && !libraryLoading.value) {
    modelStore.loadLibrary()
  }
}
</script>

<template>
  <div class="model-page">

    <AppNavbar />
    <div class="model-page-content">
      <!-- ─── Page Header ─── -->
      <div class="page-header">
        <h1 class="page-title"><span class="gradient-text">模型中心</span></h1>
        <p class="page-desc">浏览全球顶级 AI 模型，一键添加到你的模型库</p>
  
        <div class="tabs">
          <button :class="['tab', activeTab === 'market' && 'tab-active']" @click="activeTab = 'market'">
            模型广场
            <span class="tab-count">{{ marketModels.length }}</span>
          </button>
          <button :class="['tab', activeTab === 'library' && 'tab-active']" @click="switchToLibrary">
            我的模型库
            <span class="tab-count">{{ libraryModels.length }}</span>
          </button>
        </div>
      </div>
  
      <!-- ════════════════ MARKET TAB ════════════════ -->
      <div v-if="activeTab === 'market'" class="tab-content">
        <!-- Error toast -->
        <div v-if="addError" class="error-toast">{{ addError }}</div>
  
        <!-- Filter bar -->
        <div class="filter-bar">
          <div class="cat-pills">
            <button v-for="cat in categories" :key="cat"
              :class="['cat-pill', marketCategory === cat && 'cat-active']"
              @click="marketCategory = cat">{{ cat }}</button>
          </div>
          <div class="search-wrap">
            <span class="search-icon">🔍</span>
            <input v-model="marketSearch" class="search-input" placeholder="搜索模型名称、厂商、标签..." />
          </div>
        </div>
  
        <!-- Loading skeleton -->
        <div v-if="marketLoading" class="model-grid">
          <div v-for="i in 8" :key="i" class="model-card skeleton-card">
            <div class="sk sk-header" />
            <div class="sk sk-name" />
            <div class="sk sk-desc" />
            <div class="sk sk-tags" />
            <div class="sk sk-btn" />
          </div>
        </div>
  
        <!-- Error state -->
        <div v-else-if="marketError" class="empty-state">
          <div class="empty-icon">⚠️</div>
          <div>{{ marketError }}</div>
          <button class="btn-go-market" @click="modelStore.loadMarket(marketCategory)">重试</button>
        </div>
  
        <!-- Model grid -->
        <div v-else class="model-grid">
          <div v-for="model in filteredMarket" :key="model.modelKey" class="model-card"
            :style="{ '--accent': model.color || '#646cff' }">
            <div class="mc-header">
              <div class="mc-icon" :style="{ background: (model.color||'#646cff') + '22', color: model.color||'#646cff' }">
                {{ model.icon || '🤖' }}
              </div>
              <div class="mc-meta">
                <div class="mc-cat" :style="{ background: (model.color||'#646cff') + '22', color: model.color||'#646cff' }">
                  {{ model.category }}
                </div>
                <div class="mc-provider">{{ model.provider }}</div>
              </div>
            </div>
            <div class="mc-name">{{ model.name }}</div>
            <div class="mc-desc">{{ model.description }}</div>
            <div class="mc-tags">
              <span v-for="tag in (model.tags || [])" :key="tag" class="mc-tag">{{ tag }}</span>
            </div>
  
            <button v-if="!model.inLibrary && !modelStore.isInLibrary(model.modelKey)"
              class="mc-add"
              :class="addingId === model.modelKey && 'adding'"
              :disabled="addingId === model.modelKey"
              :style="{ borderColor: (model.color||'#646cff') + '66', color: model.color||'#646cff' }"
              @click="handleAddToLibrary(model)">
              <span v-if="addingId === model.modelKey">添加中...</span>
              <span v-else>+ 添加到库</span>
            </button>
            <div v-else class="mc-added" :style="{ color: model.color||'#646cff' }">
              <span>✓</span> 已在模型库
            </div>
          </div>
  
          <div v-if="!marketLoading && filteredMarket.length === 0" class="empty-state">
            <div class="empty-icon">🔭</div>
            <div>没有找到匹配的模型</div>
          </div>
        </div>
      </div>
  
      <!-- ════════════════ LIBRARY TAB ════════════════ -->
      <div v-if="activeTab === 'library'" class="tab-content">
        <!-- Not logged in -->
        <div v-if="!isLoggedIn" class="empty-state">
          <div class="empty-icon">🔐</div>
          <div>登录后即可管理你的模型库</div>
          <button class="btn-go-market" @click="router.push('/login')">去登录 →</button>
        </div>
  
        <template v-else>
          <!-- Toolbar -->
          <div class="lib-toolbar">
            <div class="cat-pills">
              <button v-for="cat in categories" :key="cat"
                :class="['cat-pill', libCategory === cat && 'cat-active']"
                @click="libCategory = cat">{{ cat }}</button>
            </div>
            <button class="btn-add-custom" @click="openCustomForm()">
              <span>+</span> 添加自定义模型
            </button>
          </div>
  
          <!-- Loading skeleton -->
          <div v-if="libraryLoading" class="model-grid">
            <div v-for="i in 4" :key="i" class="model-card skeleton-card">
              <div class="sk sk-header" />
              <div class="sk sk-name" />
              <div class="sk sk-desc" />
              <div class="sk sk-tags" />
              <div class="sk sk-btn" />
            </div>
          </div>
  
          <!-- Error -->
          <div v-else-if="libraryError" class="empty-state">
            <div class="empty-icon">⚠️</div>
            <div>{{ libraryError }}</div>
            <button class="btn-go-market" @click="modelStore.loadLibrary()">重试</button>
          </div>
  
          <!-- Empty -->
          <div v-else-if="filteredLibrary.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <div>{{ libCategory !== '全部' ? '该分类暂无模型' : '模型库为空，从广场添加或自定义模型' }}</div>
            <button class="btn-go-market" @click="activeTab = 'market'">前往模型广场 →</button>
          </div>
  
          <!-- Library grid -->
          <div v-else class="model-grid">
            <div v-for="model in filteredLibrary" :key="model.id"
              class="model-card lib-card"
              :class="!model.enabled && 'disabled'"
              :style="{ '--accent': model.color || '#646cff' }">
  
              <div class="mc-header">
                <div class="mc-icon" :style="{ background: (model.color||'#646cff') + '22', color: model.color||'#646cff' }">
                  {{ model.icon || '🤖' }}
                </div>
                <div class="mc-meta">
                  <div class="mc-cat" :style="{ background: (model.color||'#646cff') + '22', color: model.color||'#646cff' }">
                    {{ model.category }}
                  </div>
                  <div class="mc-provider">
                    {{ model.provider || (model.isCustom ? '自定义' : '') }}
                    <span v-if="model.isCustom" class="custom-badge">自定义</span>
                  </div>
                </div>
                <!-- Toggle -->
                <div
                  class="toggle"
                  :class="[model.enabled && 'toggle-on', togglingId === model.id && 'toggling']"
                  :style="model.enabled ? { background: model.color||'#646cff' } : {}"
                  @click="handleToggle(model)">
                  <div class="toggle-thumb" />
                </div>
              </div>
  
              <div class="mc-name">{{ model.name }}</div>
              <div class="mc-desc">{{ model.description || '暂无描述' }}</div>
  
              <div class="mc-tags">
                <span v-for="tag in (model.tags || [])" :key="tag" class="mc-tag">{{ tag }}</span>
              </div>
  
              <!-- Custom model API info -->
              <div v-if="model.isCustom" class="api-info">
                <div class="api-row">
                  <span class="api-label">Endpoint</span>
                  <span class="api-val">{{ model.apiEndpoint || '未设置' }}</span>
                </div>
                <div class="api-row">
                  <span class="api-label">API Key</span>
                  <span class="api-val api-key">{{ model.apiKeyMasked || '未设置' }}</span>
                </div>
              </div>
  
              <!-- Actions -->
              <div class="mc-actions">
                <button v-if="model.isCustom" class="mc-action-btn edit-btn" @click="openCustomForm(model)">编辑</button>
                <button class="mc-action-btn remove-btn"
                  :disabled="removingId === model.id"
                  @click="handleRemove(model)">
                  {{ removingId === model.id ? '移除中...' : '移除' }}
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
  
      <!-- ════════════════ Custom Model Modal ════════════════ -->
      <div v-if="showCustomForm" class="modal-overlay" @click.self="showCustomForm = false">
        <div class="modal">
          <div class="modal-header">
            <h3>{{ editingLibraryId ? '编辑自定义模型' : '添加自定义模型' }}</h3>
            <button class="modal-close" @click="showCustomForm = false">✕</button>
          </div>
  
          <div class="form-body">
            <div class="form-row">
              <label class="form-label">图标</label>
              <div class="icon-picker">
                <button v-for="ic in customIcons" :key="ic"
                  :class="['icon-opt', customForm.icon === ic && 'icon-selected']"
                  @click="customForm.icon = ic">{{ ic }}</button>
              </div>
            </div>
  
            <div class="form-row">
              <label class="form-label">名称 <span class="required">*</span></label>
              <input v-model="customForm.name" class="form-input" placeholder="例如：My LLM" />
            </div>
  
            <div class="form-row">
              <label class="form-label">分类</label>
              <div class="seg-group">
                <button v-for="cat in ['视频','图像','音频','文本']" :key="cat"
                  :class="['seg-btn', customForm.category === cat && 'seg-active']"
                  @click="customForm.category = cat">{{ cat }}</button>
              </div>
            </div>
  
            <div class="form-row">
              <label class="form-label">描述</label>
              <input v-model="customForm.description" class="form-input" placeholder="模型简介（可选）" />
            </div>
  
            <div class="form-row">
              <label class="form-label">API Endpoint</label>
              <input v-model="customForm.apiEndpoint" class="form-input" placeholder="https://api.example.com/v1" />
            </div>
  
            <div class="form-row">
              <label class="form-label">
                API Key
                <span v-if="editingLibraryId" class="form-hint">（留空则不修改）</span>
              </label>
              <input v-model="customForm.apiKey" class="form-input" type="password" placeholder="sk-..." />
            </div>
          </div>
  
          <div class="modal-footer">
            <button class="btn-cancel" :disabled="formLoading" @click="showCustomForm = false">取消</button>
            <button class="btn-confirm" :disabled="!customForm.name.trim() || formLoading" @click="submitCustomForm">
              <span v-if="formLoading">保存中...</span>
              <span v-else>{{ editingLibraryId ? '保存修改' : '添加到库' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.model-page {
  background: $bg-base;
  color: $text-primary;
  font-family: $font-family;
  min-height: 100vh;
  height: 100vh;
}

.model-page-content {
  height: 100%;
  overflow-y: auto;
}

.page-header {
  max-width: $max-width;
  margin: 0 auto;
  padding: 48px 24px 0;
}

.page-title { font-size: 36px; font-weight: 800; margin-bottom: 8px; }

.gradient-text {
  background: linear-gradient(135deg, $accent-primary 0%, $accent-green 50%, $accent-red 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-desc { font-size: 14px; color: $text-muted; margin-bottom: 32px; }

.tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid $border-subtle;
}

.tab {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  color: $text-muted;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.15s, border-color 0.15s;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: -1px;

  &:hover { color: $text-secondary; }
  &-active { color: $accent-primary; border-bottom-color: $accent-primary; }
}

.tab-count {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  background: rgba($accent-primary, 0.13);
  color: #a0aaff;
  border-radius: 10px;
}

.tab-content {
  max-width: $max-width;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.error-toast {
  background: rgba($accent-red, 0.13);
  border: 1px solid rgba($accent-red, 0.27);
  color: #ff8080;
  padding: 10px 16px;
  border-radius: $radius-md;
  font-size: 13px;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.lib-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.cat-pills { display: flex; gap: 8px; flex-wrap: wrap; }

.cat-pill {
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background: $bg-surface;
  border: 1px solid $border-default;
  color: $text-muted;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;

  &:hover { border-color: rgba($accent-primary, 0.4); color: #a0aaff; }
  &.cat-active { background: rgba($accent-primary, 0.13); border-color: rgba($accent-primary, 0.4); color: #a0aaff; }
}

.search-wrap {
  flex: 1;
  min-width: 200px;
  max-width: 320px;
  position: relative;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: #d0d0f0;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;

  &:focus { border-color: rgba($accent-primary, 0.4); }
  &::placeholder { color: $text-faint; }
}

.btn-add-custom {
  padding: 8px 18px;
  background: linear-gradient(135deg, $accent-primary, $accent-green);
  border: none;
  border-radius: $radius-md;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
  transition: opacity 0.15s;
  display: flex;
  align-items: center;
  gap: 6px;

  &:hover { opacity: 0.85; }
}

.skeleton-card { pointer-events: none; }

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.sk {
  border-radius: $radius-sm;
  background: linear-gradient(90deg, #1a1a2e 25%, #22223a 50%, #1a1a2e 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.sk-header { height: 44px; width: 100%; margin-bottom: 10px; }
.sk-name   { height: 18px; width: 60%; margin-bottom: 8px; }
.sk-desc   { height: 12px; width: 100%; margin-bottom: 4px; }
.sk-tags   { height: 12px; width: 80%; margin-bottom: 12px; }
.sk-btn    { height: 34px; width: 100%; }

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.model-card {
  background: $bg-surface;
  border: 1px solid $border-subtle;
  border-radius: $radius-xl;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: border-color 0.2s, transform 0.2s, opacity 0.2s;

  &:hover { border-color: var(--accent, #{$accent-primary}); transform: translateY(-2px); }
  &.disabled { opacity: 0.45; }
}

.mc-header { display: flex; align-items: center; gap: 12px; }

.mc-icon {
  width: 44px;
  height: 44px;
  border-radius: $radius-lg;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.mc-meta { flex: 1; min-width: 0; }

.mc-cat {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: $radius-sm;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.mc-provider {
  font-size: 11px;
  color: $text-dim;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.custom-badge {
  display: inline-block;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 5px;
  background: rgba($accent-yellow, 0.13);
  color: $accent-yellow;
  border-radius: 3px;
  margin-left: 4px;
}

.mc-name { font-size: 15px; font-weight: 700; color: $text-primary; }
.mc-desc { font-size: 12px; color: $text-muted; line-height: 1.6; flex: 1; }
.mc-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.mc-tag {
  font-size: 10px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.031);
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  color: #8080a0;
}

.mc-add {
  width: 100%;
  padding: 8px;
  border-radius: $radius-md;
  background: none;
  border: 1px solid;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;

  &:hover:not(:disabled) { background: var(--accent, #{$accent-primary}); color: #fff; border-color: var(--accent, #{$accent-primary}); }
  &:disabled { opacity: 0.6; cursor: default; }
  &.adding { background: var(--accent, #{$accent-primary}); color: #fff; border-color: var(--accent, #{$accent-primary}); }
}

.mc-added {
  width: 100%;
  padding: 8px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  opacity: 0.7;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.toggle {
  width: 36px;
  height: 20px;
  border-radius: 10px;
  background: $border-default;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;

  &.toggling { opacity: 0.6; pointer-events: none; }

  &-thumb {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: $text-dim;
    transition: transform 0.2s, background 0.2s;
  }

  &-on .toggle-thumb { transform: translateX(16px); background: #fff; }
}

.api-info {
  background: $bg-base;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.api-row { display: flex; align-items: center; gap: 8px; font-size: 11px; }
.api-label { color: $text-dim; flex-shrink: 0; width: 60px; }
.api-val { color: #8080a0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.api-key { font-family: $font-family-mono; }

.mc-actions { display: flex; gap: 8px; }

.mc-action-btn {
  flex: 1;
  padding: 7px;
  border-radius: $radius-md;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;

  &:disabled { opacity: 0.5; cursor: default; }
}

.edit-btn {
  background: rgba($accent-primary, 0.13);
  border: 1px solid rgba($accent-primary, 0.27);
  color: #a0aaff;

  &:hover { background: rgba($accent-primary, 0.27); }
}

.remove-btn {
  background: rgba($accent-red, 0.09);
  border: 1px solid rgba($accent-red, 0.2);
  color: #ff8080;

  &:hover:not(:disabled) { background: rgba($accent-red, 0.2); }
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: $text-dim;
  font-size: 14px;
}

.empty-icon { font-size: 40px; }

.btn-go-market {
  margin-top: 8px;
  padding: 8px 20px;
  background: none;
  border: 1px solid rgba($accent-primary, 0.4);
  border-radius: $radius-md;
  color: #a0aaff;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;

  &:hover { background: rgba($accent-primary, 0.13); }
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: $z-modal - 1;
  backdrop-filter: blur(4px);
}

.modal {
  background: $bg-elevated;
  border: 1px solid $border-default;
  border-radius: $radius-2xl;
  width: 480px;
  max-width: calc(100vw - 32px);
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid $border-subtle;

  h3 { font-size: 16px; font-weight: 700; color: $text-primary; margin: 0; }
}

.modal-close {
  background: none;
  border: none;
  color: $text-dim;
  font-size: 16px;
  cursor: pointer;
  padding: 0 4px;

  &:hover { color: $text-secondary; }
}

.form-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; font-weight: 600; color: #8080a0; }
.form-hint { font-size: 11px; font-weight: 400; color: $text-dim; }
.required { color: $accent-red; }

.form-input {
  padding: 9px 12px;
  background: $bg-input;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: #d0d0f0;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;

  &:focus { border-color: rgba($accent-primary, 0.4); }
  &::placeholder { color: $text-faint; }
}

.icon-picker { display: flex; gap: 8px; flex-wrap: wrap; }

.icon-opt {
  width: 38px;
  height: 38px;
  border-radius: $radius-md;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-surface;
  border: 1px solid $border-default;
  cursor: pointer;
  transition: all 0.15s;

  &:hover { border-color: rgba($accent-primary, 0.4); }
  &.icon-selected { background: rgba($accent-primary, 0.13); border-color: $accent-primary; }
}

.seg-group { display: flex; gap: 6px; }

.seg-btn {
  padding: 6px 14px;
  border-radius: $radius-sm;
  font-size: 12px;
  font-weight: 600;
  background: $bg-surface;
  border: 1px solid $border-default;
  color: $text-muted;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;

  &:hover { border-color: rgba($accent-primary, 0.4); color: #a0aaff; }
  &.seg-active { background: rgba($accent-primary, 0.13); border-color: rgba($accent-primary, 0.4); color: #a0aaff; }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 20px;
  border-top: 1px solid $border-subtle;
}

.btn-cancel {
  padding: 9px 20px;
  background: none;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: #8080a0;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;

  &:hover:not(:disabled) { border-color: rgba($accent-primary, 0.4); color: #a0aaff; }
}

.btn-confirm {
  padding: 9px 24px;
  background: linear-gradient(135deg, $accent-primary, $accent-green);
  border: none;
  border-radius: $radius-md;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: opacity 0.15s;

  &:hover:not(:disabled) { opacity: 0.85; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
</style>
