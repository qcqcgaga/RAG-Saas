package com.docchat.common.event;

import lombok.Getter;

@Getter
public class TenantCreatedEvent {

    private final Long tenantId;

    public TenantCreatedEvent(Long tenantId) {
        this.tenantId = tenantId;
    }
}
