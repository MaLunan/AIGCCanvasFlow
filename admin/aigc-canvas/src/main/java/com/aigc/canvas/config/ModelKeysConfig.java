package com.aigc.canvas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型广场（平台模型）API 凭证配置。
 * 通过 Nacos 动态下发，无需重启服务（ConfigurationPropertiesRebinder 自动处理刷新）。
 *
 * Nacos 配置（DataId: aigc-canvas-model-keys.yaml，Group: DEFAULT_GROUP）：
 *
 * aigc:
 *   model-keys:
 *     gpt-4o:
 *       api-key: sk-xxxxxxxxxxxxxxxx
 *       base-url: https://api.openai.com/v1
 *       model-name: gpt-4o
 *     deepseek-r2:
 *       api-key: sk-xxxxxxxxxxxxxxxx
 *       base-url: https://api.deepseek.com
 *       model-name: deepseek-chat
 *     kling-v3:
 *       api-key: xxxx
 *       base-url: https://api.kuaishou.com/v1
 *       model-name: kling-v3
 *
 * Map 的 key 对应 t_ai_model.model_key 字段。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aigc")
public class ModelKeysConfig {

    /**
     * key: t_ai_model.model_key（如 gpt-4o、kling-v3）
     * value: 该模型的 API 凭证
     * 对应 Nacos 中 aigc.model-keys.{model_key}.*
     */
    private Map<String, ModelKeyEntry> modelKeys = new HashMap<>();

    @Data
    public static class ModelKeyEntry {
        /** API Key */
        private String apiKey;
        /** API Base URL，如 https://api.openai.com/v1 */
        private String baseUrl;
        /** 实际调用时传给 LangChain 的模型名称 */
        private String modelName;
    }

    /**
     * 根据模型 key 获取凭证，不存在返回 null。
     */
    public ModelKeyEntry get(String modelKey) {
        if (modelKey == null || modelKeys == null) return null;
        return modelKeys.get(modelKey);
    }
}
