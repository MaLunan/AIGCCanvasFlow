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

/**
 * 用户模型库服务实现：管理用户个人的模型库（平台模型 + 自定义模型）
 * 数据存储在 t_user_model_library 表，每行对应一个用户-模型关联
 * API Key 不对外明文返回（toVO 中脱敏处理）
 */
@Service
@RequiredArgsConstructor
public class UserModelLibraryServiceImpl implements UserModelLibraryService {

    private final UserModelLibraryMapper libraryMapper;
    // Nacos 配置，用于验证 modelKey 合法性及补充模型元数据（provider/tags）
    private final ModelKeysConfig modelKeysConfig;

    /**
     * 查询用户的模型库列表，按 createTime 倒序（最新添加的在前）
     */
    @Override
    public List<UserModelLibraryVO> listByUser(Long userId) {
        List<UserModelLibrary> entries = libraryMapper.selectList(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .eq(UserModelLibrary::getUserId, userId)
                        .orderByDesc(UserModelLibrary::getCreateTime));
        if (entries.isEmpty()) return Collections.emptyList();
        return entries.stream().map(this::toVO).toList();
    }

    /**
     * 查询用户库中所有平台模型的 modelKey 集合
     * 用于模型广场的 inLibrary 标记（调用方通过 Set.contains 做 O(1) 查找）
     * 只查 modelKey 字段（select 优化，不加载全部字段）
     */
    @Override
    public Set<String> getLibraryModelKeys(Long userId) {
        return libraryMapper.selectList(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .select(UserModelLibrary::getModelKey) // 只查 modelKey，减少数据传输
                        .eq(UserModelLibrary::getUserId, userId)
                        .eq(UserModelLibrary::getIsCustom, 0)  // 只查平台模型（非自定义）
                        .isNotNull(UserModelLibrary::getModelKey) // 过滤 null 值
        ).stream()
                .map(UserModelLibrary::getModelKey)
                .collect(Collectors.toSet());
    }

