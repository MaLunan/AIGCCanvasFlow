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
const localContent = ref(props.data.content || '')

async function startEdit() {
  editing.value = true
  localContent.value = props.data.content || ''
  await nextTick()
  textareaRef.value?.focus()
  textareaRef.value?.select()
}

function stopEdit() {
  if (!editing.value) return
  editing.value = false
  store.updateNodeData(props.id, {
    content: localContent.value,
    outputValue: localContent.value,
  })
}

function onKeydown(e) {
  if (e.key === 'Escape') stopEdit()
  if (e.key === 'Enter' && e.ctrlKey) stopEdit()
  e.stopPropagation()
}

// ── 模型库（文本类） ──────────────────────────────────────────────────────────
const textModels = computed(() =>
  modelStore.libraryModels.filter(m => m.category === '文本' && m.enabled)
)
const polishModel = ref('')

onMounted(() => {
  if (!modelStore.libraryModels.length && !modelStore.libraryLoading) {
    modelStore.loadLibrary()
  }
})

watch(textModels, (models) => {
  if (models.length && !models.find(m => String(m.id) === polishModel.value)) {
    polishModel.value = String(models[0].id)
  }
}, { immediate: true })

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
    const context = store.getUpstreamContext(props.id)
    const polished = await polishText(text, Number(polishModel.value), context)
    store.updateNodeData(props.id, { content: polished, outputValue: polished }, getNodes.value)
    localContent.value = polished
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
