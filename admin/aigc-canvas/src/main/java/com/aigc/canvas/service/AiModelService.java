package com.aigc.canvas.service;

import com.aigc.canvas.dto.AiModelVO;

import java.util.List;

/**
 * AI 模型广场服务接口：提供平台内置模型列表和模型详情查询
 * 模型数据来源：Nacos 动态配置（ModelKeysConfig），非数据库
 * 实现类：AiModelServiceImpl
 */
public interface AiModelService {
    /** 查询广场模型列表，category 为中文分类，userId 非空时标注 inLibrary */
    List<AiModelVO> list(String category, Long userId);

    /** 根据模型 key 查询单个模型详情（用于生成任务的模型信息展示） */
    AiModelVO getByKey(String modelKey);
}
