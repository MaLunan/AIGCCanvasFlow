package com.aigc.canvas.mapper;

import com.aigc.canvas.entity.Asset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产 Mapper：继承 BaseMapper，提供自动 CRUD
 * 资产文件存储在磁盘，此 Mapper 只管理元数据（URL、类型、大小等）
 */
@Mapper
public interface AssetMapper extends BaseMapper<Asset> {
}
