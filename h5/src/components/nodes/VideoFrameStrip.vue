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

<style lang="scss" scoped>
@use '../../styles/variables' as *;

.frame-strip-wrap {
  background: $bg-deep;
  border-top: 1px solid $border-subtle;
  user-select: none;
}

.strip-toolbar {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px 3px;
  flex-wrap: wrap;
}

.strip-label {
  font-size: 10px;
  color: $accent-primary;
  font-weight: 600;
  white-space: nowrap;
}

.strip-dur {
  font-size: 9px;
  color: #444;
  margin-left: auto;
  white-space: nowrap;
}

.interval-pills { display: flex; gap: 3px; }

.pill {
  padding: 2px 7px;
  border-radius: 10px;
  border: 1px solid $border-default;
  background: transparent;
  color: #555;
  font-size: 9px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
  white-space: nowrap;

  &:hover { border-color: rgba($accent-primary, 0.53); color: #a0aaff; }

  &.active {
    background: rgba($accent-primary, 0.13);
    border-color: $accent-primary;
    color: #c0c5ff;
  }
}

.custom-wrap { display: flex; align-items: center; gap: 2px; }

.custom-input {
  width: 44px;
  background: $bg-surface;
  border: 1px solid $border-default;
  border-radius: 5px;
  color: #c0c0e0;
  font-size: 10px;
  padding: 2px 5px;
  outline: none;
  font-family: inherit;
  -moz-appearance: textfield;

  &::-webkit-inner-spin-button,
  &::-webkit-outer-spin-button { -webkit-appearance: none; }

  &:focus { border-color: rgba($accent-primary, 0.53); }
  &.error { border-color: rgba($accent-red, 0.53); }
}

.custom-unit { font-size: 9px; color: #444; }

.strip-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
}

.strip-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid $border-default;
  border-top-color: $accent-primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;

  &.small { width: 9px; height: 9px; border-width: 1.5px; }
}

@keyframes spin { to { transform: rotate(360deg); } }

.strip-loading-text { font-size: 10px; color: #555; }

.strip-error { padding: 6px 10px; font-size: 10px; color: rgba($accent-red, 0.6); }

.strip-reparsing {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 0 6px;
  font-size: 9px;
  color: rgba($accent-primary, 0.53);
  white-space: nowrap;
}

.strip-scroll {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 3px 6px 6px;
  overflow-x: auto;
  scrollbar-width: thin;
  scrollbar-color: $border-default transparent;
  position: relative;

  &::-webkit-scrollbar { height: 3px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: $border-default; border-radius: 2px; }
}

.strip-thumb {
  position: relative;
  flex-shrink: 0;
  width: 60px;
  height: 34px;
  cursor: pointer;
  border-radius: 3px;
  overflow: hidden;
  border: 1px solid transparent;
  transition: border-color 0.12s, transform 0.12s;

  &.hovered {
    border-color: $accent-primary;
    transform: scaleY(1.06);
    z-index: 1;
  }
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  background: #111;
}

.thumb-time {
  position: absolute;
  bottom: 1px;
  left: 2px;
  font-size: 8px;
  color: rgba(255, 255, 255, 0.8);
  text-shadow: 0 0 3px #000;
  pointer-events: none;
  line-height: 1;
}

.gen-btn {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 3px 0;
  background: rgba($accent-primary, 0.88);
  border: none;
  color: #fff;
  font-size: 9px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  letter-spacing: 0.3px;
  transition: background 0.12s;
  line-height: 1;

  &:hover { background: $accent-primary; }
  &.busy { background: rgba($accent-primary, 0.5); cursor: wait; }
}
</style>
