package com.aigc.canvas.service.impl;

import com.aigc.canvas.config.ModelKeysConfig;
import com.aigc.canvas.dto.AiModelVO;
import com.aigc.canvas.entity.AiModel;
import com.aigc.canvas.mapper.AiModelMapper;
import com.aigc.canvas.service.AiModelService;
import com.aigc.canvas.service.UserModelLibraryService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final AiModelMapper aiModelMapper;
    private final UserModelLibraryService userModelLibraryService;
    private final ModelKeysConfig modelKeysConfig;

    @Override
    public List<AiModelVO> list(String category, Long userId) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, 1)
                .eq(StringUtils.hasText(category), AiModel::getCategory, category)
                .orderByAsc(AiModel::getCategory, AiModel::getId);

        List<AiModel> models = aiModelMapper.selectList(wrapper);

        // 只展示已在 Nacos 中配置了凭证的模型
        Set<String> configuredKeys = modelKeysConfig.getModelKeys().keySet();
        models = models.stream()
                .filter(m -> configuredKeys.contains(m.getModelKey()))
                .toList();

        // 若已登录，标注 inLibrary
        Set<Long> libraryIds = userId != null
                ? userModelLibraryService.getLibraryModelIds(userId)
                : Set.of();

        return models.stream()
                .map(m -> toVO(m, libraryIds.contains(m.getId())))
                .toList();
    }

    @Override
    public AiModelVO getByKey(String modelKey) {
        AiModel model = aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getModelKey, modelKey)
                        .eq(AiModel::getStatus, 1));
        if (model == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return toVO(model, false);
    }

    private AiModelVO toVO(AiModel m, boolean inLibrary) {
        AiModelVO vo = new AiModelVO();
        vo.setId(m.getId());
        vo.setName(m.getName());
        vo.setProvider(m.getProvider());
        vo.setType(m.getType());
        vo.setCategory(m.getCategory());
        vo.setModelKey(m.getModelKey());
        vo.setCostPoints(m.getCostPoints());
        vo.setDescription(m.getDescription());
        vo.setIcon(m.getIcon());
        vo.setColor(m.getColor());
        vo.setInLibrary(inLibrary);
        if (StringUtils.hasText(m.getSupportAspects()))
            vo.setSupportAspects(Arrays.asList(m.getSupportAspects().split(",")));
        if (StringUtils.hasText(m.getSupportDurations()))
            vo.setSupportDurations(Arrays.asList(m.getSupportDurations().split(",")));
        if (StringUtils.hasText(m.getSupportResolutions()))
            vo.setSupportResolutions(Arrays.asList(m.getSupportResolutions().split(",")));
        if (StringUtils.hasText(m.getTags()))
            vo.setTags(Arrays.asList(m.getTags().split(",")));
        return vo;
    }
}
