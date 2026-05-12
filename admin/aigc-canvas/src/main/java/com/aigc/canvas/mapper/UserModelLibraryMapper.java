package com.aigc.canvas.mapper;

import com.aigc.canvas.entity.UserModelLibrary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户模型库 Mapper：继承 BaseMapper，提供自动 CRUD
 * 主要使用 LambdaQueryWrapper 进行条件查询（getUserId、getModelKey、getIsCustom 等过滤）
 */
@Mapper
public interface UserModelLibraryMapper extends BaseMapper<UserModelLibrary> {
}
