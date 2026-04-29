<script setup>
import { ref, onBeforeUnmount, watch } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { useFlowStore } from '../../stores/flowStore'
import NodeHeader from './NodeHeader.vue'
import NodeAddButton from './NodeAddButton.vue'
import VideoFrameStrip from './VideoFrameStrip.vue'
import { captureFrameHD } from '../../composables/useVideoFrames'
import videojs from 'video.js'
import 'video.js/dist/video-js.css'

const props = defineProps({
  id: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const fileInputRef = ref(null)
const isLocalFile = ref((props.data.src || '').startsWith('blob:'))
const fileName = ref(props.data.fileName || '')

// ── video.js ────────────────────────────────────────────────────────────────
let vjsPlayer = null

function onVideoMounted(el) {
  if (!el) { destroyVjs(); return }
  vjsPlayer = videojs(el, {
    controls: true,
    autoplay: false,
    preload: 'metadata',
    fluid: false,
    width: 280,
    height: 160,
  })
  if (props.data.src) vjsPlayer.src({ src: props.data.src, type: 'video/mp4' })
}

function destroyVjs() {
  if (vjsPlayer) { vjsPlayer.dispose(); vjsPlayer = null }
}

watch(() => props.data.src, (src) => {
  if (vjsPlayer && src) vjsPlayer.src({ src, type: 'video/mp4' })
})

onBeforeUnmount(destroyVjs)

function seekTo(seconds) {
  if (vjsPlayer) vjsPlayer.currentTime(seconds)
}

// ── generate image node from frame ──────────────────────────────────────────
let generatedCount = 0
const generatingFrame = ref(false)

async function generateImageFromFrame(frame) {
  if (generatingFrame.value) return
  generatingFrame.value = true
  try {
    // Re-capture this frame at native video resolution (PNG, lossless)
    const hdDataUrl = await captureFrameHD(props.data.src, frame.time)

    const blob = dataUrlToBlob(hdDataUrl)
    const blobUrl = URL.createObjectURL(blob)

    const self = store.nodes.find((n) => n.id === props.id)
    const selfX = self?.position?.x ?? 0
    const selfY = self?.position?.y ?? 0

    const newId = `image-frame-${props.id}-${Date.now()}`
    store.nodes = [...store.nodes, {
      id: newId,
      type: 'imageNode',
      position: { x: selfX + generatedCount * 260, y: selfY + 420 },
      data: {
        label: `帧 ${fmtFrameTime(frame.time)}`,
        src: blobUrl,
        outputValue: blobUrl,
        fileName: `frame_${fmtFrameTime(frame.time)}.png`,
      },
    }]
    store.addEdge({
      id: `edge-${props.id}-${newId}`,
      source: props.id,
      sourceHandle: 'sr',
      target: newId,
      targetHandle: 'tl',
    })
    generatedCount++
  } finally {
    generatingFrame.value = false
  }
}

function dataUrlToBlob(dataUrl) {
  const [header, data] = dataUrl.split(',')
  const mime = header.match(/:(.*?);/)[1]
  const binary = atob(data)
  const arr = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) arr[i] = binary.charCodeAt(i)
  return new Blob([arr], { type: mime })
}

function fmtFrameTime(sec) {
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}-${String(s).padStart(2, '0')}`
}

// ── upload ──────────────────────────────────────────────────────────────────
function triggerUpload(e) {
  e.stopPropagation()
  fileInputRef.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (isLocalFile.value) URL.revokeObjectURL(props.data.src)
  const blobUrl = URL.createObjectURL(file)
  isLocalFile.value = true
  fileName.value = file.name
  store.updateNodeData(props.id, { src: blobUrl, outputValue: blobUrl, fileName: file.name })
  e.target.value = ''
}
</script>

<template>
  <div :class="['canvas-node', 'video-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <NodeHeader :id="id" :label="data.label" current-type="videoNode" />

    <input ref="fileInputRef" type="file" accept="video/*" class="hidden-file" @change="onFileChange" @click.stop />

    <div class="node-body video-body">
      <!-- ── Has video ── -->
      <template v-if="data.src">
        <div class="video-wrap">
          <div class="vjs-wrap" @click.stop @pointerdown.stop>
            <video :ref="onVideoMounted" class="video-js vjs-default-skin vjs-big-play-centered" playsinline @click.stop />
          </div>
          <div class="video-action-bar">
            <button class="bar-btn" @click.stop="triggerUpload">📁 换文件</button>
          </div>
        </div>
        <div class="file-badge">📁 {{ fileName || '本地文件' }}</div>
        <VideoFrameStrip :src="data.src" :active="true" :on-seek="seekTo" :on-generate-image="generateImageFromFrame" :generating="generatingFrame" />
      </template>

      <!-- ── No video ── -->
      <template v-else>
        <div class="video-placeholder" @click.stop="triggerUpload">
          <div class="placeholder-icon">▶</div>
          <div class="placeholder-hint">点击上传视频</div>
          <div class="placeholder-sub">支持 MP4 / WebM / OGG / MOV</div>
        </div>
      </template>
    </div>

    <Handle id="sr" type="source" :position="Position.Right" :style="{ top: '50%' }" />
    <NodeAddButton :id="id" source-type="videoNode" />
  </div>
</template>

<style scoped>
.video-node { width: 280px; }
.hidden-file { display: none; }

.video-body {
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 0 10px 10px;
}

/* video.js */
.vjs-wrap { width: 100%; height: 160px; overflow: hidden; background: #000; line-height: 0; }
.vjs-wrap :deep(.video-js) { width: 100% !important; height: 160px !important; background: #000 !important; }
.vjs-wrap :deep(.vjs-tech) { width: 100% !important; height: 160px !important; object-fit: contain; }
.vjs-wrap :deep(.vjs-control-bar) { background: rgba(0,0,0,0.75) !important; font-size: 10px !important; height: 24px !important; }
.vjs-wrap :deep(.vjs-big-play-button) {
  border-radius: 50% !important;
  width: 36px !important; height: 36px !important; line-height: 36px !important;
  margin-top: -18px !important; margin-left: -18px !important;
  border: 2px solid #ffffff66 !important;
  background: rgba(0,0,0,0.6) !important;
}

.video-action-bar { display: flex; border-top: 1px solid #2e2e50; }
.bar-btn {
  flex: 1; padding: 5px 6px;
  background: #0f0f1a; border: none;
  color: #888; font-size: 10px; cursor: pointer; font-family: inherit;
  transition: background 0.15s, color 0.15s;
}
.bar-btn:hover { background: #1a1a2e; color: #42b883; }

.file-badge {
  padding: 4px 10px;
  background: #ff6b6b12; border-top: 1px solid #ff6b6b22;
  font-size: 10px; color: #ff9090;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* placeholder */
.video-placeholder {
  height: 150px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  background: #0d0d1a; border: 2px dashed #2e2e50; border-radius: 0 0 10px 10px;
  cursor: pointer; transition: border-color 0.15s;
}
.video-placeholder:hover { border-color: #ff6b6b66; }
.placeholder-icon { font-size: 28px; opacity: 0.3; }
.placeholder-hint { font-size: 12px; color: #666; }
.placeholder-sub { font-size: 10px; color: #444; }
</style>
