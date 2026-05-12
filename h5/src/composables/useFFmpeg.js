/**
 * useFFmpeg — ffmpeg.wasm 封装
 * 使用单线程 core（无需 SharedArrayBuffer / COOP/COEP 特殊头）
 * Core 从 unpkg CDN 懒加载，首次 ~8MB
 */
import { ref, shallowRef } from 'vue'
import { FFmpeg } from '@ffmpeg/ffmpeg'
import { fetchFile, toBlobURL } from '@ffmpeg/util'

// ffmpeg.wasm 核心包的 CDN 基础路径（单线程版本）
const CORE_BASE = 'https://unpkg.com/@ffmpeg/core@0.12.6/dist/esm'

// 全局单例：整个应用只需加载一次 ffmpeg.wasm（约 8MB）
let _instance    = null   // 已初始化的 FFmpeg 实例
let _loadPromise = null   // 防止并发多次加载的 Promise 锁

export function useFFmpeg() {
  const loaded   = ref(false)   // ffmpeg 是否已完成加载
  const loading  = ref(false)   // 加载进行中
  const progress = ref(0)       // 转码进度 0-100
  const error    = ref(null)    // 错误信息
  const ffmpeg   = shallowRef(null)  // FFmpeg 实例（shallowRef 避免深度响应）

  /**
   * 懒加载 ffmpeg.wasm：
   * - 已有实例则直接复用（单例模式）
   * - 正在加载则等待同一 Promise，防止并发重复初始化
   */
  async function load() {
    // 复用已有单例
    if (_instance) {
      ffmpeg.value = _instance
      loaded.value = true
      return
    }
    // 等待已有加载过程（其他组件先触发了加载）
    if (_loadPromise) {
      await _loadPromise
      ffmpeg.value = _instance
      loaded.value = true
      return
    }

    loading.value = true
    error.value   = null

    // 创建加载 Promise 并挂到模块级变量，防止并发调用
    _loadPromise = (async () => {
      const inst = new FFmpeg()
      // 转码进度事件（0.0 ~ 1.0）→ 转为百分比
      inst.on('progress', ({ progress: p }) => { progress.value = Math.round(p * 100) })
      // 开发环境输出 ffmpeg 日志，方便调试
      inst.on('log', ({ message }) => { if (import.meta.env.DEV) console.log('[ffmpeg]', message) })

      // 将远程 CDN 文件转换为 Blob URL，绕过 MIME 类型限制
      await inst.load({
        coreURL: await toBlobURL(`${CORE_BASE}/ffmpeg-core.js`,   'text/javascript'),
        wasmURL: await toBlobURL(`${CORE_BASE}/ffmpeg-core.wasm`, 'application/wasm'),
      })

      _instance = inst
    })()

    try {
      await _loadPromise
      ffmpeg.value = _instance
      loaded.value = true
    } catch (e) {
      error.value  = e.message
      _loadPromise = null  // 加载失败则重置，允许下次重试
    } finally {
      loading.value = false
    }
  }

  /**
   * 转码视频（如 WebM → MP4）
   * @param {Blob|File|Uint8Array} input      - 输入视频数据
   * @param {string}               inputName  - 输入文件名，如 'input.mp4'
   * @param {string}               outputName - 输出文件名，如 'output.mp4'
   * @param {string[]}             args       - 额外 ffmpeg 参数（如 ['-vf', 'scale=1280:720']）
   * @returns {Promise<Blob>} 转码后的 Blob
   */
  async function transcode(input, inputName, outputName, args = []) {
    if (!loaded.value) await load()  // 按需加载
    const ff = ffmpeg.value
    progress.value = 0

    // 将文件写入 ffmpeg 虚拟文件系统
    await ff.writeFile(inputName, await fetchFile(input))
    // 执行转码命令
    await ff.exec(['-i', inputName, ...args, outputName])
    // 从虚拟文件系统读取输出文件
    const data = await ff.readFile(outputName)
    // 清理虚拟文件系统，释放内存
    await ff.deleteFile(inputName)
    await ff.deleteFile(outputName)

    return new Blob([data.buffer], { type: 'video/mp4' })
  }

  /**
   * 拼接多个视频片段（使用 concat demuxer，速度快，但要求编码格式一致）
   * @param {Array<{blob: Blob, name: string}>} clips      - 视频片段数组
   * @param {string}                            outputName - 输出文件名
   * @returns {Promise<Blob>} 拼接后的视频 Blob
   */
  async function concat(clips, outputName = 'concat_out.mp4') {
    if (!loaded.value) await load()
    const ff = ffmpeg.value
    progress.value = 0

    // 将每个片段写入虚拟文件系统，并生成 concat list 文件
    const listLines = []
    for (const clip of clips) {
      await ff.writeFile(clip.name, await fetchFile(clip.blob))
      listLines.push(`file '${clip.name}'`)  // concat demuxer 格式
    }

    // 写入 list.txt 供 ffmpeg concat demuxer 使用
    const listTxt = new TextEncoder().encode(listLines.join('\n'))
    await ff.writeFile('list.txt', listTxt)

    // 执行拼接：-c copy 表示直接复制流，不重新编码（速度快）
    await ff.exec([
      '-f', 'concat', '-safe', '0',  // -safe 0 允许绝对路径
      '-i', 'list.txt',
      '-c', 'copy',
      outputName,
    ])

    const data = await ff.readFile(outputName)

    // 清理所有临时文件
    await ff.deleteFile('list.txt')
    for (const clip of clips) await ff.deleteFile(clip.name)
    await ff.deleteFile(outputName)

    return new Blob([data.buffer], { type: 'video/mp4' })
  }

  return { ffmpeg, loaded, loading, progress, error, load, transcode, concat }
}
