package com.aigc.canvas.service;

import com.aigc.canvas.dto.AiModelVO;

import java.util.List;

public interface AiModelService {
    List<AiModelVO> list(String type);
    AiModelVO getByKey(String modelKey);
}
