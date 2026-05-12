<script setup>
import { ref, nextTick, computed, watch, onMounted } from 'vue'
import { Handle, Position, useVueFlow } from '@vue-flow/core'
import { useRouter } from 'vue-router'
import { useFlowStore } from '../../stores/flowStore'
import { useModelStore } from '../../stores/modelStore'
import { polishText } from '../../api/aiApi'
import NodeHeader from './NodeHeader.vue'
import NodeAddButton from './NodeAddButton.vue'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const modelStore = useModelStore()
const router = useRouter()
const { getNodes } = useVueFlow()
const editing = ref(false)
const textareaRef = ref(null)
// 编辑时的本地副本，避免直接修改 props.data（单向数据流）
const localContent = ref(props.data.content || '')

async function startEdit() {
  editing.value = true
  localContent.value = props.data.content || ''
  // 等待 textarea 渲染完成后再 focus，避免无法获得焦点
  await nextTick()
  textareaRef.value?.focus()
  textareaRef.value?.select()  // 全选方便用户直接覆盖
}

function stopEdit() {
  if (!editing.value) return
  editing.value = false
  // 保存编辑内容：content 用于显示，outputValue 用于数据流传播
  store.updateNodeData(props.id, {
    content: localContent.value,
    outputValue: localContent.value,
  })
}

function onKeydown(e) {
  if (e.key === 'Escape') stopEdit()           // Esc：取消编辑
  if (e.key === 'Enter' && e.ctrlKey) stopEdit() // Ctrl+Enter：保存
  e.stopPropagation()  // 阻止键盘事件冒泡到 VueFlow（避免触发删除等快捷键）
}

// ── 模型库（文本类） ──────────────────────────────────────────────────────────
// 只过滤分类为"文本"且已启用的模型，供润化功能使用
const textModels = computed(() =>
  modelStore.libraryModels.filter(m => m.category === '文本' && m.enabled)
)
const polishModel = ref('')  // 当前选中的润化模型 ID（字符串形式）

onMounted(() => {
  // 按需加载模型库，避免重复请求
  if (!modelStore.libraryModels.length && !modelStore.libraryLoading) {
    modelStore.loadLibrary()
  }
})

// 当模型列表变化时，自动选中第一个可用模型（如果当前选择已失效）
watch(textModels, (models) => {
  if (models.length && !models.find(m => String(m.id) === polishModel.value)) {
    polishModel.value = String(models[0].id)
  }
}, { immediate: true })

// 特殊 sentinel 值处理：选择"去广场添加"时跳转模型页
watch(polishModel, (val) => {
  if (val === '__goto_market__') {
    polishModel.value = textModels.value[0] ? String(textModels.value[0].id) : ''
    router.push('/models')
  }
})

// ── AI 润化 ──────────────────────────────────────────────────────────────────
const polishing = ref(false)

const polishError = ref('')

async function polishTextContent() {
  const text = props.data.content?.trim()
  if (!text || polishing.value || !polishModel.value) return
  polishing.value = true
  polishError.value = ''
  try {
    // 收集上游节点的上下文，作为 AI 润化的参考信息
    const context = store.getUpstreamContext(props.id)
    const polished = await polishText(text, Number(polishModel.value), context)
    // 传入 getNodes.value 快照，避免更新时覆盖已拖拽节点的最新坐标
    store.updateNodeData(props.id, { content: polished, outputValue: polished }, getNodes.value)
    localContent.value = polished  // 同步更新本地编辑副本
  } catch (e) {
    polishError.value = e?.message || '润化失败，请重试'
  } finally {
    polishing.value = false
  }
}
</script>

<template>
  <div :class="['canvas-node', 'text-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <NodeHeader :id="id" :label="data.label" current-type="textNode" />

    <div class="node-body" @dblclick.stop="startEdit">
      <textarea
        v-if="editing"
        ref="textareaRef"
        v-model="localContent"
        class="text-editor"
        @blur="stopEdit"
        @keydown="onKeydown"
        @click.stop
        placeholder="输入内容..."
        rows="4"
      />
      <div v-else class="text-preview">
        {{ data.content || '双击编辑内容...' }}
      </div>
    </div>

    <div v-if="data.inputValue" class="node-data-in">
      <span class="data-badge">⬇ {{ String(data.inputValue).slice(0, 24) }}</span>
    </div>

    <!-- AI 润化工具栏 -->
    <div class="node-toolbar" @mousedown.stop @click.stop>
      <select v-model="polishModel" class="toolbar-model-select" :title="textModels.length ? '选择润化模型' : '暂无文本模型，请先添加'">
        <option v-if="!textModels.length" disabled value="">— 暂无文本模型 —</option>
        <option v-for="m in textModels" :key="m.id" :value="String(m.id)">{{ m.icon }} {{ m.name }}</option>
        <option value="__goto_market__">＋ 去广场添加</option>
      </select>
      <button
        class="toolbar-btn polish-btn"
        :class="{ polishing }"
        :disabled="!data.content?.trim() || polishing || !polishModel || !textModels.length"
        @click.stop="polishTextContent"
        title="AI 润化文本"
      >
        <span v-if="polishing" class="polish-spinner" />
        <span v-else class="polish-icon">✦</span>
        {{ polishing ? '润化中...' : 'AI 润化' }}
      </button>
    </div>

    <div v-if="polishError" class="node-error">{{ polishError }}</div>

    <Handle id="sr" type="source" :position="Position.Right" :style="{ top: '50%' }" />
    <NodeAddButton :id="id" source-type="textNode" />
  </div>
</template>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.text-node { width: 220px; }

.text-editor {
  width: 100%;
  background: $bg-surface;
  border: 1px solid rgba($accent-primary, 0.4);
  border-radius: $radius-sm;
  color: $text-primary;
  font-size: 13px;
  padding: 6px 8px;
  resize: none;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
  min-height: 80px;
}

.text-preview {
  font-size: 13px;
  color: #c8c8e8;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 40px;
}

.node-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px;
  border-top: 1px solid $border-default;
  background: rgba($accent-purple, 0.03);
}

.toolbar-model-select {
  flex: 1;
  min-width: 0;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: 5px;
  color: $text-secondary;
  font-size: 10px;
  padding: 3px 5px;
  outline: none;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.15s;

  &:focus { border-color: rgba($accent-purple, 0.5); }
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: $radius-sm;
  border: 1px solid transparent;
  background: none;
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.polish-btn {
  color: $accent-purple;
  border-color: rgba($accent-purple, 0.3);
  background: rgba($accent-purple, 0.07);

  &:hover:not(:disabled) {
    background: rgba($accent-purple, 0.18);
    border-color: rgba($accent-purple, 0.6);
  }

  &:disabled { opacity: 0.4; cursor: not-allowed; }

  &.polishing {
    color: rgba($accent-purple, 0.7);
    cursor: wait;
  }
}

.polish-icon { font-size: 12px; line-height: 1; }

.polish-spinner {
  width: 10px;
  height: 10px;
  border: 2px solid rgba($accent-purple, 0.25);
  border-top-color: $accent-purple;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin { to { transform: rotate(360deg); } }

.node-error {
  font-size: 10px;
  color: $accent-red;
  padding: 4px 8px;
  border-top: 1px solid rgba($accent-red, 0.2);
  background: rgba($accent-red, 0.06);
}
</style>
