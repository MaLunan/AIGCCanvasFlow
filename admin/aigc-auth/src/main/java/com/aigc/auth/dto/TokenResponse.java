package com.aigc.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Token 响应 DTO：登录/刷新成功后返回给前端
 * 前端将 accessToken 存储到 localStorage，并在每次请求中附加到 Authorization 头
 * refreshToken 用于 accessToken 过期后的无感续期
 */
@Data
@Builder
public class TokenResponse {
    /** 访问 Token（短期，默认 1 小时），用于 API 鉴权 */
    private String accessToken;

    /** 刷新 Token（长期，默认 7 天），用于获取新的 accessToken */
    private String refreshToken;

    /** accessToken 有效期（毫秒），前端用于计算过期时间并提前刷新 */
    private long accessTokenExpire;

    /** refreshToken 有效期（毫秒） */
    private long refreshTokenExpire;

    /** Token 类型，固定为 "Bearer"（标准 HTTP Bearer Token） */
    private String tokenType;

    /** 当前登录用户 ID（前端存储，用于发送 X-User-Id 请求头） */
    private Long userId;

    /** 当前登录用户名（前端展示，如导航栏欢迎语） */
    private String username;
}
