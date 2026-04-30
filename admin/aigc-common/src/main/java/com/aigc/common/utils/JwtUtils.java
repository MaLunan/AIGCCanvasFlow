package com.aigc.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
public class JwtUtils {

    private JwtUtils() {}

    /**
     * 生成 Token
     *
     * @param secret  签名密钥（至少 32 字符）
     * @param subject subject（通常是 userId）
     * @param claims  附加 claims
     * @param ttlMs   过期时间（毫秒）
     */
    public static String generate(String secret, String subject, Map<String, Object> claims, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token，返回 Claims；若无效或过期则返回 null
     */
    public static Claims parse(String secret, String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期");
            return null;
        } catch (JwtException e) {
            log.debug("Token 无效: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 仅获取过期 Token 的 Claims（用于 refresh token 场景）
     */
    public static Claims parseExpired(String secret, String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            return null;
        }
    }

    public static boolean isValid(String secret, String token) {
        return parse(secret, token) != null;
    }
}
