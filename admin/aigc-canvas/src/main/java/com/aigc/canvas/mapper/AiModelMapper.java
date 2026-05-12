package com.aigc.canvas.mapper;

import com.aigc.canvas.entity.AiModel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型 Mapper（对应 t_ai_model 表）
 * 注意：当前版本模型数据已迁移至 Nacos 配置（ModelKeysConfig），
 * 此 Mapper 暂时保留，未来可用于数据库存储模型数据的扩展场景
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {
}
