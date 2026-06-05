// AI 生成接口：文字润化（同步）+ 生图/生视频（异步轮询）
import request from './request'

/**
 * 文字润化（同步，直接返回润化后的文本）
 * @param {string} text 原文
 * @param {number} libraryModelId 用户模型库中的文本模型 ID
 * @param {Array}  context 上游节点上下文 [{nodeId, label, content}]
 * @returns {Promise<string>} 润化后的文本
 */
export function polishText(text, libraryModelId, context = []) {
  // 超时设 120s，AI 润化响应时间视模型而定
  return request.post('/canvas/agent/polish', { text, libraryModelId, context }, { timeout: 120000 })
}

/**
 * 提交生图任务（异步，返回 taskId）
 * @param {object} params { prompt, libraryModelId, aspect, context }
 * @returns {Promise<{taskId, status, progress}>}
 */
export function submitImageGen(params) {
  return request.post('/canvas/agent/generate', {
    targetType: 'image',              // 告知后端生成类型
    prompt: params.prompt,
    libraryModelId: params.libraryModelId,
    aspect: params.aspect || '1:1',   // 默认正方形比例
    context: params.context || [],    // 上游节点上下文信息
  })
}

/**
 * 提交生视频任务（异步，返回 taskId）
 * @param {object} params { prompt, libraryModelId, aspect, duration, resolution, audio, context }
 * @returns {Promise<{taskId, status, progress}>}
 */
export function submitVideoGen(params) {
  return request.post('/canvas/agent/generate', {
    targetType: 'video',                        // 告知后端生成类型
    prompt: params.prompt,
    libraryModelId: params.libraryModelId,
    aspect: params.aspect || '16:9',            // 默认横屏宽高比
    duration: parseInt(params.duration) || 5,   // 视频时长（秒），强制转整数
    resolution: params.resolution || '1080p',   // 默认 1080p 分辨率
    audio: params.audio || 'sound',             // 是否含音效
    context: params.context || [],
  })
}

/**
 * 轮询任务状态，直到成功或失败
 * @param {string}   taskId      任务 ID
 * @param {function} onProgress  进度回调 (progress: 0~100, status: string) => void
 * @param {number}   intervalMs  轮询间隔（默认 2500ms）
 * @param {number}   timeoutMs   超时时间（默认 300s）
 * @returns {Promise<{resultUrl, status}>}
 */
export function pollTask(taskId, onProgress, intervalMs = 2500, timeoutMs = 300_000) {
  return new Promise((resolve, reject) => {
    const start = Date.now()

    async function tick() {
      // 超过最大等待时间，主动终止轮询
      if (Date.now() - start > timeoutMs) {
        return reject(new Error('任务超时，请稍后重试'))
      }
      try {
        // 查询后端任务状态，返回 { status, progress, resultUrl, error }
        const data = await request.get(`/canvas/agent/task/${taskId}`)

        // 触发进度回调，更新 UI 进度条
        onProgress?.(data.progress ?? 0, data.status)

        // 结果 URL 已经返回时即可渲染，避免 status 映射滞后导致 100% 继续轮询。
        if (data.resultUrl) return resolve({ ...data, status: 'success' })

        // 任务完成：解析 Promise，携带结果 URL。兼容 LangChain 原始 succeeded 状态。
        if (data.status === 'success' || data.status === 'succeeded') {
          if (!data.resultUrl) return reject(new Error('生成完成但未返回结果图片'))
          return resolve({ ...data, status: 'success' })
        }
        // 任务失败：以错误信息拒绝 Promise
        if (data.status === 'failed')  return reject(new Error(data.error || '生成失败'))

        // 任务仍在进行中（pending/running），延迟后继续轮询
        setTimeout(tick, intervalMs)
      } catch (e) {
        reject(e)
      }
    }

    // 立即发起第一次轮询
    tick()
  })
}
