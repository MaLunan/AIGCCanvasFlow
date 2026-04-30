<script setup>
import { ref, markRaw, computed, onMounted } from 'vue'
import {
  VueFlow,
  useVueFlow,
  MarkerType,
  Position,
  SelectionMode,
} from '@vue-flow/core'
import { Background, BackgroundVariant } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { storeToRefs } from 'pinia'

import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'

import { useFlowStore } from '../stores/flowStore'
import Toolbar from './Toolbar.vue'
import ContextMenu from './ContextMenu.vue'

import TextNode  from './nodes/TextNode.vue'
import ImageNode from './nodes/ImageNode.vue'
import VideoNode from './nodes/VideoNode.vue'
import NoteNode  from './nodes/NoteNode.vue'
import GroupNode from './nodes/GroupNode.vue'

// Register node types (markRaw avoids reactivity overhead on components)
const nodeTypes = {
  textNode:  markRaw(TextNode),
  imageNode: markRaw(ImageNode),
  videoNode: markRaw(VideoNode),
  noteNode:  markRaw(NoteNode),
  groupNode: markRaw(GroupNode),
}

const store = useFlowStore()
const { nodes, edges, snapEnabled, gridSize, showGrid } = storeToRefs(store)

// ─── VueFlow instance ─────────────────────────────────────────────────────────
const {
  onConnect,
  onConnectStart,
  onConnectEnd,
  project,
  fitView,
  getSelectedNodes,
  getSelectedEdges,
  selectAll,
  addNodes,
  addEdges,
} = useVueFlow()

// ─── Connect handler ──────────────────────────────────────────────────────────
// Normalize connections: only source→target allowed.
// head-to-head: keep direction A→B, fix target handle to 'tl'
// tail-to-tail: reverse direction to B→A, fix both handles
let connectHandled = false
onConnect((params) => {
  connectHandled = true

  let { source, sourceHandle, target, targetHandle } = params

  const isSource = (id) => id === 'sr'  // head
  const isTarget = (id) => id === 'tl'  // tail

  if (isSource(sourceHandle) && isSource(targetHandle)) {
    // head→head: auto-connect to the other's tail instead
    targetHandle = 'tl'
  } else if (isTarget(sourceHandle) && isTarget(targetHandle)) {
    // tail→tail: reverse the direction
    ;[source, target] = [target, source]
    sourceHandle = 'sr'
    targetHandle = 'tl'
  }

  store.addEdge({
    source, sourceHandle, target, targetHandle,
    id: `edge-${source}-${sourceHandle}-${target}-${targetHandle}-${Date.now()}`,
  })
})

// ─── Edge-drop picker ─────────────────────────────────────────────────────────
// picker state: set when picker is shown, read by createFromEdgeDrop
const edgeDrop = ref({ visible: false, x: 0, y: 0, canvasPos: { x: 0, y: 0 } })
let dropSource = null   // { nodeId, handleId } — plain object, not reactive

const ghostLine = ref({ visible: false, x1: 0, y1: 0, x2: 0, y2: 0 })

// drag source captured at connectStart, consumed at connectEnd
let connectStartInfo = null  // { nodeId, handleId, hx, hy, startX, startY }

const EDGE_DROP_TYPES = [
  { type: 'textNode',  icon: 'T',  label: '文本节点', color: '#646cff' },
  { type: 'imageNode', icon: '🖼', label: '图片节点', color: '#42b883' },
  { type: 'videoNode', icon: '▶',  label: '视频节点', color: '#ff6b6b' },
  { type: 'noteNode',  icon: '📝', label: '备注',     color: '#f5c542' },
]

function ghostLinePath(x1, y1, x2, y2) {
  const dx = Math.abs(x2 - x1) * 0.5
  return `M ${x1} ${y1} C ${x1 + dx} ${y1} ${x2 - dx} ${y2} ${x2} ${y2}`
}

// VueFlow fires connectStart on mousedown on a handle — gives us nodeId/handleId directly
onConnectStart(({ event, nodeId, handleId }) => {
  connectHandled = false
  // Capture handle screen center now (element is guaranteed in DOM at mousedown)
  const handle = event?.target?.closest?.('.vue-flow__handle') ?? event?.target
  const r = handle?.getBoundingClientRect()
  connectStartInfo = {
    nodeId,
    handleId,
    hx: r ? r.left + r.width / 2 : (event?.clientX ?? 0),
    hy: r ? r.top + r.height / 2 : (event?.clientY ?? 0),
    startX: event?.clientX ?? 0,
    startY: event?.clientY ?? 0,
  }
})

