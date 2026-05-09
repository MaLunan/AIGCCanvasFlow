package com.aigc.canvas.controller;

import com.aigc.canvas.dto.AssetVO;
import com.aigc.canvas.service.AssetService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/canvas/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @Value("${file.storage.path}")
    private String storagePath;

    /** 分页查询资产列表，type: character / style / music / storyboard */
    @GetMapping
    public R<Page<AssetVO>> page(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "current", defaultValue = "1")  int current,
            @RequestParam(value = "size",    defaultValue = "20") int size,
            @RequestParam(value = "type",    required = false)    String type) {
        return R.ok(assetService.page(userId, current, size, type));
    }

    /** 上传资产文件 */
    @PostMapping
    public R<AssetVO> upload(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", defaultValue = "other") String type,
            @RequestPart("file") MultipartFile file) {
        return R.ok(assetService.upload(userId, name, type, file));
    }

    /** 访问已上传的文件（无需认证） */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws MalformedURLException {
        Path file = Paths.get(storagePath).resolve(filename).normalize();
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
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
