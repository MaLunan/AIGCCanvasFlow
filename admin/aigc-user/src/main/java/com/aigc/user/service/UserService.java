package com.aigc.user.service;

import com.aigc.user.dto.UserCreateRequest;
import com.aigc.user.dto.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserService {
    UserVO create(UserCreateRequest request);
    UserVO getById(Long id);
    Page<UserVO> page(int current, int size);
    void delete(Long id);
    /** 供 auth 服务内部调用，返回包含密码的完整用户信息 */
    java.util.Map<String, Object> loadByUsername(String username);
}
