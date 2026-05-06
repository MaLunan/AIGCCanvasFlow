package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_model_library")
public class UserModelLibrary {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 关联 t_ai_model.id，自定义模型时为 null */
    private Long modelId;

    /** 0-平台模型  1-自定义模型 */
    private Integer isCustom;

    /** 自定义模型名称（平台模型时冗余存储便于展示） */
    private String name;

    /** 分类 */
    private String category;

    /** 描述 */
    private String description;

    /** API 地址（自定义模型） */
    private String apiEndpoint;

    /** API Key（自定义模型，存储时建议加密，此处简化） */
    private String apiKey;

    /** emoji 图标 */
    private String icon;

    /** 主题色 */
    private String color;

    /** 0-禁用  1-启用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
