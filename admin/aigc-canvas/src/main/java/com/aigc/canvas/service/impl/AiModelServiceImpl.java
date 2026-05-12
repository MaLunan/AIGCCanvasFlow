package com.aigc.canvas.service.impl;

import com.aigc.canvas.config.ModelKeysConfig;
import com.aigc.canvas.dto.AiModelVO;
import com.aigc.canvas.service.AiModelService;
import com.aigc.canvas.service.UserModelLibraryService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * AI 模型服务实现：从 Nacos 动态配置（ModelKeysConfig）中读取平台预置模型
 * 模型数据存储在 Nacos 配置中心，而非数据库，方便运营随时更新模型列表
 */
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    // Nacos 动态配置：key=modelKey，value=ModelEntry（名称、提供商、类型等元数据）
    private final ModelKeysConfig modelKeysConfig;
    // 用于查询用户库中已添加的 modelKey 集合（标记 inLibrary）
    private final UserModelLibraryService userModelLibraryService;

    /**
     * 查询模型广场列表
     * - 未登录（userId=null）时 inLibrary 全为 false
     * - 登录时通过 getLibraryModelKeys 获取用户库中的 modelKey Set，O(1) 判断 inLibrary
     * - 按 category 字母序、modelKey 字母序双排序（保证前端展示顺序稳定）
     */
    @Override
    public List<AiModelVO> list(String category, Long userId) {
        // 查询用户模型库中的 modelKey 集合，用于标记 inLibrary；未登录时返回空 Set
        Set<String> libraryKeys = userId != null
                ? userModelLibraryService.getLibraryModelKeys(userId)
                : Set.of();

        return modelKeysConfig.getModels().entrySet().stream()
                // category 有值时按类型过滤，否则返回全部模型
                .filter(e -> !StringUtils.hasText(category) || category.equals(e.getValue().getCategory()))
                // 先按 category 字母序，再按 modelKey 字母序，保证列表顺序稳定
                .sorted(Comparator.comparing((java.util.Map.Entry<String, ModelKeysConfig.ModelEntry> e) ->
                                e.getValue().getCategory() == null ? "" : e.getValue().getCategory())
                        .thenComparing(java.util.Map.Entry::getKey))
                // inLibrary = libraryKeys 中是否包含该 modelKey（Set.contains 为 O(1)）
                .map(e -> toVO(e.getKey(), e.getValue(), libraryKeys.contains(e.getKey())))
                .toList();
    }

    /** 根据 modelKey 查询单个模型详情（内部调用，inLibrary 恒为 false） */
    @Override
    public AiModelVO getByKey(String modelKey) {
        ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(modelKey);
        if (entry == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return toVO(modelKey, entry, false);
    }

    /**
     * ModelEntry → AiModelVO 转换
     * 将 Nacos 配置中的模型元数据映射为前端消费的 VO 格式
     * inLibrary 标记该模型是否已被当前用户添加到个人库
     */
    private AiModelVO toVO(String modelKey, ModelKeysConfig.ModelEntry e, boolean inLibrary) {
        AiModelVO vo = new AiModelVO();
        vo.setModelKey(modelKey);
        vo.setName(e.getName());
        vo.setProvider(e.getProvider());         // 提供商（OpenAI、Stability AI 等）
        vo.setType(e.getType());                 // 模型类型（image/video/text）
        vo.setCategory(e.getCategory());         // 展示分类（图片/视频）
        vo.setDescription(e.getDescription());
        vo.setIcon(e.getIcon());                 // 模型图标 emoji
        vo.setColor(e.getColor());               // 模型主题色（用于卡片渐变背景）
        vo.setCostPoints(e.getCostPoints());     // 积分消耗（预留字段）
        vo.setTags(e.tagList());                 // 标签列表（从逗号分隔字符串转换）
        vo.setSupportAspects(e.aspectList());    // 支持的宽高比列表
        vo.setSupportDurations(e.durationList()); // 支持的视频时长列表
        vo.setSupportResolutions(e.resolutionList()); // 支持的分辨率列表
        vo.setInLibrary(inLibrary);              // 当前用户是否已添加到库
        return vo;
    }
}
