package com.aigc.common.enums;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    TOKEN_EXPIRED(4011, "Token 已过期"),
    TOKEN_INVALID(4012, "Token 无效"),
    USER_NOT_FOUND(40101, "用户不存在"),
    PASSWORD_ERROR(40102, "用户名或密码错误"),
    USER_DISABLED(40103, "账号已被禁用"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
