package com.docchat.common.config;

/**
 * 租户上下文 — 基于 ThreadLocal 存储当前请求的租户 ID
 *
 * 通过 TenantFilter 在请求开始时设置，请求结束时清除。
 * 禁止在 Service 层手动传递 tenant_id 做过滤。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
