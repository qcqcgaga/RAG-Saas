package com.docchat.module_apikey.controller;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.response.R;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_apikey.dto.ApiKeyResponse;
import com.docchat.module_apikey.dto.ConfirmRequest;
import com.docchat.module_apikey.dto.CreateApiKeyRequest;
import com.docchat.module_apikey.dto.RenameKeyRequest;
import com.docchat.module_apikey.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Key 管理控制器
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public R<ApiKeyResponse> createKey(
            @Valid @RequestBody CreateApiKeyRequest request) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(apiKeyService.createKey(tenantId, request.getName()));
    }

    @GetMapping
    public R<List<ApiKeyResponse>> listKeys() {
        Long tenantId = requireCurrentTenantId();
        return R.ok(apiKeyService.listKeys(tenantId));
    }

    @DeleteMapping("/{keyId}")
    public R<ApiKeyResponse> revokeKey(
            @PathVariable Long keyId,
            @Valid @RequestBody ConfirmRequest request) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(apiKeyService.revokeKey(tenantId, keyId));
    }

    @PutMapping("/{keyId}/name")
    public R<ApiKeyResponse> renameKey(
            @PathVariable Long keyId,
            @Valid @RequestBody RenameKeyRequest request) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(apiKeyService.renameKey(tenantId, keyId, request.getName()));
    }

    /**
     * 要求当前租户ID，为空则抛出401业务异常
     */
    private Long requireCurrentTenantId() {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        return tenantId;
    }
}
