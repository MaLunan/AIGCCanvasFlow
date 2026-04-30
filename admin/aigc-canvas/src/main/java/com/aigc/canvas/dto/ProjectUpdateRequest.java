package com.aigc.canvas.dto;

import lombok.Data;

@Data
public class ProjectUpdateRequest {
    private String name;
    private String cover;
    private String category;
    private String canvasData;
    private Integer frameCount;
    private Integer status;
}
