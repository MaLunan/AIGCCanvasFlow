package com.aigc.user.service.impl;

import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.aigc.user.dto.UserCreateRequest;
import com.aigc.user.dto.UserVO;
import com.aigc.user.entity.User;
import com.aigc.user.mapper.UserMapper;
import com.aigc.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现：用户的 CRUD 操作 + 供 Feign 调用的内部查询接口
 * 密码使用 BCrypt 加密存储（单向哈希），不支持解密
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    // BCrypt 密码编码器：cost factor 默认 10，生产环境可调高到 12
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 创建用户（注册）
     * - 校验用户名唯一性（数据库层也有唯一索引作为兜底）
     * - 密码 BCrypt 加密后存储，原始密码不入库
     * - nickname 默认使用 username（若前端未填）
     */
    @Override
    public UserVO create(UserCreateRequest request) {
        // 前置校验用户名唯一性，避免触发数据库唯一约束异常（更友好的错误信息）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(request, user);
        // 密码 BCrypt 哈希处理，盐值随机，每次生成结果不同（安全性保证）
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);  // 1=启用（新注册用户默认激活）
        // nickname 未填时使用 username 作为默认昵称
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        userMapper.insert(user);
        return toVO(user);
    }

    /** 根据 ID 查询用户（VO 中不含密码字段） */
    @Override
    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        return toVO(user);
    }

    /** 分页查询所有用户（管理端使用，VO 不含密码） */
    @Override
    public Page<UserVO> page(int current, int size) {
        Page<User> page = userMapper.selectPage(new Page<>(current, size), null);
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /** 删除用户（MyBatis-Plus 逻辑删除或物理删除，取决于实体注解配置） */
    @Override
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    /**
     * 内部查询接口，供 aigc-auth 的 UserFeignClient 调用
     * 返回包含密码字段的 Map（用于 BCrypt 密码校验）
     * 注意：此接口返回明文密码 Hash，只能在内网 Feign 调用，不可对外暴露
     * 用户不存在时返回 null，auth 服务会转换为 USER_NOT_FOUND 异常
     */
    @Override
    public Map<String, Object> loadByUsername(String username) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return null;
        // 将必要字段打包为 Map 传递（避免序列化整个 User 实体，减少敏感字段暴露）
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("password", user.getPassword()); // 加密后的密码 Hash，auth 用于 BCrypt.matches
        map.put("status", user.getStatus());     // 账号状态（0=禁用），auth 校验是否可登录
        map.put("nickname", user.getNickname());
        return map;
    }

    /** User Entity → UserVO（VO 中不含 password 字段，保护密码安全） */
    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo); // UserVO 无 password 字段，自动跳过
        return vo;
    }
}
