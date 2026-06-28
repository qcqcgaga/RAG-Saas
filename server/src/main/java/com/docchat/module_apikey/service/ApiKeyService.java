package com.docchat.module_apikey.service;

import com.docchat.module_apikey.dto.ApiKeyResponse;
import com.docchat.module_apikey.dto.ApiKeyValidationResult;

import java.util.List;

/**
 * API Key 服务接口
 */
public interface ApiKeyService {

    /** 创建 API Key */
    ApiKeyResponse createKey(Long tenantId, String name);

    /** 列出租户下所有 Key */
    List<ApiKeyResponse> listKeys(Long tenantId);

    /** 吊销 API Key */
    ApiKeyResponse revokeKey(Long tenantId, Long keyId);

    /** 重命名 API Key */
    ApiKeyResponse renameKey(Long tenantId, Long keyId, String name);

    /** 校验 API Key（用于鉴权拦截器） */
    ApiKeyValidationResult validateKey(String keyHash);

    /** 检查租户当日配额是否已用完 */
    boolean checkQuota(Long tenantId);

    /** 递增租户当日配额计数 */
    void incrementQuota(Long tenantId);
}
