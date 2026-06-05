package com.aigc.canvas.service.impl;

import com.aigc.canvas.client.LangChainClient;
import com.aigc.canvas.config.ModelKeysConfig;
import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;
import com.aigc.canvas.dto.ContextItem;
import com.aigc.canvas.dto.PolishRequest;
import com.aigc.canvas.entity.UserModelLibrary;
import com.aigc.canvas.mapper.UserModelLibraryMapper;
import com.aigc.canvas.service.AgentService;
import com.aigc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * AI 智能体服务实现：封装生图/生视频任务提交、任务状态查询、文字润化三大能力
 * - 生图/生视频：调用 LangChain 服务异步提交任务，返回 taskId，前端轮询进度
 * - 任务查询：将 LangChain 返回的原始状态映射为前端友好的状态枚举
 * - 文字润化：支持用户自定义模型或平台预置模型，优先级：用户库 > Nacos 配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    // LangChain 服务客户端：负责与 Python LangChain 服务通信（生图/生视频/润化）
    private final LangChainClient langChainClient;
    // 用户模型库 Mapper：查询用户绑定的模型配置（API Key、端点等）
    private final UserModelLibraryMapper libraryMapper;
    // Nacos 平台模型配置：存储平台预置模型的 API Key 和 BaseURL
    private final ModelKeysConfig modelKeysConfig;

    // ── 生图 / 生视频 ────────────────────────────────────────────────────────

    /**
     * 提交生图/生视频任务（异步）
     * 流程：用户选择的模型库 ID → 解析为 LangChain 模型标识 + 生成模型凭证 → 提交任务 → 返回 taskId
     * 前端收到 taskId 后通过 /agent/task/{taskId} 轮询状态，直到 success/failed
     */
    @Override
    public AgentGenerateResponse generate(Long userId, AgentGenerateRequest request) {
        String lcModel = resolveLcModel(request.getLibraryModelId());

        String[] imgCreds = resolveImgCredentials(request.getLibraryModelId());
        String prompt = buildPromptWithContext(request.getPrompt(), request.getContext());

        String taskId;
        if ("image".equals(request.getTargetType())) {
            taskId = langChainClient.submitT2I(
                    prompt, lcModel, request.getAspect(),
                    null, null, null,
                    imgCreds[0], imgCreds[1], imgCreds[2]);
        } else {
            int duration = request.getDuration() != null ? request.getDuration() : 5;
            String resolution = StringUtils.hasText(request.getResolution()) ? request.getResolution() : "1080p";
            taskId = langChainClient.submitT2V(
                    prompt, lcModel, duration, resolution, request.getAspect(),
                    null, null, null);
        }

        log.info("[Agent] generate submitted: userId={} type={} taskId={}", userId, request.getTargetType(), taskId);
        return AgentGenerateResponse.builder()
                .taskId(taskId).status("pending").progress(0).build();
    }

    // ── 任务查询 ─────────────────────────────────────────────────────────────

    /**
     * 查询 AI 任务执行状态（轮询端点）
     * 将 LangChain 返回的原始状态（succeeded/failed/processing/pending）
     * 统一映射为前端约定的状态枚举（success/failed/processing/pending）
     */
    @Override
    @SuppressWarnings("unchecked")
    public AgentGenerateResponse queryTask(String taskId) {
        // 调用 LangChain 服务查询任务状态，返回 Map 格式的原始数据
        Map<String, Object> data = langChainClient.queryTask(taskId);

        String status = (String) data.getOrDefault("status", "pending");
        // 进度字段可能是 Integer/Double，统一转 int（0~100）
        int progress = data.get("progress") instanceof Number n ? n.intValue() : 0;
        String resultUrl = (String) data.get("result_url"); // 生成完成后的资源 URL
        String error = (String) data.get("error");           // 失败时的错误描述

        // 状态映射：将 LangChain 的状态值转为前端消费的枚举
        String mappedStatus = switch (status) {
            case "succeeded"  -> "success";    // LangChain 完成 → success
            case "failed"     -> "failed";     // LangChain 失败 → failed
            case "processing" -> "processing"; // 生成中 → processing
            default           -> "pending";    // 排队等待 → pending
        };

        return AgentGenerateResponse.builder()
                .taskId(taskId).status(mappedStatus).progress(progress)
                .resultUrl(resultUrl).error(error).build();
    }

    // ── 文字润化 ─────────────────────────────────────────────────────────────

    /**
     * 文字润化：调用 LLM 对输入文本进行优化/改写
     * 模型凭证解析优先级：
     *   1. 用户自定义模型（isCustom=1）：直接使用用户填写的 apiKey + endpoint
     *   2. 平台预置模型：根据 modelKey 从 Nacos 配置（ModelKeysConfig）中读取凭证
     *   3. 未指定模型：apiKey/baseUrl/modelName 全为 null，LangChain 使用默认配置
     */
    @Override
    public String polish(Long userId, PolishRequest request) {
        String apiKey = null, baseUrl = null, modelName = null;

        if (request.getLibraryModelId() != null) {
            // 从用户模型库查询模型配置
            UserModelLibrary lib = libraryMapper.selectById(request.getLibraryModelId());
            if (lib == null || !lib.getUserId().equals(userId)) {
                // 防止越权：校验模型归属（用户只能使用自己库中的模型）
                throw new BusinessException(403, "所选模型不存在或无权使用");
            }
            if (lib.getEnabled() != 1) {
                // 模型未启用，提示用户先在模型库中开启
                throw new BusinessException(400, "所选模型已禁用，请先在模型库中启用");
            }
            if (lib.getIsCustom() == 1) {
                // 自定义模型：直接使用用户填写的 API 配置
                apiKey    = lib.getApiKey();
                baseUrl   = lib.getApiEndpoint();
                modelName = lib.getName();
            } else if (StringUtils.hasText(lib.getModelKey())) {
                // 平台模型：从 Nacos 配置中读取凭证（用户不可见 API Key）
                ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(lib.getModelKey());
                if (entry != null) {
                    apiKey    = entry.getApiKey();
                    baseUrl   = entry.getBaseUrl();
                    modelName = entry.getModelName();
                } else {
                    // Nacos 中无此模型配置，记录警告（不中断请求，降级为默认模型）
                    log.warn("[Agent] 平台模型 {} 未在 Nacos 配置中找到凭证", lib.getModelKey());
                }
            }
        }

        log.info("[Agent] polish: userId={} textLen={} ctxSize={} model={}", userId,
                request.getText().length(),
                request.getContext() == null ? 0 : request.getContext().size(),
                modelName);

        // 将文本、上下文（canvas 中上游节点内容）、模型凭证传给 LangChain 服务执行润化
        return langChainClient.polish(request.getText(), request.getContext(), apiKey, baseUrl, modelName);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildPromptWithContext(String prompt, java.util.List<ContextItem> context) {
        if (context == null || context.isEmpty()) return prompt;
        StringBuilder sb = new StringBuilder();
        for (ContextItem item : context) {
            if (item != null && StringUtils.hasText(item.getContent())) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(item.getLabel()).append(": ").append(item.getContent());
            }
        }
        if (sb.isEmpty()) return prompt;
        return prompt + "\n\n参考上游节点内容：\n" + sb;
    }

    /**
     * 将用户模型库 ID 解析为 LangChain 可识别的模型标识字符串
     * 自定义模型用 name，平台模型用 modelKey，再通过 toLangChainModel 做格式转换
     */
    private String resolveLcModel(Long libraryModelId) {
        if (libraryModelId == null) return null;
        UserModelLibrary lib = libraryMapper.selectById(libraryModelId);
        if (lib == null) return null;
        // 自定义模型用模型名称，平台模型用标准 modelKey
        String key = lib.getIsCustom() == 1 ? lib.getName() : lib.getModelKey();
        return toLangChainModel(key);
    }

    /**
     * 解析用户模型的生图/生视频凭证，返回 [apiKey, baseUrl, modelName]
     * 用于替换 Python 侧 image_models / video_models 中的 .env 默认值
     */
    private String[] resolveImgCredentials(Long libraryModelId) {
        if (libraryModelId == null) return new String[]{null, null, null};
        UserModelLibrary lib = libraryMapper.selectById(libraryModelId);
        if (lib == null || lib.getEnabled() != 1) return new String[]{null, null, null};

        if (lib.getIsCustom() == 1) {
            return new String[]{lib.getApiKey(), lib.getApiEndpoint(), lib.getName()};
        } else if (StringUtils.hasText(lib.getModelKey())) {
            ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(lib.getModelKey());
            if (entry != null) {
                return new String[]{entry.getApiKey(), entry.getBaseUrl(), entry.getModelName()};
            }
        }
        return new String[]{null, null, null};
    }

    /**
     * 将内部模型标识映射为 LangChain 服务约定的模型名称
     * 通过关键字模糊匹配（toLowerCase），兼容不同命名风格
     * 未能匹配时返回 null，LangChain 侧使用默认模型
     */
    private String toLangChainModel(String modelKeyOrName) {
        if (!StringUtils.hasText(modelKeyOrName)) return null;
        String k = modelKeyOrName.toLowerCase();
        if (k.contains("dalle") || k.contains("dall-e")) return "dalle3";   // OpenAI DALL-E 3
        if (k.contains("seedream"))                       return "dalle3";   // 豆包 Seedream → OpenAI 兼容接口
        if (k.contains("flux"))                           return "flux";     // Flux 开源生图模型
        if (k.contains("sdxl") || k.contains("stable"))  return "sdxl";     // Stable Diffusion XL
        if (k.contains("kling"))                          return "kling";    // 可灵视频生成
        if (k.contains("wan"))                            return "wan";      // 万象视频生成
        if (k.contains("minimax"))                        return "minimax";  // MiniMax 视频生成
        return null;  // 未知模型，LangChain 侧使用默认配置
    }

}
