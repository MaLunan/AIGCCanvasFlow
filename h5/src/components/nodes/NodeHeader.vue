<script setup>
import { ref } from 'vue'
import { useFlowStore } from '../../stores/flowStore'

const props = defineProps({
  id: { type: String, required: true },
  label: { type: String, default: '节点' },
  currentType: { type: String, required: true },
})

const store = useFlowStore()

const TYPE_OPTIONS = [
  { type: 'textNode',  icon: 'T',  label: '文本节点', color: '#646cff', bg: '#646cff22' },
  { type: 'imageNode', icon: '🖼', label: '图片节点', color: '#42b883', bg: '#42b88322' },
  { type: 'videoNode', icon: '▶',  label: '视频节点', color: '#ff6b6b', bg: '#ff6b6b22' },
]

const currentMeta = () => TYPE_OPTIONS.find((o) => o.type === props.currentType) ?? TYPE_OPTIONS[0]

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
    store.changeNodeType(props.id, type)
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

    <span class="node-label">{{ label }}</span>

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

<style scoped>
.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid #2e2e50;
  border-radius: 9px 9px 0 0;
  position: relative;
}

/* ── Type badge ── */
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
}
.type-badge:hover {
  filter: brightness(1.35);
  border-color: currentColor;
}
.type-icon { font-size: 12px; }
.type-caret { font-size: 9px; opacity: 0.7; margin-left: 1px; }

.node-label {
  font-size: 11px;
  font-weight: 600;
  color: #a0a0c0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.node-del {
  background: none;
  border: none;
  color: #444466;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0 2px;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
  flex-shrink: 0;
}
.node-del:hover {
  color: #ff4d4d;
  background: #ff4d4d18;
}
</style>

<!-- Teleported elements need global styles (not scoped) -->
<style>
.picker-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9000;
}

.type-picker {
  position: fixed;
  z-index: 9001;
  background: #1a1a2e;
  border: 1px solid #2e2e50;
  border-radius: 10px;
  padding: 4px;
  min-width: 148px;
  box-shadow: 0 8px 28px rgba(0,0,0,0.65), 0 0 0 1px rgba(100,108,255,0.12);
  animation: picker-in 0.12s ease;
}
@keyframes picker-in {
  from { opacity: 0; transform: translateY(-4px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0)   scale(1);    }
}

.picker-title {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: #444466;
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
}
.picker-item:last-child { margin-bottom: 0; }
.picker-item:hover:not(.active) {
  background: #ffffff0a;
  color: #e0e0f0;
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
