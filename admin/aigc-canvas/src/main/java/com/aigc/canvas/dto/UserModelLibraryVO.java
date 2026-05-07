package com.aigc.canvas.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserModelLibraryVO {
    private Long id;
    /** 平台模型 key（对应 Nacos model_key），自定义时为 null */
    private String modelKey;
    private Boolean isCustom;
    private String name;
    private String provider;
    private String category;
    private String description;
    private String apiEndpoint;
    /** 展示时脱敏：返回前端时只保留前4位 + **** */
    private String apiKeyMasked;
    private String icon;
    private String color;
    private Boolean enabled;
    private LocalDateTime createTime;
    /** 标签列表（平台模型携带，自定义模型固定为["自定义"]） */
    private java.util.List<String> tags;
}
