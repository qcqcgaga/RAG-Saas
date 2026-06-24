package com.docchat.module_tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "ADMIN|MEMBER|READONLY", message = "角色必须为ADMIN/MEMBER/READONLY")
    private String role;
}
