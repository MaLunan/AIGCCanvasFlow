package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 画布模板实体：对应数据库表 t_template
 * 平台预置的项目模板，用户可以基于模板一键创建项目（复制画布数据）
 */
@Data
@TableName("t_template")
public class Template {

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 模板名称（展示在模板广场） */
    private String name;

    /** 模板分类：short_drama（短剧）/ oral（口播）/ ad（广告）/ mv / vlog / edu（教育）/ other */
    private String category;

    /** 封面图 URL（模板列表卡片缩略图） */
    private String cover;

    /** 模板描述（介绍模板的用途和特点） */
    private String description;

    /**
     * 模板画布 JSON 数据（VueFlow 格式）
     * 用户选择模板后，将此数据复制到新项目的 canvasData 字段
     */
    private String canvasData;

    /** 使用次数（每次 useTemplate 时 +1，用于模板排序和热门统计） */
    private Integer useCount;

    /** 是否热门标记：0=普通，1=热门（在模板列表中优先展示） */
    private Integer hot;

    /** 上线状态：0=下线（不展示），1=上线 */
    private Integer status;

    /** 创建时间（INSERT 时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间（INSERT 和 UPDATE 时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
