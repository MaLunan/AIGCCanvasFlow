package com.aigc.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCustomModelRequest {

    @NotBlank(message = "模型名称不能为空")
    private String name;

    /** 视频/图像/音频/文本 */
    private String category;

    private String description;

    private String apiEndpoint;

    private String apiKey;

    private String icon;

    private String color;
}
