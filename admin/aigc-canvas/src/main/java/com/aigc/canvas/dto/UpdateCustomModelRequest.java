package com.aigc.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新自定义模型请求 DTO
 * 前端编辑自定义模型时发送，apiKey 特殊处理：空字符串表示不修改
 */
@Data
public class UpdateCustomModelRequest {

    /** 模型名称（必填） */
    @NotBlank(message = "模型名称不能为空")
    private String name;

    /** 分类（可选） */
    private String category;

    /** 描述（可选） */
    private String description;

    /** API 端点 URL */
    private String apiEndpoint;

    /**
     * API Key（安全策略）：
     * - 前端编辑表单不回填密钥（防止密钥泄露给页面 DOM）
     * - 传空字符串 "" 表示"不修改原有密钥"
     * - 传新值时 Service 层才覆盖存储
     */
    private String apiKey;

    /** emoji 图标（可选） */
    private String icon;

    /** 主题色 hex（可选） */
    private String color;
}
