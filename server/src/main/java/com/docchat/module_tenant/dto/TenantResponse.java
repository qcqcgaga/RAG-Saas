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
public class TenantResponse {
    private Long id;
    private String name;
    private String slug;
    private Short status;
    private long memberCount;
    private long documentCount;
    private OffsetDateTime createdAt;
}
