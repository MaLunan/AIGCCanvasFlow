package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模型实体：对应数据库表 t_ai_model（平台预置模型数据）
 * 注意：当前版本模型数据已迁移至 Nacos 配置（ModelKeysConfig），
 * 此实体类保留以备数据库存储模式使用，当前 AiModelService 不直接查此表
 */
@Data
@TableName("t_ai_model")
public class AiModel {

    /** 主键 ID，使用 TimestampIdGenerator 生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 展示名称，如 "Kling 3.0"、"DALL-E 3" */
    private String name;

    /** 提供商标识：kuaishou / openai / runway / stability 等 */
    private String provider;

    /** 模型能力类型：video（文生视频）/ image（文生图）/ audio（文生音频） */
    private String type;

    /** 模型 API 标识符（LangChain 侧使用的 model key，如 dalle3、flux） */
    private String modelKey;

    /** 支持的宽高比，逗号分隔（如 "1:1,16:9,9:16"） */
    private String supportAspects;

    /** 支持的视频时长（秒），逗号分隔（如 "5,10,15"，仅视频类模型有此字段） */
    private String supportDurations;

    /** 支持的分辨率，逗号分隔（如 "720p,1080p"） */
    private String supportResolutions;

    /** 每次调用消耗的算力点数（预留收费体系字段） */
    private Integer costPoints;

    /** 模型简介描述（展示在模型卡片上） */
    private String description;

    /** 中文展示分类：视频 / 图像 / 音频 / 文本 */
    private String category;

    /** 标签列表，逗号分隔（如 "文生视频,高质量,4K"），用于模型广场筛选 */
    private String tags;

    /** emoji 图标（如 🎬、🖼️），展示在模型卡片左上角 */
    private String icon;

    /** 模型主题色 hex（如 #646cff），用于卡片渐变背景 */
    private String color;

    /** 上线状态：0=下线，1=上线 */
    private Integer status;

    /** 创建时间（INSERT 时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
