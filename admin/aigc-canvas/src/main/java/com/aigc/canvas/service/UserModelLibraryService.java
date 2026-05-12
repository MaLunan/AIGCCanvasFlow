package com.aigc.canvas.service;

import com.aigc.canvas.dto.AddCustomModelRequest;
import com.aigc.canvas.dto.UpdateCustomModelRequest;
import com.aigc.canvas.dto.UserModelLibraryVO;

import java.util.List;
import java.util.Set;

/**
 * 用户模型库服务接口：管理用户个人模型库（平台模型收藏 + 自定义模型）
 * 数据存储：t_user_model_library 表
 * API Key 在 VO 转换时脱敏（保留前4位 + "****"）
 * 实现类：UserModelLibraryServiceImpl
 */
public interface UserModelLibraryService {

    /** 获取用户模型库列表 */
    List<UserModelLibraryVO> listByUser(Long userId);

    /** 获取用户库中已添加的平台模型 key 集合（用于 inLibrary 标记） */
    Set<String> getLibraryModelKeys(Long userId);

    /** 从广场添加平台模型到库（以 modelKey 标识） */
    UserModelLibraryVO addFromMarket(Long userId, String modelKey);

    /** 添加自定义模型到库 */
    UserModelLibraryVO addCustom(Long userId, AddCustomModelRequest request);

    /** 更新自定义模型 */
    UserModelLibraryVO updateCustom(Long userId, Long libraryId, UpdateCustomModelRequest request);

    /** 切换启用/禁用 */
    void toggleEnabled(Long userId, Long libraryId);

    /** 从库中移除 */
    void remove(Long userId, Long libraryId);
}
