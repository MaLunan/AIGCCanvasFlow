package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户模型库实体：对应数据库表 t_user_model_library
 * 记录用户添加到个人库的模型（包括从广场添加的平台模型和用户自建的自定义模型）
 */
@Data
@TableName("t_user_model_library")
public class UserModelLibrary {

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /**
     * 平台模型标识符（对应 Nacos ModelKeysConfig 中的 key）
     * 自定义模型时此字段为 null
     */
    private String modelKey;

    /**
     * 已废弃字段（原关联 t_ai_model.id）
     * 保留列避免数据库迁移风险，exist=false 告知 MyBatis-Plus 不映射此字段
     */
    @TableField(exist = false)
    private Long modelId;

    /** 模型来源：0=平台模型（从广场添加），1=用户自定义模型 */
    private Integer isCustom;

    /** 模型名称（平台模型时从 Nacos 快照，自定义模型时由用户填写） */
    private String name;

    /** 模型分类（视频 / 图像 / 文本） */
    private String category;

    /** 模型描述（前端展示用） */
    private String description;

    /** 自定义模型的 API 端点 URL（如 https://api.openai.com/v1，仅 isCustom=1 时有效） */
    private String apiEndpoint;

    /**
     * API Key（仅自定义模型有效）
     * 存储用户提供的密钥，VO 转换时脱敏（仅显示前 4 位 + ****）
     * 生产环境建议使用 AES 加密存储
     */
    private String apiKey;

    /** emoji 图标 */
    private String icon;

    /** 模型主题色 hex */
    private String color;

    /** 启用状态：0=禁用，1=启用（禁用后不出现在 AI 生成模型选择器中） */
    private Integer enabled;

    /** 添加到库的时间（INSERT 时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
