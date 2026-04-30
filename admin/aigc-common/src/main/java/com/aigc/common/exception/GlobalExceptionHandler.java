package com.aigc.common.exception;

import com.aigc.common.enums.ResultCode;
import com.aigc.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValidationException(Exception e) {
        String message = e instanceof MethodArgumentNotValidException ex
                ? ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .findFirst().orElse("参数校验失败")
                : e.getMessage();
        return R.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return R.fail(ResultCode.INTERNAL_ERROR);
    }
}
