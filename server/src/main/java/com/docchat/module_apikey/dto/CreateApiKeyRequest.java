package com.docchat.module_apikey.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建 API Key 请求
 */
@Data
public class CreateApiKeyRequest {

    @Size(max = 50, message = "名称最多50字符")
    private String name;
}
