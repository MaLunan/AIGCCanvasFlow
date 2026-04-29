<script setup>
import { ref } from 'vue'
import { useVueFlow } from '@vue-flow/core'
import { useFlowStore } from '../../stores/flowStore'

const props = defineProps({
  id: { type: String, required: true },
  sourceType: { type: String, required: true },
})

const store = useFlowStore()
const { findNode, getNodes, addNodes, addEdges, setCenter, getViewport } = useVueFlow()

const GAP = 24 // 节点间最小间距

/** 检测矩形 a 与矩形 b 是否重叠（含间距 GAP） */
function overlaps(ax, ay, aw, ah, bx, by, bw, bh) {
  return ax < bx + bw + GAP &&
         ax + aw + GAP > bx &&
         ay < by + bh + GAP &&
         ay + ah + GAP > by
}

/**
 * 从候选位置 (startX, startY) 开始，沿 Y 轴向下寻找不与任何已有节点重叠的空位。
 * newW / newH 是待放置节点的预估尺寸。
 */
function findFreePosition(startX, startY, newW, newH) {
  const all = getNodes.value
  let y = startY
  const MAX_TRIES = 30
  for (let i = 0; i < MAX_TRIES; i++) {
    const blocked = all.some(n => {
      const nw = n.dimensions?.width  ?? 220
      const nh = n.dimensions?.height ?? 120
      return overlaps(startX, y, newW, newH, n.position.x, n.position.y, nw, nh)
    })
    if (!blocked) return { x: startX, y }
    // 找出该列所有节点里，底边最低的那个，跳到它下方
    const colNodes = all.filter(n => {
      const nw = n.dimensions?.width ?? 220
      return n.position.x < startX + newW + GAP && n.position.x + nw + GAP > startX
    })
    if (colNodes.length === 0) break
    const maxBottom = Math.max(...colNodes.map(n => n.position.y + (n.dimensions?.height ?? 120)))
    y = maxBottom + GAP
  }
  return { x: startX, y }
}

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

  // 从 VueFlow 内部读取当前位置和实际宽度（拖拽后也准确）
  const src = findNode(props.id)
  const x = src?.position?.x ?? 0
  const y = src?.position?.y ?? 0
  const w = src?.dimensions?.width ?? 220

  // 预估新节点尺寸（dimensions 在渲染前未知，用各类型默认值）
  const NEW_W = { textNode: 220, imageNode: 240, videoNode: 280, noteNode: 180 }[type] ?? 220
  const NEW_H = 120

  // 在目标列（源节点右侧）找一个不与任何已有节点重叠的空位
  const pos = findFreePosition(x + w + 80, y, NEW_W, NEW_H)
  const newNode = store.createNode(type, pos)
  if (!newNode) return

  // 直接注入 VueFlow 内部状态，完全不替换 nodes.value，
  // 避免 VueFlow prop-reconciliation 把已拖拽节点弹回原位
  addNodes([newNode])

  // 平滑移动视角到新节点中心，保持当前缩放
  const { zoom } = getViewport()
  setCenter(pos.x + NEW_W / 2, pos.y + NEW_H / 2, { zoom, duration: 350 })

  addEdges([{
    id: `edge-${props.id}-${newNode.id}-${Date.now()}`,
    source: props.id,
    sourceHandle: 'sr',
    target: newNode.id,
    targetHandle: 'tl',
    animated: true,
    type: 'bezier',
    style: { stroke: '#646cff', strokeWidth: 2 },
    markerEnd: { type: 'arrowclosed', color: '#646cff' },
    data: { value: null },
  }])
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
