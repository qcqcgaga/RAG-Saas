package com.docchat.common.exception;

/**
 * 系统级异常
 *
 * 用于非预期的系统错误，如"数据库连接失败"、"Milvus 不可达"等。
 * 对外返回 5xx 状态码，错误信息不暴露给用户（生产环境）。
 */
public class SystemException extends BaseException {

    public SystemException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public SystemException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
}
