package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_model")
public class AiModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 展示名称，如 Kling 3.0 */
    private String name;

    /** 供应商，如 kuaishou / openai / runway */
    private String provider;

    /** video / image / audio */
    private String type;

    /** 模型 API 标识符 */
    private String modelKey;

    /** 支持的宽高比，逗号分隔 */
    private String supportAspects;

    /** 支持的时长（视频），逗号分隔，如 5,10,15 */
    private String supportDurations;

    /** 支持的分辨率，逗号分隔 */
    private String supportResolutions;

    /** 每次调用消耗算力点 */
    private Integer costPoints;

    /** 简介 */
    private String description;

    /** 0-下线 1-上线 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
