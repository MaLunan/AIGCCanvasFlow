package com.aigc.common.constant;

/**
 * 跨服务共享常量：所有微服务模块均可引用（位于 aigc-common）
 * 统一定义请求头名称和 Redis Key 前缀，避免各服务硬编码字符串导致不一致
 */
public interface CommonConstants {

    /** 下游服务接收网关透传的用户 ID 的请求头名称（由网关从 JWT 中解析并注入） */
    String HEADER_USER_ID = "X-User-Id";

    /** 下游服务接收网关透传的用户名的请求头名称（由网关从 JWT claims 中注入） */
    String HEADER_USERNAME = "X-Username";

    /** JWT Bearer Token 前缀（Authorization 头的值格式：Bearer eyJhbGc...） */
    String TOKEN_PREFIX = "Bearer ";

    /** HTTP Authorization 请求头名称（标准 HTTP 鉴权头） */
    String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Redis 黑名单 Key 前缀
     * 完整 Key 格式：token:blacklist:{token_string}
     * 登出时写入，TTL = Token 剩余有效期，网关过滤器检查此 Key 是否存在
     */
    String REDIS_TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Redis 用户 Token Key 前缀（预留字段，用于单点登录/踢出场景）
     * 完整 Key 格式：token:user:{userId}
     */
    String REDIS_USER_TOKEN_PREFIX = "token:user:";
}
