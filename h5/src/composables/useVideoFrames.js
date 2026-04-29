import { ref, watch, onUnmounted } from 'vue'

const THUMB_W = 120
const THUMB_H = 68   // 16:9
const MAX_FRAMES = 200

/**
 * Canvas-based 按间隔截帧
 * @param {import('vue').Ref<string>}  srcRef      - reactive video src
 * @param {import('vue').Ref<boolean>} activeRef   - only extract when true
 * @param {import('vue').Ref<number>}  intervalRef - seconds between frames (default 1)
 */
export function useVideoFrames(srcRef, activeRef, intervalRef) {
  const frames = ref([])
  const loading = ref(false)
  const progress = ref(0)
  const duration = ref(0)
  const error = ref(null)

  let abortCtrl = null

  async function extract(src, interval) {
    if (abortCtrl) abortCtrl.abort()
    abortCtrl = new AbortController()
    const signal = abortCtrl.signal

    frames.value = []
    error.value = null
    progress.value = 0
    duration.value = 0

    if (!src || !activeRef.value) return

    loading.value = true

    try {
      const video = document.createElement('video')
      video.src = src
      video.crossOrigin = 'anonymous'
      video.muted = true
      video.preload = 'metadata'

      await new Promise((resolve, reject) => {
        video.addEventListener('loadedmetadata', resolve, { once: true })
        video.addEventListener('error', () => reject(new Error('视频加载失败')), { once: true })
        setTimeout(() => reject(new Error('加载超时')), 15000)
      })

      if (signal.aborted) return

      duration.value = video.duration
      const step = Math.max(0.5, interval)

      // build timestamps
      const times = []
      for (let t = 0; t < video.duration; t += step) {
        times.push(parseFloat(t.toFixed(2)))
        if (times.length >= MAX_FRAMES) break
      }
      if (times.length === 0) return

      const canvas = document.createElement('canvas')
      canvas.width = THUMB_W
      canvas.height = THUMB_H
      const ctx = canvas.getContext('2d')

      for (let i = 0; i < times.length; i++) {
        if (signal.aborted) break

        video.currentTime = times[i]

        await new Promise((resolve) => {
          video.addEventListener('seeked', resolve, { once: true })
          setTimeout(resolve, 800)
        })

        if (signal.aborted) break

        ctx.drawImage(video, 0, 0, THUMB_W, THUMB_H)
        frames.value.push({ time: times[i], dataUrl: canvas.toDataURL('image/jpeg', 0.65) })
        progress.value = Math.round(((i + 1) / times.length) * 100)
      }

      video.src = ''
    } catch (e) {
      if (!signal.aborted) error.value = e.message
    } finally {
      if (!signal.aborted) loading.value = false
    }
  }

  watch(
    [srcRef, activeRef, intervalRef],
    ([src, active, interval]) => {
      if (active && src) extract(src, interval ?? 1)
      else { frames.value = []; loading.value = false; progress.value = 0 }
    },
    { immediate: true },
  )

  onUnmounted(() => { if (abortCtrl) abortCtrl.abort() })

  return { frames, loading, progress, duration, error }
}

/**
 * 单帧高清截取 —— 以视频原始分辨率（最大 1920×1080）截取指定时间点的帧
 * @param {string} src   - video src (blob URL or direct URL)
 * @param {number} time  - seconds
 * @returns {Promise<string>} dataUrl (PNG)
 */
export async function captureFrameHD(src, time) {
  const video = document.createElement('video')
  video.src = src
  video.crossOrigin = 'anonymous'
  video.muted = true
  video.preload = 'metadata'

  await new Promise((resolve, reject) => {
    video.addEventListener('loadedmetadata', resolve, { once: true })
    video.addEventListener('error', () => reject(new Error('视频加载失败')), { once: true })
    setTimeout(() => reject(new Error('加载超时')), 15000)
  })

  video.currentTime = time
  await new Promise((resolve) => {
    video.addEventListener('seeked', resolve, { once: true })
    setTimeout(resolve, 1000)
  })

  // use video's native resolution, capped at 1920×1080
  const vw = video.videoWidth  || 1280
  const vh = video.videoHeight || 720
  const scale = Math.min(1, 1920 / vw, 1080 / vh)
  const w = Math.round(vw * scale)
  const h = Math.round(vh * scale)

  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  canvas.getContext('2d').drawImage(video, 0, 0, w, h)

  video.src = ''
  return canvas.toDataURL('image/png')   // PNG = lossless, sharp
}
