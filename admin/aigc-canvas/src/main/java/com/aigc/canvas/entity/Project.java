package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** 封面图 URL */
    private String cover;

    /** short_drama / oral / ad / mv / vlog / other */
    private String category;

    /** canvas 画布 JSON 数据 */
    private String canvasData;

    /** 分镜数量 */
    private Integer frameCount;

    /** 0-草稿 1-已发布 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
