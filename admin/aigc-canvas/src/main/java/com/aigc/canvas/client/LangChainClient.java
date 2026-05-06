package com.aigc.canvas.client;

import com.aigc.canvas.dto.ContextItem;
import com.aigc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain Python 服务 HTTP 客户端。
 * 封装对 FastAPI 各接口的调用，统一处理异常。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LangChainClient {

    @Value("${langchain.base-url:http://localhost:8000}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    // ── 文字润化（同步，直接返回结果）────────────────────────────────────────

    /**
     * 调用润化接口，返回润化后的文本。
     */
    @SuppressWarnings("unchecked")
    public String polish(String text, List<ContextItem> context) {
        String url = baseUrl + "/api/v1/polish/text";

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

        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("context", ctxList);

        try {
            log.info("[LangChain] POST {} text.len={} ctx.size={}", url, text.length(), ctxList.size());
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            Map<String, Object> data = resp.getBody();
            if (data == null || !data.containsKey("polished")) {
                throw new BusinessException(500, "润化服务返回数据异常");
            }
            return (String) data.get("polished");
        } catch (RestClientException e) {
            log.error("[LangChain] polish failed: {}", e.getMessage());
            throw new BusinessException(503, "润化服务暂不可用，请稍后重试");
        }
    }

    // ── 文字生图（异步，返回 task_id）────────────────────────────────────────

    /**
     * 提交文字生图任务，返回 LangChain task_id。
     *
     * @param prompt       用户描述
     * @param lcModel      LangChain 模型标识（dalle3 / flux / sdxl）
     * @param aspect       宽高比（1:1 / 16:9 …）
     */
    @SuppressWarnings("unchecked")
    public String submitT2I(String prompt, String lcModel, String aspect) {
        String url = baseUrl + "/api/v1/t2i/generate";

        int[] wh = aspectToSize(aspect);
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        body.put("model", lcModel != null ? lcModel : "flux");
        body.put("style", "default");
        body.put("width", wh[0]);
        body.put("height", wh[1]);
        body.put("num_images", 1);
        body.put("enhance_prompt", true);

        try {
            log.info("[LangChain] POST {} model={} aspect={}", url, lcModel, aspect);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            return extractTaskId(resp.getBody());
        } catch (RestClientException e) {
            log.error("[LangChain] submitT2I failed: {}", e.getMessage());
            throw new BusinessException(503, "生图服务暂不可用，请稍后重试");
        }
    }

    // ── 文字生视频（异步，返回 task_id）──────────────────────────────────────

    /**
     * 提交文字生视频任务，返回 LangChain task_id。
     *
     * @param prompt     用户描述
     * @param lcModel    LangChain 模型标识（kling / wan / minimax）
     * @param duration   时长（秒）
     * @param resolution 分辨率（480p / 720p / 1080p / 4K → 自动降级到 1080p）
     * @param aspect     宽高比
     */
    @SuppressWarnings("unchecked")
    public String submitT2V(String prompt, String lcModel,
                            int duration, String resolution, String aspect) {
        String url = baseUrl + "/api/v1/t2v/generate";

        // LangChain T2V 仅支持 480p / 720p / 1080p
        String safeRes = "4K".equalsIgnoreCase(resolution) ? "1080p" : resolution;
        // LangChain T2V duration 范围 3~10，超出则限制
        int safeDur = Math.max(3, Math.min(10, duration));
        // aspect_ratio 仅支持 16:9 / 9:16 / 1:1
        String safeAspect = List.of("16:9", "9:16", "1:1").contains(aspect) ? aspect : "16:9";

        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        body.put("model", lcModel != null ? lcModel : "kling");
        body.put("duration", safeDur);
        body.put("resolution", safeRes);
        body.put("aspect_ratio", safeAspect);
        body.put("enhance_prompt", true);

        try {
            log.info("[LangChain] POST {} model={} dur={}s res={}", url, lcModel, safeDur, safeRes);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            return extractTaskId(resp.getBody());
        } catch (RestClientException e) {
            log.error("[LangChain] submitT2V failed: {}", e.getMessage());
            throw new BusinessException(503, "生视频服务暂不可用，请稍后重试");
        }
    }

    // ── 任务状态查询 ──────────────────────────────────────────────────────────

    /**
     * 查询 LangChain 任务状态。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> queryTask(String taskId) {
        String url = baseUrl + "/api/v1/tasks/" + taskId;
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getBody() == null) throw new BusinessException(500, "任务查询返回数据异常");
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("[LangChain] queryTask({}) failed: {}", taskId, e.getMessage());
            throw new BusinessException(503, "任务查询服务暂不可用");
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String extractTaskId(Map body) {
        if (body == null || !body.containsKey("task_id")) {
            throw new BusinessException(500, "AI 服务返回数据异常，缺少 task_id");
        }
        return (String) body.get("task_id");
    }

    /** 宽高比 → [width, height]，供 T2I 接口使用 */
    private int[] aspectToSize(String aspect) {
        if (aspect == null) return new int[]{1024, 1024};
        return switch (aspect) {
            case "16:9"  -> new int[]{1280, 720};
            case "9:16"  -> new int[]{720, 1280};
            case "4:3"   -> new int[]{1024, 768};
            case "3:4"   -> new int[]{768, 1024};
            default      -> new int[]{1024, 1024};  // 1:1
        };
    }
}
