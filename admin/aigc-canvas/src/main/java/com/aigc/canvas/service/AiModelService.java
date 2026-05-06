package com.aigc.canvas.service;

import com.aigc.canvas.dto.AiModelVO;

import java.util.List;

public interface AiModelService {
    /** 查询广场模型列表，category 为中文分类，userId 非空时标注 inLibrary */
    List<AiModelVO> list(String category, Long userId);

    AiModelVO getByKey(String modelKey);
}
