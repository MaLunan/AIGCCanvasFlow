package com.aigc.auth.controller;

import com.aigc.auth.dto.LoginRequest;
import com.aigc.auth.dto.TokenResponse;
import com.aigc.auth.service.AuthService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器：提供登录、Token 刷新、登出三个接口
 * 这三个接口均在网关白名单中（login/refresh）或由网关注入 Token 信息（logout）
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * POST /auth/login
     * 请求体：{ username, password }
     * 响应：{ accessToken, refreshToken, userId, username, ... }
     * @Valid 触发 LoginRequest 中的校验注解（@NotBlank 等）
     */
    @PostMapping("/login")
    public R<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /**
     * 刷新 Token
     * POST /auth/refresh?refreshToken=xxx
     * 前端在 access_token 即将过期时调用，无感续期
     */
    @PostMapping("/refresh")
    public R<TokenResponse> refresh(@RequestParam("refreshToken") String refreshToken) {
        return R.ok(authService.refresh(refreshToken));
    }

    /**
     * 用户登出
     * POST /auth/logout（需携带 Authorization: Bearer {token}）
     * 将 access_token 加入 Redis 黑名单，网关后续请求将被拒绝
     * 即使 Token 不存在或已过期也返回成功（幂等操作）
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(CommonConstants.AUTHORIZATION_HEADER) String authHeader) {
        // 仅当 authHeader 以 "Bearer " 开头时才处理，截取纯 Token 字符串
        if (authHeader != null && authHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
            authService.logout(authHeader.substring(CommonConstants.TOKEN_PREFIX.length()));
        }
        return R.ok();
    }
}
