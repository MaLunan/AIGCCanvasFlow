package com.aigc.canvas.service;

import com.aigc.canvas.dto.TemplateVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface TemplateService {
    Page<TemplateVO> page(int current, int size, String category, Boolean hot);
    TemplateVO getById(Long id);
    /** 使用模板：基于模板创建新项目 */
    Long useTemplate(Long templateId, Long userId, String projectName);
}
