package com.aigc.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建/注册请求 DTO
 * 使用 Bean Validation 注解（@NotBlank、@Size、@Email）做参数校验
 * 校验失败由 GlobalExceptionHandler.handleValidationException 统一处理
 */
@Data
public class UserCreateRequest {
    /** 用户名：必填，长度 3-20 位，唯一性在 Service 层校验 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    /** 密码（明文）：必填，长度 6-20 位，Service 层 BCrypt 加密后存储 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    /** 昵称（可选），未填时 Service 层默认使用 username */
    private String nickname;

    /** 邮箱（可选），填写时校验格式 */
    @Email(message = "邮箱格式不正确")
    private String email;
}
