package com.docchat.common.response;

import com.docchat.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码常量定义
 *
 * 数字编码规则：
 * - 通用：40000/40100/40300/40400/40900/50000
 * - AUTH 模块：401xx
 * - MEMBER 模块：403xx
 * - KNOWLEDGE 模块：404xx
 * - TASK 模块：405xx
 * - CHAT 模块：406xx
 * - WIDGET 模块：407xx
 * - STAT 模块：408xx
 * - EVAL 模块：410xx
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    SUCCESS(0, "success"),
    PARAM_INVALID(40000, "参数校验失败"),
    UNAUTHORIZED(40100, "未认证"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    INTERNAL_ERROR(50000, "服务器内部错误"),

    // AUTH 模块
    AUTH_LOGIN_FAILED(40101, "邮箱或密码错误"),
    AUTH_ACCOUNT_LOCKED(40102, "账户已锁定，请30分钟后重试"),
    AUTH_ACCOUNT_DISABLED(40103, "账户已禁用"),
    AUTH_EMAIL_EXISTS(40104, "邮箱已注册"),

    // MEMBER 模块
    MEMBER_ALREADY_EXISTS(40301, "该邮箱已是团队成员"),
    MEMBER_INVITE_FORBIDDEN(40302, "无权邀请成员"),

    // KNOWLEDGE 模块
    KNOWLEDGE_FILE_TYPE_NOT_ALLOWED(40401, "不支持的文件类型"),
    KNOWLEDGE_FILE_TOO_LARGE(40402, "文件大小超过限制"),
    KNOWLEDGE_FILE_HEADER_MISMATCH(40403, "文件头与扩展名不匹配"),
    KNOWLEDGE_DOCUMENT_NOT_FOUND(40404, "文档不存在"),
    KNOWLEDGE_DOCUMENT_LIMIT_EXCEEDED(40405, "文档数量超过限制"),

    // TASK 模块
    TASK_NOT_FOUND(40500, "任务不存在"),
    TASK_NOT_FAILED(40501, "任务未失败，无法重试"),
    TASK_MAX_RETRY_EXCEEDED(40502, "已达最大重试次数"),

    // CHAT 模块
    CHAT_QUESTION_EMPTY(40601, "问题不能为空"),
    CHAT_QUESTION_TOO_LONG(40602, "问题超过500字符"),
    CHAT_WIDGET_DISABLED(40603, "聊天组件已禁用"),
    CHAT_LLM_UNAVAILABLE(40604, "LLM服务暂时不可用"),

    // WIDGET 模块
    WIDGET_TOKEN_INVALID(40701, "Widget Token无效"),
    WIDGET_NOT_FOUND(40702, "聊天组件配置不存在"),

    // STAT 模块
    STAT_PERIOD_INVALID(40801, "统计周期参数无效"),
    STAT_METRIC_INVALID(40802, "统计指标参数无效"),

    // APIKEY 模块
    APIKEY_NOT_FOUND(40910, "API Key 不存在"),
    APIKEY_LIMIT_EXCEEDED(40913, "API Key 数量超过限制"),
    APIKEY_ALREADY_REVOKED(40914, "API Key 已被吊销"),

    // EVAL 模块
    EVAL_SET_NOT_FOUND(41001, "评测集不存在"),
    EVAL_SET_LIMIT_EXCEEDED(41002, "评测集数量超过限制"),
    EVAL_PAIR_LIMIT_EXCEEDED(41003, "问答对数量超过限制"),
    EVAL_ALREADY_RUNNING(41004, "评测已在执行中"),
    EVAL_SET_EMPTY(41005, "评测集无问答对");

    private final int code;
    private final String msg;

    public BizException asBizException() {
        return new BizException(this.code, this.msg);
    }

    public BizException asBizException(String detail) {
        return new BizException(this.code, this.msg + ": " + detail);
    }
}
