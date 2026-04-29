/**
 * useFFmpeg — ffmpeg.wasm 封装
 * 使用单线程 core（无需 SharedArrayBuffer / COOP/COEP 特殊头）
 * Core 从 unpkg CDN 懒加载，首次 ~8MB
 */
import { ref, shallowRef } from 'vue'
import { FFmpeg } from '@ffmpeg/ffmpeg'
import { fetchFile, toBlobURL } from '@ffmpeg/util'

const CORE_BASE = 'https://unpkg.com/@ffmpeg/core@0.12.6/dist/esm'

let _instance = null
let _loadPromise = null

export function useFFmpeg() {
  const loaded = ref(false)
  const loading = ref(false)
  const progress = ref(0)   // 0-100 transcode progress
  const error = ref(null)
  const ffmpeg = shallowRef(null)

  async function load() {
    if (_instance) {
      ffmpeg.value = _instance
      loaded.value = true
      return
    }
    if (_loadPromise) {
      await _loadPromise
      ffmpeg.value = _instance
      loaded.value = true
      return
    }

    loading.value = true
    error.value = null

    _loadPromise = (async () => {
      const inst = new FFmpeg()
      inst.on('progress', ({ progress: p }) => { progress.value = Math.round(p * 100) })
      inst.on('log', ({ message }) => { if (import.meta.env.DEV) console.log('[ffmpeg]', message) })

      await inst.load({
        coreURL: await toBlobURL(`${CORE_BASE}/ffmpeg-core.js`, 'text/javascript'),
        wasmURL: await toBlobURL(`${CORE_BASE}/ffmpeg-core.wasm`, 'application/wasm'),
      })

      _instance = inst
    })()

    try {
      await _loadPromise
      ffmpeg.value = _instance
      loaded.value = true
    } catch (e) {
      error.value = e.message
      _loadPromise = null
    } finally {
      loading.value = false
    }
  }

  /**
   * 转码视频
   * @param {Blob|File|Uint8Array} input
   * @param {string} inputName  e.g. 'input.mp4'
   * @param {string} outputName e.g. 'output.mp4'
   * @param {string[]} args     额外 ffmpeg 参数
   * @returns {Promise<Blob>}
   */
  async function transcode(input, inputName, outputName, args = []) {
    if (!loaded.value) await load()
    const ff = ffmpeg.value
    progress.value = 0
    await ff.writeFile(inputName, await fetchFile(input))
    await ff.exec(['-i', inputName, ...args, outputName])
    const data = await ff.readFile(outputName)
    await ff.deleteFile(inputName)
    await ff.deleteFile(outputName)
    return new Blob([data.buffer], { type: 'video/mp4' })
  }

  /**
   * 拼接多个视频 (concat demuxer)
   * @param {Array<{blob: Blob, name: string}>} clips
   * @param {string} outputName
   * @returns {Promise<Blob>}
   */
  async function concat(clips, outputName = 'concat_out.mp4') {
    if (!loaded.value) await load()
    const ff = ffmpeg.value
    progress.value = 0

    // write each clip
    const listLines = []
    for (const clip of clips) {
      await ff.writeFile(clip.name, await fetchFile(clip.blob))
      listLines.push(`file '${clip.name}'`)
    }

    const listTxt = new TextEncoder().encode(listLines.join('\n'))
    await ff.writeFile('list.txt', listTxt)

    await ff.exec([
      '-f', 'concat', '-safe', '0',
      '-i', 'list.txt',
      '-c', 'copy',
      outputName,
    ])

    const data = await ff.readFile(outputName)
    await ff.deleteFile('list.txt')
    for (const clip of clips) await ff.deleteFile(clip.name)
    await ff.deleteFile(outputName)

    return new Blob([data.buffer], { type: 'video/mp4' })
  }

  return { ffmpeg, loaded, loading, progress, error, load, transcode, concat }
}
