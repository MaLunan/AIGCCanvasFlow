package com.aigc.user.config;

import com.aigc.common.id.TimestampIdGenerator;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * aigc-user 服务的 MyBatis-Plus 配置
 * 与 aigc-canvas 的配置类似，但 workerId 默认值不同（2），
 * 保证多实例部署时 ID 不冲突（TimestampIdGenerator 基于 workerId 区分实例）
 */
@Configuration
public class MybatisPlusConfig {

    /** worker-id 默认 2（aigc-user），多实例时配置不同值 */
    @Value("${snowflake.worker-id:2}")
    private long workerId;

    /** 注册 MySQL 分页插件 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /** 注册自定义时间戳 ID 生成器 */
    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new TimestampIdGenerator(workerId);
    }

    /**
     * 自动填充处理器（内部静态类）：
     * INSERT 时填充 createTime + updateTime，UPDATE 时仅更新 updateTime
     */
    @Component
    public static class MetaObjectFillHandler implements MetaObjectHandler {
        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }
    }
}
