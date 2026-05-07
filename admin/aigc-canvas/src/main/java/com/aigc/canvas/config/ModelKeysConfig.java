package com.aigc.canvas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型广场全量配置（元数据 + API 凭证），完全由 Nacos 驱动，无需维护 t_ai_model 表。
 *
 * Nacos 配置（DataId: aigc-canvas-model-keys.yaml，Group: DEFAULT_GROUP）：
 *
 * aigc:
 *   models:
 *     gpt-4o:
 *       name: GPT-4o
 *       provider: openai
 *       type: text
 *       category: 文本
 *       description: OpenAI 最强多模态模型，理解与生成均衡
 *       tags: 多模态,对话
 *       icon: 💬
 *       color: "#7fd1f5"
 *       cost-points: 15
 *       support-aspects: ""
 *       support-durations: ""
 *       support-resolutions: ""
 *       api-key: sk-xxxxxxxxxxxxxxxx
 *       base-url: https://api.openai.com/v1
 *       model-name: gpt-4o
 *     kling-v3:
 *       name: Kling 3.0
 *       provider: kuaishou
 *       type: video
 *       category: 视频
 *       description: 快手可灵 3.0，高质量视频生成
 *       tags: 文生视频,高质量
 *       icon: 🎬
 *       color: "#ff6b6b"
 *       cost-points: 50
 *       support-aspects: 16:9,9:16,1:1
 *       support-durations: 5,10
 *       support-resolutions: 720p,1080p
 *       api-key: xxxx
 *       base-url: https://api.kuaishou.com/v1
 *       model-name: kling-v3
 *
 * Map 的 key 即 model_key（如 gpt-4o、kling-v3）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aigc")
public class ModelKeysConfig {

    /**
     * key: model_key（如 gpt-4o、kling-v3）
     * value: 完整模型信息（元数据 + API 凭证）
     */
    private Map<String, ModelEntry> models = new HashMap<>();

    @Data
    public static class ModelEntry {
        // ── 元数据 ──────────────────────────────────────
        private String name;
        private String provider;
        /** video / image / audio / text */
        private String type;
        /** 中文分类：视频/图像/音频/文本 */
        private String category;
        private String description;
        /** 标签，逗号分隔，如 文生视频,高质量 */
        private String tags;
        /** emoji 图标 */
        private String icon;
        /** 主题色 hex */
        private String color;
        private Integer costPoints;
        /** 支持宽高比，逗号分隔 */
        private String supportAspects;
        /** 支持时长（秒），逗号分隔 */
        private String supportDurations;
        /** 支持分辨率，逗号分隔 */
        private String supportResolutions;

        // ── API 凭证 ─────────────────────────────────────
        private String apiKey;
        private String baseUrl;
        /** 实际传给 LangChain 的模型名称 */
        private String modelName;

        public List<String> tagList() {
            return StringUtils.hasText(tags) ? Arrays.asList(tags.split(",")) : Collections.emptyList();
        }

        public List<String> aspectList() {
            return StringUtils.hasText(supportAspects) ? Arrays.asList(supportAspects.split(",")) : Collections.emptyList();
        }

        public List<String> durationList() {
            return StringUtils.hasText(supportDurations) ? Arrays.asList(supportDurations.split(",")) : Collections.emptyList();
        }

        public List<String> resolutionList() {
            return StringUtils.hasText(supportResolutions) ? Arrays.asList(supportResolutions.split(",")) : Collections.emptyList();
        }
    }

    /** 根据 model_key 获取完整信息，不存在返回 null */
    public ModelEntry get(String modelKey) {
        if (modelKey == null || models == null) return null;
        return models.get(modelKey);
    }
}
