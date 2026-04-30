package com.aigc.auth.service;

import com.aigc.auth.dto.LoginRequest;
import com.aigc.auth.dto.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
    void logout(String accessToken);
}
