package com.aigc.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCustomModelRequest {

    @NotBlank(message = "模型名称不能为空")
    private String name;

    private String category;

    private String description;

    private String apiEndpoint;

    /** 传空字符串表示不修改 Key */
    private String apiKey;

    private String icon;

    private String color;
}
