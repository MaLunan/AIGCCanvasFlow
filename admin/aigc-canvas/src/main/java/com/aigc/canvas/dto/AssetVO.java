package com.aigc.canvas.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssetVO {
    private Long id;
    private Long userId;
    private String name;
    private String type;
    private String url;
    private String thumb;
    private Long fileSize;
    private String ext;
    private LocalDateTime createTime;
}
