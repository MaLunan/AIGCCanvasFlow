<script setup>
import { ref, computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { useFlowStore } from '../../stores/flowStore'
import NodeHeader from './NodeHeader.vue'
import NodeAddButton from './NodeAddButton.vue'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const fileInputRef = ref(null)

const isLocalFile = computed(() => (props.data.src || '').startsWith('blob:'))
const fileName = computed(() => props.data.fileName || '')

function triggerUpload(e) {
  e.stopPropagation()
  fileInputRef.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (isLocalFile.value) URL.revokeObjectURL(props.data.src)
  const blobUrl = URL.createObjectURL(file)
  store.updateNodeData(props.id, { src: blobUrl, outputValue: blobUrl, fileName: file.name })
  e.target.value = ''
}
</script>

<template>
  <div :class="['canvas-node', 'image-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <NodeHeader :id="id" :label="data.label" current-type="imageNode" />

    <input ref="fileInputRef" type="file" accept="image/*" class="hidden-file" @change="onFileChange" @click.stop />

    <div class="node-body img-body">
      <!-- ── Has image ── -->
      <template v-if="data.src">
        <div class="img-preview-wrap">
          <img :src="data.src" :alt="data.alt || '图片'" class="node-image" @error="e => e.target.classList.add('img-error')" />
          <div class="img-overlay">
            <button class="overlay-btn" @click.stop="triggerUpload" title="重新上传">📁 换图片</button>
          </div>
        </div>
        <div v-if="isLocalFile" class="file-badge">📁 {{ fileName || '本地文件' }}</div>
      </template>

      <!-- ── No image ── -->
      <template v-else>
        <div class="img-placeholder" @click.stop="triggerUpload">
          <div class="placeholder-icon">🖼</div>
          <div class="placeholder-hint">点击上传图片</div>
          <div class="placeholder-sub">支持 JPG / PNG / GIF / WebP</div>
        </div>
      </template>
    </div>

    <Handle id="sr" type="source" :position="Position.Right" :style="{ top: '50%' }" />
    <NodeAddButton :id="id" source-type="imageNode" />
  </div>
</template>

<style scoped>
.image-node { width: 240px; }
.hidden-file { display: none; }

.img-body {
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 0 10px 10px;
}

/* preview */
.img-preview-wrap { position: relative; line-height: 0; }
.node-image { width: 100%; height: 148px; object-fit: cover; display: block; }
.node-image.img-error { filter: grayscale(1) opacity(0.25); }

.img-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.18s;
}
.img-preview-wrap:hover .img-overlay { opacity: 1; }

.overlay-btn {
  background: rgba(255,255,255,0.14);
  border: 1px solid rgba(255,255,255,0.22);
  border-radius: 8px;
  color: #fff;
  font-size: 12px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.overlay-btn:hover { background: rgba(255,255,255,0.26); }

.file-badge {
  padding: 4px 10px;
  background: #42b88318;
  border-top: 1px solid #42b88330;
  font-size: 10px;
  color: #42b883;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* placeholder */
.img-placeholder {
  height: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #0d0d1a;
  border: 2px dashed #2e2e50;
  border-radius: 0 0 10px 10px;
  cursor: pointer;
  transition: border-color 0.15s;
}
.img-placeholder:hover { border-color: #42b88366; }
.placeholder-icon { font-size: 26px; opacity: 0.3; }
.placeholder-hint { font-size: 12px; color: #666; }
.placeholder-sub { font-size: 10px; color: #444; }
</style>
