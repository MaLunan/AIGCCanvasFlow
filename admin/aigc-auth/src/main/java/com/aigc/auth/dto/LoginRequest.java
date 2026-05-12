package com.aigc.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 * 使用 @NotBlank 校验必填字段，GlobalExceptionHandler 统一处理校验失败
 */
@Data
public class LoginRequest {
    /** 用户名（不能为空或全空白） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（明文，后端使用 BCrypt 校验） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
