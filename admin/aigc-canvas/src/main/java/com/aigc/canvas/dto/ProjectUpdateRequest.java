package com.aigc.canvas.dto;

import lombok.Data;

/**
 * 更新项目请求 DTO（部分更新，所有字段均为可选）
 * Service 层对各字段做非空判断（null 字段不覆盖原值）
 */
@Data
public class ProjectUpdateRequest {
    /** 项目名称（修改时传入） */
    private String name;
    /** 封面图 URL */
    private String cover;
    /** 项目分类 */
    private String category;
    /** 画布 JSON 数据（仅在批量更新场景使用，通常通过 saveCanvas 接口单独保存） */
    private String canvasData;
    /** 节点数量（前端统计画布中的节点数后传入） */
    private Integer frameCount;
    /** 项目状态：0=草稿，1=已发布 */
    private Integer status;
}
