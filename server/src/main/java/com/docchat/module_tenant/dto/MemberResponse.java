package com.docchat.module_tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private Long userId;
    private String email;
    private String displayName;
    private String role;
    private Short status;
    private OffsetDateTime createdAt;

    /**
     * 被邀请成员的初始密码，仅在邀请接口返回，其他接口不返回该字段
     */
    private String initialPassword;
}
