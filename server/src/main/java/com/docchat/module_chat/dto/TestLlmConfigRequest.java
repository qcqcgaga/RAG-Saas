package com.docchat.module_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * LLM 连通性测试请求
 */
@Data
public class TestLlmConfigRequest {

    @NotBlank(message = "API URL 不能为空")
    @Pattern(regexp = "^https://.*", message = "API URL 必须是 HTTPS")
    @Size(max = 500)
    private String apiUrl;

    @NotBlank(message = "API Key 不能为空")
    @Size(max = 200)
    private String apiKey;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 50)
    private String modelName;
}
