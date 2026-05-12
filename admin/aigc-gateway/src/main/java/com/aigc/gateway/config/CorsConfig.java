package com.aigc.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 网关全局 CORS 跨域配置
 * 在 Spring Cloud Gateway（WebFlux）中使用 CorsWebFilter 处理跨域
 * 注意：各下游服务（如 aigc-canvas）不应再配置 CORS，统一在网关处理
 *
 * 开发环境策略：全放行（allowedOriginPatterns = *）
 * 生产环境建议：将 allowedOriginPatterns 改为具体前端域名，如 https://yourdomain.com
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许携带 Cookie/Session（配合 allowedOriginPatterns 使用，不能与 allowedOrigins("*") 同时使用）
        config.setAllowCredentials(true);
        // 允许所有来源（开发环境）；生产环境应改为 List.of("https://yourdomain.com")
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许所有请求头（含自定义头如 Authorization、X-User-Id）
        config.setAllowedHeaders(List.of("*"));
        // 允许所有 HTTP 方法（含 OPTIONS，浏览器预检请求必须）
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 预检请求缓存时间（秒），1 小时内不重复发送 OPTIONS 请求
        config.setMaxAge(3600L);

        // 对所有路径生效
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
