package com.docchat.module_apikey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * API Key 校验结果
 */
@Data
@AllArgsConstructor
public class ApiKeyValidationResult {

    /** 校验是否通过 */
    private boolean valid;

    /** 所属租户 ID */
    private Long tenantId;

    /** API Key ID */
    private Long apiKeyId;

    /** 认证类型标识 */
    private String authType;
}
