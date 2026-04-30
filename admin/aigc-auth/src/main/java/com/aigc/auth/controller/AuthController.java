package com.aigc.auth.controller;

import com.aigc.auth.dto.LoginRequest;
import com.aigc.auth.dto.TokenResponse;
import com.aigc.auth.service.AuthService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public R<TokenResponse> refresh(@RequestParam("refreshToken") String refreshToken) {
        return R.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(CommonConstants.AUTHORIZATION_HEADER) String authHeader) {
        if (authHeader != null && authHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
            authService.logout(authHeader.substring(CommonConstants.TOKEN_PREFIX.length()));
        }
        return R.ok();
    }
}
