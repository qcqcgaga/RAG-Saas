package com.docchat.common.exception;

import lombok.Getter;

/**
 * 业务异常基类
 *
 * 所有业务异常必须继承此类或其子类（BizException / SystemException）。
 * 全局异常处理器（GlobalExceptionHandler）统一捕获并转换为 R<T> 响应。
 */
@Getter
public class BaseException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public BaseException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public BaseException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
