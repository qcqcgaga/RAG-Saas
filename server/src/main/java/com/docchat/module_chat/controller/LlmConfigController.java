package com.docchat.module_chat.controller;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.response.R;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_chat.dto.LlmConfigResponse;
import com.docchat.module_chat.dto.TestLlmConfigRequest;
import com.docchat.module_chat.dto.UpdateLlmConfigRequest;
import com.docchat.module_chat.entity.TenantLlmConfig;
import com.docchat.module_chat.service.LlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * LLM 配置控制器
 *
 * 提供租户级别的 LLM API 配置管理。
 * 仅管理员可操作（getCurrentTenantId 内含管理员校验）。
 */
@RestController
@RequestMapping("/api/v1/llm-config")
@RequiredArgsConstructor
public class LlmConfigController {

    private final LlmService llmService;

    /**
     * 获取当前租户的 LLM 配置
     */
    @GetMapping
    public R<LlmConfigResponse> getConfig() {
        Long tenantId = requireAdminTenantId();
        TenantLlmConfig config = llmService.getTenantLlmConfig(tenantId);
        if (config == null) {
            return R.ok(LlmConfigResponse.builder()
                    .apiUrl("")
                    .apiKeyMasked("")
                    .modelName("")
                    .status((short) 0)
                    .build());
        }
        String keyMasked = maskKey(config.getApiKeyEncrypted());
        return R.ok(LlmConfigResponse.builder()
                .apiUrl(config.getApiUrl())
                .apiKeyMasked(keyMasked)
                .modelName(config.getModelName())
                .status(config.getStatus())
                .updatedAt(config.getUpdatedAt().toString())
                .build());
    }

    /**
     * 更新 LLM 配置
     */
    @PutMapping
    public R<LlmConfigResponse> updateConfig(
            @Valid @RequestBody UpdateLlmConfigRequest request) {
        Long tenantId = requireAdminTenantId();

        TenantLlmConfig config = llmService.getTenantLlmConfig(tenantId);
        if (config == null) {
            config = new TenantLlmConfig();
            config.setTenantId(tenantId);
            config.setCreatedAt(Instant.now());
        }
        config.setApiUrl(request.getApiUrl());
        // TODO: 加密 API Key，当前为临时方案
        config.setApiKeyEncrypted(request.getApiKey());
        config.setModelName(request.getModelName());
        config.setStatus((short) 1);
        config.setUpdatedAt(Instant.now());

        llmService.saveTenantLlmConfig(config);

        String keyMasked = maskKey(request.getApiKey());
        return R.ok(LlmConfigResponse.builder()
                .apiUrl(config.getApiUrl())
                .apiKeyMasked(keyMasked)
                .modelName(config.getModelName())
                .status(config.getStatus())
                .updatedAt(config.getUpdatedAt().toString())
                .build());
    }

    /**
     * 删除 LLM 配置（恢复系统默认）
     */
    @DeleteMapping
    public R<Void> deleteConfig() {
        Long tenantId = requireAdminTenantId();
        llmService.deleteTenantLlmConfig(tenantId);
        return R.ok(null);
    }

    /**
     * 测试 LLM 连通性
     */
    @PostMapping("/test")
    public R<LlmService.LlmTestResult> testConnection(
            @Valid @RequestBody TestLlmConfigRequest request) {
        requireAdminTenantId();
        LlmService.LlmTestResult result = llmService.testConnection(
                request.getApiUrl(), request.getApiKey(), request.getModelName());
        if (!result.connected()) {
            throw new BizException(41102, "LLM 连通性测试失败");
        }
        return R.ok(result);
    }

    /**
     * 脱敏 Key
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****"
                + key.substring(key.length() - 4);
    }

    /**
     * 要求当前用户是管理员，并返回租户 ID。
     * 非管理员抛出 FORBIDDEN 异常。
     */
    private Long requireAdminTenantId() {
        if (!SecurityUtil.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可操作 LLM 配置");
        }
        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        return tenantId;
    }
}
