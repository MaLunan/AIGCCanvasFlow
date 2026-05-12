package com.aigc.canvas.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 实现 MetaObjectHandler 接口，在 INSERT/UPDATE 时自动填充时间字段
 * 配合实体类中的 @TableField(fill = FieldFill.INSERT) 和 @TableField(fill = FieldFill.INSERT_UPDATE) 注解使用
 * 避免每个 Service 方法手动设置 createTime/updateTime
 */
@Component
public class MetaObjectFillHandler implements MetaObjectHandler {

    /**
     * INSERT 时自动填充：createTime 和 updateTime 均设为当前时间
     * 使用 strictInsertFill 方法：只有字段值为 null 时才填充（不覆盖已设置的值）
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /**
     * UPDATE 时自动填充：updateTime 设为当前时间
     * strictUpdateFill 只填充非 null 字段（与 strictInsertFill 行为一致）
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
