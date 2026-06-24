package com.docchat.module_tenant.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.response.PageResult;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_tenant.dto.*;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public TenantResponse getCurrentTenant() {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "租户不存在"));

        long memberCount = userRepository.countByTenantId(tenantId);

        return TenantResponse.builder()
            .id(tenant.getId())
            .name(tenant.getName())
            .slug(tenant.getSlug())
            .status(tenant.getStatus())
            .memberCount(memberCount)
            .documentCount(0L)
            .createdAt(tenant.getCreatedAt())
            .build();
    }

    @Override
    @Transactional
    public TenantResponse updateCurrentTenant(UpdateTenantRequest request) {
        checkAdminRole();

        Long tenantId = SecurityUtil.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "租户不存在"));

        tenant.setName(request.getName());
        tenant = tenantRepository.save(tenant);

        return TenantResponse.builder()
            .id(tenant.getId())
            .name(tenant.getName())
            .slug(tenant.getSlug())
            .status(tenant.getStatus())
            .memberCount(userRepository.countByTenantId(tenantId))
            .documentCount(0L)
            .createdAt(tenant.getCreatedAt())
            .build();
    }

    @Override
    public PageResult<MemberResponse> listMembers(int page, int size) {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        Page<User> userPage = userRepository.findByTenantId(
            tenantId, PageRequest.of(page - 1, size)
        );

        var list = userPage.getContent().stream()
            .map(this::toMemberResponse)
            .toList();

        return PageResult.of(list, userPage.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public MemberResponse inviteMember(InviteMemberRequest request) {
        checkAdminRole();

        Long tenantId = SecurityUtil.getCurrentTenantId();

        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new BizException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        String randomPassword = UUID.randomUUID().toString().substring(0, 12);

        User user = User.builder()
            .tenantId(tenantId)
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(randomPassword))
            .role(request.getRole())
            .status((short) 1)
            .build();

        user = userRepository.save(user);

        log.info("成员邀请成功: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        return toMemberResponse(user);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long userId, UpdateRoleRequest request) {
        checkAdminRole();
        checkNotSelf(userId);

        User user = findUserAndCheckOwnership(userId);
        user.setRole(request.getRole());
        userRepository.save(user);

        log.info("成员角色变更: userId={}, newRole={}", userId, request.getRole());
    }

    @Override
    @Transactional
    public void removeMember(Long userId) {
        checkAdminRole();
        checkNotSelf(userId);

        User user = findUserAndCheckOwnership(userId);
        userRepository.delete(user);

        log.info("成员移除: userId={}", userId);
    }

    private void checkAdminRole() {
        if (!SecurityUtil.isAdmin()) {
            throw new BizException(ErrorCode.MEMBER_INVITE_FORBIDDEN);
        }
    }

    private void checkNotSelf(Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能操作自己");
        }
    }

    private User findUserAndCheckOwnership(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "用户不存在"));

        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (!user.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    private MemberResponse toMemberResponse(User user) {
        return MemberResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .role(user.getRole())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
