package com.aigc.user.service;

import com.aigc.user.dto.UserCreateRequest;
import com.aigc.user.dto.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户服务接口：提供用户 CRUD 和内部认证查询能力
 * 注意：loadByUsername 为内部 Feign 接口，不通过网关暴露，仅供 aigc-auth 服务调用
 * 密码以 BCrypt 格式存储，UserVO 中不包含密码字段
 * 实现类：UserServiceImpl
 */
public interface UserService {
    /** 创建新用户（注册），密码自动 BCrypt 加密后存储 */
    UserVO create(UserCreateRequest request);

    /** 根据 ID 查询用户信息（不含密码） */
    UserVO getById(Long id);

    /** 分页查询用户列表（管理端使用） */
    Page<UserVO> page(int current, int size);

    /** 逻辑删除用户（@TableLogic） */
    void delete(Long id);

    /** 供 auth 服务内部调用，返回包含密码 Hash 的完整用户信息；用户不存在时返回 null */
    java.util.Map<String, Object> loadByUsername(String username);
}
