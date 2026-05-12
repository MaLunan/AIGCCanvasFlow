// 视频帧提取 composable：基于 HTML5 Canvas 对视频进行逐帧截图
import { ref, watch, onUnmounted } from 'vue'

// 缩略图尺寸（16:9 比例，宽120px）
const THUMB_W = 120
const THUMB_H = 68   // 16:9
// 最多截取帧数，防止内存溢出
const MAX_FRAMES = 200

/**
 * Canvas-based 按间隔截帧
 * @param {import('vue').Ref<string>}  srcRef      - reactive video src
 * @param {import('vue').Ref<boolean>} activeRef   - only extract when true
 * @param {import('vue').Ref<number>}  intervalRef - seconds between frames (default 1)
 */
export function useVideoFrames(srcRef, activeRef, intervalRef) {
  const frames   = ref([])     // 帧数组 [{ time: number, dataUrl: string }]
  const loading  = ref(false)  // 截帧进行中
  const progress = ref(0)      // 截帧进度 0-100
  const duration = ref(0)      // 视频总时长（秒）
  const error    = ref(null)   // 错误信息

  // 用于在新 src 到来时中止上一次未完成的截帧
  let abortCtrl = null

  async function extract(src, interval) {
    // 中止上一次截帧（src 变化时防止竞态）
    if (abortCtrl) abortCtrl.abort()
    abortCtrl = new AbortController()
    const signal = abortCtrl.signal

    // 重置状态
    frames.value   = []
    error.value    = null
    progress.value = 0
    duration.value = 0

    if (!src || !activeRef.value) return

    loading.value = true

    try {
      // 创建隐藏的 video 元素，用于 Canvas 截帧（不需要挂载到 DOM）
      const video = document.createElement('video')
      video.src         = src
      video.crossOrigin = 'anonymous'  // 允许跨域视频截帧
      video.muted       = true
      video.preload     = 'metadata'   // 只预加载元数据，不下载全部视频

      // 等待元数据加载（获取 duration、分辨率等信息）
      await new Promise((resolve, reject) => {
        video.addEventListener('loadedmetadata', resolve, { once: true })
        video.addEventListener('error', () => reject(new Error('视频加载失败')), { once: true })
        setTimeout(() => reject(new Error('加载超时')), 15000)  // 15s 超时
      })

      if (signal.aborted) return

      duration.value = video.duration
      const step = Math.max(0.5, interval)  // 截帧间隔最小 0.5 秒

      // 预先计算所有要截取的时间点
      const times = []
      for (let t = 0; t < video.duration; t += step) {
        times.push(parseFloat(t.toFixed(2)))  // 避免浮点精度问题
        if (times.length >= MAX_FRAMES) break  // 超出最大帧数限制则截断
      }
      if (times.length === 0) return

      // 创建复用的 Canvas 进行截帧（避免反复创建）
      const canvas = document.createElement('canvas')
      canvas.width  = THUMB_W
      canvas.height = THUMB_H
      const ctx = canvas.getContext('2d')

      for (let i = 0; i < times.length; i++) {
        if (signal.aborted) break

        // 跳转到目标时间点
        video.currentTime = times[i]

        // 等待视频 seek 完成（800ms 超时兜底）
        await new Promise((resolve) => {
          video.addEventListener('seeked', resolve, { once: true })
          setTimeout(resolve, 800)
        })

        if (signal.aborted) break

        // 将当前帧绘制到 Canvas，并导出 JPEG（质量 0.65 平衡大小与清晰度）
        ctx.drawImage(video, 0, 0, THUMB_W, THUMB_H)
        frames.value.push({ time: times[i], dataUrl: canvas.toDataURL('image/jpeg', 0.65) })
        progress.value = Math.round(((i + 1) / times.length) * 100)
      }

      video.src = ''  // 释放视频资源
    } catch (e) {
      if (!signal.aborted) error.value = e.message
    } finally {
      if (!signal.aborted) loading.value = false
    }
  }

  // 监听 src / active / interval 变化，自动触发截帧或清空
  watch(
    [srcRef, activeRef, intervalRef],
    ([src, active, interval]) => {
      if (active && src) extract(src, interval ?? 1)
      else { frames.value = []; loading.value = false; progress.value = 0 }
    },
    { immediate: true },  // 组件挂载时立即执行一次
  )

  // 组件卸载时中止截帧，避免内存泄漏
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
  video.src         = src
  video.crossOrigin = 'anonymous'
  video.muted       = true
  video.preload     = 'metadata'

  // 等待元数据加载
  await new Promise((resolve, reject) => {
    video.addEventListener('loadedmetadata', resolve, { once: true })
    video.addEventListener('error', () => reject(new Error('视频加载失败')), { once: true })
    setTimeout(() => reject(new Error('加载超时')), 15000)
  })

  // 精确跳转到目标时间点
  video.currentTime = time
  await new Promise((resolve) => {
    video.addEventListener('seeked', resolve, { once: true })
    setTimeout(resolve, 1000)  // 1s 超时兜底
  })

  // 计算缩放比例：保持原始宽高比，同时限制最大分辨率为 1920×1080
  const vw    = video.videoWidth  || 1280
  const vh    = video.videoHeight || 720
  const scale = Math.min(1, 1920 / vw, 1080 / vh)  // 不放大，只缩小
  const w     = Math.round(vw * scale)
  const h     = Math.round(vh * scale)

  const canvas = document.createElement('canvas')
  canvas.width  = w
  canvas.height = h
  canvas.getContext('2d').drawImage(video, 0, 0, w, h)

  video.src = ''  // 释放视频资源
  return canvas.toDataURL('image/png')  // PNG = 无损，截图清晰
}
