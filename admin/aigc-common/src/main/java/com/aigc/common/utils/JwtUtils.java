package com.aigc.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类：封装 Token 生成、解析、验证等操作
 * 使用 HMAC-SHA 算法，密钥由配置注入
 */
@Slf4j
public class JwtUtils {

    // 工具类，禁止实例化
    private JwtUtils() {}

    /**
     * 生成 Token
     *
     * @param secret  签名密钥（至少 32 字符）
     * @param subject subject（通常是 userId 字符串形式）
     * @param claims  附加 claims（如 username、roles 等）
     * @param ttlMs   过期时间（毫秒）
     * @return 签名后的 JWT 字符串
     */
    public static String generate(String secret, String subject, Map<String, Object> claims, long ttlMs) {
        // 将字符串密钥转换为 HMAC SecretKey 对象
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)                                               // 通常设为 userId
                .claims(claims)                                                 // 附加业务数据（username等）
                .issuedAt(new Date())                                           // 签发时间
                .expiration(new Date(System.currentTimeMillis() + ttlMs))       // 过期时间
                .signWith(key)                                                  // 使用 HMAC 签名
                .compact();
    }

    /**
     * 解析 Token，返回 Claims；若无效或过期则返回 null
     * 调用方应检查返回值是否为 null 再使用
     *
     * @param secret 签名密钥
     * @param token  JWT 字符串
     * @return Claims 对象，无效时返回 null
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
            return null;  // 过期返回 null，不抛异常
        } catch (JwtException e) {
            log.debug("Token 无效: {}", e.getMessage());
            return null;  // 签名不合法、格式错误等
        }
    }

    /**
     * 仅获取过期 Token 的 Claims（用于 refresh token 场景）
     * 过期 Token 的 claims 仍然有效，可用于颁发新 token
     *
     * @return Claims 对象，token 完全无效时返回 null
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
            // 捕获过期异常，从中取出 claims（签名仍然合法）
            return e.getClaims();
        } catch (JwtException e) {
            return null;  // 签名不合法则无法信任 claims
        }
    }

    /** 快速校验 Token 是否有效（非 null 即有效）*/
    public static boolean isValid(String secret, String token) {
        return parse(secret, token) != null;
    }
}
