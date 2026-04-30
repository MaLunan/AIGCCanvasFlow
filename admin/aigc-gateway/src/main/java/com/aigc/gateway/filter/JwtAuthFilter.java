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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** 不需要 Token 的路径白名单 */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/refresh",
            "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行
        if (WHITE_LIST.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(CommonConstants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return unauthorized(exchange, "缺少认证 Token");
        }

        String token = authHeader.substring(CommonConstants.TOKEN_PREFIX.length());

        // 检查 Token 黑名单（Redis）
        String blacklistKey = CommonConstants.REDIS_TOKEN_BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(blacklistKey)
                .flatMap(inBlacklist -> {
                    if (Boolean.TRUE.equals(inBlacklist)) {
                        return unauthorized(exchange, "Token 已失效");
                    }
                    Claims claims = JwtUtils.parse(jwtSecret, token);
                    if (claims == null) {
                        return unauthorized(exchange, "Token 无效或已过期");
                    }
                    // 将用户信息注入请求头，透传给下游
                    String userId = claims.getSubject();
                    String username = claims.get("username", String.class);
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header(CommonConstants.HEADER_USER_ID, userId)
                            .header(CommonConstants.HEADER_USERNAME, username != null ? username : "")
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("code", 401, "message", message, "data", null));
            DataBuffer buffer = response.bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
