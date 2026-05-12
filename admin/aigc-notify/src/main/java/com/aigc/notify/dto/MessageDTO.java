package com.aigc.notify.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 消息数据传输对象（发送给前端/WebSocket 推送）
 * - createTime 使用 String 类型（格式化后的字符串），避免前端时区转换问题
 * - 不包含 receiverId（接收者自己知道自己的 ID）
 */
@Data
@Builder
public class MessageDTO {
    /** 消息 ID */
    private Long id;
    /** 发送者 ID（null=系统消息） */
    private Long senderId;
    /** 消息类型：system / business / alert */
    private String type;
    /** 消息标题 */
    private String title;
    /** 消息正文 */
    private String content;
    /** 扩展数据 JSON（如关联的 projectId、taskId，供前端点击跳转） */
    private String extra;
    /** 已读状态：0=未读，1=已读 */
    private Integer isRead;
    /** 消息时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String createTime;
}
