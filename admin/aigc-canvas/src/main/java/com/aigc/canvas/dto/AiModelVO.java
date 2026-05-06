package com.aigc.canvas.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiModelVO {
    private Long id;
    private String name;
    private String provider;
    /** video / image / audio / text */
    private String type;
    /** 中文分类：视频/图像/音频/文本 */
    private String category;
    private String modelKey;
    private List<String> supportAspects;
    private List<String> supportDurations;
    private List<String> supportResolutions;
    private Integer costPoints;
    private String description;
    private List<String> tags;
    private String icon;
    private String color;
    /** 当前用户是否已加入模型库（需登录时才有意义） */
    private Boolean inLibrary;
}
