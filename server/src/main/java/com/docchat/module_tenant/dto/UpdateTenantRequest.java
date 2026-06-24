package com.docchat.module_tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTenantRequest {

    @NotBlank(message = "租户名称不能为空")
    @Size(min = 1, max = 100, message = "租户名称1-100字符")
    private String name;
}
