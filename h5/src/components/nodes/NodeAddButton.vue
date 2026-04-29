<script setup>
import { ref } from 'vue'
import { useFlowStore } from '../../stores/flowStore'

const props = defineProps({
  id: { type: String, required: true },
  sourceType: { type: String, required: true },
})

const store = useFlowStore()

const TYPE_OPTIONS = [
  { type: 'textNode',  icon: 'T',  label: '文本节点', color: '#646cff' },
  { type: 'imageNode', icon: '🖼', label: '图片节点', color: '#42b883' },
  { type: 'videoNode', icon: '▶',  label: '视频节点', color: '#ff6b6b' },
  { type: 'noteNode',  icon: '📝', label: '备注',     color: '#f5c542' },
]

const open = ref(false)
const pickerStyle = ref({ top: '0px', left: '0px' })

function toggle(e) {
  e.stopPropagation()
  if (open.value) { open.value = false; return }
  const rect = e.currentTarget.getBoundingClientRect()
  pickerStyle.value = {
    top:  `${rect.top - 4}px`,
    left: `${rect.right + 8}px`,
  }
  open.value = true
  setTimeout(() => document.addEventListener('click', close, { once: true }), 0)
}

function close() { open.value = false }

function create(e, type) {
  e.stopPropagation()
  close()
  store.addConnectedNode(props.id, type)
}
</script>

<template>
  <!-- "+" button fixed on the right edge of the node -->
  <button
    class="node-add-btn"
    @click="toggle"
    @mousedown.stop
    title="基于本节点创建下一个"
  >+</button>

  <!-- Picker teleported to body (avoids stacking-context issues) -->
  <Teleport to="body">
    <template v-if="open">
      <div class="nab-backdrop" @click="close" @mousedown.stop />
      <div class="nab-picker" :style="pickerStyle" @click.stop @mousedown.stop>
        <div class="nab-title">创建下一节点</div>
        <button
          v-for="opt in TYPE_OPTIONS"
          :key="opt.type"
          class="nab-item"
          :class="{ current: opt.type === sourceType }"
          @click="create($event, opt.type)"
          @mousedown.stop
        >
          <span class="nab-icon" :style="{ color: opt.color }">{{ opt.icon }}</span>
          <span class="nab-label">{{ opt.label }}</span>
          <span v-if="opt.type === sourceType" class="nab-same">同类型</span>
        </button>
      </div>
    </template>
  </Teleport>
</template>

<style scoped>
.node-add-btn {
  /* Positioned outside the node's right edge, vertically centerd */
  position: absolute;
  right: -38px;
  top: 50%;
  transform: translateY(-50%);
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #1e1e35;
  border: 2px solid #646cff88;
  color: #a0aaff;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  /* Hidden by default, shown on node hover via global CSS */
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, border-color 0.15s, transform 0.15s;
  z-index: 10;
}
.node-add-btn:hover {
  background: #646cff;
  border-color: #a0aaff;
  color: #fff;
  transform: translateY(-50%) scale(1.15);
}
</style>

<!-- Global: show button when parent canvas-node is hovered -->
<style>
.canvas-node:hover .node-add-btn,
.canvas-node.selected .node-add-btn {
  opacity: 1;
}
</style>

<!-- Teleported elements -->
<style>
.nab-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9000;
}
.nab-picker {
  position: fixed;
  z-index: 9001;
  background: #1a1a2e;
  border: 1px solid #2e2e50;
  border-radius: 10px;
  padding: 4px;
  min-width: 152px;
  box-shadow: 0 8px 28px rgba(0,0,0,0.65), 0 0 0 1px rgba(100,108,255,0.12);
  animation: nab-in 0.12s ease;
}
@keyframes nab-in {
  from { opacity: 0; transform: translateX(-6px) scale(0.97); }
  to   { opacity: 1; transform: translateX(0)    scale(1);    }
}
.nab-title {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: #444466;
  padding: 4px 8px 6px;
}
.nab-item {
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
.nab-item:last-child { margin-bottom: 0; }
.nab-item:hover {
  background: #ffffff0c;
  color: #e0e0f0;
}
.nab-item.current {
  background: #646cff12;
  border-color: #646cff33;
}
.nab-icon { width: 16px; text-align: center; font-size: 13px; flex-shrink: 0; }
.nab-label { flex: 1; font-size: 11px; }
.nab-same {
  font-size: 9px;
  color: #646cff88;
  border: 1px solid #646cff44;
  border-radius: 4px;
  padding: 1px 4px;
}
</style>
