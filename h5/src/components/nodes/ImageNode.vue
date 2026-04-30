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

// ── mode ────────────────────────────────────────────────────────────────────
const genMode = ref(false)

// ── upload ──────────────────────────────────────────────────────────────────
function triggerUpload(e) {
  e.stopPropagation()
  fileInputRef.value?.click()
}

function clearImage(e) {
  e.stopPropagation()
  if (isLocalFile.value) URL.revokeObjectURL(props.data.src)
  store.updateNodeData(props.id, { src: '', outputValue: '', fileName: '' })
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (isLocalFile.value) URL.revokeObjectURL(props.data.src)
  const blobUrl = URL.createObjectURL(file)
  store.updateNodeData(props.id, { src: blobUrl, outputValue: blobUrl, fileName: file.name })
  e.target.value = ''
}

// ── generate ─────────────────────────────────────────────────────────────────
const genPrompt  = ref('')
const genAspect  = ref('1:1')
const genModel   = ref('flux')
const generating = ref(false)

const ASPECT_OPTIONS = ['1:1', '4:3', '3:4', '16:9', '9:16']
const MODEL_OPTIONS  = [
  { value: 'flux',        label: 'Flux' },
  { value: 'dalle3',      label: 'DALL·E 3' },
  { value: 'sd3',         label: 'SD 3.5' },
  { value: 'midjourney',  label: 'Midjourney' },
  { value: 'ideogram',    label: 'Ideogram' },
]

async function generateImage() {
  if (!genPrompt.value.trim() || generating.value) return
  generating.value = true
  try {
    // TODO: 接入实际大模型 API
    await new Promise(r => setTimeout(r, 1200))
    // placeholder: store prompt as content
    store.updateNodeData(props.id, { genPrompt: genPrompt.value, genModel: genModel.value, genAspect: genAspect.value })
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <div :class="['canvas-node', 'image-node', { selected }]">
    <Handle id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <NodeHeader :id="id" :label="data.label" current-type="imageNode" />

    <input ref="fileInputRef" type="file" accept="image/*" class="hidden-file" @change="onFileChange" @click.stop />

    <!-- ── Mode tabs ── -->
    <div class="mode-tabs" @mousedown.stop>
      <button :class="['mode-tab', !genMode && 'active']" @click.stop="genMode = false">上传</button>
      <button :class="['mode-tab', genMode && 'active']"  @click.stop="genMode = true">AI 生成</button>
    </div>

    <div class="node-body img-body">
      <!-- ══ Upload mode ══ -->
      <template v-if="!genMode">
        <template v-if="data.src">
          <div class="img-preview-wrap">
            <img :src="data.src" :alt="data.alt || '图片'" class="node-image" @error="e => e.target.classList.add('img-error')" />
            <div class="img-overlay">
              <button class="overlay-btn" @click.stop="triggerUpload" title="重新上传">📁 换图片</button>
              <button class="overlay-btn overlay-btn-del" @click.stop="clearImage" title="清除图片">✕ 清除</button>
            </div>
          </div>
          <div v-if="isLocalFile" class="file-badge">📁 {{ fileName || '本地文件' }}</div>
        </template>
        <template v-else>
          <div class="img-placeholder" @click.stop="triggerUpload">
            <div class="placeholder-icon">🖼</div>
            <div class="placeholder-hint">点击上传图片</div>
            <div class="placeholder-sub">支持 JPG / PNG / GIF / WebP</div>
          </div>
        </template>
      </template>

      <!-- ══ Generate mode ══ -->
      <template v-else>
        <div v-if="data.src" class="img-preview-wrap">
          <img :src="data.src" :alt="data.alt || '图片'" class="node-image" @error="e => e.target.classList.add('img-error')" />
          <div class="img-overlay">
            <button class="overlay-btn overlay-btn-del" @click.stop="clearImage" title="清除图片">✕ 清除</button>
          </div>
        </div>

        <div class="gen-panel" @mousedown.stop @click.stop>
          <textarea
            v-model="genPrompt"
            class="gen-prompt"
            placeholder="描述你想生成的图片内容..."
            rows="3"
            @keydown.stop
          />
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
          <button
            class="gen-btn"
            :disabled="!genPrompt.trim() || generating"
            @click.stop="generateImage"
          >
            <span v-if="generating" class="gen-spinner" />
            {{ generating ? '生成中...' : '生成图片' }}
          </button>
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
  color: #a0aaff;
  background: #646cff12;
  border-bottom: 2px solid #646cff;
}
.mode-tab:hover:not(.active) { color: #8888aa; }

.img-body {
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 0 10px 10px;
}

/* ── Preview ── */
.img-preview-wrap { position: relative; line-height: 0; }
.node-image { width: 100%; height: auto; display: block; }
.node-image.img-error { filter: grayscale(1) opacity(0.25); }

.img-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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
.overlay-btn-del:hover { background: rgba(255,60,60,0.5) !important; }

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

/* ── Placeholder ── */
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
.gen-prompt:focus { border-color: #646cff88; }
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
.gen-select:focus { border-color: #646cff88; }
.gen-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 7px;
  background: #646cff22;
  border: 1px solid #646cff66;
  border-radius: 6px;
  color: #a0aaff;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;
}
.gen-btn:hover:not(:disabled) { background: #646cff44; color: #e0e4ff; }
.gen-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.gen-spinner {
  width: 10px; height: 10px;
  border: 2px solid #646cff44;
  border-top-color: #a0aaff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
