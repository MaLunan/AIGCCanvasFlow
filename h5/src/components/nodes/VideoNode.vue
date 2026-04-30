<script setup>
import { ref, onBeforeUnmount, watch } from 'vue'
import { Handle, Position, useVueFlow } from '@vue-flow/core'
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
const { findNode, getNodes } = useVueFlow()
const fileInputRef = ref(null)

const GAP = 24
function overlaps(ax, ay, aw, ah, bx, by, bw, bh) {
  return ax < bx + bw + GAP && ax + aw + GAP > bx && ay < by + bh + GAP && ay + ah + GAP > by
}
function findFreePosition(startX, startY, newW, newH) {
  const all = getNodes.value
  let y = startY
  for (let i = 0; i < 30; i++) {
    const blocked = all.some(n => {
      const nw = n.dimensions?.width ?? 220
      const nh = n.dimensions?.height ?? 120
      return overlaps(startX, y, newW, newH, n.position.x, n.position.y, nw, nh)
    })
    if (!blocked) return { x: startX, y }
    const colNodes = all.filter(n => {
      const nw = n.dimensions?.width ?? 220
      return n.position.x < startX + newW + GAP && n.position.x + nw + GAP > startX
    })
    if (colNodes.length === 0) break
    const maxBottom = Math.max(...colNodes.map(n => n.position.y + (n.dimensions?.height ?? 120)))
    y = maxBottom + GAP
  }
  return { x: startX, y }
}

const isLocalFile = ref((props.data.src || '').startsWith('blob:'))
const fileName = ref(props.data.fileName || '')

// ── mode ────────────────────────────────────────────────────────────────────
const genMode = ref(false)

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
const generatingFrame = ref(false)

