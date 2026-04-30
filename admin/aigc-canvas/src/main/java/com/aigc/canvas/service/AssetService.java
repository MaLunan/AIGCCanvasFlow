package com.aigc.canvas.service;

import com.aigc.canvas.dto.AssetVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

public interface AssetService {
    Page<AssetVO> page(Long userId, int current, int size, String type);
    AssetVO upload(Long userId, String name, String type, MultipartFile file);
    void delete(Long id, Long userId);
}
