package com.aigc.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpire;
    private long refreshTokenExpire;
    private String tokenType;
    private Long userId;
    private String username;
}
