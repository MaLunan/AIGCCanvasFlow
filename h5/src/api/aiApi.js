import request from './request'

/**
 * 文字润化（同步，直接返回润化后的文本）
 * @param {string} text 原文
 * @param {number} libraryModelId 用户模型库中的文本模型 ID
 * @param {Array}  context 上游节点上下文 [{nodeId, label, content}]
 * @returns {Promise<string>} 润化后的文本
 */
export function polishText(text, libraryModelId, context = []) {
  return request.post('/canvas/agent/polish', { text, libraryModelId, context }, { timeout: 120000 })
}

/**
 * 提交生图任务（异步，返回 taskId）
 * @param {object} params { prompt, libraryModelId, aspect, context }
 * @returns {Promise<{taskId, status, progress}>}
 */
export function submitImageGen(params) {
  return request.post('/canvas/agent/generate', {
    targetType: 'image',
    prompt: params.prompt,
    libraryModelId: params.libraryModelId,
    aspect: params.aspect || '1:1',
    context: params.context || [],
  })
}

/**
 * 提交生视频任务（异步，返回 taskId）
 * @param {object} params { prompt, libraryModelId, aspect, duration, resolution, audio, context }
 * @returns {Promise<{taskId, status, progress}>}
 */
export function submitVideoGen(params) {
  return request.post('/canvas/agent/generate', {
    targetType: 'video',
    prompt: params.prompt,
    libraryModelId: params.libraryModelId,
    aspect: params.aspect || '16:9',
    duration: parseInt(params.duration) || 5,
    resolution: params.resolution || '1080p',
    audio: params.audio || 'sound',
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
      if (Date.now() - start > timeoutMs) {
        return reject(new Error('任务超时，请稍后重试'))
      }
      try {
        const data = await request.get(`/canvas/agent/task/${taskId}`)
        onProgress?.(data.progress ?? 0, data.status)

        if (data.status === 'success') return resolve(data)
        if (data.status === 'failed')  return reject(new Error(data.error || '生成失败'))

        setTimeout(tick, intervalMs)
      } catch (e) {
        reject(e)
      }
    }

    tick()
  })
}
