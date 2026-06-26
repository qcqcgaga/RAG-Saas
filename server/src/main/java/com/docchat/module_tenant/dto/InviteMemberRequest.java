package com.docchat.module_tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InviteMemberRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "ADMIN|MEMBER|READONLY", message = "角色必须为ADMIN/MEMBER/READONLY")
    private String role;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在8-64位之间")
    @Pattern(regexp = ".*[A-Z].*", message = "密码必须包含大写字母")
    @Pattern(regexp = ".*[a-z].*", message = "密码必须包含小写字母")
    @Pattern(regexp = ".*\\d.*", message = "密码必须包含数字")
    private String password;
}
