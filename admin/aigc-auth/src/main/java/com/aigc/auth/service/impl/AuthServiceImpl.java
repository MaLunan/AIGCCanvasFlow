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

/**
 * 认证服务实现：登录、Token 刷新、登出
 * 核心流程：
 *   登录 → Feign 调用 aigc-user 获取用户信息 → BCrypt 密码验证 → 生成双 Token
 *   刷新 → 验证 refresh_token → 颁发新 access_token
 *   登出 → 将 access_token 加入 Redis 黑名单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // Feign 客户端：跨服务调用 aigc-user，获取用户密码等信息
    private final UserFeignClient userFeignClient;
    // 同步 Redis 模板（非 Gateway 的 Reactive 场景，普通 Service 用同步版本即可）
    private final StringRedisTemplate redisTemplate;
    // BCrypt 密码编码器：Spring Security 内置，cost factor 默认 10
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** JWT 签名密钥，从 Nacos 注入 */
    @Value("${jwt.secret}")
    private String jwtSecret;
    /** Access Token 有效期（毫秒），默认 1 小时 */
    @Value("${jwt.access-token-expire:3600000}")
    private long accessTokenExpire;   // 1h
    /** Refresh Token 有效期（毫秒），默认 7 天 */
    @Value("${jwt.refresh-token-expire:604800000}")
    private long refreshTokenExpire;  // 7d

    /**
     * 用户登录：验证用户名/密码，通过后颁发 access_token + refresh_token
     */
    @Override
    public TokenResponse login(LoginRequest request) {
        // 通过 Feign 调用 aigc-user 服务，根据用户名查询用户信息（含加密密码）
        R<Map<String, Object>> result = userFeignClient.loadUserByUsername(request.getUsername());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            // 用户不存在，抛出业务异常（不暴露详细原因，防止枚举攻击）
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        Map<String, Object> user = result.getData();

        // BCrypt 密码校验：比对明文密码与数据库中的哈希值
        String encodedPassword = (String) user.get("password");
        if (!passwordEncoder.matches(request.getPassword(), encodedPassword)) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 检查账号是否被禁用（status=0 表示禁用）
        Integer status = (Integer) user.get("status");
        if (status != null && status == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 提取用户 ID（Feign 传回的 JSON 反序列化为 Number，需转 Long）
        Long userId = ((Number) user.get("id")).longValue();
        String username = (String) user.get("username");
        return buildTokenResponse(userId, username);
    }

    /**
     * 刷新 Token：验证 refresh_token 有效性，颁发新的 access_token + refresh_token
     * refresh_token 本身不加入黑名单，使用后生成新的双 Token（旋转刷新策略）
     */
    @Override
    public TokenResponse refresh(String refreshToken) {
        // 解析并验证 refresh_token（包含签名验证和有效期检查）
        Claims claims = JwtUtils.parse(jwtSecret, refreshToken);
        if (claims == null) {
            // refresh_token 无效或已过期，要求用户重新登录
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        // 颁发全新的双 Token，旧 refresh_token 到期后自动失效
        return buildTokenResponse(userId, username);
    }

    /**
     * 登出：将 access_token 加入 Redis 黑名单
     * - 黑名单 TTL = Token 剩余有效期，到期后 Redis 自动清除，无需额外维护
     * - 登出后网关过滤器检测到黑名单 Key 存在，拒绝该 Token 的后续请求
     */
    @Override
    public void logout(String accessToken) {
        Claims claims = JwtUtils.parse(jwtSecret, accessToken);
        if (claims == null) return;  // Token 已过期，自然失效，无需加黑名单
        // 计算 Token 剩余有效时间（毫秒），作为 Redis key 的 TTL
        long remainMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainMs > 0) {
            // 以 "blacklist:{token}" 为 key 写入 Redis，值为 "1"（仅用于标记存在）
            redisTemplate.opsForValue().set(
                    CommonConstants.REDIS_TOKEN_BLACKLIST_PREFIX + accessToken,
                    "1",
                    remainMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * 生成双 Token 响应：access_token（短期）+ refresh_token（长期）
     * - subject 存储 userId（字符串形式），用于网关解析后注入 X-User-Id 请求头
     * - claims 中附加 username，方便网关注入 X-Username 头，无需再查库
     */
    private TokenResponse buildTokenResponse(Long userId, String username) {
        Map<String, Object> claims = Map.of("username", username);
        // 生成 access_token（1h）
        String accessToken = JwtUtils.generate(jwtSecret, String.valueOf(userId), claims, accessTokenExpire);
        // 生成 refresh_token（7d），claims 与 access_token 相同，差异仅在有效期
        String refreshToken = JwtUtils.generate(jwtSecret, String.valueOf(userId), claims, refreshTokenExpire);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpire(accessTokenExpire)
                .refreshTokenExpire(refreshTokenExpire)
                .tokenType("Bearer")  // 标准 Bearer Token 类型，前端拼接成 "Bearer {token}"
                .userId(userId)
                .username(username)
                .build();
    }
}
