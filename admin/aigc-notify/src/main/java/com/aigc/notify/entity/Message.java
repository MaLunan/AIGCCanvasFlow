package com.aigc.notify.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体：对应数据库表 t_message
 * 存储系统通知/业务消息，支持单条已读和全部已读操作
 * 消息发送时同时触发 WebSocket 实时推送（用户在线时即时显示通知）
 */
@Data
@TableName("t_message")
public class Message {

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收者用户 ID */
    private Long receiverId;

    /** 发送者用户 ID，null 表示系统自动发送的消息（如 AI 任务完成通知） */
    private Long senderId;

    /** 消息类型：system（系统通知）/ business（业务消息）/ alert（告警） */
    private String type;

    /** 消息标题（展示在通知列表中） */
    private String title;

    /** 消息正文 */
    private String content;

    /** 扩展数据 JSON（如关联的项目 ID、任务 ID，供前端点击跳转） */
    private String extra;

    /** 已读状态：0=未读，1=已读 */
    private Integer isRead;

    /** 消息发送时间（INSERT 时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
