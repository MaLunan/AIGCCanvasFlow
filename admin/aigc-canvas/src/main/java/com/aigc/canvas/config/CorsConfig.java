package com.aigc.canvas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * aigc-canvas 服务 CORS 配置（Spring MVC WebMvcConfigurer）
 * 注意：统一跨域处理应在网关（aigc-gateway）完成
 * 此处配置是为了在直接调用 canvas 服务（绕过网关）时也能正常工作（如本地开发调试）
 * 生产环境若所有请求均通过网关，此配置可省略
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 允许所有来源和方法（开发环境配置）
     * 生产环境建议将 allowedOriginPatterns 改为实际前端域名
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")             // 对所有路径生效
                .allowedOriginPatterns("*")     // 允许所有来源（开发环境）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")            // 允许所有请求头（含 Authorization、X-User-Id）
                .allowCredentials(true)         // 允许携带 Cookie
                .maxAge(3600);                  // 预检请求缓存 1 小时
    }
}
