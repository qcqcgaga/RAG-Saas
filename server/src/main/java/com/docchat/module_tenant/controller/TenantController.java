package com.docchat.module_tenant.controller;

import com.docchat.common.response.PageResult;
import com.docchat.common.response.R;
import com.docchat.module_tenant.dto.*;
import com.docchat.module_tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/current")
    public R<TenantResponse> getCurrentTenant() {
        return R.ok(tenantService.getCurrentTenant());
    }

    @PutMapping("/current")
    public R<TenantResponse> updateCurrentTenant(@Valid @RequestBody UpdateTenantRequest request) {
        return R.ok(tenantService.updateCurrentTenant(request));
    }

    @GetMapping("/members")
    public R<PageResult<MemberResponse>> listMembers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return R.ok(tenantService.listMembers(page, size));
    }

    @PostMapping("/members")
    public R<MemberResponse> inviteMember(@Valid @RequestBody InviteMemberRequest request) {
        return R.ok(tenantService.inviteMember(request));
    }

    @PutMapping("/members/{userId}/role")
    public R<Void> updateMemberRole(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateRoleRequest request
    ) {
        tenantService.updateMemberRole(userId, request);
        return R.ok(null);
    }

    @DeleteMapping("/members/{userId}")
    public R<Void> removeMember(@PathVariable Long userId) {
        tenantService.removeMember(userId);
        return R.ok(null);
    }
}
