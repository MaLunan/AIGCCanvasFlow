package com.aigc.common.enums;

import lombok.Getter;

/**
 * 统一业务状态码枚举
 * 与前端约定：code=200 表示成功，其余均为各类错误
 * GlobalExceptionHandler 使用此枚举返回标准化错误响应
 */
@Getter
public enum ResultCode {
    // ── 成功 ──────────────────────────────────────────────────────────────
    SUCCESS(200, "操作成功"),

    // ── 客户端错误（4xx） ─────────────────────────────────────────────────
    BAD_REQUEST(400, "请求参数错误"),   // 参数校验失败（@Valid）
    UNAUTHORIZED(401, "未认证，请先登录"), // 未携带 Token 或 Token 无效
    FORBIDDEN(403, "无权限访问"),       // 越权操作（如操作他人资源）
    NOT_FOUND(404, "资源不存在"),       // 查询的数据不存在

    // ── Token 相关错误（4011/4012，细化 401） ────────────────────────────
    TOKEN_EXPIRED(4011, "Token 已过期"),  // access_token 到期，前端应尝试 refresh
    TOKEN_INVALID(4012, "Token 无效"),   // 签名不合法或格式错误

    // ── 用户相关错误（401xx，细化用户认证） ─────────────────────────────────
    USER_NOT_FOUND(40101, "用户不存在"),      // 登录时用户名不存在
    PASSWORD_ERROR(40102, "用户名或密码错误"), // 密码 BCrypt 校验失败（不细分防枚举）
    USER_DISABLED(40103, "账号已被禁用"),     // 账号 status=0

    // ── 服务端错误（5xx） ─────────────────────────────────────────────────
    INTERNAL_ERROR(500, "服务器内部错误");  // 未知系统异常，对外不暴露详情

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
