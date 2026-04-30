package com.aigc.notify.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接收者 userId */
    private Long receiverId;
    /** 发送者 userId，null 表示系统消息 */
    private Long senderId;
    /** system/business/alert */
    private String type;
    private String title;
    private String content;
    /** 扩展数据（JSON） */
    private String extra;
    /** 0-未读 1-已读 */
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
