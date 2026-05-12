package com.aigc.canvas.service.impl;

import com.aigc.canvas.dto.AssetVO;
import com.aigc.canvas.entity.Asset;
import com.aigc.canvas.mapper.AssetMapper;
import com.aigc.canvas.service.AssetService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 资产服务实现：文件上传存储、资产 CRUD
 * 文件存储在本地磁盘（storagePath），通过 /canvas/assets/files/{filename} 静态端点访问
 * 文件名使用 UUID 随机化，避免文件名冲突和路径遍历攻击
 */
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetMapper assetMapper;

    /** 文件存储根目录（如 /data/aigc/files），由配置注入 */
    @Value("${file.storage.path}")
    private String storagePath;

    /** 文件访问 Base URL（如 http://localhost:8080），拼接为完整访问地址 */
    @Value("${file.storage.base-url}")
    private String baseUrl;

    /**
     * 分页查询用户资产列表
     * 按 createTime 倒序（最新上传的在前），支持按 type 过滤
     */
    @Override
    public Page<AssetVO> page(Long userId, int current, int size, String type) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .eq(Asset::getUserId, userId)
                // type 有值时加过滤条件，无值时查全部类型
                .eq(StringUtils.hasText(type), Asset::getType, type)
                .orderByDesc(Asset::getCreateTime);
        Page<Asset> dbPage = assetMapper.selectPage(new Page<>(current, size), wrapper);
        Page<AssetVO> voPage = new Page<>(dbPage.getCurrent(), dbPage.getSize(), dbPage.getTotal());
        voPage.setRecords(dbPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 上传资产文件
     * - 提取原始文件扩展名，拼接 UUID 生成唯一文件名（防冲突）
     * - 使用 Files.createDirectories 确保目录存在（幂等操作）
     * - 访问 URL = baseUrl + /canvas/assets/files/ + 随机文件名
     * - thumb 与 url 相同（图片类型可由前端自行生成缩略图）
     */
    @Override
    public AssetVO upload(Long userId, String name, String type, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(400, "文件不能为空");

        // 提取扩展名（如 mp4、jpg），用于保留 MIME 类型推断能力
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                : "";
        // UUID + 扩展名作为存储文件名，防止用户上传恶意文件名
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);  // 目录不存在时自动创建（包含父目录）
            file.transferTo(dir.resolve(filename));  // 将上传的文件写入磁盘
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }

        // 拼接完整访问 URL（经过网关白名单，无需登录即可访问）
        String fileUrl = baseUrl + "/canvas/assets/files/" + filename;

        // 创建资产记录，name 使用用户传入值（无则用原始文件名）
        Asset asset = new Asset();
        asset.setUserId(userId);
        asset.setName(StringUtils.hasText(name) ? name : originalFilename);
        asset.setType(StringUtils.hasText(type) ? type : "other");
        asset.setUrl(fileUrl);   // 原始文件访问地址
        asset.setThumb(fileUrl); // 缩略图（暂与原图相同，可后续接 OSS 处理）
        asset.setFileSize(file.getSize());
        asset.setExt(ext);       // 存储扩展名方便文件类型筛选
        assetMapper.insert(asset);
        return toVO(asset);
    }

    /**
     * 删除资产记录（软删除/硬删除由 MyBatis-Plus 配置决定）
     * 注意：本方法只删除数据库记录，不删除磁盘文件（文件清理需额外任务）
     */
    @Override
    public void delete(Long id, Long userId) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) throw new BusinessException(ResultCode.NOT_FOUND);
        // 校验归属，防止用户删除他人上传的资产
        if (!asset.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        assetMapper.deleteById(id);
    }

    /** Asset Entity → AssetVO 转换 */
    private AssetVO toVO(Asset a) {
        AssetVO vo = new AssetVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }
}
