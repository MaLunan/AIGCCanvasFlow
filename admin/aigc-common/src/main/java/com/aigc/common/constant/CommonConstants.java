package com.aigc.common.constant;

public interface CommonConstants {
    String HEADER_USER_ID = "X-User-Id";
    String HEADER_USERNAME = "X-Username";
    String TOKEN_PREFIX = "Bearer ";
    String AUTHORIZATION_HEADER = "Authorization";
    String REDIS_TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    String REDIS_USER_TOKEN_PREFIX = "token:user:";
}
