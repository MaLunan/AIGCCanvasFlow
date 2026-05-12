package com.aigc.canvas.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体：对应数据库表 t_project
 * 每个项目代表一个 AI 创作工作流画布，存储节点/边的 JSON 数据
 */
@Data
@TableName("t_project")
public class Project {

    /** 主键 ID，使用 TimestampIdGenerator 生成 19 位时间戳 ID（ASSIGN_ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建者用户 ID */
    private Long userId;

    /** 项目名称（用户自定义） */
    private String name;

    /** 封面图 URL（项目列表卡片缩略图） */
    private String cover;

    /** 项目分类：short_drama（短剧）/ oral（口播）/ ad（广告）/ mv / vlog / other */
    private String category;

    /**
     * VueFlow 画布 JSON 数据（{ nodes: [], edges: [], viewport: {} }）
     * 前端自动保存（1.5s 防抖），存储为原始 JSON 字符串，不做解析
     */
    private String canvasData;

    /** 画布节点数量（分镜数，由前端计数后传入，作为列表卡片的展示信息） */
    private Integer frameCount;

    /** 项目状态：0=草稿，1=已发布 */
    private Integer status;

    /** 创建时间（由 MetaObjectFillHandler 自动填充，INSERT 时触发） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间（由 MetaObjectFillHandler 自动填充，INSERT 和 UPDATE 时触发） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0=正常，1=已删除（MyBatis-Plus @TableLogic 自动处理） */
    @TableLogic
    private Integer deleted;
}