    /**
     * 从广场添加平台模型到用户库
     * 校验逻辑：1) modelKey 在 Nacos 中存在；2) 用户未重复添加
     * 复制 Nacos 中的模型元数据（name/category/icon/color），方便后续即使 Nacos 更新也保留快照
     */
    @Override
    public UserModelLibraryVO addFromMarket(Long userId, String modelKey) {
        // 校验 Nacos 中是否存在该模型（防止添加不存在或已下架的模型）
        ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(modelKey);
        if (entry == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该模型不存在或暂未开放");
        }
        // 防止重复添加同一平台模型
        Long exists = libraryMapper.selectCount(
                new LambdaQueryWrapper<UserModelLibrary>()
                        .eq(UserModelLibrary::getUserId, userId)
                        .eq(UserModelLibrary::getModelKey, modelKey)
                        .eq(UserModelLibrary::getIsCustom, 0));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该模型已在模型库中");
        }
        // 创建库记录，快照模型元数据
        UserModelLibrary lib = new UserModelLibrary();
        lib.setUserId(userId);
        lib.setModelKey(modelKey);  // 关联平台模型
        lib.setIsCustom(0);         // 0=平台模型（区别于用户自定义模型）
        lib.setName(entry.getName());
        lib.setCategory(entry.getCategory());
        lib.setDescription(entry.getDescription());
        lib.setIcon(entry.getIcon());
        lib.setColor(entry.getColor());
        lib.setEnabled(1);          // 默认启用
        libraryMapper.insert(lib);
        return toVO(lib);
    }

    /**
     * 添加用户自定义模型（如 OpenAI 兼容接口）
     * isCustom=1 标记为自定义模型，使用用户提供的 apiKey + apiEndpoint 访问
     * category/icon/color 有默认值兜底，前端可选填
     */
    @Override
    public UserModelLibraryVO addCustom(Long userId, AddCustomModelRequest request) {
        UserModelLibrary lib = new UserModelLibrary();
        lib.setUserId(userId);
        lib.setIsCustom(1);  // 1=自定义模型
        lib.setName(request.getName());
        // category 默认"文本"（自定义模型通常是 LLM 类型）
        lib.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "文本");
        lib.setDescription(request.getDescription());
        lib.setApiEndpoint(request.getApiEndpoint());
        lib.setApiKey(request.getApiKey());  // 存储用户的 API Key（返回时脱敏）
        lib.setIcon(StringUtils.hasText(request.getIcon()) ? request.getIcon() : "⚙️");
        lib.setColor(StringUtils.hasText(request.getColor()) ? request.getColor() : "#646cff");
        lib.setEnabled(1);  // 默认启用
        libraryMapper.insert(lib);
        return toVO(lib);
    }

    /**
     * 更新自定义模型配置
     * 仅允许编辑 isCustom=1 的记录（平台模型不支持修改）
     * apiKey 只有非空时才更新（前端不回填 API Key，空值表示"不修改"）
     */
    @Override
    public UserModelLibraryVO updateCustom(Long userId, Long libraryId, UpdateCustomModelRequest request) {
        UserModelLibrary lib = getOwned(userId, libraryId);
        if (lib.getIsCustom() != 1) {
            // 平台模型的元数据来自 Nacos，不允许用户直接修改
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只能编辑自定义模型");
        }
        lib.setName(request.getName());
        if (StringUtils.hasText(request.getCategory()))  lib.setCategory(request.getCategory());
        lib.setDescription(request.getDescription());
        lib.setApiEndpoint(request.getApiEndpoint());
        // API Key 只在非空时更新（前端安全策略：编辑时不回填密钥，空值不覆盖）
        if (StringUtils.hasText(request.getApiKey())) lib.setApiKey(request.getApiKey());
        if (StringUtils.hasText(request.getIcon()))  lib.setIcon(request.getIcon());
        if (StringUtils.hasText(request.getColor())) lib.setColor(request.getColor());
        libraryMapper.updateById(lib);
        return toVO(lib);
    }

    /**
     * 切换模型启用/禁用状态（1→0 或 0→1）
     * 禁用后该模型不出现在 AI 生成的模型选择器中
     */
    @Override
    public void toggleEnabled(Long userId, Long libraryId) {
        UserModelLibrary lib = getOwned(userId, libraryId);
        // XOR 效果：1→0，0→1
        lib.setEnabled(lib.getEnabled() == 1 ? 0 : 1);
        libraryMapper.updateById(lib);
    }

    /** 从库中移除模型（硬删除） */
    @Override
    public void remove(Long userId, Long libraryId) {
        UserModelLibrary lib = getOwned(userId, libraryId);
        libraryMapper.deleteById(lib.getId());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * 查询并校验模型库记录归属
     * 记录不存在抛 NOT_FOUND，不属于当前用户抛 FORBIDDEN（防止越权操作他人模型）
     */
    private UserModelLibrary getOwned(Long userId, Long libraryId) {
        UserModelLibrary lib = libraryMapper.selectById(libraryId);
        if (lib == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!lib.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        return lib;
    }

    /**
     * UserModelLibrary → VO 转换
     * - API Key 脱敏（仅显示前 4 位 + ****)
     * - 平台模型从 Nacos 补充 provider 和 tags（运营可更新 Nacos 实时生效）
     * - isCustom int → boolean（前端友好）
     */
    private UserModelLibraryVO toVO(UserModelLibrary e) {
        UserModelLibraryVO vo = new UserModelLibraryVO();
        vo.setId(e.getId());
        vo.setModelKey(e.getModelKey());
        vo.setIsCustom(e.getIsCustom() == 1);  // int → boolean
        vo.setName(e.getName());
        vo.setCategory(e.getCategory());
        vo.setDescription(e.getDescription());
        vo.setApiEndpoint(e.getApiEndpoint());
        vo.setIcon(e.getIcon());
        vo.setColor(e.getColor() != null ? e.getColor() : "#646cff"); // 默认紫色
        vo.setEnabled(e.getEnabled() == 1);    // int → boolean
        vo.setCreateTime(e.getCreateTime());
        // API Key 脱敏：仅显示前 4 位，其余替换为 ****（不可反推原始密钥）
        String key = e.getApiKey();
        if (StringUtils.hasText(key)) {
            vo.setApiKeyMasked(key.length() > 4 ? key.substring(0, 4) + "****" : "****");
        }
        if (e.getIsCustom() == 1) {
            vo.setTags(List.of("自定义")); // 自定义模型固定标签
        } else if (StringUtils.hasText(e.getModelKey())) {
            // 从 Nacos 实时补充 provider 和 tags（Nacos 更新后自动生效）
            ModelKeysConfig.ModelEntry entry = modelKeysConfig.get(e.getModelKey());
            if (entry != null) {
                vo.setProvider(entry.getProvider());
                vo.setTags(entry.tagList());
            } else {
                vo.setTags(List.of()); // 模型已从 Nacos 删除时，tags 为空
            }
        } else {
            vo.setTags(List.of());
        }
        return vo;
    }
}
