package com.aigc.canvas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String headerName = "X-User-Id";
        return new OpenAPI()
                .info(new Info()
                        .title("AIGCCanvasFlow - Canvas API")
                        .description("画布服务：项目 / 模板 / 模型 / 资产 / Agent")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(headerName))
                .components(new Components()
                        .addSecuritySchemes(headerName,
                                new SecurityScheme()
                                        .name(headerName)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("用户ID（开发阶段直接填 1）")));
    }
}