// VueFlow fires connectEnd on mouseup — AFTER connect (so connectHandled is already set)
onConnectEnd((event) => {
  const info = connectStartInfo
  connectStartInfo = null

  if (!info || !event) return

  // Ignore if picker is already open
  if (edgeDrop.value.visible) return

  // A real connection was made — nothing to do
  if (connectHandled) { connectHandled = false; return }

  // Ignore tiny movements (simple click on a handle, not a drag)
  const dx = (event.clientX ?? 0) - info.startX
  const dy = (event.clientY ?? 0) - info.startY
  if (Math.sqrt(dx * dx + dy * dy) < 8) return

  // Released on another handle — VueFlow would have fired connect already
  if (event.target?.closest?.('.vue-flow__handle')) return

  const bounds = canvasRef.value?.getBoundingClientRect()
  const canvasPos = project({
    x: event.clientX - (bounds?.left ?? 0),
    y: event.clientY - (bounds?.top ?? 0),
  })

  ghostLine.value = { visible: true, x1: info.hx, y1: info.hy, x2: event.clientX, y2: event.clientY }
  dropSource = { nodeId: info.nodeId, handleId: info.handleId }
  edgeDrop.value = { visible: true, x: event.clientX, y: event.clientY, canvasPos }
})

function closeEdgeDrop() {
  edgeDrop.value.visible = false
  ghostLine.value.visible = false
  dropSource = null
}

function createFromEdgeDrop(e, type) {
  e.stopPropagation()

  // Snapshot everything before closing (closeEdgeDrop clears dropSource)
  const src = dropSource
  const canvasPos = { ...edgeDrop.value.canvasPos }
  closeEdgeDrop()

  const NEW_W = { textNode: 220, imageNode: 240, videoNode: 280, noteNode: 180 }[type] ?? 220
  const newNode = store.createNode(type, { x: canvasPos.x - NEW_W / 2, y: canvasPos.y - 60 })
  if (!newNode) return

  addNodes([newNode])

  if (src?.nodeId) {
    addEdges([{
      id: `edge-${src.nodeId}-${newNode.id}-${Date.now()}`,
      source: src.nodeId,
      sourceHandle: src.handleId ?? 'sr',
      target: newNode.id,
      targetHandle: 'tl',
      animated: true,
      type: 'bezier',
      style: { stroke: '#646cff', strokeWidth: 2 },
      markerEnd: { type: 'arrowclosed', color: '#646cff' },
      data: { value: null },
    }])
  }
}

// ─── Fit view ─────────────────────────────────────────────────────────────────
function doFitView() {
  fitView({ padding: 0.15, duration: 400 })
}

onMounted(() => {
  setTimeout(() => fitView({ padding: 0.15 }), 50)
})

// ─── Drag-and-drop from toolbar ───────────────────────────────────────────────
const canvasRef = ref(null)

