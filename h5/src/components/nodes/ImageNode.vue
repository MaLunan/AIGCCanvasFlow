<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Handle, Position, useVueFlow } from '@vue-flow/core'
import { useRouter } from 'vue-router'
import { useFlowStore } from '../../stores/flowStore'
import { useModelStore } from '../../stores/modelStore'
import { submitImageGen, pollTask } from '../../api/aiApi'
import { projectApi } from '../../api/projectApi'
import NodeHeader from './NodeHeader.vue'
import NodeAddButton from './NodeAddButton.vue'

const props = defineProps({
  id: String,
  type: String,
  data: Object,
  selected: Boolean,
})

const store = useFlowStore()
const modelStore = useModelStore()
const router = useRouter()
const { getNodes } = useVueFlow()
const fileInputRef = ref(null)

const fileName = computed(() => props.data.fileName || '')
const uploading = ref(false)

// ── mode: determined by node type, not a tab ─────────────────────────────────
const isGenMode = computed(() => props.type === 'imageGenNode')

// ── upload ──────────────────────────────────────────────────────────────────
function triggerUpload(e) {
  e.stopPropagation()
  fileInputRef.value?.click()
}

function clearImage(e) {
  e.stopPropagation()
  store.updateNodeData(props.id, { src: '', outputValue: '', fileName: '' })
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  e.target.value = ''
  uploading.value = true
  try {
    const asset = await projectApi.uploadAsset(file, 'image')
    store.updateNodeData(props.id, { src: asset.url, outputValue: '', fileName: file.name })
  } catch {
    // 上传失败时降级用本地预览（刷新后会丢失）
    const blobUrl = URL.createObjectURL(file)
    store.updateNodeData(props.id, { src: blobUrl, outputValue: '', fileName: file.name })
  } finally {
    uploading.value = false
  }
}

// ── 模型库（图像类） ──────────────────────────────────────────────────────────
const imageModels = computed(() =>
  modelStore.libraryModels.filter(m => m.category === '图像' && m.enabled)
)

onMounted(() => {
  if (!modelStore.libraryModels.length && !modelStore.libraryLoading) {
    modelStore.loadLibrary()
  }
})

// ── generate ─────────────────────────────────────────────────────────────────
const genPrompt   = ref('')
const genAspect   = ref('1:1')
const genModel    = ref('')
const generating  = ref(false)
const genProgress = ref(0)
const genError    = ref('')

const ASPECT_OPTIONS = ['1:1', '4:3', '3:4', '16:9', '9:16']

watch(imageModels, (models) => {
  if (models.length && !models.find(m => String(m.id) === genModel.value)) {
    genModel.value = String(models[0].id)
  }
}, { immediate: true })

watch(genModel, (val) => {
  if (val === '__goto_market__') {
    genModel.value = imageModels.value[0] ? String(imageModels.value[0].id) : ''
    router.push('/models')
  }
})

