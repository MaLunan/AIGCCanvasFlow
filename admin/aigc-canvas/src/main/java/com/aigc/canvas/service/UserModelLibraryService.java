package com.aigc.canvas.service;

import com.aigc.canvas.dto.AddCustomModelRequest;
import com.aigc.canvas.dto.UpdateCustomModelRequest;
import com.aigc.canvas.dto.UserModelLibraryVO;

import java.util.List;
import java.util.Set;

public interface UserModelLibraryService {

    /** 获取用户模型库列表 */
    List<UserModelLibraryVO> listByUser(Long userId);

    /** 获取用户模型库中的平台模型 ID 集合（用于 inLibrary 标记） */
    Set<Long> getLibraryModelIds(Long userId);

    /** 从广场添加平台模型到库 */
    UserModelLibraryVO addFromMarket(Long userId, Long modelId);

    /** 添加自定义模型到库 */
    UserModelLibraryVO addCustom(Long userId, AddCustomModelRequest request);

    /** 更新自定义模型 */
    UserModelLibraryVO updateCustom(Long userId, Long libraryId, UpdateCustomModelRequest request);

    /** 切换启用/禁用 */
    void toggleEnabled(Long userId, Long libraryId);

    /** 从库中移除 */
    void remove(Long userId, Long libraryId);
}
