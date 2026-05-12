package com.aigc.common.exception;

import com.aigc.common.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常：表示预期的业务逻辑错误（如用户不存在、密码错误、无权限等）
 * 与系统异常（Exception）的区别：
 * - 业务异常：可预期、可处理，GlobalExceptionHandler 以 WARN 级别记录
 * - 系统异常：不可预期，以 ERROR 级别记录并打印堆栈
 *
 * 提供两种构造方式：
 * 1. 使用 ResultCode 枚举（标准化错误码和消息）
 * 2. 使用自定义 code + message（灵活处理特殊业务场景）
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码，与 ResultCode 枚举或自定义 code 对应 */
    private final int code;

    /**
     * 使用预定义 ResultCode 构造业务异常
     * 推荐优先使用此构造方式，保持错误码一致性
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage()); // 将 message 传给 RuntimeException，支持 e.getMessage()
        this.code = resultCode.getCode();
    }

    /**
     * 使用自定义 code 和 message 构造业务异常
     * 用于 ResultCode 枚举未覆盖的场景（如第三方服务返回的特定错误码）
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
