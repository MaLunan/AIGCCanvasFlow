package com.aigc.canvas.dto;

import lombok.Data;
import java.util.Arrays;
import java.util.List;

@Data
public class AiModelVO {
    private Long id;
    private String name;
    private String provider;
    private String type;
    private String modelKey;
    private List<String> supportAspects;
    private List<String> supportDurations;
    private List<String> supportResolutions;
    private Integer costPoints;
    private String description;
}
