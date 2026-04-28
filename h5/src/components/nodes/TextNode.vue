<script setup>
import { ref, nextTick } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { useFlowStore } from '../../stores/flowStore'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
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
</script>

<template>
  <div :class="['canvas-node', 'text-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <div class="node-header">
      <span class="node-icon text-icon">T</span>
      <span class="node-label">{{ data.label }}</span>
      <button class="node-del" @click.stop="store.removeNodeById(id)" title="删除">×</button>
    </div>

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

    <Handle id="sr" type="source" :position="Position.Right" :style="{ top: '50%' }" />
    <Handle id="sb" type="source" :position="Position.Bottom" :style="{ left: '50%' }" />
  </div>
</template>

<style scoped>
.text-node {
  width: 220px;
}
.text-icon {
  background: #646cff22;
  color: #a8b0ff;
  font-weight: 700;
  font-size: 12px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}
.text-editor {
  width: 100%;
  background: #12121e;
  border: 1px solid #646cff66;
  border-radius: 6px;
  color: #e0e0f0;
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
</style>
