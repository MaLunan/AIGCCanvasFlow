package com.aigc.canvas.client;

import com.aigc.canvas.dto.ContextItem;
import com.aigc.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain Python 服务 HTTP 客户端
 * 封装对 FastAPI 各接口的调用，统一处理网络异常和错误响应
 * - /api/v1/polish/text：同步文字润化
 * - /api/v1/t2i/generate：异步文字生图（返回 task_id）
 * - /api/v1/t2v/generate：异步文字生视频（返回 task_id）
 * - /api/v1/tasks/{taskId}：任务状态查询
 *
 * 异常处理策略：
 * - 4xx（客户端错误）：从 FastAPI 响应体提取 detail 字段，透传给前端
 * - 5xx（服务端错误）：记录 error 日志，抛出 502 让前端知道下游服务出错
 * - 连接失败（超时/拒绝）：记录 error 日志，抛出 503 提示服务未启动
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LangChainClient {

    /** LangChain 服务地址（默认 localhost:8000，通过 Nacos 配置覆盖） */
    @Value("${langchain.base-url:http://localhost:8888}")
    private String baseUrl;

    // RestTemplate 由 RestTemplateConfig 配置（含超时设置）
    private final RestTemplate restTemplate;

    // ── 文字润化（同步，直接返回结果）────────────────────────────────────────

    /**
     * 调用润化接口，返回润化后的文本
     * 将 canvas 上下文（上游节点内容）传给 LangChain，作为 LLM 的参考信息
     *
     * @param text            待润化文本
     * @param context         上游节点上下文列表（可为 null）
     * @param apiKey          用户/平台的 LLM API Key（空字符串时 LangChain 使用默认配置）
     * @param baseUrlOverride 用户自定义的 LLM Base URL（如私有部署的 OpenAI 兼容接口）
     * @param modelName       模型名称（如 gpt-4o、qwen-max）
     */
    @SuppressWarnings("unchecked")
    public String polish(String text, List<ContextItem> context,
                         String apiKey, String baseUrlOverride, String modelName) {
        String url = baseUrl + "/api/v1/polish/text";

        // 将 ContextItem 列表转为 LangChain 期望的 Map 列表格式（过滤空内容节点）
        List<Map<String, String>> ctxList = context == null ? List.of() :
                context.stream()
                        .filter(c -> c.getContent() != null && !c.getContent().isBlank())
                        .map(c -> {
                            Map<String, String> m = new HashMap<>();
                            m.put("node_id", c.getNodeId() != null ? c.getNodeId() : "");
                            m.put("label", c.getLabel() != null ? c.getLabel() : "");
                            m.put("content", c.getContent());
                            return m;
                        })
                        .toList();

        // 构造请求体，null 值替换为空字符串（FastAPI Pydantic 校验不接受 null）
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("context", ctxList);
        body.put("api_key", apiKey != null ? apiKey : "");
        body.put("base_url", baseUrlOverride != null ? baseUrlOverride : "");
        body.put("model_name", modelName != null ? modelName : "");

        try {
            log.info("[LangChain] POST {} text.len={} ctx.size={} model={}", url, text.length(), ctxList.size(), modelName);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            Map<String, Object> data = resp.getBody();
            if (data == null || !data.containsKey("polished")) {
                throw new BusinessException(500, "润化服务返回数据异常");
            }
            return (String) data.get("polished"); // 提取润化后的文本
        } catch (HttpClientErrorException e) {
            String detail = extractDetail(e);
            log.warn("[LangChain] polish 400/4xx: {}", detail);
            throw new BusinessException(e.getStatusCode().value(), detail);
        } catch (HttpServerErrorException e) {
            log.error("[LangChain] polish 5xx: {}", e.getMessage());
            throw new BusinessException(502, "润化服务内部错误：" + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.error("[LangChain] polish 连接失败: {}", e.getMessage());
            throw new BusinessException(503, "润化服务无法连接，请确认 LangChain 服务已启动");
        }
    }

    // ── 文字生图（异步，返回 task_id）────────────────────────────────────────

    /**
     * 提交文字生图任务，返回 LangChain task_id（异步）
     * 前端轮询 /api/v1/tasks/{taskId} 获取进度和结果
     *
     * @param prompt       用户描述（prompt）
     * @param lcModel      LangChain 模型标识（dalle3 / flux / sdxl）
     * @param aspect       宽高比（1:1 / 16:9 / 9:16 / 4:3 / 3:4）
     * @param llmApiKey    提示词增强用的 LLM API Key（可空）
     * @param llmBaseUrl   提示词增强用的 LLM API 地址（可空）
     * @param llmModelName 提示词增强用的 LLM 模型名（可空）
     * @param imgApiKey    生图模型 API Key（可空）
     * @param imgBaseUrl   生图模型 API 地址（可空）
     * @param imgModelName 生图模型名（可空，如 doubao-seedream-5-0-260128）
     */
    @SuppressWarnings("unchecked")
    public String submitT2I(String prompt, String lcModel, String aspect,
                            String llmApiKey, String llmBaseUrl, String llmModelName,
                            String imgApiKey, String imgBaseUrl, String imgModelName) {
        String url = baseUrl + "/api/v1/t2i/generate";

        int[] wh = aspectToSize(aspect);
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        body.put("model", lcModel != null ? lcModel : "flux");
        body.put("style", "default");
        body.put("width", wh[0]);
        body.put("height", wh[1]);
        body.put("num_images", 1);
        body.put("enhance_prompt", false);
        body.put("llm_api_key", llmApiKey != null ? llmApiKey : "");
        body.put("llm_base_url", llmBaseUrl != null ? llmBaseUrl : "");
        body.put("llm_model_name", llmModelName != null ? llmModelName : "");
        body.put("img_api_key", imgApiKey != null ? imgApiKey : "");
        body.put("img_base_url", imgBaseUrl != null ? imgBaseUrl : "");
        body.put("img_model_name", imgModelName != null ? imgModelName : "");

        try {
            log.info("[LangChain] POST {} model={} aspect={}", url, lcModel, aspect);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            return extractTaskId(resp.getBody()); // 从响应体中提取 task_id
        } catch (HttpClientErrorException e) {
            String detail = extractDetail(e);
            log.warn("[LangChain] submitT2I 4xx: {}", detail);
            throw new BusinessException(e.getStatusCode().value(), detail);
        } catch (HttpServerErrorException e) {
            log.error("[LangChain] submitT2I 5xx: {}", e.getMessage());
            throw new BusinessException(502, "生图服务内部错误：" + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.error("[LangChain] submitT2I 连接失败: {}", e.getMessage());
            throw new BusinessException(503, "生图服务无法连接，请确认 LangChain 服务已启动");
        }
    }

    // ── 文字生视频（异步，返回 task_id）──────────────────────────────────────

    /**
     * 提交文字生视频任务，返回 LangChain task_id（异步）
     * 对入参做合法性限制（LangChain T2V 的参数范围约束）
     *
     * @param prompt       用户描述
     * @param lcModel      LangChain 模型标识（kling / wan / minimax）
     * @param duration     时长（秒），会被限制在 [3, 10] 范围内
     * @param resolution   分辨率（480p / 720p / 1080p / 4K → 自动降级到 1080p）
     * @param aspect       宽高比（仅支持 16:9 / 9:16 / 1:1，其他降级为 16:9）
     * @param llmApiKey    提示词增强用的 LLM API Key（可空）
     * @param llmBaseUrl   提示词增强用的 LLM API 地址（可空）
     * @param llmModelName 提示词增强用的 LLM 模型名（可空）
     */
    @SuppressWarnings("unchecked")
    public String submitT2V(String prompt, String lcModel,
                            int duration, String resolution, String aspect,
                            String llmApiKey, String llmBaseUrl, String llmModelName) {
        String url = baseUrl + "/api/v1/t2v/generate";

        String safeRes = "4K".equalsIgnoreCase(resolution) ? "1080p" : resolution;
        int safeDur = Math.max(3, Math.min(10, duration));
        String safeAspect = List.of("16:9", "9:16", "1:1").contains(aspect) ? aspect : "16:9";

        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        body.put("model", lcModel != null ? lcModel : "kling");
        body.put("duration", safeDur);
        body.put("resolution", safeRes);
        body.put("aspect_ratio", safeAspect);
        body.put("enhance_prompt", false);
        body.put("llm_api_key", llmApiKey != null ? llmApiKey : "");
        body.put("llm_base_url", llmBaseUrl != null ? llmBaseUrl : "");
        body.put("llm_model_name", llmModelName != null ? llmModelName : "");

        try {
            log.info("[LangChain] POST {} model={} dur={}s res={}", url, lcModel, safeDur, safeRes);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            return extractTaskId(resp.getBody());
        } catch (HttpClientErrorException e) {
            String detail = extractDetail(e);
            log.warn("[LangChain] submitT2V 4xx: {}", detail);
            throw new BusinessException(e.getStatusCode().value(), detail);
        } catch (HttpServerErrorException e) {
            log.error("[LangChain] submitT2V 5xx: {}", e.getMessage());
            throw new BusinessException(502, "生视频服务内部错误：" + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.error("[LangChain] submitT2V 连接失败: {}", e.getMessage());
            throw new BusinessException(503, "生视频服务无法连接，请确认 LangChain 服务已启动");
        }
    }

    // ── 任务状态查询 ──────────────────────────────────────────────────────────

    /**
     * 查询 LangChain 任务状态（前端轮询调用）
     * 返回原始 Map，由 AgentServiceImpl 做状态映射和 VO 封装
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> queryTask(String taskId) {
        String url = baseUrl + "/api/v1/tasks/" + taskId;
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getBody() == null) throw new BusinessException(500, "任务查询返回数据异常");
            Map<String, Object> body = resp.getBody();
            Object resultUrl = body.get("result_url");
            if (resultUrl instanceof String urlValue && urlValue.startsWith("/outputs/")) {
                body.put("result_url", baseUrl + urlValue);
            }
            return body;
        } catch (HttpClientErrorException e) {
            String detail = extractDetail(e);
            log.warn("[LangChain] queryTask({}) 4xx: {}", taskId, detail);
            throw new BusinessException(e.getStatusCode().value(), detail);
        } catch (HttpServerErrorException e) {
            log.error("[LangChain] queryTask({}) 5xx: {}", taskId, e.getMessage());
            throw new BusinessException(502, "任务查询服务内部错误：" + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.error("[LangChain] queryTask({}) 连接失败: {}", taskId, e.getMessage());
            throw new BusinessException(503, "任务查询服务无法连接，请确认 LangChain 服务已启动");
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** 从响应 body 中提取 task_id 字段，缺少时抛出异常 */
    @SuppressWarnings("unchecked")
    private String extractTaskId(Map body) {
        if (body == null || !body.containsKey("task_id")) {
            throw new BusinessException(500, "AI 服务返回数据异常，缺少 task_id");
        }
        return (String) body.get("task_id");
    }

    /**
     * 从 FastAPI HTTPException 响应体中提取 detail 字段
     * FastAPI 4xx 错误格式：{"detail": "错误描述"}
     * 解析失败时降级返回 Spring 的原始错误消息
     */
    private String extractDetail(HttpClientErrorException e) {
        try {
            JsonNode node = new ObjectMapper().readTree(e.getResponseBodyAsString());
            JsonNode detail = node.get("detail");
            if (detail != null && !detail.isNull()) return detail.asText();
        } catch (Exception ignored) {}
        return e.getMessage(); // 解析失败时返回原始消息
    }

    /**
     * 将宽高比字符串转换为 T2I 接口需要的 [width, height] 像素尺寸
     * 使用标准 HD/Full HD 分辨率，兼顾质量和生成速度
     */
    private int[] aspectToSize(String aspect) {
        if (aspect == null) return new int[]{1024, 1024}; // 默认 1:1
        return switch (aspect) {
            case "16:9"  -> new int[]{1280, 720};   // 横屏 HD
            case "9:16"  -> new int[]{720, 1280};   // 竖屏（手机壁纸）
            case "4:3"   -> new int[]{1024, 768};   // 传统比例
            case "3:4"   -> new int[]{768, 1024};   // 竖版传统
            default      -> new int[]{1024, 1024};  // 1:1（正方形）
        };
    }
}
