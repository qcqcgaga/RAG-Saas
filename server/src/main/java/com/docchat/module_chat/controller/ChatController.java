package com.docchat.module_chat.controller;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.R;
import com.docchat.module_apikey.service.ApiKeyService;
import com.docchat.module_apikey.dto.ApiKeyValidationResult;
import com.docchat.module_chat.aop.AuthContext;
import com.docchat.module_chat.dto.ChatRequest;
import com.docchat.module_chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 对话控制器
 *
 * V1 变更：支持双鉴权模式
 * - API Key (dc_ 前缀)：正式访客对话，计入用量统计
 * - JWT Token (eyJ 前缀)：预览对话，不计入用量统计
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ApiKeyService apiKeyService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 发起对话（SSE 流式）
     *
     * V1 双鉴权：
     * - Authorization: Bearer dc_xxxx → API Key 鉴权（正式访客）
     * - Authorization: Bearer eyJ... → JWT 鉴权（预览对话）
     */
    @PostMapping(value = "/conversations",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter converse(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChatRequest request) {
        try {
            AuthResult authResult = resolveAuth(authHeader);

            // 设置鉴权上下文（供 ChatStatAspect 使用）
            AuthContext.set(
                    authResult.tenantId,
                    authResult.apiKeyId,
                    authResult.authType,
                    authResult.modelName
            );

            return chatService.converse(request, authResult.tenantId);
        } finally {
            // 注意：不在此处清理 AuthContext，因为异步对话需要用到
            // 清理在 SSE 完成回调中处理（或通过 Filter 后置处理）
        }
    }

    /**
     * 解析鉴权方式
     *
     * 根据 token 前缀自动判断鉴权类型：
     * - dc_ → API Key 鉴权
     * - eyJ → JWT 鉴权
     * - 其他 → Widget Token 鉴权
     */
    private AuthResult resolveAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(40100, "未认证");
        }

        String token = authHeader.substring(7).trim();

        if (token.startsWith("dc_")) {
            return resolveApiKeyAuth(token);
        } else if (token.startsWith("eyJ")) {
            return resolveJwtAuth(token);
        } else {
            // Widget Token 鉴权（UUID 格式，用于聊天组件）
            return resolveWidgetTokenAuth(token);
        }
    }

    /**
     * Widget Token 鉴权（聊天组件使用）
     */
    private AuthResult resolveWidgetTokenAuth(String widgetToken) {
        // 通过 WidgetConfigRepository 查找 token 对应的租户
        // 由于 ChatController 不直接依赖 module_widget，使用简化的查找逻辑
        try {
            Long tenantId = widgetConfigLookupTenantId(widgetToken);
            if (tenantId == null) {
                throw new BizException(40100, "无效的 Widget Token");
            }
            return new AuthResult(tenantId, null, "WIDGET", null);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(40100, "Widget Token 验证失败");
        }
    }

    /**
     * 通过 Widget Token 查找租户 ID
     */
    private Long widgetConfigLookupTenantId(String widgetToken) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM widget_configs WHERE widget_token = ? AND enabled = 1",
                Long.class, widgetToken);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * API Key 鉴权
     */
    private AuthResult resolveApiKeyAuth(String apiKey) {
        String keyHash = sha256(apiKey);
        ApiKeyValidationResult result = apiKeyService.validateKey(keyHash);

        if (!result.isValid()) {
            throw new BizException(40802, "API Key 无效或已吊销");
        }

        // 检查每日限额
        if (!apiKeyService.checkQuota(result.getTenantId())) {
            throw new BizException(40804, "每日调用次数超限");
        }

        // 递增限额计数器
        apiKeyService.incrementQuota(result.getTenantId());

        return new AuthResult(
                result.getTenantId(),
                result.getApiKeyId(),
                "API_KEY",
                null
        );
    }

    /**
     * JWT 鉴权（预览对话）
     *
     * MVP 简化：从 JWT 中解析 tenantId。
     * 后续应集成 Spring Security JWT 解析。
     */
    private AuthResult resolveJwtAuth(String jwtToken) {
        // TODO: 集成 Spring Security JWT 解析，从 Claims 获取 tenantId
        // 当前简化：使用 MVP 的 parseTenantIdFromToken 逻辑
        Long tenantId = parseTenantIdFromJwt(jwtToken);

        return new AuthResult(tenantId, null, "JWT", null);
    }

    /**
     * 简化的 JWT tenantId 解析
     *
     * TODO: 替换为正式的 JWT Claims 解析
     */
    private Long parseTenantIdFromJwt(String jwtToken) {
        // 临时方案：从 JWT 解析 tenantId
        // 后续通过 Spring Security 正式解析
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(
                        java.util.Base64.getUrlDecoder().decode(parts[1]));
                // 简化提取 tenantId
                if (payload.contains("\"tenantId\"")) {
                    int idx = payload.indexOf("\"tenantId\"");
                    int colonIdx = payload.indexOf(":", idx);
                    int commaIdx = payload.indexOf(",", colonIdx);
                    int braceIdx = payload.indexOf("}", colonIdx);
                    int endIdx = Math.min(
                            commaIdx > 0 ? commaIdx : Integer.MAX_VALUE,
                            braceIdx > 0 ? braceIdx : Integer.MAX_VALUE
                    );
                    if (endIdx == Integer.MAX_VALUE) endIdx = payload.length();
                    String value = payload.substring(colonIdx + 1, endIdx).trim();
                    return Long.parseLong(value);
                }
            }
        } catch (Exception e) {
            log.warn("JWT 解析失败，使用默认 tenantId", e);
        }
        return 1L;
    }

    /**
     * SHA-256 哈希
     */
    private String sha256(String input) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * 鉴权结果
     */
    private record AuthResult(Long tenantId, Long apiKeyId, String authType, String modelName) {
    }
}
