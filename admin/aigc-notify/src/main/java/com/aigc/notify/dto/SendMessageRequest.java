package com.aigc.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull(message = "接收者不能为空")
    private Long receiverId;
    private Long senderId;
    @NotBlank(message = "消息类型不能为空")
    private String type;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    private String extra;
}
