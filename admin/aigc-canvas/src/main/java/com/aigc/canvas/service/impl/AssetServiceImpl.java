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

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetMapper assetMapper;

    @Value("${file.storage.path}")
    private String storagePath;

    @Value("${file.storage.base-url}")
    private String baseUrl;

    @Override
    public Page<AssetVO> page(Long userId, int current, int size, String type) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .eq(Asset::getUserId, userId)
                .eq(StringUtils.hasText(type), Asset::getType, type)
                .orderByDesc(Asset::getCreateTime);
        Page<Asset> dbPage = assetMapper.selectPage(new Page<>(current, size), wrapper);
        Page<AssetVO> voPage = new Page<>(dbPage.getCurrent(), dbPage.getSize(), dbPage.getTotal());
        voPage.setRecords(dbPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public AssetVO upload(Long userId, String name, String type, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(400, "文件不能为空");

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                : "";
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }

        String fileUrl = baseUrl + "/canvas/assets/files/" + filename;

        Asset asset = new Asset();
        asset.setUserId(userId);
        asset.setName(StringUtils.hasText(name) ? name : originalFilename);
        asset.setType(StringUtils.hasText(type) ? type : "other");
        asset.setUrl(fileUrl);
        asset.setThumb(fileUrl);
        asset.setFileSize(file.getSize());
        asset.setExt(ext);
        assetMapper.insert(asset);
        return toVO(asset);
    }

    @Override
    public void delete(Long id, Long userId) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!asset.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        assetMapper.deleteById(id);
    }

    private AssetVO toVO(Asset a) {
        AssetVO vo = new AssetVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }
}
