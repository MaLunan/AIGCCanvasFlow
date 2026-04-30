package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_template")
public class Template {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** short_drama / oral / ad / mv / vlog / edu / other */
    private String category;

    private String cover;

    private String description;

    /** 模板画布 JSON */
    private String canvasData;

    /** 使用次数 */
    private Integer useCount;

    /** 是否热门 0-否 1-是 */
    private Integer hot;

    /** 0-下线 1-上线 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
