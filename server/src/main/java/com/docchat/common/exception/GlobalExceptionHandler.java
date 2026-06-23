package com.docchat.common.exception;

import com.docchat.common.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 统一捕获所有异常，转换为标准 R<T> 响应格式。
 * - BizException → 4xx，错误信息可展示
 * - SystemException → 5xx，错误信息脱敏
 * - 其他 Exception → 5xx，通用错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBizException(BizException ex) {
        log.warn("业务异常: [{}] {}", ex.getErrorCode(), ex.getErrorMessage());
        return R.fail(ex.getErrorCode(), ex.getErrorMessage());
    }

    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleSystemException(SystemException ex) {
        log.error("系统异常: [{}] {}", ex.getErrorCode(), ex.getErrorMessage(), ex);
        return R.fail(ex.getErrorCode(), "系统繁忙，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception ex) {
        log.error("未预期异常", ex);
        return R.fail("SYSTEM_ERROR", "系统繁忙，请稍后重试");
    }
}
