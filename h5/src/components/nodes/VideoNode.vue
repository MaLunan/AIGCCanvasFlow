<script setup>
import { ref, computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { useFlowStore } from '../../stores/flowStore'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const editingUrl = ref(!props.data.src)
const localSrc = ref(props.data.src || '')

// Detect if it's a YouTube/Bilibili URL and convert to embed
const embedSrc = computed(() => {
  const src = props.data.src || ''
  // YouTube
  const ytMatch = src.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&\s]+)/)
  if (ytMatch) return `https://www.youtube.com/embed/${ytMatch[1]}`
  // Bilibili
  const bvMatch = src.match(/bilibili\.com\/video\/(BV[^/?]+)/)
  if (bvMatch) return `https://player.bilibili.com/player.html?bvid=${bvMatch[1]}&page=1`
  // Direct video or iframe embed
  return src
})

const isDirectVideo = computed(() => {
  const s = props.data.src || ''
  return /\.(mp4|webm|ogg)(\?.*)?$/.test(s)
})

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
  <div :class="['canvas-node', 'video-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <div class="node-header">
      <span class="node-icon">▶</span>
      <span class="node-label">{{ data.label }}</span>
      <button class="node-edit-btn" @click.stop="editingUrl = true; localSrc = data.src" title="编辑">✎</button>
      <button class="node-del" @click.stop="store.removeNodeById(id)" title="删除">×</button>
    </div>

    <div class="node-body video-body">
      <template v-if="data.src && !editingUrl">
        <video v-if="isDirectVideo" :src="data.src" class="video-player" controls @click.stop />
        <iframe
          v-else
          :src="embedSrc"
          class="video-iframe"
          frameborder="0"
          allowfullscreen
          @click.stop
        />
      </template>
      <template v-else>
        <div v-if="!editingUrl" class="video-placeholder" @click.stop="editingUrl = true">
          <div class="placeholder-icon">▶</div>
          <div class="placeholder-hint">点击输入视频 URL<br><small>支持 MP4 / YouTube / Bilibili</small></div>
        </div>
        <div v-else class="url-input-wrap">
          <input
            v-model="localSrc"
            class="url-input"
            placeholder="视频 URL 或 YouTube/Bilibili 链接"
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
.video-node {
  width: 280px;
}
.node-icon {
  font-size: 12px;
  color: #ff6b6b;
}
.node-edit-btn {
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  padding: 0 4px;
  font-size: 13px;
  margin-left: auto;
  line-height: 1;
}
.node-edit-btn:hover { color: #ccc; }
.video-body {
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 0 10px 10px;
}
.video-player {
  width: 100%;
  height: 160px;
  display: block;
  background: #000;
}
.video-iframe {
  width: 100%;
  height: 160px;
  display: block;
  border: none;
  background: #000;
}
.video-placeholder {
  height: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  background: #0d0d1a;
  border: 2px dashed #383854;
  border-radius: 0 0 10px 10px;
  transition: border-color 0.2s;
}
.video-placeholder:hover { border-color: #ff6b6b; }
.placeholder-icon {
  font-size: 32px;
  opacity: 0.4;
}
.placeholder-hint {
  font-size: 11px;
  color: #666;
  text-align: center;
  line-height: 1.5;
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
