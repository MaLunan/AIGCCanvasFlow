<script setup>
import { computed, ref } from 'vue'
import { useVideoFrames } from '../../composables/useVideoFrames'

const props = defineProps({
  src: { type: String, default: '' },
  active: { type: Boolean, default: true },
  onSeek: { type: Function, default: null },
  onGenerateImage: { type: Function, default: null },   // (frame: {time, dataUrl}) => void
  generating: { type: Boolean, default: false },        // HD capture in progress
})

// ── interval selector ───────────────────────────────────────────────────────
const PRESETS = [
  { label: '每帧', value: 1 },
  { label: '15s',  value: 15 },
  { label: '30s',  value: 30 },
  { label: '自定义', value: 'custom' },
]

const selectedPreset = ref(1)     // current preset value or 'custom'
const customInput = ref('')       // raw text for custom input
const customError = ref(false)

// the actual interval (number) fed to composable
const interval = ref(1)

function applyPreset(preset) {
  selectedPreset.value = preset.value
  customError.value = false
  if (preset.value !== 'custom') {
    interval.value = preset.value
  }
}

function onCustomInput(e) {
  const raw = e.target.value
  customInput.value = raw
  const n = parseFloat(raw)
  if (!isNaN(n) && n >= 0.5) {
    customError.value = false
    interval.value = n
  } else {
    customError.value = true
  }
}

// ── frame extraction ────────────────────────────────────────────────────────
const srcRef = computed(() => props.src)
const activeRef = computed(() => props.active && !!props.src)

const { frames, loading, progress, duration, error } = useVideoFrames(srcRef, activeRef, interval)

// ── helpers ─────────────────────────────────────────────────────────────────
const hoveredIdx = ref(-1)

