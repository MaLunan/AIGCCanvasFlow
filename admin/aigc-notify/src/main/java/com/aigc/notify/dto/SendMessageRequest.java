package com.aigc.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求 DTO（内部调用接口，供其他服务发送通知使用）
 * 如 AI 任务完成后，aigc-canvas 调用此接口向用户推送通知
 */
@Data
public class SendMessageRequest {
    /** 接收者用户 ID（必填） */
    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    /** 发送者用户 ID（可选，null 表示系统消息） */
    private Long senderId;

    /** 消息类型（必填）：system（系统通知）/ business（业务消息）/ alert（告警） */
    @NotBlank(message = "消息类型不能为空")
    private String type;

    /** 消息标题（必填，展示在通知列表中） */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 消息正文（必填） */
    @NotBlank(message = "内容不能为空")
    private String content;

    /** 扩展数据 JSON（可选，如 {"projectId": "xxx", "taskId": "xxx"}） */
    private String extra;
}
