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

    private final int code;
    private final String msg;

    public BaseException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BaseException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}
