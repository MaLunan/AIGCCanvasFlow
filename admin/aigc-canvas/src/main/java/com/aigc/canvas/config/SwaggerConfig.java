package com.aigc.canvas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc（OpenAPI 3.0）Swagger UI 配置
 * 访问地址：http://localhost:{port}/swagger-ui.html
 * 在 Swagger UI 中填写 X-User-Id 请求头（开发阶段直接填用户 ID），
 * 模拟网关透传的用户信息，方便接口调试
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String headerName = "X-User-Id";
        return new OpenAPI()
                // API 基本信息
                .info(new Info()
                        .title("AIGCCanvasFlow - Canvas API")
                        .description("画布服务：项目 / 模板 / 模型 / 资产 / Agent")
                        .version("1.0.0"))
                // 全局安全要求：所有接口默认需要 X-User-Id 请求头
                .addSecurityItem(new SecurityRequirement().addList(headerName))
                // 注册安全方案：API Key 方式，通过 Header 传递
                .components(new Components()
                        .addSecuritySchemes(headerName,
                                new SecurityScheme()
                                        .name(headerName)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("用户ID（开发阶段直接填 1）")));
    }
}
