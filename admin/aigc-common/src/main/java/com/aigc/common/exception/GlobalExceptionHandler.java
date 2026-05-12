package com.aigc.common.exception;

import com.aigc.common.enums.ResultCode;
import com.aigc.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：统一捕获各类异常并转为标准 R 响应
 * 避免在每个 Controller 方法中重复 try-catch
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（主动抛出的预期异常）
     * 只记录 WARN 级别日志，不打印堆栈
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常（@Valid 校验失败）
     * 提取第一个字段校验错误信息返回给前端
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValidationException(Exception e) {
        String message = e instanceof MethodArgumentNotValidException ex
                // 从 BindingResult 中提取第一个字段错误
                ? ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .findFirst().orElse("参数校验失败")
                : e.getMessage();
        return R.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 未知异常（系统异常）
     * 记录 ERROR 日志（含堆栈），对外只返回通用错误信息
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return R.fail(ResultCode.INTERNAL_ERROR);
    }
}
