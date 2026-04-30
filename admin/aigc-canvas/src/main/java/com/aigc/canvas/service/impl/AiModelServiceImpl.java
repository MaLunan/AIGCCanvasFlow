package com.aigc.canvas.service.impl;

import com.aigc.canvas.dto.AiModelVO;
import com.aigc.canvas.entity.AiModel;
import com.aigc.canvas.mapper.AiModelMapper;
import com.aigc.canvas.service.AiModelService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final AiModelMapper aiModelMapper;

    @Override
    public List<AiModelVO> list(String type) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, 1)
                .eq(StringUtils.hasText(type), AiModel::getType, type)
                .orderByAsc(AiModel::getType, AiModel::getId);
        return aiModelMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public AiModelVO getByKey(String modelKey) {
        AiModel model = aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getModelKey, modelKey)
                        .eq(AiModel::getStatus, 1));
        if (model == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return toVO(model);
    }

    private AiModelVO toVO(AiModel m) {
        AiModelVO vo = new AiModelVO();
        BeanUtils.copyProperties(m, vo);
        if (StringUtils.hasText(m.getSupportAspects()))
            vo.setSupportAspects(Arrays.asList(m.getSupportAspects().split(",")));
        if (StringUtils.hasText(m.getSupportDurations()))
            vo.setSupportDurations(Arrays.asList(m.getSupportDurations().split(",")));
        if (StringUtils.hasText(m.getSupportResolutions()))
            vo.setSupportResolutions(Arrays.asList(m.getSupportResolutions().split(",")));
        return vo;
    }
}
