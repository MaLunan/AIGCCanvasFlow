package com.aigc.canvas.controller;

import com.aigc.canvas.dto.*;
import com.aigc.canvas.service.AiModelService;
import com.aigc.canvas.service.UserModelLibraryService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型控制器：模型广场（公开）+ 用户模型库（需登录）
 * - 模型广场：展示平台预置模型，登录时额外标记 inLibrary 字段
 * - 用户模型库：用户个人的模型管理，支持从广场添加、添加自定义模型、启用/禁用、删除
 */
@RestController
@RequestMapping("/canvas/models")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelService aiModelService;
    private final UserModelLibraryService userModelLibraryService;

    // ════════════════════ 模型广场 ════════════════════

    /**
     * 模型广场列表（公开，无需登录；登录时携带 inLibrary 标记）
     * POST /canvas/models/list
     * body: { "category": "视频" }（可选）
     * userId 为 optional，未登录时 inLibrary 全为 false
     * 使用 POST 是为了支持后续传递更复杂的过滤条件
     */
    @PostMapping("/list")
    public R<List<AiModelVO>> list(
            @RequestParam(value = "category", required = false) String category,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        return R.ok(aiModelService.list(category, userId));
    }

    // ════════════════════ 用户模型库 ════════════════════

    /**
     * 获取当前用户模型库
     * POST /canvas/models/library/list
     * 返回用户添加的所有模型（平台模型 + 自定义模型），API Key 已脱敏
     */
    @PostMapping("/library/list")
    public R<List<UserModelLibraryVO>> libraryList(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        return R.ok(userModelLibraryService.listByUser(userId));
    }

    /**
     * 从广场添加平台模型到库
     * POST /canvas/models/library/add-market?modelKey=xxx
     * 同一 modelKey 不可重复添加（服务层校验）
     */
    @PostMapping("/library/add-market")
    public R<UserModelLibraryVO> addFromMarket(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam String modelKey) {
        return R.ok(userModelLibraryService.addFromMarket(userId, modelKey));
    }

    /**
     * 添加自定义模型到库
     * POST /canvas/models/library/add-custom
     * 用户填写自己的 API Key、Base URL 和模型名称（如 OpenAI 兼容接口）
     */
    @PostMapping("/library/add-custom")
    public R<UserModelLibraryVO> addCustom(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @Valid @RequestBody AddCustomModelRequest request) {
        return R.ok(userModelLibraryService.addCustom(userId, request));
    }

    /**
     * 更新自定义模型
     * POST /canvas/models/library/update-custom?libraryId=xxx
     * 仅能修改自定义模型（isCustom=1），平台模型不支持编辑
     */
    @PostMapping("/library/update-custom")
    public R<UserModelLibraryVO> updateCustom(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long libraryId,
            @Valid @RequestBody UpdateCustomModelRequest request) {
        return R.ok(userModelLibraryService.updateCustom(userId, libraryId, request));
    }

    /**
     * 切换启用/禁用
     * POST /canvas/models/library/toggle-enabled?libraryId=xxx
     * 禁用后在 AI 生成选择器中不显示该模型
     */
    @PostMapping("/library/toggle-enabled")
    public R<Void> toggleEnabled(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long libraryId) {
        userModelLibraryService.toggleEnabled(userId, libraryId);
        return R.ok();
    }

    /**
     * 从库中移除
     * POST /canvas/models/library/remove?libraryId=xxx
     * 硬删除，移除后模型广场中对应模型的 inLibrary 恢复为 false
     */
    @PostMapping("/library/remove")
    public R<Void> remove(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long libraryId) {
        userModelLibraryService.remove(userId, libraryId);
        return R.ok();
    }
}
