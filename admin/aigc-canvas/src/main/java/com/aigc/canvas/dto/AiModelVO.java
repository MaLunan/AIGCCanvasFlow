package com.aigc.canvas.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 模型视图 VO：模型广场展示用
 * 数据来源：Nacos ModelKeysConfig（动态配置，不查数据库）
 * API Key 等敏感字段不包含在此 VO 中，防止泄露
 */
@Data
public class AiModelVO {
    /** 数据库 ID（预留，当前版本模型数据来自 Nacos，此字段通常为 null） */
    private Long id;
    /** 模型展示名称（如 "DALL-E 3"、"Kling 3.0"） */
    private String name;
    /** 提供商（如 openai、kuaishou、stability） */
    private String provider;
    /** 模型类型：video / image / audio / text */
    private String type;
    /** 中文展示分类：视频 / 图像 / 音频 / 文本 */
    private String category;
    /** 模型标识符（对应 Nacos 配置的 key，如 dalle3、kling-v3） */
    private String modelKey;
    /** 支持的宽高比列表（如 ["1:1", "16:9", "9:16"]） */
    private List<String> supportAspects;
    /** 支持的视频时长列表（如 ["5", "10"]，单位秒，仅视频类） */
    private List<String> supportDurations;
    /** 支持的分辨率列表（如 ["720p", "1080p"]） */
    private List<String> supportResolutions;
    /** 单次调用消耗算力点（预留收费体系） */
    private Integer costPoints;
    /** 模型描述 */
    private String description;
    /** 标签列表（如 ["文生视频", "高质量"]） */
    private List<String> tags;
    /** emoji 图标 */
    private String icon;
    /** 模型主题色 hex */
    private String color;
    /** 当前用户是否已将此模型添加到个人库（登录时有效，未登录时固定为 false） */
    private Boolean inLibrary;
}
