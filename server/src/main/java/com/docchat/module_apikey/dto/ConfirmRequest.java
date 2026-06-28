package com.docchat.module_apikey.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 确认操作请求（用于吊销等危险操作的二次确认）
 */
@Data
public class ConfirmRequest {

    @NotNull(message = "请确认操作")
    private Boolean confirm;
}
