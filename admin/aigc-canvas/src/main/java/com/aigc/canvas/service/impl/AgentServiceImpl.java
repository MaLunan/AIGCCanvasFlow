package com.aigc.canvas.service.impl;

import com.aigc.canvas.client.LangChainClient;
import com.aigc.canvas.config.ModelKeysConfig;
import com.aigc.canvas.dto.AgentGenerateRequest;
import com.aigc.canvas.dto.AgentGenerateResponse;
import com.aigc.canvas.dto.PolishRequest;
import com.aigc.canvas.entity.AiModel;
import com.aigc.canvas.entity.UserModelLibrary;
import com.aigc.canvas.mapper.AiModelMapper;
import com.aigc.canvas.mapper.UserModelLibraryMapper;
import com.aigc.canvas.service.AgentService;
import com.aigc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final LangChainClient langChainClient;
    private final UserModelLibraryMapper libraryMapper;
    private final AiModelMapper aiModelMapper;
    private final ModelKeysConfig modelKeysConfig;

    // ── 生图 / 生视频 ────────────────────────────────────────────────────────

    @Override
    public AgentGenerateResponse generate(Long userId, AgentGenerateRequest request) {
        String lcModel = resolveLcModel(request.getLibraryModelId());

        String taskId;
        if ("image".equals(request.getTargetType())) {
            taskId = langChainClient.submitT2I(
                    request.getPrompt(),
                    lcModel,
                    request.getAspect()
            );
        } else {
            // video（默认）
            int duration = request.getDuration() != null ? request.getDuration() : 5;
            String resolution = StringUtils.hasText(request.getResolution()) ? request.getResolution() : "1080p";
            taskId = langChainClient.submitT2V(
                    request.getPrompt(),
                    lcModel,
                    duration,
                    resolution,
                    request.getAspect()
            );
        }

        log.info("[Agent] generate submitted: userId={} type={} taskId={}", userId, request.getTargetType(), taskId);
        return AgentGenerateResponse.builder()
                .taskId(taskId)
                .status("pending")
                .progress(0)
                .build();
    }

    // ── 任务查询 ─────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public AgentGenerateResponse queryTask(String taskId) {
        Map<String, Object> data = langChainClient.queryTask(taskId);

        String status = (String) data.getOrDefault("status", "pending");
        int progress = data.get("progress") instanceof Number n ? n.intValue() : 0;
        String resultUrl = (String) data.get("result_url");
        String error = (String) data.get("error");

        // LangChain 状态 → 统一状态
        String mappedStatus = switch (status) {
            case "succeeded" -> "success";
            case "failed"    -> "failed";
            case "processing"-> "processing";
            default          -> "pending";
        };

        return AgentGenerateResponse.builder()
                .taskId(taskId)
                .status(mappedStatus)
                .progress(progress)
                .resultUrl(resultUrl)
                .error(error)
                .build();
    }

    // ── 文字润化 ─────────────────────────────────────────────────────────────

    @Override
    public String polish(Long userId, PolishRequest request) {
        String apiKey = null, baseUrl = null, modelName = null;

        if (request.getLibraryModelId() != null) {
            UserModelLibrary lib = libraryMapper.selectById(request.getLibraryModelId());
            if (lib == null || !lib.getUserId().equals(userId)) {
                throw new BusinessException(403, "所选模型不存在或无权使用");
            }
            if (lib.getEnabled() != 1) {
                throw new BusinessException(400, "所选模型已禁用，请先在模型库中启用");
            }
            if (lib.getIsCustom() == 1) {
                // 自定义模型：直接使用用户填写的 API 配置
                apiKey    = lib.getApiKey();
                baseUrl   = lib.getApiEndpoint();
                modelName = lib.getName();
            } else if (lib.getModelId() != null) {
                // 平台模型：从 Nacos 配置中查找凭证
                AiModel model = aiModelMapper.selectById(lib.getModelId());
                if (model != null) {
                    ModelKeysConfig.ModelKeyEntry entry = modelKeysConfig.get(model.getModelKey());
                    if (entry != null) {
                        apiKey    = entry.getApiKey();
                        baseUrl   = entry.getBaseUrl();
                        modelName = entry.getModelName();
                    } else {
                        log.warn("[Agent] 平台模型 {} 未在 Nacos 配置中找到凭证", model.getModelKey());
                    }
                }
            }
        }

        log.info("[Agent] polish: userId={} textLen={} ctxSize={} model={}", userId,
                request.getText().length(),
                request.getContext() == null ? 0 : request.getContext().size(),
                modelName);

        return langChainClient.polish(request.getText(), request.getContext(), apiKey, baseUrl, modelName);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * 根据用户模型库 ID 解析出对应的 LangChain 模型标识。
     * 若未传或查不到，返回 null（由 LangChain 使用默认模型）。
     */
    private String resolveLcModel(Long libraryModelId) {
        if (libraryModelId == null) return null;
        UserModelLibrary lib = libraryMapper.selectById(libraryModelId);
        if (lib == null) return null;
        if (lib.getIsCustom() == 1) {
            // 自定义模型：用 name 做简单映射，或直接返回 null 让 LangChain 用默认
            return toLangChainModel(lib.getName());
        }
        // 平台模型：通过关联的 AiModel 获取 modelKey
        if (lib.getModelId() == null) return null;
        AiModel model = aiModelMapper.selectById(lib.getModelId());
        return model != null ? toLangChainModel(model.getModelKey()) : null;
    }

    /**
     * 将平台模型 key 映射到 LangChain 模型标识。
     * LangChain T2I 支持：dalle3 / flux / sdxl
     * LangChain T2V 支持：kling / wan / minimax
     */
    private String toLangChainModel(String modelKeyOrName) {
        if (!StringUtils.hasText(modelKeyOrName)) return null;
        String k = modelKeyOrName.toLowerCase();
        // T2I
        if (k.contains("dalle") || k.contains("dall-e")) return "dalle3";
        if (k.contains("flux"))                            return "flux";
        if (k.contains("sdxl") || k.contains("stable"))   return "sdxl";
        // T2V
        if (k.contains("kling"))                           return "kling";
        if (k.contains("wan"))                             return "wan";
        if (k.contains("minimax"))                         return "minimax";
        // Text models (for polish, lcModel is ignored by polish chain)
        return null;
    }
}
