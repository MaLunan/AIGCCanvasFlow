package com.aigc.canvas.service.impl;

import com.aigc.canvas.dto.AddCustomModelRequest;
import com.aigc.canvas.dto.UpdateCustomModelRequest;
import com.aigc.canvas.dto.UserModelLibraryVO;
import com.aigc.canvas.entity.AiModel;
import com.aigc.canvas.entity.UserModelLibrary;
import com.aigc.canvas.mapper.AiModelMapper;
import com.aigc.canvas.mapper.UserModelLibraryMapper;
import com.aigc.canvas.service.UserModelLibraryService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserModelLibraryServiceImpl implements UserModelLibraryService {

    private final UserModelLibraryMapper libraryMapper;
    private final AiModelMapper aiModelMapper;

    @Override
    public List<UserModelLibraryVO> listByUser(Long userId) {
        List<UserModelLibrary> entries = libraryMapper.selectList(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .eq(UserModelLibrary::getUserId, userId)
                        .orderByDesc(UserModelLibrary::getCreateTime));
        if (entries.isEmpty()) return Collections.emptyList();

        // 一次性批量加载所有平台模型，避免 N+1
        Set<Long> modelIds = entries.stream()
                .filter(e -> e.getIsCustom() == 0 && e.getModelId() != null)
                .map(UserModelLibrary::getModelId)
                .collect(Collectors.toSet());
        Map<Long, AiModel> modelMap = modelIds.isEmpty() ? Collections.emptyMap()
                : aiModelMapper.selectBatchIds(modelIds).stream()
                        .collect(Collectors.toMap(AiModel::getId, Function.identity()));

        return entries.stream().map(e -> toVO(e, modelMap)).toList();
    }

    @Override
    public Set<Long> getLibraryModelIds(Long userId) {
        return libraryMapper.selectList(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .select(UserModelLibrary::getModelId)
                        .eq(UserModelLibrary::getUserId, userId)
                        .eq(UserModelLibrary::getIsCustom, 0)
                        .isNotNull(UserModelLibrary::getModelId)
        ).stream()
                .map(UserModelLibrary::getModelId)
                .collect(Collectors.toSet());
    }

    @Override
    public UserModelLibraryVO addFromMarket(Long userId, Long modelId) {
        // 检查平台模型是否存在
        AiModel model = aiModelMapper.selectById(modelId);
        if (model == null || model.getStatus() == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 检查是否已添加
        Long exists = libraryMapper.selectCount(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .eq(UserModelLibrary::getUserId, userId)
                        .eq(UserModelLibrary::getModelId, modelId)
                        .eq(UserModelLibrary::getIsCustom, 0));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该模型已在模型库中");
        }
        UserModelLibrary entry = new UserModelLibrary();
        entry.setUserId(userId);
        entry.setModelId(modelId);
        entry.setIsCustom(0);
        entry.setName(model.getName());
        entry.setCategory(model.getCategory());
        entry.setDescription(model.getDescription());
        entry.setIcon(model.getIcon());
        entry.setColor(model.getColor());
        entry.setEnabled(1);
        libraryMapper.insert(entry);
        return toVO(entry, Map.of(model.getId(), model));
    }

    @Override
    public UserModelLibraryVO addCustom(Long userId, AddCustomModelRequest request) {
        UserModelLibrary entry = new UserModelLibrary();
        entry.setUserId(userId);
        entry.setIsCustom(1);
        entry.setName(request.getName());
        entry.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "文本");
        entry.setDescription(request.getDescription());
        entry.setApiEndpoint(request.getApiEndpoint());
        entry.setApiKey(request.getApiKey());
        entry.setIcon(StringUtils.hasText(request.getIcon()) ? request.getIcon() : "⚙️");
        entry.setColor(StringUtils.hasText(request.getColor()) ? request.getColor() : "#646cff");
        entry.setEnabled(1);
        libraryMapper.insert(entry);
        return toVO(entry, Collections.emptyMap());
    }

    @Override
    public UserModelLibraryVO updateCustom(Long userId, Long libraryId, UpdateCustomModelRequest request) {
        UserModelLibrary entry = getOwned(userId, libraryId);
        if (entry.getIsCustom() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只能编辑自定义模型");
        }
        entry.setName(request.getName());
        if (StringUtils.hasText(request.getCategory())) entry.setCategory(request.getCategory());
        entry.setDescription(request.getDescription());
        entry.setApiEndpoint(request.getApiEndpoint());
        if (StringUtils.hasText(request.getApiKey())) entry.setApiKey(request.getApiKey());
        if (StringUtils.hasText(request.getIcon())) entry.setIcon(request.getIcon());
        if (StringUtils.hasText(request.getColor())) entry.setColor(request.getColor());
        libraryMapper.updateById(entry);
        return toVO(entry, Collections.emptyMap());
    }

    @Override
    public void toggleEnabled(Long userId, Long libraryId) {
        UserModelLibrary entry = getOwned(userId, libraryId);
        entry.setEnabled(entry.getEnabled() == 1 ? 0 : 1);
        libraryMapper.updateById(entry);
    }

    @Override
    public void remove(Long userId, Long libraryId) {
        UserModelLibrary entry = getOwned(userId, libraryId);
        libraryMapper.deleteById(entry.getId());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserModelLibrary getOwned(Long userId, Long libraryId) {
        UserModelLibrary entry = libraryMapper.selectById(libraryId);
        if (entry == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!entry.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        return entry;
    }

    private UserModelLibraryVO toVO(UserModelLibrary e, Map<Long, AiModel> modelMap) {
        UserModelLibraryVO vo = new UserModelLibraryVO();
        vo.setId(e.getId());
        vo.setModelId(e.getModelId());
        vo.setIsCustom(e.getIsCustom() == 1);
        vo.setName(e.getName());
        vo.setCategory(e.getCategory());
        vo.setDescription(e.getDescription());
        vo.setApiEndpoint(e.getApiEndpoint());
        vo.setIcon(e.getIcon());
        vo.setColor(e.getColor() != null ? e.getColor() : "#646cff");
        vo.setEnabled(e.getEnabled() == 1);
        vo.setCreateTime(e.getCreateTime());
        // API Key 脱敏
        String key = e.getApiKey();
        if (StringUtils.hasText(key)) {
            vo.setApiKeyMasked(key.length() > 4 ? key.substring(0, 4) + "****" : "****");
        }
        // 标签
        if (e.getIsCustom() == 1) {
            vo.setTags(List.of("自定义"));
        } else {
            // 从关联的平台模型拿 tags（如果有）
            if (e.getModelId() != null) {
                AiModel model = modelMap.get(e.getModelId());
                if (model != null && StringUtils.hasText(model.getTags())) {
                    vo.setTags(Arrays.asList(model.getTags().split(",")));
                } else {
                    vo.setTags(List.of());
                }
                // provider
                if (model != null) vo.setProvider(model.getProvider());
            } else {
                vo.setTags(List.of());
            }
        }
        return vo;
    }
}
