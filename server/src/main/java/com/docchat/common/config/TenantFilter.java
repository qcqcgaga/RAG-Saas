package com.docchat.common.config;

import com.docchat.common.constant.CommonConstant;
import com.docchat.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 租户上下文过滤器
 *
 * 每个请求：
 * 1. 生成 Request-Id 写入 MDC（链路追踪）
 * 2. 解析 JWT Token，设置 TenantContext
 * 3. 请求结束后清理 TenantContext 和 MDC
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 1. 设置 Request-Id
            String requestId = UUID.randomUUID().toString().replace("-", "");
            MDC.put(CommonConstant.REQUEST_ID_KEY, requestId);

            // 2. 从 JWT Token 解析租户 ID
            String token = extractToken(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.parseToken(token);
                Long tenantId = jwtUtil.getTenantId(claims);
                TenantContext.setTenantId(tenantId);
                MDC.put(CommonConstant.TENANT_ID_KEY, tenantId.toString());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(CommonConstant.AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstant.TOKEN_PREFIX.length());
        }
        return null;
    }
}
