package com.aigc.canvas.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模板视图 VO（列表页不含 canvasData，节省传输量）
 * 获取详情时通过 getById 接口单独获取，包含完整 canvasData
 */
@Data
public class TemplateVO {
    /** 模板 ID */
    private Long id;
    /** 模板名称 */
    private String name;
    /** 分类（short_drama / oral / ad / mv / vlog / edu / other） */
    private String category;
    /** 封面图 URL */
    private String cover;
    /** 模板描述 */
    private String description;
    /** 使用次数（排行榜数据） */
    private Integer useCount;
    /** 是否热门：0=否，1=是 */
    private Integer hot;
    /** 创建时间 */
    private LocalDateTime createTime;
}
