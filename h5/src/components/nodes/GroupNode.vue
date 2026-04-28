<script setup>
import { ref } from 'vue'
import { NodeResizer } from '@vue-flow/node-resizer'
import { useFlowStore } from '../../stores/flowStore'
import '@vue-flow/node-resizer/dist/style.css'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const editingLabel = ref(false)
const localLabel = ref(props.data.label || '分组')

function stopEditLabel() {
  editingLabel.value = false
  store.updateNodeData(props.id, { label: localLabel.value })
}
</script>

<template>
  <NodeResizer :min-width="200" :min-height="150" :is-visible="selected" />

  <div :class="['group-node', { selected }]">
    <div class="group-header">
      <span class="group-icon">⊞</span>
      <input
        v-if="editingLabel"
        v-model="localLabel"
        class="group-label-input"
        @blur="stopEditLabel"
        @keydown.enter="stopEditLabel"
        @keydown.escape="editingLabel = false"
        @click.stop
        autofocus
      />
      <span v-else class="group-label" @dblclick.stop="editingLabel = true">
        {{ data.label || '分组' }}
      </span>
      <button class="group-ungroup" @click.stop="store.ungroupNodes(id)" title="取消分组">
        ⊟
      </button>
      <button class="node-del" @click.stop="store.removeNodeById(id)" title="删除">×</button>
    </div>
  </div>
</template>

<style scoped>
.group-node {
  width: 100%;
  height: 100%;
  background: rgba(100, 108, 255, 0.04);
  border: 2px dashed #646cff44;
  border-radius: 12px;
  box-sizing: border-box;
  position: relative;
}
.group-node.selected {
  border-color: #646cff99;
  background: rgba(100, 108, 255, 0.07);
}
.group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(100, 108, 255, 0.12);
  border-radius: 10px 10px 0 0;
  border-bottom: 1px dashed #646cff33;
}
.group-icon {
  font-size: 14px;
  opacity: 0.7;
}
.group-label {
  font-size: 12px;
  font-weight: 600;
  color: #a8b0ff;
  cursor: text;
  flex: 1;
  user-select: none;
}
.group-label-input {
  flex: 1;
  background: transparent;
  border: none;
  border-bottom: 1px solid #646cff;
  color: #a8b0ff;
  font-size: 12px;
  font-weight: 600;
  outline: none;
  padding: 0;
}
.group-ungroup {
  background: none;
  border: none;
  color: #646cff88;
  cursor: pointer;
  font-size: 14px;
  padding: 0 2px;
  line-height: 1;
  transition: color 0.2s;
}
.group-ungroup:hover { color: #646cff; }
</style>