function fmtTime(sec) {
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

function handleClick(frame, e) {
  e.stopPropagation()
  props.onSeek?.(frame.time)
}

function handleGenerate(frame, e) {
  e.stopPropagation()
  props.onGenerateImage?.(frame)
}
</script>

<template>
  <div class="frame-strip-wrap" @click.stop @mousedown.stop @pointerdown.stop>

    <!-- ── Toolbar: interval selector ── -->
    <div class="strip-toolbar" @wheel.stop>
      <span class="strip-label">🎞 帧预览</span>
      <div class="interval-pills">
        <button
          v-for="p in PRESETS"
          :key="p.value"
          class="pill"
          :class="{ active: selectedPreset === p.value }"
          @click.stop="applyPreset(p)"
        >{{ p.label }}</button>
      </div>
      <!-- custom input, shown only when custom preset active -->
      <div v-if="selectedPreset === 'custom'" class="custom-wrap">
        <input
          class="custom-input"
          :class="{ error: customError }"
          type="number"
          min="0.5"
          step="0.5"
          :value="customInput || interval"
          placeholder="秒"
          @input="onCustomInput"
          @click.stop
          @keydown.stop
          @pointerdown.stop
        />
        <span class="custom-unit">s/帧</span>
      </div>
      <span class="strip-dur" v-if="duration">{{ fmtTime(duration) }} · {{ frames.length }} 帧</span>
    </div>

    <!-- ── Loading ── -->
    <div v-if="loading && frames.length === 0" class="strip-loading">
      <div class="strip-spinner" />
      <span class="strip-loading-text">解析帧中… {{ progress }}%</span>
    </div>

    <!-- ── Error ── -->
    <div v-else-if="error" class="strip-error">⚠ {{ error }}</div>

    <!-- ── Frame scroll strip ── -->
    <div
      v-if="frames.length > 0"
      class="strip-scroll"
      @wheel.stop
      @pointermove.stop
      @touchmove.stop
    >
      <!-- re-parsing indicator (overlaid on first thumb) -->
      <div v-if="loading" class="strip-reparsing">
        <div class="strip-spinner small" />
        {{ progress }}%
      </div>

      <div
        v-for="(frame, idx) in frames"
        :key="frame.time"
        class="strip-thumb"
        :class="{ hovered: hoveredIdx === idx }"
        @mouseenter="hoveredIdx = idx"
        @mouseleave="hoveredIdx = -1"
        @click="handleClick(frame, $event)"
        :title="fmtTime(frame.time)"
      >
        <img :src="frame.dataUrl" class="thumb-img" draggable="false" />
        <span class="thumb-time">{{ fmtTime(frame.time) }}</span>
        <button
          v-if="onGenerateImage && hoveredIdx === idx"
          class="gen-btn"
          :class="{ busy: generating }"
          :disabled="generating"
          @click="handleGenerate(frame, $event)"
          @mousedown.stop
          :title="generating ? '生成中…' : '生成图片节点'"
        >{{ generating ? '…' : '+图' }}</button>
      </div>
    </div>

  </div>
</template>

<style scoped>
.frame-strip-wrap {
  background: #08080f;
  border-top: 1px solid #1e1e3a;
  user-select: none;
}

/* ── toolbar ── */
.strip-toolbar {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px 3px;
  flex-wrap: wrap;
}
.strip-label {
  font-size: 10px;
  color: #646cff;
  font-weight: 600;
  white-space: nowrap;
}
.strip-dur {
  font-size: 9px;
  color: #444;
  margin-left: auto;
  white-space: nowrap;
}

/* pills */
.interval-pills {
  display: flex;
  gap: 3px;
}
.pill {
  padding: 2px 7px;
  border-radius: 10px;
  border: 1px solid #2e2e50;
  background: transparent;
  color: #555;
  font-size: 9px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
  white-space: nowrap;
}
.pill:hover { border-color: #646cff88; color: #a0aaff; }
.pill.active {
  background: #646cff22;
  border-color: #646cff;
  color: #c0c5ff;
}

/* custom input */
.custom-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}
.custom-input {
  width: 44px;
  background: #12121f;
  border: 1px solid #2e2e50;
  border-radius: 5px;
  color: #c0c0e0;
  font-size: 10px;
  padding: 2px 5px;
  outline: none;
  font-family: inherit;
  -moz-appearance: textfield;
}
.custom-input::-webkit-inner-spin-button,
.custom-input::-webkit-outer-spin-button { -webkit-appearance: none; }
.custom-input:focus { border-color: #646cff88; }
.custom-input.error { border-color: #ff6b6b88; }
.custom-unit { font-size: 9px; color: #444; }

/* ── loading ── */
.strip-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
}
.strip-spinner {
  width: 12px; height: 12px;
  border: 2px solid #2e2e50;
  border-top-color: #646cff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}
.strip-spinner.small { width: 9px; height: 9px; border-width: 1.5px; }
@keyframes spin { to { transform: rotate(360deg); } }
.strip-loading-text { font-size: 10px; color: #555; }

/* error */
.strip-error { padding: 6px 10px; font-size: 10px; color: #ff6b6b99; }

/* re-parsing badge inside strip */
.strip-reparsing {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 0 6px;
  font-size: 9px;
  color: #646cff88;
  white-space: nowrap;
}

/* ── scroll strip ── */
.strip-scroll {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 3px 6px 6px;
  overflow-x: auto;
  scrollbar-width: thin;
  scrollbar-color: #2e2e50 transparent;
  position: relative;
}
.strip-scroll::-webkit-scrollbar { height: 3px; }
.strip-scroll::-webkit-scrollbar-track { background: transparent; }
.strip-scroll::-webkit-scrollbar-thumb { background: #2e2e50; border-radius: 2px; }

/* thumb */
.strip-thumb {
  position: relative;
  flex-shrink: 0;
  width: 60px; height: 34px;
  cursor: pointer;
  border-radius: 3px;
  overflow: hidden;
  border: 1px solid transparent;
  transition: border-color 0.12s, transform 0.12s;
}
.strip-thumb.hovered {
  border-color: #646cff;
  transform: scaleY(1.06);
  z-index: 1;
}
.thumb-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
  background: #111;
}
.thumb-time {
  position: absolute;
  bottom: 1px; left: 2px;
  font-size: 8px;
  color: #ffffffcc;
  text-shadow: 0 0 3px #000;
  pointer-events: none;
  line-height: 1;
}

/* generate image node button */
.gen-btn {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  padding: 3px 0;
  background: rgba(100, 108, 255, 0.88);
  border: none;
  color: #fff;
  font-size: 9px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  letter-spacing: 0.3px;
  transition: background 0.12s;
  line-height: 1;
}
.gen-btn:hover { background: rgba(100, 108, 255, 1); }
.gen-btn.busy { background: rgba(100, 108, 255, 0.5); cursor: wait; }
</style>
