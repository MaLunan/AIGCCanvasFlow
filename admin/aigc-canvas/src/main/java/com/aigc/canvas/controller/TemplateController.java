package com.aigc.canvas.controller;

import com.aigc.canvas.dto.TemplateVO;
import com.aigc.canvas.service.TemplateService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/canvas/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    /** 分页查询模板列表 */
    @GetMapping
    public R<Page<TemplateVO>> page(
            @RequestParam(defaultValue = "1")  int current,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false)    String category,
            @RequestParam(required = false)    Boolean hot) {
        return R.ok(templateService.page(current, size, category, hot));
    }

    /** 获取模板详情（含 canvasData） */
    @GetMapping("/{id}")
    public R<TemplateVO> getById(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    /** 使用模板创建项目 */
    @PostMapping("/{id}/use")
    public R<Long> useTemplate(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(required = false) String projectName) {
        return R.ok(templateService.useTemplate(id, userId, projectName));
    }
}
