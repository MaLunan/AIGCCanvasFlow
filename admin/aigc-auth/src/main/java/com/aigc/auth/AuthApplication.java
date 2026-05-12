package com.aigc.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证服务启动类
 * 提供：登录（/auth/login）、Token 刷新（/auth/refresh）、登出（/auth/logout）
 * 依赖 aigc-user 服务（通过 UserFeignClient 调用）查询用户信息进行密码校验
 * @EnableFeignClients 启用 Feign 客户端扫描，basePackages = "com.aigc" 包含所有 Feign 接口
 */
@SpringBootApplication(scanBasePackages = "com.aigc")
@EnableFeignClients(basePackages = "com.aigc")
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
