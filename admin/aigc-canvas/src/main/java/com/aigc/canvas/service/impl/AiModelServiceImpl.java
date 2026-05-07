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

@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final ModelKeysConfig modelKeysConfig;
    private final UserModelLibraryService userModelLibraryService;

    @Override
    public List<AiModelVO> list(String category, Long userId) {
        Set<String> libraryKeys = userId != null
                ? userModelLibraryService.getLibraryModelKeys(userId)
                : Set.of();

        return modelKeysConfig.getModels().entrySet().stream()
                .filter(e -> !StringUtils.hasText(category) || category.equals(e.getValue().getCategory()))
                .sorted(Comparator.comparing((java.util.Map.Entry<String, ModelKeysConfig.ModelEntry> e) ->
                                e.getValue().getCategory() == null ? "" : e.getValue().getCategory())
                        .thenComparing(java.util.Map.Entry::getKey))
                .map(e -> toVO(e.getKey(), e.getValue(), libraryKeys.contains(e.getKey())))
                .toList();
    }

    @Override
    public AiModelVO getByKey(String modelKey) {
        ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(modelKey);
        if (entry == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return toVO(modelKey, entry, false);
    }

    private AiModelVO toVO(String modelKey, ModelKeysConfig.ModelEntry e, boolean inLibrary) {
        AiModelVO vo = new AiModelVO();
        vo.setModelKey(modelKey);
        vo.setName(e.getName());
        vo.setProvider(e.getProvider());
        vo.setType(e.getType());
        vo.setCategory(e.getCategory());
        vo.setDescription(e.getDescription());
        vo.setIcon(e.getIcon());
        vo.setColor(e.getColor());
        vo.setCostPoints(e.getCostPoints());
        vo.setTags(e.tagList());
        vo.setSupportAspects(e.aspectList());
        vo.setSupportDurations(e.durationList());
        vo.setSupportResolutions(e.resolutionList());
        vo.setInLibrary(inLibrary);
        return vo;
    }
}
