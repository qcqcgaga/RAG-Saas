package com.docchat.module_chat.aop;

/**
 * 鉴权上下文（ThreadLocal）
 *
 * 在鉴权阶段由 AuthResolver 设置，供 ChatStatAspect 读取。
 * 包含：租户ID、API Key ID、鉴权类型、模型名称、Token使用量。
 *
 * 生命周期：请求开始时设置，请求结束时清理。
 *
 * V1 R5: 新增 promptTokens / completionTokens 字段，
 * 由 ChatServiceImpl 在 LLM 调用完成后设置，供 ChatStatAspect 记录。
 */
public class AuthContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> API_KEY_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> AUTH_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<String> MODEL_NAME = new ThreadLocal<>();
    private static final ThreadLocal<Integer> PROMPT_TOKENS = new ThreadLocal<>();
    private static final ThreadLocal<Integer> COMPLETION_TOKENS = new ThreadLocal<>();

    public static void set(Long tenantId, Long apiKeyId, String authType, String modelName) {
        TENANT_ID.set(tenantId);
        API_KEY_ID.set(apiKeyId);
        AUTH_TYPE.set(authType);
        MODEL_NAME.set(modelName);
    }

    /** 设置 Token 使用量（由 ChatServiceImpl 在 LLM 调用完成后设置） */
    public static void setTokenUsage(int promptTokens, int completionTokens) {
        PROMPT_TOKENS.set(promptTokens);
        COMPLETION_TOKENS.set(completionTokens);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Long getApiKeyId() {
        return API_KEY_ID.get();
    }

    public static String getAuthType() {
        return AUTH_TYPE.get();
    }

    public static String getModelName() {
        return MODEL_NAME.get();
    }

    public static int getPromptTokens() {
        Integer val = PROMPT_TOKENS.get();
        return val != null ? val : 0;
    }

    public static int getCompletionTokens() {
        Integer val = COMPLETION_TOKENS.get();
        return val != null ? val : 0;
    }

    public static void clear() {
        TENANT_ID.remove();
        API_KEY_ID.remove();
        AUTH_TYPE.remove();
        MODEL_NAME.remove();
        PROMPT_TOKENS.remove();
        COMPLETION_TOKENS.remove();
    }
}
