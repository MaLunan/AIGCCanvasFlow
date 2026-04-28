<script setup>
import { useFlowStore } from '../stores/flowStore'
import { storeToRefs } from 'pinia'

const store = useFlowStore()
const { snapEnabled, showGrid, selectedNodes, nodes, edges } = storeToRefs(store)

const nodeItems = [
  { type: 'textNode',  icon: 'T',  label: '文本节点', color: '#646cff' },
  { type: 'imageNode', icon: '🖼', label: '图片节点', color: '#42b883' },
  { type: 'videoNode', icon: '▶', label: '视频节点', color: '#ff6b6b' },
  { type: 'noteNode',  icon: '📝', label: '备注',     color: '#f5c542' },
]

function onDragStart(e, type) {
  e.dataTransfer.setData('application/vueflow', type)
  e.dataTransfer.effectAllowed = 'move'
}

const emit = defineEmits(['fit-view'])
</script>

<template>
  <aside class="toolbar">
    <div class="toolbar-logo">
      <span class="logo-icon">⬡</span>
      <span class="logo-text">Canvas</span>
    </div>

    <!-- Node types -->
    <div class="section-title">节点</div>
    <div class="node-list">
      <div
        v-for="item in nodeItems"
        :key="item.type"
        class="node-item"
        draggable="true"
        @dragstart="onDragStart($event, item.type)"
        :title="`拖拽到画布创建 ${item.label}`"
      >
        <span class="item-icon" :style="{ color: item.color }">{{ item.icon }}</span>
        <span class="item-label">{{ item.label }}</span>
        <span class="drag-hint">⠿</span>
      </div>
    </div>

    <div class="divider" />

    <!-- Canvas controls -->
    <div class="section-title">画布</div>
    <div class="control-list">
      <label class="toggle-row" title="吸附到网格">
        <span>吸附网格</span>
        <button
          class="toggle-btn"
          :class="{ active: snapEnabled }"
          @click="snapEnabled = !snapEnabled"
        >{{ snapEnabled ? 'ON' : 'OFF' }}</button>
      </label>
      <label class="toggle-row" title="显示网格">
        <span>显示网格</span>
        <button
          class="toggle-btn"
          :class="{ active: showGrid }"
          @click="showGrid = !showGrid"
        >{{ showGrid ? 'ON' : 'OFF' }}</button>
      </label>
    </div>

    <div class="divider" />

    <!-- Actions -->
    <div class="section-title">操作</div>
    <div class="action-list">
      <button class="action-btn" @click="$emit('fit-view')" title="适应视图 (F)">
        <span>⊙</span> 适应视图
      </button>
      <button
        class="action-btn group-btn"
        :disabled="selectedNodes.length < 2"
        @click="store.groupSelectedNodes()"
        title="将选中节点分组"
      >
        <span>⊞</span> 分组 ({{ selectedNodes.length }})
      </button>
      <button
        class="action-btn danger-btn"
        :disabled="selectedNodes.length === 0"
        @click="store.removeSelectedNodes()"
        title="删除选中节点 (Delete)"
      >
        <span>⊗</span> 删除选中
      </button>
    </div>

    <div class="divider" />

    <!-- Stats -->
    <div class="stats">
      <div class="stat-row">
        <span class="stat-label">节点</span>
        <span class="stat-val">{{ nodes.length }}</span>
      </div>
      <div class="stat-row">
        <span class="stat-label">连线</span>
        <span class="stat-val">{{ edges.length }}</span>
      </div>
      <div class="stat-row">
        <span class="stat-label">已选</span>
        <span class="stat-val">{{ selectedNodes.length }}</span>
      </div>
    </div>

    <div class="divider" />

    <!-- Keyboard hints -->
    <div class="hints">
      <div class="hint-row"><kbd>Shift</kbd> 框选</div>
      <div class="hint-row"><kbd>Del</kbd> 删除选中</div>
      <div class="hint-row"><kbd>Ctrl+A</kbd> 全选</div>
      <div class="hint-row"><kbd>滚轮</kbd> 缩放</div>
    </div>
  </aside>
</template>

<style scoped>
.toolbar {
  width: 160px;
  flex-shrink: 0;
  background: #13131f;
  border-right: 1px solid #222238;
  display: flex;
  flex-direction: column;
  padding: 12px 0;
  overflow-y: auto;
  overflow-x: hidden;
  user-select: none;
}

.toolbar-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px 12px;
  border-bottom: 1px solid #222238;
  margin-bottom: 10px;
}
.logo-icon {
  font-size: 20px;
  color: #646cff;
}
.logo-text {
  font-size: 14px;
  font-weight: 700;
  color: #e0e0f0;
  letter-spacing: 0.5px;
}

.section-title {
  font-size: 10px;
  font-weight: 600;
  color: #555578;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 0 12px;
  margin-bottom: 6px;
}

.node-list {
  padding: 0 8px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-bottom: 4px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 8px;
  cursor: grab;
  background: #1a1a2e;
  border: 1px solid #222238;
  transition: background 0.15s, border-color 0.15s;
}
.node-item:hover {
  background: #222240;
  border-color: #646cff44;
}
.node-item:active { cursor: grabbing; }
.item-icon {
  font-size: 14px;
  width: 18px;
  text-align: center;
}
.item-label {
  font-size: 11px;
  color: #c0c0e0;
  flex: 1;
}
.drag-hint {
  font-size: 12px;
  color: #444;
  letter-spacing: -2px;
}

.divider {
  height: 1px;
  background: #222238;
  margin: 8px 0;
}

.control-list {
  padding: 0 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 4px;
}
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 4px 4px 6px;
  font-size: 11px;
  color: #a0a0c0;
  cursor: pointer;
}
.toggle-btn {
  font-size: 9px;
  padding: 2px 6px;
  border-radius: 10px;
  border: 1px solid #333;
  background: #0f0f1a;
  color: #666;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}
.toggle-btn.active {
  background: #646cff22;
  border-color: #646cff;
  color: #a0aaff;
}

.action-list {
  padding: 0 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 4px;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 7px;
  border: 1px solid #222238;
  background: #1a1a2e;
  color: #c0c0e0;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}
.action-btn:hover:not(:disabled) {
  background: #222240;
  border-color: #646cff44;
  color: #e0e0f0;
}
.action-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.group-btn:not(:disabled):hover {
  border-color: #42b88344;
  color: #42b883;
}
.danger-btn:not(:disabled):hover {
  border-color: #ff4d4d44;
  color: #ff4d4d;
  background: #1f1010;
}

.stats {
  padding: 0 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
}
.stat-label { color: #555578; }
.stat-val { color: #a0a0c0; font-weight: 600; }

.hints {
  padding: 0 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.hint-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: #444466;
}
kbd {
  background: #1a1a2e;
  border: 1px solid #333;
  border-radius: 3px;
  padding: 1px 4px;
  font-size: 9px;
  color: #666;
  font-family: inherit;
  white-space: nowrap;
}
</style>
