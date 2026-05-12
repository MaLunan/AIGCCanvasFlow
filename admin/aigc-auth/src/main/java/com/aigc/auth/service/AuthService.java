package com.aigc.auth.service;

import com.aigc.auth.dto.LoginRequest;
import com.aigc.auth.dto.TokenResponse;

/**
 * 认证服务接口：定义登录、Token 刷新、登出三个核心认证操作
 * 实现类：AuthServiceImpl（BCrypt 密码校验 + JWT 生成 + Redis 黑名单）
 */
public interface AuthService {
    /** 用户登录：验证用户名/密码，成功返回双 Token */
    TokenResponse login(LoginRequest request);
    /** 刷新 Token：验证 refreshToken 后颁发新的双 Token */
    TokenResponse refresh(String refreshToken);
    /** 登出：将 accessToken 加入 Redis 黑名单，TTL = Token 剩余有效期 */
    void logout(String accessToken);
}
