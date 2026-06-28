package com.docchat.module_chat.dto;

import lombok.Builder;
import lombok.Data;

/**
 * LLM 配置响应
 */
@Data
@Builder
public class LlmConfigResponse {

    private String apiUrl;
    private String apiKeyMasked;
    private String modelName;
    private Short status;
    private String updatedAt;
}
