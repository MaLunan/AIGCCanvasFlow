package com.aigc.gateway.filter;

import com.aigc.common.constant.CommonConstants;
import com.aigc.common.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 网关全局 JWT 认证过滤器
 * - 拦截所有请求，白名单路径直接放行
 * - 验证 Authorization 头中的 Bearer Token
 * - 检查 Redis 黑名单（已登出的 Token）
 * - 解析 Token 中的用户信息，通过请求头透传给下游服务
 * 使用 Reactive Redis（ReactiveStringRedisTemplate）以契合 Gateway 的 WebFlux 响应式模型
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // Reactive Redis 模板（Gateway 基于 WebFlux，必须用 Reactive 版本）
    private final ReactiveStringRedisTemplate redisTemplate;
    // JSON 序列化器，用于将 401 响应体序列化为 JSON 字符串
    private final ObjectMapper objectMapper;

    /** JWT 签名密钥，从 Nacos/application.yml 注入 */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** 不需要 Token 的路径白名单，使用 Ant 风格匹配 */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",          // 登录接口
            "/auth/refresh",        // Token 刷新接口
            "/user/register",       // 用户注册接口
            "/actuator/**",         // Spring Boot Actuator 健康检查（运维用）
            "/canvas/models/list",  // 模型广场：无需登录也可浏览
            "/canvas/assets/files/**" // 静态资源文件：直接访问不鉴权
    );

    // Ant 风格路径匹配器，支持 * 和 ** 通配符
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 过滤器核心逻辑：JWT 验证 → Redis 黑名单检查 → 用户信息透传
     * 返回 Mono<Void> 符合 WebFlux 响应式规范
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单路径直接放行，无需 Token
        if (WHITE_LIST.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            return chain.filter(exchange);
        }

        // 从请求头读取 Authorization: Bearer <token>
        String authHeader = exchange.getRequest().getHeaders().getFirst(CommonConstants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return unauthorized(exchange, "缺少认证 Token");
        }

        // 截取 "Bearer " 前缀后的纯 Token 字符串
        String token = authHeader.substring(CommonConstants.TOKEN_PREFIX.length());

        // 异步检查 Token 是否在 Redis 黑名单（logout 时加入，TTL = 剩余有效期）
        String blacklistKey = CommonConstants.REDIS_TOKEN_BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(blacklistKey)
                .flatMap(inBlacklist -> {
                    if (Boolean.TRUE.equals(inBlacklist)) {
                        // Token 已被登出，拒绝访问
                        return unauthorized(exchange, "Token 已失效");
                    }
                    // 验证 Token 签名和有效期，解析出 Claims
                    Claims claims = JwtUtils.parse(jwtSecret, token);
                    if (claims == null) {
                        // Token 格式错误、签名不合法或已过期
                        return unauthorized(exchange, "Token 无效或已过期");
                    }
                    // 从 Claims 中取出用户信息，注入下游请求头
                    // 下游服务（如 aigc-canvas）通过 X-User-Id 获取当前用户，无需再解 Token
                    String userId = claims.getSubject();       // subject 存储 userId
                    String username = claims.get("username", String.class);
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header(CommonConstants.HEADER_USER_ID, userId)
                            .header(CommonConstants.HEADER_USERNAME, username != null ? username : "")
                            .build();
                    // 用修改后的请求继续传递给下一个过滤器/路由
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    /**
     * 构造并写入 401 Unauthorized 响应
     * 直接写响应体并结束请求（不再走后续过滤器链）
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            // 与业务接口保持一致的 JSON 格式：{code, message, data}
            String body = objectMapper.writeValueAsString(
                    Map.of("code", 401, "message", message, "data", null));
            DataBuffer buffer = response.bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            // JSON 序列化失败时直接结束响应（极端情况兜底）
            return response.setComplete();
        }
    }

    /**
     * 过滤器优先级：-100，确保在所有业务过滤器之前执行鉴权
     * Spring Gateway 内置路由过滤器默认 order = 0，所以 -100 优先级更高
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
