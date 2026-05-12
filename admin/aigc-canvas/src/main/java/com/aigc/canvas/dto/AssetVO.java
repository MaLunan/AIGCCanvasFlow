package com.aigc.canvas.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资产视图 VO：文件上传后返回给前端
 * url 字段可直接用于 ImageNode/VideoNode 的 data.url 属性
 */
@Data
public class AssetVO {
    /** 资产 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 资产名称（原始文件名或用户自定义名称） */
    private String name;
    /** 资产类型（character / style / music / storyboard / other） */
    private String type;
    /** 文件访问 URL（完整 HTTP 地址，可直接嵌入 img src 或 video src） */
    private String url;
    /** 缩略图 URL（图片类型与 url 相同） */
    private String thumb;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 文件扩展名（不含点号，如 mp4、jpg） */
    private String ext;
    /** 上传时间 */
    private LocalDateTime createTime;
}
