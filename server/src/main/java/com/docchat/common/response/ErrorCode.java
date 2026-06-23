package com.docchat.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码常量定义
 *
 * 格式：{MODULE}_{ERROR_TYPE}
 * 示例：TENANT_NOT_FOUND、KNOWLEDGE_UPLOAD_FAILED
 *
 * 每个模块可在此定义通用错误码，模块专属错误码建议在模块内定义。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== 通用错误码 =====
    SYSTEM_ERROR("SYSTEM_ERROR", "系统繁忙，请稍后重试"),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    UNAUTHORIZED("UNAUTHORIZED", "未登录或登录已过期"),
    FORBIDDEN("FORBIDDEN", "无权限访问"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),

    // ===== 租户模块 =====
    TENANT_NOT_FOUND("TENANT_NOT_FOUND", "租户不存在"),
    TENANT_EXPIRED("TENANT_EXPIRED", "租户已过期"),

    // ===== 知识库模块 =====
    KNOWLEDGE_NOT_FOUND("KNOWLEDGE_NOT_FOUND", "文档不存在"),
    KNOWLEDGE_UPLOAD_FAILED("KNOWLEDGE_UPLOAD_FAILED", "文档上传失败"),
    KNOWLEDGE_TYPE_NOT_ALLOWED("KNOWLEDGE_TYPE_NOT_ALLOWED", "不支持的文件类型"),

    // ===== 任务模块 =====
    TASK_NOT_FOUND("TASK_NOT_FOUND", "任务不存在"),
    TASK_PROCESSING_FAILED("TASK_PROCESSING_FAILED", "任务处理失败"),

    // ===== 对话模块 =====
    CHAT_KNOWLEDGE_EMPTY("CHAT_KNOWLEDGE_EMPTY", "知识库为空，无法对话"),

    // ===== 聊天组件模块 =====
    WIDGET_NOT_FOUND("WIDGET_NOT_FOUND", "聊天组件配置不存在"),

    // ===== API Key 模块 (V1) =====
    APIKEY_NOT_FOUND("APIKEY_NOT_FOUND", "API Key 不存在"),
    APIKEY_REVOKED("APIKEY_REVOKED", "API Key 已吊销"),
    APIKEY_LIMIT_EXCEEDED("APIKEY_LIMIT_EXCEEDED", "调用次数已达上限"),

    // ===== 评测模块 (V1) =====
    EVAL_NOT_FOUND("EVAL_NOT_FOUND", "评测集不存在");

    private final String code;
    private final String message;
}
