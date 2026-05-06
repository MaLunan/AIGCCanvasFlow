<script setup>
import { ref, nextTick } from 'vue'
import { useVueFlow } from '@vue-flow/core'
import { useFlowStore } from '../../stores/flowStore'
import ScopeToggle from './ScopeToggle.vue'

const props = defineProps({
  id: { type: String, required: true },
  label: { type: String, default: '节点' },
  currentType: { type: String, required: true },
})

const editingLabel = ref(false)
const labelInput = ref(null)
const localLabel = ref(props.label)

async function startLabelEdit(e) {
  e.stopPropagation()
  localLabel.value = props.label
  editingLabel.value = true
  await nextTick()
  labelInput.value?.focus()
  labelInput.value?.select()
}

function stopLabelEdit() {
  if (!editingLabel.value) return
  editingLabel.value = false
  const val = localLabel.value.trim() || props.label
  if (val !== props.label) {
    store.updateNodeData(props.id, { label: val })
  }
}

function onLabelKeydown(e) {
  if (e.key === 'Enter' || e.key === 'Escape') stopLabelEdit()
  e.stopPropagation()
}

const store = useFlowStore()
const { findNode } = useVueFlow()

const TYPE_OPTIONS = [
  { type: 'textNode',        icon: 'T',  label: '文本节点', color: '#646cff', bg: '#646cff22' },
  { type: 'imageUploadNode', icon: '🖼', label: '图片上传', color: '#42b883', bg: '#42b88322' },
  { type: 'imageGenNode',    icon: '🎨', label: 'AI 图片',  color: '#42b883', bg: '#42b88322' },
  { type: 'videoUploadNode', icon: '▶',  label: '视频上传', color: '#ff6b6b', bg: '#ff6b6b22' },
  { type: 'videoGenNode',    icon: '🎬', label: 'AI 视频',  color: '#ff6b6b', bg: '#ff6b6b22' },
  { type: 'noteNode',        icon: '📝', label: '备注',     color: '#f5c542', bg: '#f5c54222' },
]

const LEGACY_TYPE_MAP = { imageNode: 'imageUploadNode', videoNode: 'videoUploadNode' }
const currentMeta = () => {
  const t = LEGACY_TYPE_MAP[props.currentType] ?? props.currentType
  return TYPE_OPTIONS.find((o) => o.type === t) ?? TYPE_OPTIONS[0]
}

const open = ref(false)
// Picker will be teleported to body; store its screen position here
const pickerStyle = ref({ top: '0px', left: '0px' })

function togglePicker(e) {
  e.stopPropagation()
  if (open.value) {
    open.value = false
    return
  }
  // Calculate position from the trigger button's bounding rect
  const rect = e.currentTarget.getBoundingClientRect()
  pickerStyle.value = {
    top:  `${rect.bottom + 6}px`,
    left: `${rect.left}px`,
  }
  open.value = true
  // Defer so this click doesn't immediately trigger the document listener
  setTimeout(() => {
    document.addEventListener('click', closePicker, { once: true })
  }, 0)
}

function closePicker() {
  open.value = false
}

function selectType(e, type) {
  e.stopPropagation()
  closePicker()
  if (type !== props.currentType) {
    const liveNode = findNode(props.id)
    store.changeNodeType(props.id, type, liveNode?.position)
  }
}
</script>