async function generateImageFromFrame(frame) {
  if (generatingFrame.value) return
  generatingFrame.value = true
  try {
    const hdDataUrl = await captureFrameHD(props.data.src, frame.time)
    const blob = dataUrlToBlob(hdDataUrl)
    const blobUrl = URL.createObjectURL(blob)

    const src = findNode(props.id)
    const x = src?.position?.x ?? 0
    const y = src?.position?.y ?? 0
    const w = src?.dimensions?.width ?? 280

    const NEW_W = 240
    const NEW_H = 120
    const pos = findFreePosition(x + w + 80, y, NEW_W, NEW_H)

    const newId = `image-frame-${props.id}-${Date.now()}`
    const newNode = {
      id: newId,
      type: 'imageNode',
      position: pos,
      data: {
        label: `帧 ${fmtFrameTime(frame.time)}`,
        src: blobUrl,
        outputValue: blobUrl,
        fileName: `frame_${fmtFrameTime(frame.time)}.png`,
      },
    }

    const vfSnapshot = getNodes.value
    store.nodes = [
      ...store.nodes.map((n) => {
        const live = vfSnapshot.find((v) => v.id === n.id)
        return live ? { ...n, position: { x: live.position.x, y: live.position.y } } : n
      }),
      newNode,
    ]

    store.addEdge({
      id: `edge-${props.id}-${newId}-${Date.now()}`,
      source: props.id,
      sourceHandle: 'sr',
      target: newId,
      targetHandle: 'tl',
    })
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

// ── upload / clear ──────────────────────────────────────────────────────────
function triggerUpload(e) {
  e.stopPropagation()
  fileInputRef.value?.click()
}

function clearVideo(e) {
  e.stopPropagation()
  if (isLocalFile.value) URL.revokeObjectURL(props.data.src)
  isLocalFile.value = false
  fileName.value = ''
  store.updateNodeData(props.id, { src: '', outputValue: '', fileName: '' })
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

// ── generate video ──────────────────────────────────────────────────────────
const genPrompt     = ref('')
const genAspect     = ref('16:9')
const genModel      = ref('kling')
const genAudio      = ref('sound')
const genResolution = ref('1080p')
const genDuration   = ref('5s')
const generating    = ref(false)

const ASPECT_OPTIONS = ['16:9', '9:16', '1:1', '4:3', '3:4']
const MODEL_OPTIONS  = [
  { value: 'kling',    label: 'Kling' },
  { value: 'sora',     label: 'Sora' },
  { value: 'runway',   label: 'Runway' },
  { value: 'pika',     label: 'Pika' },
  { value: 'hailuo',   label: 'Hailuo' },
]
const RESOLUTION_OPTIONS = ['480p', '720p', '1080p', '4K']
const DURATION_OPTIONS   = ['3s', '5s', '10s', '15s', '30s']

async function generateVideo() {
  if (!genPrompt.value.trim() || generating.value) return
  generating.value = true
  try {
    // TODO: 接入实际大模型 API
    await new Promise(r => setTimeout(r, 1500))
    store.updateNodeData(props.id, {
      genPrompt: genPrompt.value,
      genModel: genModel.value,
      genAspect: genAspect.value,
      genAudio: genAudio.value,
      genResolution: genResolution.value,
      genDuration: genDuration.value,
    })
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <div :class="['canvas-node', 'video-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <NodeHeader :id="id" :label="data.label" current-type="videoNode" />

    <input ref="fileInputRef" type="file" accept="video/*" class="hidden-file" @change="onFileChange" @click.stop />

    <!-- ── Mode tabs ── -->
    <div class="mode-tabs" @mousedown.stop>
      <button :class="['mode-tab', !genMode && 'active']" @click.stop="genMode = false">上传</button>
      <button :class="['mode-tab', genMode && 'active']"  @click.stop="genMode = true">AI 生成</button>
    </div>

    <div class="node-body video-body">
      <!-- ══ Upload mode ══ -->
      <template v-if="!genMode">
        <template v-if="data.src">
          <div class="video-wrap">
            <div class="vjs-wrap" @click.stop @pointerdown.stop>
              <video :ref="onVideoMounted" class="video-js vjs-default-skin vjs-big-play-centered" playsinline @click.stop />
            </div>
            <div class="video-action-bar">
              <button class="bar-btn" @click.stop="triggerUpload">📁 换文件</button>
              <button class="bar-btn bar-btn-del" @click.stop="clearVideo">✕ 清除</button>
            </div>
          </div>
          <div class="file-badge">📁 {{ fileName || '本地文件' }}</div>
          <VideoFrameStrip :src="data.src" :active="true" :on-seek="seekTo" :on-generate-image="generateImageFromFrame" :generating="generatingFrame" />
        </template>
        <template v-else>
          <div class="video-placeholder" @click.stop="triggerUpload">
            <div class="placeholder-icon">▶</div>
            <div class="placeholder-hint">点击上传视频</div>
            <div class="placeholder-sub">支持 MP4 / WebM / OGG / MOV</div>
          </div>
        </template>
      </template>

      <!-- ══ Generate mode ══ -->
      <template v-else>
        <div class="gen-panel" @mousedown.stop @click.stop>
          <textarea
            v-model="genPrompt"
            class="gen-prompt"
            placeholder="描述你想生成的视频内容..."
            rows="3"
            @keydown.stop
          />

          <!-- Row 1: 比例 + 模型 -->
          <div class="gen-row">
            <div class="gen-field">
              <span class="gen-label">比例</span>
              <select v-model="genAspect" class="gen-select">
                <option v-for="a in ASPECT_OPTIONS" :key="a" :value="a">{{ a }}</option>
              </select>
            </div>
            <div class="gen-field">
              <span class="gen-label">模型</span>
              <select v-model="genModel" class="gen-select">
                <option v-for="m in MODEL_OPTIONS" :key="m.value" :value="m.value">{{ m.label }}</option>
              </select>
            </div>
          </div>

          <!-- Row 2: 时长 + 清晰度 -->
          <div class="gen-row">
            <div class="gen-field">
              <span class="gen-label">时长</span>
              <select v-model="genDuration" class="gen-select">
                <option v-for="d in DURATION_OPTIONS" :key="d" :value="d">{{ d }}</option>
              </select>
            </div>
            <div class="gen-field">
              <span class="gen-label">清晰度</span>
              <select v-model="genResolution" class="gen-select">
                <option v-for="r in RESOLUTION_OPTIONS" :key="r" :value="r">{{ r }}</option>
              </select>
            </div>
          </div>

          <!-- Row 3: 音频 -->
          <div class="gen-row">
            <div class="gen-field gen-field-full">
              <span class="gen-label">音频</span>
              <div class="audio-toggle">
                <button
                  :class="['audio-btn', genAudio === 'sound' && 'active']"
                  @click.stop="genAudio = 'sound'"
                >🔊 有声</button>
                <button
                  :class="['audio-btn', genAudio === 'mute' && 'active']"
                  @click.stop="genAudio = 'mute'"
                >🔇 无声</button>
              </div>
            </div>
          </div>

          <button
            class="gen-btn"
            :disabled="!genPrompt.trim() || generating"
            @click.stop="generateVideo"
          >
            <span v-if="generating" class="gen-spinner" />
            {{ generating ? '生成中...' : '生成视频' }}
          </button>
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

/* ── Mode tabs ── */
.mode-tabs {
  display: flex;
  border-bottom: 1px solid #2e2e50;
}
.mode-tab {
  flex: 1;
  padding: 5px 0;
  background: none;
  border: none;
  font-size: 11px;
  font-weight: 600;
  color: #555577;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
  font-family: inherit;
}
.mode-tab.active {
  color: #ffaa88;
  background: #ff6b6b12;
  border-bottom: 2px solid #ff6b6b;
}
.mode-tab:hover:not(.active) { color: #8888aa; }

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
.bar-btn-del:hover { color: #ff4d4d !important; }

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

/* ── Generate panel ── */
.gen-panel {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #0d0d1a;
}
.gen-prompt {
  width: 100%;
  box-sizing: border-box;
  background: #12121e;
  border: 1px solid #2e2e50;
  border-radius: 6px;
  color: #d0d0f0;
  font-size: 11px;
  padding: 6px 8px;
  resize: none;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
  transition: border-color 0.15s;
}
.gen-prompt:focus { border-color: #ff6b6b88; }
.gen-prompt::placeholder { color: #444466; }
.gen-row {
  display: flex;
  gap: 6px;
}
.gen-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.gen-field-full { flex: unset; width: 100%; }
.gen-label {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  color: #444466;
}
.gen-select {
  background: #12121e;
  border: 1px solid #2e2e50;
  border-radius: 5px;
  color: #a0a0c0;
  font-size: 11px;
  padding: 4px 6px;
  outline: none;
  cursor: pointer;
  font-family: inherit;
  width: 100%;
  transition: border-color 0.15s;
}
.gen-select:focus { border-color: #ff6b6b88; }

/* audio toggle */
.audio-toggle {
  display: flex;
  gap: 4px;
}
.audio-btn {
  flex: 1;
  padding: 4px 6px;
  background: #12121e;
  border: 1px solid #2e2e50;
  border-radius: 5px;
  color: #666688;
  font-size: 10px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.audio-btn.active {
  background: #ff6b6b22;
  border-color: #ff6b6b66;
  color: #ff9090;
}
.audio-btn:hover:not(.active) { color: #a0a0c0; }

.gen-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 7px;
  background: #ff6b6b22;
  border: 1px solid #ff6b6b66;
  border-radius: 6px;
  color: #ff9090;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;
}
.gen-btn:hover:not(:disabled) { background: #ff6b6b44; color: #ffbbaa; }
.gen-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.gen-spinner {
  width: 10px; height: 10px;
  border: 2px solid #ff6b6b44;
  border-top-color: #ff9090;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
