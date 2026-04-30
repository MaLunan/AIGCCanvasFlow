package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_asset")
public class Asset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** character / style / music / storyboard / other */
    private String type;

    /** 文件 URL */
    private String url;

    /** 缩略图 URL */
    private String thumb;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件扩展名 */
    private String ext;

    /** 扩展元信息 JSON */
    private String meta;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
