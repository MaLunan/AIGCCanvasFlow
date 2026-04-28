<script setup>
import { ref } from 'vue'
import { useFlowStore } from '../../stores/flowStore'
import { NodeResizer } from '@vue-flow/node-resizer'
import '@vue-flow/node-resizer/dist/style.css'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const editing = ref(false)
const localContent = ref(props.data.content || '')

const NOTE_COLORS = ['#f5c542', '#ff7f7f', '#7fd1f5', '#7ff5a0', '#d07ff5']

function startEdit() {
  editing.value = true
  localContent.value = props.data.content
}

function stopEdit() {
  editing.value = false
  store.updateNodeData(props.id, { content: localContent.value })
}

function onKeydown(e) {
  if (e.key === 'Escape') stopEdit()
  e.stopPropagation()
}

function setColor(c) {
  store.updateNodeData(props.id, { color: c })
}
</script>

<template>
  <NodeResizer v-if="selected" :min-width="160" :min-height="100" />

  <div
    :class="['note-node', { selected }]"
    :style="{ background: data.color + '18', borderColor: data.color + '88' }"
    @dblclick.stop="startEdit"
  >
    <div class="note-toolbar">
      <div class="color-dots">
        <span
          v-for="c in NOTE_COLORS"
          :key="c"
          class="color-dot"
          :style="{ background: c }"
          @click.stop="setColor(c)"
        />
      </div>
      <button class="node-del" @click.stop="store.removeNodeById(id)" title="删除">×</button>
    </div>

    <textarea
      v-if="editing"
      v-model="localContent"
      class="note-editor"
      :style="{ color: data.color }"
      @blur="stopEdit"
      @keydown="onKeydown"
      @click.stop
      autofocus
    />
    <div
      v-else
      class="note-content"
      :style="{ color: data.color }"
    >
      {{ data.content || '双击添加备注...' }}
    </div>

    <div class="note-fold" :style="{ borderColor: `transparent transparent ${data.color}44 transparent` }" />
  </div>
</template>

<style scoped>
.note-node {
  min-width: 160px;
  min-height: 100px;
  width: 100%;
  height: 100%;
  border-radius: 4px 12px 12px 12px;
  border: 1.5px solid;
  padding: 8px 10px 20px;
  box-shadow: 2px 4px 12px rgba(0,0,0,0.4);
  position: relative;
  cursor: default;
  box-sizing: border-box;
}
.note-node.selected {
  box-shadow: 0 0 0 2px #646cff, 2px 4px 12px rgba(0,0,0,0.4);
}
.note-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.color-dots {
  display: flex;
  gap: 4px;
}
.color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s, transform 0.2s;
}
.color-dot:hover {
  opacity: 1;
  transform: scale(1.3);
}
.note-content {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.note-editor {
  width: 100%;
  min-height: 60px;
  background: transparent;
  border: none;
  outline: none;
  font-size: 13px;
  line-height: 1.6;
  font-family: inherit;
  resize: none;
  white-space: pre-wrap;
}
.note-fold {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 0 20px 20px;
}
</style>
