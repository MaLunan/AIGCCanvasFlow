package com.aigc.user.mapper;

import com.aigc.user.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper：继承 BaseMapper，提供基础 CRUD
 * loadByUsername 等特殊查询通过 LambdaQueryWrapper 在 Service 层实现，无需额外 XML
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
