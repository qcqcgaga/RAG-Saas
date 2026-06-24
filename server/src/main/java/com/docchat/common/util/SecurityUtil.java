package com.docchat.common.util;

import com.docchat.common.config.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 *
 * 提供获取当前用户信息的便捷方法。
 * 禁止从请求参数获取 tenantId，必须从 Token 解析。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** 获取当前用户 ID */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /** 获取当前租户 ID（从 TenantContext，由 TenantFilter 设置） */
    public static Long getCurrentTenantId() {
        return TenantContext.getTenantId();
    }

    /** 获取当前用户角色 */
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().iterator().hasNext()) {
            String authority = auth.getAuthorities().iterator().next().getAuthority();
            return authority.replace("ROLE_", "");
        }
        return null;
    }

    /** 是否是管理员 */
    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentRole());
    }
}
