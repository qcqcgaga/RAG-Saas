package com.docchat.common.exception;

/**
 * 可预期的业务异常
 *
 * 用于业务规则校验失败的场景，如"文档不存在"、"租户已过期"等。
 * 对外返回 4xx 状态码，错误信息可安全展示给用户。
 */
public class BizException extends BaseException {

    public BizException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public BizException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
}
