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
     */
    @PostMapping("/library/list")
    public R<List<UserModelLibraryVO>> libraryList(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        return R.ok(userModelLibraryService.listByUser(userId));
    }

    /**
     * 从广场添加平台模型到库
     * POST /canvas/models/library/add-market
     * param: modelId
     */
    @PostMapping("/library/add-market")
    public R<UserModelLibraryVO> addFromMarket(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long modelId) {
        return R.ok(userModelLibraryService.addFromMarket(userId, modelId));
    }

    /**
     * 添加自定义模型到库
     * POST /canvas/models/library/add-custom
     */
    @PostMapping("/library/add-custom")
    public R<UserModelLibraryVO> addCustom(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @Valid @RequestBody AddCustomModelRequest request) {
        return R.ok(userModelLibraryService.addCustom(userId, request));
    }

    /**
     * 更新自定义模型
     * POST /canvas/models/library/update-custom
     * param: libraryId
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
     * POST /canvas/models/library/toggle-enabled
     * param: libraryId
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
     * POST /canvas/models/library/remove
     * param: libraryId
     */
    @PostMapping("/library/remove")
    public R<Void> remove(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long libraryId) {
        userModelLibraryService.remove(userId, libraryId);
        return R.ok();
    }
}
