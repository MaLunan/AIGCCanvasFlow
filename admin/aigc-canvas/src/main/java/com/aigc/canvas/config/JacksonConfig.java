package com.aigc.canvas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson 配置：将 Long / long 序列化为字符串。
 * 防止雪花 ID 超过 JS Number 安全整数上限（2^53-1）导致精度丢失。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance)
                                 .serializerByType(long.class, ToStringSerializer.instance);
    }
}
