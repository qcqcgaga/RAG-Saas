package com.docchat.module_apikey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重命名 API Key 请求
 */
@Data
public class RenameKeyRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 50, message = "名称最多50字符")
    private String name;
}
