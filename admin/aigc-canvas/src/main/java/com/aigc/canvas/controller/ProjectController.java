package com.aigc.canvas.controller;

import com.aigc.canvas.dto.ProjectCreateRequest;
import com.aigc.canvas.dto.ProjectUpdateRequest;
import com.aigc.canvas.dto.ProjectVO;
import com.aigc.canvas.service.ProjectService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/canvas/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** 创建项目 */
    @PostMapping
    public R<ProjectVO> create(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @Valid @RequestBody ProjectCreateRequest request) {
        return R.ok(projectService.create(userId, request));
    }

    /** 获取项目详情 */
    @GetMapping("/{id}")
    public R<ProjectVO> getById(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        return R.ok(projectService.getById(id, userId));
    }

    /** 分页查询用户项目列表 */
    @GetMapping
    public R<Page<ProjectVO>> page(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1")  int current,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false)    String category) {
        return R.ok(projectService.page(userId, current, size, category));
    }

    /** 更新项目基本信息 */
    @PutMapping("/{id}")
    public R<ProjectVO> update(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestBody ProjectUpdateRequest request) {
        return R.ok(projectService.update(id, userId, request));
    }

    /** 删除项目 */
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        projectService.delete(id, userId);
        return R.ok();
    }

    /** 保存画布数据（自动保存） */
    @PostMapping("/{id}/canvas")
    public R<Void> saveCanvas(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestBody String canvasData) {
        projectService.saveCanvas(id, userId, canvasData);
        return R.ok();
    }
}
