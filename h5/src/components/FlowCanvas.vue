<script setup>
import { ref, markRaw, computed } from 'vue'
import {
  VueFlow,
  useVueFlow,
  MarkerType,
  Position,
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
  onNodeDoubleClick,
  project,
  fitView,
  getSelectedNodes,
  getSelectedEdges,
  selectAll,
} = useVueFlow()

// ─── Connect handler ──────────────────────────────────────────────────────────
onConnect((params) => {
  store.addEdge({
    ...params,
    id: `edge-${params.source}-${params.sourceHandle}-${params.target}-${params.targetHandle}-${Date.now()}`,
  })
})

// ─── Fit view (exposed to Toolbar) ───────────────────────────────────────────
function doFitView() {
  fitView({ padding: 0.15, duration: 400 })
}

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
  if ((e.target).tagName === 'INPUT' || (e.target).tagName === 'TEXTAREA') return
  if (e.key === 'f' || e.key === 'F') doFitView()
  if (e.key === 'a' && (e.ctrlKey || e.metaKey)) { e.preventDefault(); selectAll(true) }
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
        :delete-key-code="'Delete'"
        :selection-key-code="'Shift'"
        :multi-selection-key-code="'Meta'"
        :pan-on-drag="true"
        :pan-on-scroll="true"
        pan-on-scroll-mode="free"
        :zoom-on-scroll="false"
        :zoom-on-pinch="true"
        :zoom-on-double-click="false"
        :prevent-scrolling="true"
        fit-view-on-init
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
  width: 10px !important;
  height: 10px !important;
  border-radius: 50% !important;
  background: #2e2e50 !important;
  border: 2px solid #646cff88 !important;
  transition: background 0.15s, border-color 0.15s, transform 0.15s !important;
}
.vue-flow__handle:hover {
  background: #646cff !important;
  border-color: #a0aaff !important;
  transform: scale(1.4) !important;
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
</style>
