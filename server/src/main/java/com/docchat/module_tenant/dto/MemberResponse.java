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
}
