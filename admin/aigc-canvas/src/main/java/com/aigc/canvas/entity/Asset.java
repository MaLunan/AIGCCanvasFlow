package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产实体：对应数据库表 t_asset
 * 存储用户上传的媒体文件（图片、视频、音频等）的元数据
 * 文件内容存储在本地磁盘（storagePath），通过 /canvas/assets/files/{filename} 访问
 */
@Data
@TableName("t_asset")
public class Asset {

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 资产名称（用户自定义或原始文件名） */
    private String name;

    /** 资产类型：character（角色）/ style（风格）/ music（音乐）/ storyboard（分镜）/ other */
    private String type;

    /** 文件访问 URL（完整 HTTP 地址，如 http://localhost:8080/canvas/assets/files/uuid.mp4） */
    private String url;

    /** 缩略图 URL（暂与 url 相同，可后续接 OSS 图片处理生成缩略图） */
    private String thumb;

    /** 文件大小（字节，用于显示文件大小信息） */
    private Long fileSize;

    /** 文件扩展名（如 mp4、jpg、png，不含点号） */
    private String ext;

    /** 扩展元信息 JSON（预留字段，如视频时长、分辨率等） */
    private String meta;

    /** 上传时间（INSERT 时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
