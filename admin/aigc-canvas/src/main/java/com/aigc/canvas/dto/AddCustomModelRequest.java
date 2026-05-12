package com.aigc.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加自定义模型请求 DTO
 * 用户填写自己的模型信息（OpenAI 兼容接口或其他 LLM API）
 * apiKey 由后端加密/脱敏存储，不以明文返回
 */
@Data
public class AddCustomModelRequest {

    /** 模型名称（必填，用于在 LangChain 侧标识模型） */
    @NotBlank(message = "模型名称不能为空")
    private String name;

    /** 分类（可选，默认"文本"）：视频 / 图像 / 音频 / 文本 */
    private String category;

    /** 模型描述（可选，展示在模型卡片上） */
    private String description;

    /** API 端点 URL（如 https://api.openai.com/v1 或私有部署地址） */
    private String apiEndpoint;

    /** API 密钥（存储后脱敏显示，仅在 AI 调用时使用） */
    private String apiKey;

    /** emoji 图标（可选，默认 ⚙️） */
    private String icon;

    /** 主题色 hex（可选，默认 #646cff） */
    private String color;
}