function onDragOver(e) {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

function onDrop(e) {
  e.preventDefault()
  const type = e.dataTransfer.getData('application/vueflow')
  if (!type) return
  const bounds = canvasRef.value?.getBoundingClientRect()
  const pos = project({
    x: e.clientX - (bounds?.left ?? 0),
    y: e.clientY - (bounds?.top ?? 0),
  })
  store.addNodeOfType(type, pos)
}

// ─── Context menu ─────────────────────────────────────────────────────────────
const ctxMenu = ref({ visible: false, x: 0, y: 0, target: null, targetType: 'pane' })
// Store drop position for "add node at cursor" from context menu
const ctxDropPos = ref({ x: 0, y: 0 })

function openCtxMenu(e, target, targetType) {
  e.preventDefault()
  e.stopPropagation()
  ctxMenu.value = { visible: true, x: e.clientX, y: e.clientY, target, targetType }
  const bounds = canvasRef.value?.getBoundingClientRect()
  ctxDropPos.value = project({
    x: e.clientX - (bounds?.left ?? 0),
    y: e.clientY - (bounds?.top ?? 0),
  })
}

function closeCtxMenu() {
  ctxMenu.value.visible = false
}

function onCtxAction(action) {
  const { target, targetType } = ctxMenu.value
  const pos = ctxDropPos.value

  if (targetType === 'node' && target) {
    if (action === 'delete') {
      store.removeNodeById(target.id)
    } else if (action === 'duplicate') {
      const orig = nodes.value.find((n) => n.id === target.id)
      if (orig) {
        store.addNodeOfType(orig.type, {
          x: orig.position.x + 30,
          y: orig.position.y + 30,
        })
        // Copy data
        const newNode = nodes.value[nodes.value.length - 1]
        store.updateNodeData(newNode.id, { ...orig.data })
      }
    }
  } else if (targetType === 'edge' && target) {
    if (action === 'delete') {
      edges.value = edges.value.filter((e) => e.id !== target.id)
    } else if (action === 'toggle-animated') {
      store.updateEdgeData(target.id, { animated: !target.animated })
    }
  } else {
    // Pane actions
    if (action === 'select-all') selectAll(true)
    else if (action === 'fit-view') doFitView()
    else if (action === 'add-text') store.addNodeOfType('textNode', pos)
    else if (action === 'add-image') store.addNodeOfType('imageNode', pos)
    else if (action === 'add-note') store.addNodeOfType('noteNode', pos)
  }
}

// ─── Keyboard shortcuts ───────────────────────────────────────────────────────
function onKeydown(e) {
  if (['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return
  if (e.key === 'f' || e.key === 'F') doFitView()
  if (e.key === 'a' && (e.ctrlKey || e.metaKey)) { e.preventDefault(); selectAll(true) }
  if (e.key === 'Delete' || e.key === 'Backspace') store.removeSelectedNodes()
}
</script>

<template>
  <div class="app-shell" @keydown="onKeydown" tabindex="0">
    <Toolbar @fit-view="doFitView" />

    <div
      ref="canvasRef"
      class="canvas-wrap"
      @dragover="onDragOver"
      @drop="onDrop"
    >
      <VueFlow
        :nodes="nodes"
        :edges="edges"
        :node-types="nodeTypes"
        :snap-to-grid="snapEnabled"
        :snap-grid="[gridSize, gridSize]"
        :default-zoom="1"
        :min-zoom="0.1"
        :max-zoom="4"
        :delete-key-code="['Delete', 'Backspace']"
        :selection-key-code="'Shift'"
        :selection-mode="SelectionMode.Partial"
        :multi-selection-key-code="'Meta'"
        :pan-on-drag="true"
        :pan-on-scroll="true"
        pan-on-scroll-mode="free"
        :zoom-on-scroll="false"
        :zoom-on-pinch="true"
        :zoom-on-double-click="false"
        :prevent-scrolling="true"
        elevate-edges-on-select
        @nodes-change="store.handleNodesChange"
        @edges-change="store.handleEdgesChange"
        @pane-context-menu="(e) => openCtxMenu(e, null, 'pane')"
        @node-context-menu="(e, node) => openCtxMenu(e, node, 'node')"
        @edge-context-menu="(e, edge) => openCtxMenu(e, edge, 'edge')"
        @pane-click="closeCtxMenu"
        @node-click="closeCtxMenu"
      >
        <!-- Background grid -->
        <Background
          v-if="showGrid"
          :variant="BackgroundVariant.Lines"
          :gap="gridSize"
          :size="1"
          :color="'#1e1e35'"
          :line-width="1"
        />
        <Background
          v-if="showGrid"
          :variant="BackgroundVariant.Dots"
          :gap="gridSize * 4"
          :size="1.5"
          :color="'#2e2e50'"
        />

        <Controls :show-interactive="true" />

        <MiniMap
          :node-color="() => '#646cff'"
          :node-stroke-color="() => '#000'"
          :node-border-radius="4"
          mask-color="rgba(0,0,0,0.7)"
        />
      </VueFlow>
    </div>

    <ContextMenu
      v-bind="ctxMenu"
      @close="closeCtxMenu"
      @action="onCtxAction"
    />

    <!-- Ghost connection line (SVG overlay, screen-space, no VueFlow) -->
    <Teleport to="body">
      <svg v-if="ghostLine.visible" class="ghost-line-svg" style="pointer-events:none">
        <defs>
          <marker id="ghost-arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill="#646cff99" />
          </marker>
        </defs>
        <path
          :d="ghostLinePath(ghostLine.x1, ghostLine.y1, ghostLine.x2, ghostLine.y2)"
          stroke="#646cff99"
          stroke-width="2"
          stroke-dasharray="6 4"
          fill="none"
          marker-end="url(#ghost-arrow)"
        />
      </svg>
    </Teleport>

    <!-- Edge-drop node picker -->
    <Teleport to="body">
      <template v-if="edgeDrop.visible">
        <div class="edrop-backdrop" @click.stop="closeEdgeDrop" @mousedown.stop @pointerdown.stop />
        <div
          class="edrop-picker"
          :style="{ top: edgeDrop.y + 'px', left: edgeDrop.x + 'px' }"
          @click.stop @mousedown.stop
        >
          <div class="edrop-title">创建节点并连接</div>
          <button
            v-for="opt in EDGE_DROP_TYPES"
            :key="opt.type"
            class="edrop-item"
            @click="createFromEdgeDrop($event, opt.type)"
            @mousedown.stop
          >
            <span class="edrop-icon" :style="{ color: opt.color }">{{ opt.icon }}</span>
            <span class="edrop-label">{{ opt.label }}</span>
          </button>
        </div>
      </template>
    </Teleport>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  outline: none;
}

.canvas-wrap {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #0b0b16;
  touch-action: none;
  overscroll-behavior: none;
}
</style>

<style>
/* ─── Global node shared styles ─────────────────────────────────────── */
.canvas-node {
  background: #1a1a2e;
  border: 1.5px solid #2e2e50;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  transition: border-color 0.15s, box-shadow 0.15s;
  overflow: visible;
}
.canvas-node.selected {
  border-color: #646cff;
  box-shadow: 0 0 0 2px #646cff44, 0 4px 20px rgba(100, 108, 255, 0.25);
}

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid #2e2e50;
  border-radius: 9px 9px 0 0;
}
.node-icon {
  font-size: 12px;
  flex-shrink: 0;
}
.node-label {
  font-size: 11px;
  font-weight: 600;
  color: #a0a0c0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.node-body {
  padding: 10px;
}

.node-data-in {
  padding: 4px 10px 6px;
  border-top: 1px solid #1e1e35;
}
.data-badge {
  font-size: 10px;
  color: #42b883;
  background: #42b88318;
  border: 1px solid #42b88333;
  border-radius: 4px;
  padding: 2px 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
  max-width: 100%;
}

/* ─── VueFlow handle customization ─────────────────────────────────── */
.vue-flow__handle {
  width: 12px !important;
  height: 12px !important;
  border-radius: 50% !important;
  background: #2e2e50 !important;
  border: 2px solid #646cff88 !important;
  transition: background 0.15s, border-color 0.15s !important;
  /* NO transform/scale — CSS transform triggers lostpointercapture, breaking drag */
}
.vue-flow__handle:hover {
  background: #646cff !important;
  border-color: #a0aaff !important;
}
.vue-flow__handle-connecting {
  background: #42b883 !important;
  border-color: #42b883 !important;
}

/* ─── VueFlow edge customization ─────────────────────────────────────  */
.vue-flow__edge-path {
  stroke: #646cff;
  stroke-width: 2;
}
.vue-flow__edge.selected .vue-flow__edge-path {
  stroke: #a0aaff;
  stroke-width: 2.5;
}
.vue-flow__edge-label {
  font-size: 10px;
  fill: #a0a0c0;
}

/* ─── Selection box ─────────────────────────────────────────────────── */
.vue-flow__selection {
  background: rgba(100, 108, 255, 0.06) !important;
  border: 1.5px solid #646cff99 !important;
}

/* ─── Controls ──────────────────────────────────────────────────────── */
.vue-flow__controls {
  background: #1a1a2e !important;
  border: 1px solid #2e2e50 !important;
  border-radius: 10px !important;
  box-shadow: 0 4px 16px rgba(0,0,0,0.4) !important;
}
.vue-flow__controls-button {
  background: transparent !important;
  border: none !important;
  border-bottom: 1px solid #2e2e50 !important;
  color: #a0a0c0 !important;
  transition: background 0.15s !important;
}
.vue-flow__controls-button:hover {
  background: #646cff22 !important;
  color: #e0e0ff !important;
}
.vue-flow__controls-button:last-child {
  border-bottom: none !important;
}

/* ─── MiniMap ───────────────────────────────────────────────────────── */
.vue-flow__minimap {
  background: #0f0f1a !important;
  border: 1px solid #2e2e50 !important;
  border-radius: 10px !important;
}

/* ─── Group node (parent container) ────────────────────────────────── */
.vue-flow__node-groupNode {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

/* ─── Node resizer ─────────────────────────────────────────────────── */
.vue-flow__resize-control {
  background: #646cff !important;
  border-color: #a0aaff !important;
}

/* ─── Ghost line SVG overlay ────────────────────────────────────────── */
.ghost-line-svg {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  z-index: 8999;
  overflow: visible;
}

/* ─── Edge-drop picker ──────────────────────────────────────────────── */
.edrop-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9000;
}
.edrop-picker {
  position: fixed;
  z-index: 9001;
  transform: translate(-50%, 8px);
  background: #1a1a2e;
  border: 1px solid #2e2e50;
  border-radius: 10px;
  padding: 4px;
  min-width: 160px;
  box-shadow: 0 8px 28px rgba(0,0,0,0.65), 0 0 0 1px rgba(100,108,255,0.12);
  animation: edrop-in 0.12s ease;
}
@keyframes edrop-in {
  from { opacity: 0; transform: translate(-50%, 0px) scale(0.96); }
  to   { opacity: 1; transform: translate(-50%, 8px) scale(1); }
}
.edrop-title {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: #444466;
  padding: 4px 8px 6px;
}
.edrop-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 10px;
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
.edrop-item:last-child { margin-bottom: 0; }
.edrop-item:hover {
  background: #ffffff0c;
  color: #e0e0f0;
}
.edrop-icon { width: 16px; text-align: center; font-size: 13px; flex-shrink: 0; }
.edrop-label { flex: 1; font-size: 11px; }
</style>
