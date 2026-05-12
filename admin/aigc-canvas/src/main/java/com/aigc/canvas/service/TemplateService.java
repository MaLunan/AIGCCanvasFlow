package com.aigc.canvas.service;

import com.aigc.canvas.dto.TemplateVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 模板服务接口：提供模板列表、详情查询和基于模板创建项目能力
 * 模板列表为公开接口（无需登录），使用模板创建项目需要登录
 * 实现类：TemplateServiceImpl
 */
public interface TemplateService {
    /** 分页查询模板列表，hot=true 时只返回推荐模板，category 为分类筛选 */
    Page<TemplateVO> page(int current, int size, String category, Boolean hot);

    /** 查询模板详情（含完整 canvasData，供前端预览/加载画布用） */
    TemplateVO getById(Long id);

    /** 使用模板：基于模板创建新项目，返回新项目 ID */
    Long useTemplate(Long templateId, Long userId, String projectName);
}
