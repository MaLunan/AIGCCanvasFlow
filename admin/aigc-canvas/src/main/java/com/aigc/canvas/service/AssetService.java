package com.aigc.canvas.service;

import com.aigc.canvas.dto.AssetVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资产管理服务接口：提供用户上传文件的增删查能力
 * 文件存储：磁盘（上传目录由 AssetServiceImpl.uploadDir 配置）
 * 元数据存储：t_asset 表（URL、类型、大小等）
 * 实现类：AssetServiceImpl
 */
public interface AssetService {
    /** 分页查询用户资产列表，type 为资产类型（image/video/audio 等，不传则查全部） */
    Page<AssetVO> page(Long userId, int current, int size, String type);

    /** 上传文件：UUID 重命名防冲突，保存磁盘后写入元数据 */
    AssetVO upload(Long userId, String name, String type, MultipartFile file);

    /** 删除资产元数据（仅逻辑删除，磁盘文件保留） */
    void delete(Long id, Long userId);
}
