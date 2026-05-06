<script setup>
import { useFlowStore } from '../stores/flowStore'
import { storeToRefs } from 'pinia'

const store = useFlowStore()
const { snapEnabled, showGrid, selectedNodes, nodes, edges } = storeToRefs(store)

const nodeItems = [
  { type: 'textNode',        icon: 'T',  label: '文本节点', color: '#646cff' },
  { type: 'imageUploadNode', icon: '🖼', label: '图片上传', color: '#42b883' },
  { type: 'imageGenNode',    icon: '🎨', label: 'AI 图片',  color: '#42b883' },
  { type: 'videoUploadNode', icon: '▶',  label: '视频上传', color: '#ff6b6b' },
  { type: 'videoGenNode',    icon: '🎬', label: 'AI 视频',  color: '#ff6b6b' },
  { type: 'noteNode',        icon: '📝', label: '备注',     color: '#f5c542' },
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

<style lang="scss" scoped>
@use '../styles/variables' as *;

$toolbar-border: #222238;

.toolbar {
  width: 160px;
  flex-shrink: 0;
  background: $bg-canvas;
  border-right: 1px solid $toolbar-border;
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
  border-bottom: 1px solid $toolbar-border;
  margin-bottom: 10px;
}

.logo-icon {
  font-size: 20px;
  color: $accent-primary;
}

.logo-text {
  font-size: 14px;
  font-weight: 700;
  color: $text-primary;
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
  border-radius: $radius-md;
  cursor: grab;
  background: #1a1a2e;
  border: 1px solid $toolbar-border;
  transition: background 0.15s, border-color 0.15s;

  &:hover {
    background: #222240;
    border-color: rgba($accent-primary, 0.27);
  }

  &:active { cursor: grabbing; }
}

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
  background: $toolbar-border;
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
  color: $text-secondary;
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
  transition: $transition-normal;

  &.active {
    background: rgba($accent-primary, 0.13);
    border-color: $accent-primary;
    color: #a0aaff;
  }
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
  border: 1px solid $toolbar-border;
  background: #1a1a2e;
  color: #c0c0e0;
  font-size: 11px;
  cursor: pointer;
  transition: $transition-fast;
  text-align: left;

  &:hover:not(:disabled) {
    background: #222240;
    border-color: rgba($accent-primary, 0.27);
    color: $text-primary;
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }

  &.group-btn:not(:disabled):hover {
    border-color: rgba($accent-green, 0.27);
    color: $accent-green;
  }

  &.danger-btn:not(:disabled):hover {
    border-color: rgba(#ff4d4d, 0.27);
    color: #ff4d4d;
    background: #1f1010;
  }
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
.stat-val { color: $text-secondary; font-weight: 600; }

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
  color: $text-dim;
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
