package com.docchat.common.exception;

import com.docchat.common.response.ErrorCode;

/**
 * 系统级异常
 *
 * 用于非预期的系统错误，如"数据库连接失败"、"Milvus 不可达"等。
 * 对外返回 5xx 状态码，错误信息不暴露给用户（生产环境）。
 */
public class SystemException extends BaseException {

    public SystemException(int code, String msg) {
        super(code, msg);
    }

    public SystemException(int code, String msg, Throwable cause) {
        super(code, msg, cause);
    }

    public SystemException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMsg());
    }

    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), errorCode.getMsg(), cause);
    }
}
