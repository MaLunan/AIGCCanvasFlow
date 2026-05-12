package com.aigc.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户视图 VO：对外返回的用户信息（不含 password 字段）
 * BeanUtils.copyProperties 时 UserVO 无 password 字段，自动过滤密码
 */
@Data
public class UserVO {
    /** 用户 ID */
    private Long id;
    /** 用户名 */
    private String username;
    /** 昵称 */
    private String nickname;
    /** 邮箱 */
    private String email;
    /** 头像 URL */
    private String avatar;
    /** 账号状态：0=禁用，1=正常 */
    private Integer status;
    /** 注册时间 */
    private LocalDateTime createTime;
}
