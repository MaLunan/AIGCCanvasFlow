package com.aigc.auth.service.impl;

import com.aigc.auth.dto.LoginRequest;
import com.aigc.auth.dto.TokenResponse;
import com.aigc.auth.feign.UserFeignClient;
import com.aigc.auth.service.AuthService;
import com.aigc.common.constant.CommonConstants;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.aigc.common.model.R;
import com.aigc.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserFeignClient userFeignClient;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.access-token-expire:3600000}")
    private long accessTokenExpire;   // 1h
    @Value("${jwt.refresh-token-expire:604800000}")
    private long refreshTokenExpire;  // 7d

    @Override
    public TokenResponse login(LoginRequest request) {
        R<Map<String, Object>> result = userFeignClient.loadUserByUsername(request.getUsername());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        Map<String, Object> user = result.getData();
        String encodedPassword = (String) user.get("password");
        if (!passwordEncoder.matches(request.getPassword(), encodedPassword)) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        Integer status = (Integer) user.get("status");
        if (status != null && status == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        Long userId = ((Number) user.get("id")).longValue();
        String username = (String) user.get("username");
        return buildTokenResponse(userId, username);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        Claims claims = JwtUtils.parse(jwtSecret, refreshToken);
        if (claims == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        return buildTokenResponse(userId, username);
    }

    @Override
    public void logout(String accessToken) {
        Claims claims = JwtUtils.parse(jwtSecret, accessToken);
        if (claims == null) return;
        // 将 Access Token 加入黑名单，TTL 与剩余有效期一致
        long remainMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainMs > 0) {
            redisTemplate.opsForValue().set(
                    CommonConstants.REDIS_TOKEN_BLACKLIST_PREFIX + accessToken,
                    "1",
                    remainMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private TokenResponse buildTokenResponse(Long userId, String username) {
        Map<String, Object> claims = Map.of("username", username);
        String accessToken = JwtUtils.generate(jwtSecret, String.valueOf(userId), claims, accessTokenExpire);
        String refreshToken = JwtUtils.generate(jwtSecret, String.valueOf(userId), claims, refreshTokenExpire);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpire(accessTokenExpire)
                .refreshTokenExpire(refreshTokenExpire)
                .tokenType("Bearer")
                .userId(userId)
                .username(username)
                .build();
    }
}
