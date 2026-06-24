package com.docchat.common.exception;

import com.docchat.common.response.ErrorCode;

/**
 * 可预期的业务异常
 *
 * 用于业务规则校验失败的场景，如"文档不存在"、"租户已过期"等。
 * 对外返回 4xx 状态码，错误信息可安全展示给用户。
 */
public class BizException extends BaseException {

    public BizException(int code, String msg) {
        super(code, msg);
    }

    public BizException(int code, String msg, Throwable cause) {
        super(code, msg, cause);
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMsg());
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.getCode(), errorCode.getMsg() + ": " + detail);
    }
}
