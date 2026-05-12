package com.aigc.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关启动类（Spring Cloud Gateway）
 * 核心职责：
 * - 统一入口：所有前端请求经过网关路由转发到各微服务
 * - JWT 鉴权：JwtAuthFilter 全局过滤器验证 Token，白名单路径直接放行
 * - 跨域处理：CorsConfig 统一处理 CORS（下游服务不需重复配置）
 * - 路由配置：通过 Nacos 动态路由规则转发到对应服务（/auth/** → aigc-auth 等）
 * 注意：使用 WebFlux 响应式框架，不支持 Spring MVC
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
