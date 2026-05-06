<script setup>
import { ref, computed } from 'vue'
import { useFlowStore } from '../../stores/flowStore'

const props = defineProps({
  nodeId: { type: String, required: true },
})

const store = useFlowStore()

const currentScope = computed(() => {
  const node = store.nodes.find(n => n.id === props.nodeId)
  return node?.data?.scope ?? 'direct'
})

function toggle(e) {
  e.stopPropagation()
  store.updateNodeData(props.nodeId, { scope: currentScope.value === 'direct' ? 'global' : 'direct' })
}

const infoOpen = ref(false)
const infoStyle = ref({})

function openInfo(e) {
  e.stopPropagation()
  const rect = e.currentTarget.getBoundingClientRect()
  infoStyle.value = {
    top:  `${rect.bottom + 6}px`,
    left: `${Math.max(4, rect.right - 244)}px`,
  }
  infoOpen.value = true
  setTimeout(() => document.addEventListener('click', closeInfo, { once: true }), 0)
}

function closeInfo() {
  infoOpen.value = false
}
</script>

<template>
  <div class="scope-wrap" @mousedown.stop @click.stop>
    <button
      class="scope-toggle"
      :class="currentScope"
      @click.stop="toggle"
      :title="`当前：${currentScope === 'direct' ? '直连生效' : '全局生效'}（点击切换）`"
    >{{ currentScope === 'direct' ? '直连' : '全局' }}</button>
    <button class="scope-info-btn" @click.stop="openInfo" title="了解生效范围">ⓘ</button>
  </div>

  <Teleport to="body">
    <template v-if="infoOpen">
      <div class="scope-backdrop" @click="closeInfo" @mousedown.stop />
      <div class="scope-popup" :style="infoStyle" @click.stop @mousedown.stop>
        <div class="scope-popup-title">生效范围说明</div>
        <div class="scope-popup-row">
          <span class="scope-chip direct">直连</span>
          <p>仅<strong>直接连接</strong>的后续节点调用 AI 时，会将本节点内容作为参考上下文传入。</p>
        </div>
        <div class="scope-popup-row">
          <span class="scope-chip global">全局</span>
          <p>所有间接或直接连接的后续节点调用 AI 时，均纳入本节点内容作参考，不论中间跨越多少节点。</p>
        </div>
      </div>
    </template>
  </Teleport>
</template>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.scope-wrap {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

.scope-toggle {
  font-size: 9px;
  font-weight: 700;
  padding: 2px 5px;
  border-radius: 4px 0 0 4px;
  border: 1px solid transparent;
  cursor: pointer;
  font-family: inherit;
  line-height: 1.5;
  transition: background 0.15s, color 0.15s, border-color 0.15s;

  &.direct {
    color: #7fd1f5;
    background: rgba(127, 209, 245, 0.1);
    border-color: rgba(127, 209, 245, 0.25);
    &:hover { background: rgba(127, 209, 245, 0.2); border-color: rgba(127, 209, 245, 0.5); }
  }

  &.global {
    color: #d07ff5;
    background: rgba(208, 127, 245, 0.1);
    border-color: rgba(208, 127, 245, 0.25);
    &:hover { background: rgba(208, 127, 245, 0.2); border-color: rgba(208, 127, 245, 0.5); }
  }
}

.scope-info-btn {
  font-size: 9px;
  padding: 2px 4px;
  border-radius: 0 4px 4px 0;
  border: 1px solid $border-default;
  border-left: none;
  background: rgba(255, 255, 255, 0.07);
  color: $text-secondary;
  cursor: pointer;
  font-family: inherit;
  line-height: 1.5;
  transition: color 0.15s, background 0.15s;

  &:hover { color: $text-primary; background: rgba(255, 255, 255, 0.13); }
}
</style>

<!-- Teleported popup needs global (unscoped) styles -->
<style lang="scss">
@use '../../styles/variables' as *;

.scope-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9000;
}

.scope-popup {
  position: fixed;
  z-index: 9001;
  background: #1a1a2e;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  padding: 10px 12px;
  width: 244px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.65), 0 0 0 1px rgba($accent-primary, 0.12);
  animation: scope-popup-in 0.12s ease;
}

@keyframes scope-popup-in {
  from { opacity: 0; transform: translateY(-4px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.scope-popup-title {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: $text-dim;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid $border-default;
}

.scope-popup-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-bottom: 8px;

  &:last-child { margin-bottom: 0; }

  p {
    margin: 0;
    font-size: 11px;
    color: $text-secondary;
    line-height: 1.5;

    strong { color: $text-primary; }
  }
}

.scope-chip {
  flex-shrink: 0;
  font-size: 9px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 1px;

  &.direct {
    color: #7fd1f5;
    background: rgba(127, 209, 245, 0.12);
    border: 1px solid rgba(127, 209, 245, 0.3);
  }

  &.global {
    color: #d07ff5;
    background: rgba(208, 127, 245, 0.12);
    border: 1px solid rgba(208, 127, 245, 0.3);
  }
}
</style>
