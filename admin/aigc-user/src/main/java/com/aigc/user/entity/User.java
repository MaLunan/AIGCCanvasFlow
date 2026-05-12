package com.aigc.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体：对应数据库表 t_user
 * 存储用户基本信息，密码字段存储 BCrypt 哈希值（不可逆）
 */
@Data
@TableName("t_user")
public class User {

    /** 主键 ID，使用 TimestampIdGenerator 生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录用户名（唯一，数据库有唯一索引约束） */
    private String username;

    /** BCrypt 加密后的密码哈希（60 位字符串，含算法标识、cost factor 和盐值） */
    private String password;

    /** 昵称（前端展示，默认与 username 相同） */
    private String nickname;

    /** 邮箱（可选，用于找回密码等功能） */
    private String email;

    /** 头像 URL */
    private String avatar;

    /** 账号状态：0=禁用（登录时返回 USER_DISABLED），1=正常 */
    private Integer status;

    /** 注册时间（INSERT 时由 MetaObjectFillHandler 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间（INSERT 和 UPDATE 时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0=正常，1=已删除（删除操作不物理删除记录） */
    @TableLogic
    private Integer deleted;
}
