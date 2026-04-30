package com.aigc.notify.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDTO {
    private Long id;
    private Long senderId;
    private String type;
    private String title;
    private String content;
    private String extra;
    private Integer isRead;
    private String createTime;
}
