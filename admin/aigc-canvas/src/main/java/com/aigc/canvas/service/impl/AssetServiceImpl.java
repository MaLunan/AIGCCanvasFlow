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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetMapper assetMapper;

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

        // TODO: 替换为实际的 OSS/S3 上传逻辑
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                : "";
        String fakeUrl = "/assets/" + UUID.randomUUID() + "." + ext;

        Asset asset = new Asset();
        asset.setUserId(userId);
        asset.setName(StringUtils.hasText(name) ? name : originalFilename);
        asset.setType(StringUtils.hasText(type) ? type : "other");
        asset.setUrl(fakeUrl);
        asset.setThumb(fakeUrl);
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
