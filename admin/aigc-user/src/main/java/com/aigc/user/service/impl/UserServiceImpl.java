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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserVO create(UserCreateRequest request) {
        // 检查用户名唯一
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        return toVO(user);
    }

    @Override
    public Page<UserVO> page(int current, int size) {
        Page<User> page = userMapper.selectPage(new Page<>(current, size), null);
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> loadByUsername(String username) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("password", user.getPassword());
        map.put("status", user.getStatus());
        map.put("nickname", user.getNickname());
        return map;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
