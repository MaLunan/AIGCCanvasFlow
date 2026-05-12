<script setup>
// 右键上下文菜单：根据点击目标类型（节点/边/画布）显示不同操作项
// 通过 Teleport 挂载到 body，确保不受父组件 overflow:hidden 的影响
const props = defineProps({
  visible: Boolean,
  x: Number,          // 鼠标 X 坐标（clientX）
  y: Number,          // 鼠标 Y 坐标（clientY）
  target: Object,     // null = 画布空白区域，否则为节点或边对象
  targetType: String, // 'node' | 'edge' | 'pane'
})

const emit = defineEmits(['action', 'close'])

/** 触发操作并自动关闭菜单 */
function act(action) {
  emit('action', action)  // 通知父组件执行对应操作
  emit('close')           // 关闭菜单
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="ctx-overlay"
      @click="$emit('close')"
      @contextmenu.prevent="$emit('close')"
    />
    <div
      v-if="visible"
      class="ctx-menu"
      :style="{ left: `${x}px`, top: `${y}px` }"
    >
      <template v-if="targetType === 'node'">
        <div class="ctx-header">{{ target?.type || '节点' }}</div>
        <button class="ctx-item" @click="act('duplicate')">
          <span>⧉</span> 复制节点
        </button>
        <button class="ctx-item" @click="act('edit')">
          <span>✎</span> 编辑
        </button>
        <div class="ctx-divider" />
        <button class="ctx-item danger" @click="act('delete')">
          <span>⊗</span> 删除节点
        </button>
      </template>

      <template v-else-if="targetType === 'edge'">
        <div class="ctx-header">连线</div>
        <button class="ctx-item" @click="act('toggle-animated')">
          <span>⟳</span> 切换动画
        </button>
        <div class="ctx-divider" />
        <button class="ctx-item danger" @click="act('delete')">
          <span>⊗</span> 删除连线
        </button>
      </template>

      <template v-else>
        <div class="ctx-header">画布</div>
        <button class="ctx-item" @click="act('select-all')">
          <span>⊡</span> 全选
        </button>
        <button class="ctx-item" @click="act('fit-view')">
          <span>⊙</span> 适应视图
        </button>
        <div class="ctx-divider" />
        <button class="ctx-item" @click="act('add-text')">
          <span>T</span> 添加文本节点
        </button>
        <button class="ctx-item" @click="act('add-image')">
          <span>🖼</span> 添加图片节点
        </button>
        <button class="ctx-item" @click="act('add-note')">
          <span>📝</span> 添加备注
        </button>
      </template>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.ctx-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
}

.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: #1a1a2e;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: 4px;
  min-width: 160px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba($accent-primary, 0.15);
  animation: ctx-in 0.1s ease;
}

@keyframes ctx-in {
  from { opacity: 0; transform: scale(0.95); }
  to   { opacity: 1; transform: scale(1); }
}

.ctx-header {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: #555578;
  padding: 4px 10px 6px;
}

.ctx-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 12px;
  background: none;
  border: none;
  border-radius: 7px;
  color: #c0c0e0;
  font-size: 12px;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;

  span {
    width: 16px;
    text-align: center;
    opacity: 0.8;
  }

  &:hover {
    background: rgba($accent-primary, 0.13);
    color: #e0e0ff;
  }

  &.danger {
    color: #ff8080;

    &:hover {
      background: rgba(#ff4d4d, 0.09);
      color: #ff4d4d;
    }
  }
}

.ctx-divider {
  height: 1px;
  background: $border-default;
  margin: 3px 8px;
}
</style>
