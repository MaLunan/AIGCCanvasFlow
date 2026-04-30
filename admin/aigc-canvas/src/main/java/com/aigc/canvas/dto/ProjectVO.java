package com.aigc.canvas.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectVO {
    private Long id;
    private Long userId;
    private String name;
    private String cover;
    private String category;
    private Integer frameCount;
    private Integer status;
    private String canvasData;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
