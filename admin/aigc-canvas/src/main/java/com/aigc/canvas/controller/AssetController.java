package com.aigc.canvas.controller;

import com.aigc.canvas.dto.AssetVO;
import com.aigc.canvas.service.AssetService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/canvas/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    /** 分页查询资产列表，type: character / style / music / storyboard */
    @GetMapping
    public R<Page<AssetVO>> page(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(defaultValue = "1")  int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String type) {
        return R.ok(assetService.page(userId, current, size, type));
    }

    /** 上传资产文件 */
    @PostMapping
    public R<AssetVO> upload(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "other") String type,
            @RequestPart("file") MultipartFile file) {
        return R.ok(assetService.upload(userId, name, type, file));
    }

    /** 删除资产 */
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        assetService.delete(id, userId);
        return R.ok();
    }
}
