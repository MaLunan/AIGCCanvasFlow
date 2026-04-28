<script setup>
import { ref } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { useFlowStore } from '../../stores/flowStore'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const editingUrl = ref(false)
const localSrc = ref(props.data.src || '')

function confirmUrl() {
  editingUrl.value = false
  store.updateNodeData(props.id, { src: localSrc.value, outputValue: localSrc.value })
}

function onKeydown(e) {
  if (e.key === 'Enter') confirmUrl()
  if (e.key === 'Escape') editingUrl.value = false
  e.stopPropagation()
}
</script>

<template>
  <div :class="['canvas-node', 'image-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <div class="node-header">
      <span class="node-icon img-icon">🖼</span>
      <span class="node-label">{{ data.label }}</span>
      <button class="node-del" @click.stop="store.removeNodeById(id)" title="删除">×</button>
    </div>

    <div class="node-body img-body">
      <template v-if="data.src && !editingUrl">
        <img
          :src="data.src"
          :alt="data.alt || '图片'"
          class="node-image"
          @dblclick.stop="editingUrl = true; localSrc = data.src"
          @error="e => e.target.classList.add('img-error')"
        />
      </template>
      <template v-else>
        <div v-if="!editingUrl" class="img-placeholder" @click.stop="editingUrl = true; localSrc = ''">
          <div class="placeholder-icon">🖼</div>
          <div class="placeholder-hint">点击输入图片 URL</div>
        </div>
        <div v-else class="url-input-wrap">
          <input
            v-model="localSrc"
            class="url-input"
            placeholder="https://example.com/image.jpg"
            @keydown="onKeydown"
            @blur="confirmUrl"
            autofocus
            @click.stop
          />
          <button class="url-confirm" @click.stop="confirmUrl">确认</button>
        </div>
      </template>
    </div>

    <Handle id="sr" type="source" :position="Position.Right" :style="{ top: '50%' }" />
  </div>
</template>

<style scoped>
.image-node {
  width: 240px;
}
.img-icon {
  font-size: 14px;
}
.img-body {
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 0 10px 10px;
}
.node-image {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
  cursor: pointer;
}
.node-image.img-error {
  filter: grayscale(1) opacity(0.3);
}
.img-placeholder {
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  background: #0d0d1a;
  border: 2px dashed #383854;
  border-radius: 0 0 10px 10px;
  transition: border-color 0.2s;
}
.img-placeholder:hover {
  border-color: #646cff;
}
.placeholder-icon {
  font-size: 28px;
  opacity: 0.4;
}
.placeholder-hint {
  font-size: 11px;
  color: #666;
}
.url-input-wrap {
  display: flex;
  gap: 6px;
  padding: 8px;
  background: #0d0d1a;
}
.url-input {
  flex: 1;
  background: #1a1a2e;
  border: 1px solid #646cff66;
  border-radius: 6px;
  color: #e0e0f0;
  font-size: 11px;
  padding: 4px 8px;
  outline: none;
}
.url-confirm {
  padding: 4px 10px;
  background: #646cff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 11px;
  cursor: pointer;
}
</style>
