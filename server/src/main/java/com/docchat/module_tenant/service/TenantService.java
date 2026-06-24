package com.docchat.module_tenant.service;

import com.docchat.common.response.PageResult;
import com.docchat.module_tenant.dto.*;

public interface TenantService {

    TenantResponse getCurrentTenant();

    TenantResponse updateCurrentTenant(UpdateTenantRequest request);

    PageResult<MemberResponse> listMembers(int page, int size);

    MemberResponse inviteMember(InviteMemberRequest request);

    void updateMemberRole(Long userId, UpdateRoleRequest request);

    void removeMember(Long userId);
}
