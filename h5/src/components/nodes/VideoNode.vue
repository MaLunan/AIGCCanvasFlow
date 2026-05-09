<script setup>
import { ref, computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { Handle, Position, useVueFlow } from '@vue-flow/core'
import { useRouter } from 'vue-router'
import { useFlowStore } from '../../stores/flowStore'
import { useModelStore } from '../../stores/modelStore'
import { submitVideoGen, pollTask } from '../../api/aiApi'
import { projectApi } from '../../api/projectApi'
import NodeHeader from './NodeHeader.vue'
import NodeAddButton from './NodeAddButton.vue'
import VideoFrameStrip from './VideoFrameStrip.vue'
import { captureFrameHD } from '../../composables/useVideoFrames'
import videojs from 'video.js'
import 'video.js/dist/video-js.css'

const props = defineProps({
  id: String,
  type: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const modelStore = useModelStore()
const router = useRouter()
const { findNode, getNodes } = useVueFlow()
const fileInputRef = ref(null)

// ── mode: determined by node type ────────────────────────────────────────────
const isGenMode = computed(() => props.type === 'videoGenNode')

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

const fileName = ref(props.data.fileName || '')
const uploading = ref(false)

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
        fromFrame: true,
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
  fileName.value = ''
  store.updateNodeData(props.id, { src: '', outputValue: '', fileName: '' })
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  e.target.value = ''
  uploading.value = true
  try {
    const asset = await projectApi.uploadAsset(file, 'video')
    fileName.value = file.name
    store.updateNodeData(props.id, { src: asset.url, outputValue: '', fileName: file.name })
  } catch {
    // 上传失败时降级用本地预览（刷新后会丢失）
    const blobUrl = URL.createObjectURL(file)
    fileName.value = file.name
    store.updateNodeData(props.id, { src: blobUrl, outputValue: '', fileName: file.name })
  } finally {
    uploading.value = false
  }
}

// ── 模型库（视频类） ──────────────────────────────────────────────────────────
const videoModels = computed(() =>
  modelStore.libraryModels.filter(m => m.category === '视频' && m.enabled)
)

onMounted(() => {
  if (!modelStore.libraryModels.length && !modelStore.libraryLoading) {
    modelStore.loadLibrary()
  }
})

// ── generate video ──────────────────────────────────────────────────────────
const genPrompt     = ref('')
const genAspect     = ref('16:9')
const genModel      = ref('')
const genAudio      = ref('sound')
const genResolution = ref('1080p')
const genDuration   = ref('5s')
const generating    = ref(false)
const genProgress   = ref(0)
const genError      = ref('')

const ASPECT_OPTIONS     = ['16:9', '9:16', '1:1', '4:3', '3:4']
const RESOLUTION_OPTIONS = ['480p', '720p', '1080p', '4K']
const DURATION_OPTIONS   = ['3s', '5s', '10s', '15s', '30s']

watch(videoModels, (models) => {
  if (models.length && !models.find(m => String(m.id) === genModel.value)) {
    genModel.value = String(models[0].id)
  }
}, { immediate: true })

watch(genModel, (val) => {
  if (val === '__goto_market__') {
    genModel.value = videoModels.value[0] ? String(videoModels.value[0].id) : ''
    router.push('/models')
  }
})

async function generateVideo() {
  if (!genPrompt.value.trim() || generating.value || !genModel.value || !videoModels.value.length) return
  generating.value = true
  genProgress.value = 0
  genError.value = ''
  try {
    const context = store.getUpstreamContext(props.id)
    const { taskId } = await submitVideoGen({
      prompt: genPrompt.value,
      libraryModelId: Number(genModel.value),
      aspect: genAspect.value,
      duration: parseInt(genDuration.value) || 5,
      resolution: genResolution.value,
      audio: genAudio.value,
      context,
    })
    const result = await pollTask(taskId, (p) => { genProgress.value = p })
    store.updateNodeData(props.id, {
      src: result.resultUrl,
      outputValue: result.resultUrl,
      genPrompt: genPrompt.value,
      genModel: genModel.value,
      genAspect: genAspect.value,
      genAudio: genAudio.value,
      genResolution: genResolution.value,
      genDuration: genDuration.value,
    }, getNodes.value)
  } catch (e) {
    genError.value = e?.message || '生视频失败，请重试'
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <div :class="['canvas-node', 'video-node', { selected }]">
    <!-- AI生成节点：有输入/输出两个连接点；上传节点：只有输出连接点 -->
    <Handle v-if="isGenMode" id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <NodeHeader :id="id" :label="data.label" :current-type="type" />

    <input ref="fileInputRef" type="file" accept="video/*" class="hidden-file" @change="onFileChange" @click.stop />

    <div class="node-body video-body">
      <!-- ══ Upload mode ══ -->
      <template v-if="!isGenMode">
        <template v-if="uploading">
          <div class="video-placeholder">
            <div class="placeholder-icon">⏳</div>
            <div class="placeholder-hint">上传中...</div>
          </div>
        </template>
        <template v-else-if="data.src">
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
                <option v-if="!videoModels.length" disabled value="">— 暂无视频模型 —</option>
                <option v-for="m in videoModels" :key="m.id" :value="String(m.id)">{{ m.icon }} {{ m.name }}</option>
                <option value="__goto_market__">＋ 去广场添加</option>
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
            :disabled="!genPrompt.trim() || generating || !videoModels.length"
            @click.stop="generateVideo"
          >
            <span v-if="generating" class="gen-spinner" />
            {{ generating ? `生成中 ${genProgress}%` : '生成视频' }}
          </button>
          <div v-if="generating" class="gen-progress-bar">
            <div class="gen-progress-fill" :style="{ width: genProgress + '%' }" />
          </div>
          <div v-if="genError" class="gen-error">{{ genError }}</div>
        </div>
      </template>
    </div>

    <Handle id="sr" type="source" :position="Position.Right" :style="{ top: '50%' }" />
    <NodeAddButton :id="id" :source-type="type" />
  </div>
</template>

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.video-node { width: 280px; }
.hidden-file { display: none; }

.video-body {
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 0 10px 10px;
}

.vjs-wrap {
  width: 100%;
  height: 160px;
  overflow: hidden;
  background: #000;
  line-height: 0;

  :deep(.video-js) { width: 100% !important; height: 160px !important; background: #000 !important; }
  :deep(.vjs-tech) { width: 100% !important; height: 160px !important; object-fit: contain; }
  :deep(.vjs-control-bar) { background: rgba(0, 0, 0, 0.75) !important; font-size: 10px !important; height: 24px !important; }
  :deep(.vjs-big-play-button) {
    border-radius: 50% !important;
    width: 36px !important;
    height: 36px !important;
    line-height: 36px !important;
    margin-top: -18px !important;
    margin-left: -18px !important;
    border: 2px solid rgba(255, 255, 255, 0.4) !important;
    background: rgba(0, 0, 0, 0.6) !important;
  }
}

.video-action-bar {
  display: flex;
  border-top: 1px solid $border-default;
}

.bar-btn {
  flex: 1;
  padding: 5px 6px;
  background: #0f0f1a;
  border: none;
  color: #888;
  font-size: 10px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;

  &:hover { background: #1a1a2e; color: $accent-green; }
  &-del:hover { color: $accent-red !important; }
}

.file-badge {
  padding: 4px 10px;
  background: rgba($accent-red, 0.07);
  border-top: 1px solid rgba($accent-red, 0.13);
  font-size: 10px;
  color: #ff9090;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-placeholder {
  height: 150px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #0d0d1a;
  border: 2px dashed $border-default;
  border-radius: 0 0 10px 10px;
  cursor: pointer;
  transition: border-color 0.15s;

  &:hover { border-color: rgba($accent-red, 0.4); }
}

.placeholder-icon { font-size: 28px; opacity: 0.3; }
.placeholder-hint { font-size: 12px; color: #666; }
.placeholder-sub { font-size: 10px; color: #444; }

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
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  color: $text-primary;
  font-size: 11px;
  padding: 6px 8px;
  resize: none;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
  transition: border-color 0.15s;

  &:focus { border-color: rgba($accent-red, 0.53); }
  &::placeholder { color: $text-dim; }
}

.gen-row { display: flex; gap: 6px; }

.gen-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;

  &-full { flex: unset; width: 100%; }
}

.gen-label {
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  color: $text-dim;
}

.gen-select {
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: 5px;
  color: $text-secondary;
  font-size: 11px;
  padding: 4px 6px;
  outline: none;
  cursor: pointer;
  font-family: inherit;
  width: 100%;
  transition: border-color 0.15s;

  &:focus { border-color: rgba($accent-red, 0.53); }
}

.audio-toggle { display: flex; gap: 4px; }

.audio-btn {
  flex: 1;
  padding: 4px 6px;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: 5px;
  color: #666688;
  font-size: 10px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s, border-color 0.15s;

  &.active {
    background: rgba($accent-red, 0.13);
    border-color: rgba($accent-red, 0.4);
    color: #ff9090;
  }

  &:hover:not(.active) { color: $text-secondary; }
}

.gen-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 7px;
  background: rgba($accent-red, 0.13);
  border: 1px solid rgba($accent-red, 0.4);
  border-radius: $radius-sm;
  color: #ff9090;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;

  &:hover:not(:disabled) { background: rgba($accent-red, 0.27); color: #ffbbaa; }
  &:disabled { opacity: 0.45; cursor: not-allowed; }
}

.gen-spinner {
  width: 10px;
  height: 10px;
  border: 2px solid rgba($accent-red, 0.27);
  border-top-color: #ff9090;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin { to { transform: rotate(360deg); } }

.gen-progress-bar {
  height: 3px;
  background: rgba($accent-red, 0.15);
  border-radius: 2px;
  overflow: hidden;
}

.gen-progress-fill {
  height: 100%;
  background: $accent-red;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.gen-error {
  font-size: 10px;
  color: $accent-red;
  padding: 2px 0;
}
</style>
