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

/**
 * 资产管理控制器：用户上传的图片/视频/音频等文件的管理
 * - 文件存储在本地磁盘（storagePath 配置）
 * - /files/** 端点无需认证，直接提供文件下载（在网关白名单中）
 * - 其余接口需要登录
 */
@RestController
@RequestMapping("/canvas/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    /** 文件存储根目录（从配置注入，如 /data/aigc/files） */
    @Value("${file.storage.path}")
    private String storagePath;

    /**
     * 分页查询资产列表
     * GET /canvas/assets?type=character&current=1&size=20
     * type 可选：character（角色）/ style（风格）/ music（音乐）/ storyboard（分镜）/ other
     */
    @GetMapping
    public R<Page<AssetVO>> page(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "current", defaultValue = "1")  int current,
            @RequestParam(value = "size",    defaultValue = "20") int size,
            @RequestParam(value = "type",    required = false)    String type) {
        return R.ok(assetService.page(userId, current, size, type));
    }

    /**
     * 上传资产文件
     * POST /canvas/assets（multipart/form-data）
     * - file：文件二进制内容
     * - name：可选，文件展示名称（默认使用原始文件名）
     * - type：资产类型，默认 "other"
     * 返回 AssetVO 包含访问 URL，前端直接用于 ImageNode/VideoNode 的 url 字段
     */
    @PostMapping
    public R<AssetVO> upload(
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", defaultValue = "other") String type,
            @RequestPart("file") MultipartFile file) {
        return R.ok(assetService.upload(userId, name, type, file));
    }

    /**
     * 访问已上传的文件（无需认证，静态资源服务）
     * GET /canvas/assets/files/{filename}
     * 根据文件扩展名自动推断 Content-Type（图片/视频/音频等）
     * 文件不存在时返回 404
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws MalformedURLException {
        // resolve 拼接路径，normalize 防止路径穿越攻击（如 ../../../etc/passwd）
        Path file = Paths.get(storagePath).resolve(filename).normalize();
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        // 自动推断 MIME 类型（mp4 → video/mp4, jpg → image/jpeg 等）
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM); // 未知类型降级为二进制流
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    /** 删除资产：DELETE /canvas/assets/{id}（同时校验归属） */
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @PathVariable Long id,
            @RequestHeader(CommonConstants.HEADER_USER_ID) Long userId) {
        assetService.delete(id, userId);
        return R.ok();
    }
}