<template>
  <div class="node-header">
    <!-- Type badge: click to open picker -->
    <button
      class="type-badge"
      :style="{ color: currentMeta().color, background: currentMeta().bg }"
      @click="togglePicker"
      @mousedown.stop
      title="切换节点类型"
    >
      <span class="type-icon">{{ currentMeta().icon }}</span>
      <span class="type-caret">▾</span>
    </button>

    <input
      v-if="editingLabel"
      ref="labelInput"
      v-model="localLabel"
      class="node-label-input"
      @blur="stopLabelEdit"
      @keydown="onLabelKeydown"
      @click.stop
      @mousedown.stop
    />
    <span v-else class="node-label" @dblclick.stop="startLabelEdit" title="双击编辑名称">{{ label }}</span>

    <ScopeToggle :node-id="id" />

    <button class="node-del" @click.stop="store.removeNodeById(id)" title="删除节点">×</button>
  </div>

  <!-- Picker + backdrop teleported to body to escape VueFlow stacking context -->
  <Teleport to="body">
    <template v-if="open">
      <!-- Full-screen backdrop: clicking it closes picker -->
      <div class="picker-backdrop" @click="closePicker" @mousedown.stop @contextmenu.prevent />

      <!-- Picker popup positioned at trigger button -->
      <div class="type-picker" :style="pickerStyle" @click.stop @mousedown.stop>
        <div class="picker-title">切换为</div>
        <button
          v-for="opt in TYPE_OPTIONS"
          :key="opt.type"
          class="picker-item"
          :class="{ active: opt.type === currentType }"
          :style="opt.type === currentType
            ? { color: opt.color, background: opt.bg, borderColor: opt.color + '66' }
            : {}"
          @click="selectType($event, opt.type)"
          @mousedown.stop
        >
          <span class="picker-icon" :style="{ color: opt.color }">{{ opt.icon }}</span>
          <span class="picker-label">{{ opt.label }}</span>
          <span v-if="opt.type === currentType" class="picker-check">✓</span>
        </button>
      </div>
    </template>
  </Teleport>
</template>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid $border-default;
  border-radius: 9px 9px 0 0;
  position: relative;
}

.type-badge {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  border-radius: 5px;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: 11px;
  font-weight: 600;
  transition: filter 0.15s, border-color 0.15s;
  flex-shrink: 0;
  line-height: 1.4;

  &:hover {
    filter: brightness(1.35);
    border-color: currentColor;
  }
}

.type-icon { font-size: 12px; }
.type-caret { font-size: 9px; opacity: 0.7; margin-left: 1px; }

.node-label {
  font-size: 11px;
  font-weight: 600;
  color: $text-secondary;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
  cursor: text;
}

.node-label-input {
  flex: 1;
  min-width: 0;
  font-size: 11px;
  font-weight: 600;
  color: $text-primary;
  background: $bg-surface;
  border: 1px solid rgba($accent-primary, 0.53);
  border-radius: $radius-sm;
  padding: 1px 5px;
  outline: none;
  font-family: inherit;
}

.node-del {
  background: none;
  border: 1px solid #3a3a5c;
  color: #8888aa;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  width: 18px;
  height: 18px;
  padding: 0;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s, background 0.15s, border-color 0.15s;
  flex-shrink: 0;

  &:hover {
    color: #ff4d4d;
    background: rgba(#ff4d4d, 0.13);
    border-color: rgba(#ff4d4d, 0.4);
  }
}
</style>

<!-- Teleported elements need global styles (not scoped) -->
<style lang="scss">
@use '../../styles/variables' as *;

.picker-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9000;
}

.type-picker {
  position: fixed;
  z-index: 9001;
  background: #1a1a2e;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: 4px;
  min-width: 148px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.65), 0 0 0 1px rgba($accent-primary, 0.12);
  animation: picker-in 0.12s ease;
}

@keyframes picker-in {
  from { opacity: 0; transform: translateY(-4px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.picker-title {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: $text-dim;
  padding: 4px 8px 6px;
}

.picker-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 10px;
  background: none;
  border: 1px solid transparent;
  border-radius: 7px;
  color: #c0c0e0;
  font-size: 12px;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;
  margin-bottom: 2px;
  font-family: inherit;

  &:last-child { margin-bottom: 0; }

  &:hover:not(.active) {
    background: rgba(255, 255, 255, 0.04);
    color: $text-primary;
  }
}

.picker-icon {
  width: 16px;
  text-align: center;
  font-size: 13px;
  flex-shrink: 0;
}

.picker-label {
  flex: 1;
  font-size: 11px;
}

.picker-check {
  font-size: 11px;
  opacity: 0.8;
}
</style>
