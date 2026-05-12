package com.aigc.canvas.config;

import com.aigc.common.id.TimestampIdGenerator;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 核心配置
 * - 注册分页插件（MySQL 方言）
 * - 注册自定义 ID 生成器（TimestampIdGenerator）
 * - 扫描 Mapper 接口（@MapperScan）
 */
@Configuration
@org.mybatis.spring.annotation.MapperScan("com.aigc.canvas.mapper")
public class MybatisPlusConfig {

    /**
     * 节点编号（0-31），多实例部署时每个实例配置不同的 worker-id 保证 ID 唯一性
     * 单机开发环境默认为 1
     */
    @Value("${snowflake.worker-id:1}")
    private long workerId;

    /**
     * MyBatis-Plus 插件链配置
     * 添加分页插件（PaginationInnerInterceptor），自动处理 MySQL 的 LIMIT 分页
     * 不配置此插件时，Page 查询会返回全量数据
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自定义 ID 生成器：使用 TimestampIdGenerator（时间戳 + workerId + 序列号）
     * 替代 MyBatis-Plus 默认的 Snowflake ID，格式可读性更好（含时间信息）
     * 对应实体类中 @TableId(type = IdType.ASSIGN_ID) 注解
     */
    @Bean
    public IdentifierGenerator identifierGenerator() {
        return new TimestampIdGenerator(workerId);
    }
}
