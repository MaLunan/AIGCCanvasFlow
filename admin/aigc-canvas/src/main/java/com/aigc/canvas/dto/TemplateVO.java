package com.aigc.canvas.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TemplateVO {
    private Long id;
    private String name;
    private String category;
    private String cover;
    private String description;
    private Integer useCount;
    private Integer hot;
    private LocalDateTime createTime;
}