async function generateImage() {
  if (!genPrompt.value.trim() || generating.value || !genModel.value || !imageModels.value.length) return
  generating.value = true
  genProgress.value = 0
  genError.value = ''
  try {
    const context = store.getUpstreamContext(props.id)
    const { taskId } = await submitImageGen({
      prompt: genPrompt.value,
      libraryModelId: Number(genModel.value),
      aspect: genAspect.value,
      context,
    })
    const result = await pollTask(taskId, (p) => { genProgress.value = p })
    store.updateNodeData(props.id, {
      src: result.resultUrl,
      outputValue: result.resultUrl,
      genPrompt: genPrompt.value,
      genModel: genModel.value,
      genAspect: genAspect.value,
    }, getNodes.value)
  } catch (e) {
    genError.value = e?.message || '生图失败，请重试'
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <div :class="['canvas-node', 'image-node', { selected }]">
    <!-- AI生成节点 或 视频帧截取生成的节点：有输入/输出两个连接点；上传节点：只有输出连接点 -->
    <Handle v-if="isGenMode || data.fromFrame" id="tl" type="target" :position="Position.Left" :style="{ top: '50%' }" />

    <NodeHeader :id="id" :label="data.label" :current-type="type" />

    <input ref="fileInputRef" type="file" accept="image/*" class="hidden-file" @change="onFileChange" @click.stop />

    <div class="node-body img-body">
      <!-- ══ Upload mode ══ -->
      <template v-if="!isGenMode">
        <template v-if="uploading">
          <div class="img-placeholder">
            <div class="placeholder-icon">⏳</div>
            <div class="placeholder-hint">上传中...</div>
          </div>
        </template>
        <template v-else-if="data.src">
          <div class="img-preview-wrap">
            <img :src="data.src" :alt="data.alt || '图片'" class="node-image" @error="e => e.target.classList.add('img-error')" />
            <div class="img-overlay">
              <button class="overlay-btn" @click.stop="triggerUpload" title="重新上传">📁 换图片</button>
              <button class="overlay-btn overlay-btn-del" @click.stop="clearImage" title="清除图片">✕ 清除</button>
            </div>
          </div>
          <div v-if="fileName" class="file-badge">📁 {{ fileName }}</div>
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
                <option v-if="!imageModels.length" disabled value="">— 暂无图像模型 —</option>
                <option v-for="m in imageModels" :key="m.id" :value="String(m.id)">{{ m.icon }} {{ m.name }}</option>
                <option value="__goto_market__">＋ 去广场添加</option>
              </select>
            </div>
          </div>
          <button
            class="gen-btn"
            :disabled="!genPrompt.trim() || generating || !imageModels.length"
            @click.stop="generateImage"
          >
            <span v-if="generating" class="gen-spinner" />
            {{ generating ? `生成中 ${genProgress}%` : '生成图片' }}
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

.image-node { width: 240px; }
.hidden-file { display: none; }

.img-body {
  padding: 0 !important;
  overflow: hidden;
  border-radius: 0 0 10px 10px;
}

.img-preview-wrap {
  position: relative;
  line-height: 0;

  &:hover .img-overlay { opacity: 1; }
}

.node-image {
  width: 100%;
  height: auto;
  display: block;

  &.img-error { filter: grayscale(1) opacity(0.25); }
}

.img-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.18s;
}

.overlay-btn {
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: $radius-md;
  color: #fff;
  font-size: 12px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;

  &:hover { background: rgba(255, 255, 255, 0.26); }
  &-del:hover { background: rgba(255, 60, 60, 0.5) !important; }
}

.file-badge {
  padding: 4px 10px;
  background: rgba($accent-green, 0.09);
  border-top: 1px solid rgba($accent-green, 0.19);
  font-size: 10px;
  color: $accent-green;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.img-placeholder {
  height: 130px;
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

  &:hover { border-color: rgba($accent-green, 0.4); }
}

.placeholder-icon { font-size: 26px; opacity: 0.3; }
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
  color: #d0d0f0;
  font-size: 11px;
  padding: 6px 8px;
  resize: none;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
  transition: border-color 0.15s;

  &:focus { border-color: rgba($accent-primary, 0.53); }
  &::placeholder { color: $text-dim; }
}

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

  &:focus { border-color: rgba($accent-primary, 0.53); }
}

.gen-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 7px;
  background: rgba($accent-primary, 0.13);
  border: 1px solid rgba($accent-primary, 0.4);
  border-radius: $radius-sm;
  color: #a0aaff;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, color 0.15s;

  &:hover:not(:disabled) { background: rgba($accent-primary, 0.27); color: #e0e4ff; }
  &:disabled { opacity: 0.45; cursor: not-allowed; }
}

.gen-spinner {
  width: 10px;
  height: 10px;
  border: 2px solid rgba($accent-primary, 0.27);
  border-top-color: #a0aaff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin { to { transform: rotate(360deg); } }

.gen-progress-bar {
  height: 3px;
  background: rgba($accent-primary, 0.15);
  border-radius: 2px;
  overflow: hidden;
}

.gen-progress-fill {
  height: 100%;
  background: $accent-primary;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.gen-error {
  font-size: 10px;
  color: $accent-red;
  padding: 2px 0;
}
</style>
