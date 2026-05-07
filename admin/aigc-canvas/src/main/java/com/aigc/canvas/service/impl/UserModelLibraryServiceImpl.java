package com.aigc.canvas.service.impl;

import com.aigc.canvas.config.ModelKeysConfig;
import com.aigc.canvas.dto.AddCustomModelRequest;
import com.aigc.canvas.dto.UpdateCustomModelRequest;
import com.aigc.canvas.dto.UserModelLibraryVO;
import com.aigc.canvas.entity.UserModelLibrary;
import com.aigc.canvas.mapper.UserModelLibraryMapper;
import com.aigc.canvas.service.UserModelLibraryService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserModelLibraryServiceImpl implements UserModelLibraryService {

    private final UserModelLibraryMapper libraryMapper;
    private final ModelKeysConfig modelKeysConfig;

    @Override
    public List<UserModelLibraryVO> listByUser(Long userId) {
        List<UserModelLibrary> entries = libraryMapper.selectList(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .eq(UserModelLibrary::getUserId, userId)
                        .orderByDesc(UserModelLibrary::getCreateTime));
        if (entries.isEmpty()) return Collections.emptyList();
        return entries.stream().map(this::toVO).toList();
    }

    @Override
    public Set<String> getLibraryModelKeys(Long userId) {
        return libraryMapper.selectList(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .select(UserModelLibrary::getModelKey)
                        .eq(UserModelLibrary::getUserId, userId)
                        .eq(UserModelLibrary::getIsCustom, 0)
                        .isNotNull(UserModelLibrary::getModelKey)
        ).stream()
                .map(UserModelLibrary::getModelKey)
                .collect(Collectors.toSet());
    }

    @Override
    public UserModelLibraryVO addFromMarket(Long userId, String modelKey) {
        // 校验 Nacos 中是否存在该模型
        ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(modelKey);
        if (entry == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该模型不存在或暂未开放");
        }
        // 校验是否已添加
        Long exists = libraryMapper.selectCount(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .eq(UserModelLibrary::getUserId, userId)
                        .eq(UserModelLibrary::getModelKey, modelKey)
                        .eq(UserModelLibrary::getIsCustom, 0));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该模型已在模型库中");
        }
        UserModelLibrary lib = new UserModelLibrary();
        lib.setUserId(userId);
        lib.setModelKey(modelKey);
        lib.setIsCustom(0);
        lib.setName(entry.getName());
        lib.setCategory(entry.getCategory());
        lib.setDescription(entry.getDescription());
        lib.setIcon(entry.getIcon());
        lib.setColor(entry.getColor());
        lib.setEnabled(1);
        libraryMapper.insert(lib);
        return toVO(lib);
    }

    @Override
    public UserModelLibraryVO addCustom(Long userId, AddCustomModelRequest request) {
        UserModelLibrary lib = new UserModelLibrary();
        lib.setUserId(userId);
        lib.setIsCustom(1);
        lib.setName(request.getName());
        lib.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "文本");
        lib.setDescription(request.getDescription());
        lib.setApiEndpoint(request.getApiEndpoint());
        lib.setApiKey(request.getApiKey());
        lib.setIcon(StringUtils.hasText(request.getIcon()) ? request.getIcon() : "⚙️");
        lib.setColor(StringUtils.hasText(request.getColor()) ? request.getColor() : "#646cff");
        lib.setEnabled(1);
        libraryMapper.insert(lib);
        return toVO(lib);
    }

    @Override
    public UserModelLibraryVO updateCustom(Long userId, Long libraryId, UpdateCustomModelRequest request) {
        UserModelLibrary lib = getOwned(userId, libraryId);
        if (lib.getIsCustom() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只能编辑自定义模型");
        }
        lib.setName(request.getName());
        if (StringUtils.hasText(request.getCategory())) lib.setCategory(request.getCategory());
        lib.setDescription(request.getDescription());
        lib.setApiEndpoint(request.getApiEndpoint());
        if (StringUtils.hasText(request.getApiKey())) lib.setApiKey(request.getApiKey());
        if (StringUtils.hasText(request.getIcon())) lib.setIcon(request.getIcon());
        if (StringUtils.hasText(request.getColor())) lib.setColor(request.getColor());
        libraryMapper.updateById(lib);
        return toVO(lib);
    }

    @Override
    public void toggleEnabled(Long userId, Long libraryId) {
        UserModelLibrary lib = getOwned(userId, libraryId);
        lib.setEnabled(lib.getEnabled() == 1 ? 0 : 1);
        libraryMapper.updateById(lib);
    }

    @Override
    public void remove(Long userId, Long libraryId) {
        UserModelLibrary lib = getOwned(userId, libraryId);
        libraryMapper.deleteById(lib.getId());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserModelLibrary getOwned(Long userId, Long libraryId) {
        UserModelLibrary lib = libraryMapper.selectById(libraryId);
        if (lib == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!lib.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        return lib;
    }

    private UserModelLibraryVO toVO(UserModelLibrary e) {
        UserModelLibraryVO vo = new UserModelLibraryVO();
        vo.setId(e.getId());
        vo.setModelKey(e.getModelKey());
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
        if (e.getIsCustom() == 1) {
            vo.setTags(List.of("自定义"));
        } else if (StringUtils.hasText(e.getModelKey())) {
            // 从 Nacos 补充 provider 和 tags
            ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(e.getModelKey());
            if (entry != null) {
                vo.setProvider(entry.getProvider());
                vo.setTags(entry.tagList());
            } else {
                vo.setTags(List.of());
            }
        } else {
            vo.setTags(List.of());
        }
        return vo;
    }
}
