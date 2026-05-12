package com.aigc.canvas.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目视图 VO：对外返回的项目信息
 * 包含 canvasData 字段（详情接口使用）
 * 列表接口中 canvasData 不为 null，但前端列表页不使用此字段（由画布编辑页按需加载）
 */
@Data
public class ProjectVO {
    /** 项目 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 项目名称 */
    private String name;
    /** 封面图 URL */
    private String cover;
    /** 项目分类 */
    private String category;
    /** 节点数量（前端统计后存入） */
    private Integer frameCount;
    /** 状态：0=草稿，1=已发布 */
    private Integer status;
    /** VueFlow 画布 JSON 数据 */
    private String canvasData;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 最后更新时间（用于前端显示"X 分钟前保存"） */
    private LocalDateTime updateTime;
}
