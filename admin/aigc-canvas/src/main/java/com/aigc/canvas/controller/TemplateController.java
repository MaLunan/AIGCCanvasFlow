package com.aigc.canvas.controller;

import com.aigc.canvas.dto.TemplateVO;
import com.aigc.canvas.service.TemplateService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 画布模板控制器：提供模板广场展示和使用模板创建项目的功能
 * 模板列表是公开接口（无需登录即可浏览），使用模板需要登录
 */
@RestController
@RequestMapping("/canvas/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    /**
     * 分页查询模板列表（公开接口）
     * GET /canvas/templates?category=short_drama&hot=true&current=1&size=12
     * - category：可选，按分类过滤（short_drama/oral/ad/mv/vlog/edu/other）
     * - hot：可选，true 只返回热门模板
     */
    @GetMapping
    public R<Page<TemplateVO>> page(
            @RequestParam(value = "current",  defaultValue = "1")  int current,
            @RequestParam(value = "size",     defaultValue = "12") int size,
            @RequestParam(value = "category", required = false)    String category,
            @RequestParam(value = "hot",      required = false)    Boolean hot) {
        return R.ok(templateService.page(current, size, category, hot));
    }

    /**
     * 获取模板详情（含完整画布 JSON 数据）
     * GET /canvas/templates/{id}
     */
    @GetMapping("/{id}")
    public R<TemplateVO> getById(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    /**
     * 使用模板创建新项目（需要登录）
     * POST /canvas/templates/{id}/use?projectName=xxx
     * 返回新创建项目的 ID，前端直接跳转到 /canvas?projectId={id}
     */
    @PostMapping("/{id}/use")
    public R<Long> useTemplate(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "projectName", required = false) String projectName) {
        return R.ok(templateService.useTemplate(id, userId, projectName));
    }
}
