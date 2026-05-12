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

/**
 * 项目管理控制器：画布项目的 CRUD + 画布数据自动保存
 * 所有接口均需登录，userId 由网关从 JWT 中解析后通过 X-User-Id 头注入
 * 服务层会进一步校验该项目是否属于当前用户（防止越权访问）
 */
@RestController
@RequestMapping("/canvas/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** 创建项目：POST /canvas/projects */
    @PostMapping
    public R<ProjectVO> create(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @Valid @RequestBody ProjectCreateRequest request) {
        return R.ok(projectService.create(userId, request));
    }

    /** 获取项目详情：GET /canvas/projects/{id}（同时校验归属） */
    @GetMapping("/{id}")
    public R<ProjectVO> getById(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        return R.ok(projectService.getById(id, userId));
    }

    /**
     * 分页查询用户项目列表：GET /canvas/projects
     * category 可选过滤，按 updateTime 倒序排列（最近编辑的在前）
     */
    @GetMapping
    public R<Page<ProjectVO>> page(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "current", defaultValue = "1")  int current,
            @RequestParam(value = "size",    defaultValue = "12") int size,
            @RequestParam(value = "category", required = false)   String category) {
        return R.ok(projectService.page(userId, current, size, category));
    }

    /** 更新项目基本信息（名称/封面/分类/节点数等）：PUT /canvas/projects/{id} */
    @PutMapping("/{id}")
    public R<ProjectVO> update(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestBody ProjectUpdateRequest request) {
        return R.ok(projectService.update(id, userId, request));
    }

    /** 删除项目：DELETE /canvas/projects/{id} */
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        projectService.delete(id, userId);
        return R.ok();
    }

    /**
     * 保存画布数据（自动保存）：POST /canvas/projects/{id}/canvas
     * 前端 1.5s 防抖触发，请求体为原始 JSON 字符串（Content-Type: text/plain）
     * 直接存入 t_project.canvas_data 字段，无需解析 JSON 结构
     */
    @PostMapping("/{id}/canvas")
    public R<Void> saveCanvas(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestBody String canvasData) {
        projectService.saveCanvas(id, userId, canvasData);
        return R.ok();
    }
}
