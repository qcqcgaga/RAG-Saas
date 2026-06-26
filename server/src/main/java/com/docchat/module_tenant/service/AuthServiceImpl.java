package com.docchat.module_tenant.service;

import com.docchat.common.event.TenantCreatedEvent;
import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_tenant.dto.AuthResponse;
import com.docchat.module_tenant.dto.LoginRequest;
import com.docchat.module_tenant.dto.RegisterRequest;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String LOGIN_FAIL_KEY_PREFIX = "docchat:auth:login_fail:";
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final int LOGIN_LOCK_MINUTES = 30;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BizException(ErrorCode.AUTH_EMAIL_EXISTS);
        }

        String slug = generateSlug(request.getTenantName());
        Tenant tenant = Tenant.builder()
            .name(request.getTenantName())
            .slug(slug)
            .status((short) 1)
            .build();
        tenant = tenantRepository.save(tenant);
        applicationEventPublisher.publishEvent(new TenantCreatedEvent(tenant.getId()));

        User user = User.builder()
            .tenantId(tenant.getId())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role("ADMIN")
            .status((short) 1)
            .build();
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), tenant.getId(), user.getRole());

        log.info("用户注册成功: userId={}, tenantId={}", user.getId(), tenant.getId());

        return AuthResponse.builder()
            .userId(user.getId())
            .tenantId(tenant.getId())
            .role(user.getRole())
            .token(token)
            .expiresIn(86400L)
            .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();

        checkLoginFailCount(email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                incrementLoginFailCount(email);
                return new BizException(ErrorCode.AUTH_LOGIN_FAILED);
            });

        if (user.getStatus() == 0) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            incrementLoginFailCount(email);
            throw new BizException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        resetLoginFailCount(email);

        String token = jwtUtil.generateToken(user.getId(), user.getTenantId(), user.getRole());

        log.info("用户登录成功: userId={}, tenantId={}", user.getId(), user.getTenantId());

        return AuthResponse.builder()
            .userId(user.getId())
            .tenantId(user.getTenantId())
            .role(user.getRole())
            .token(token)
            .expiresIn(86400L)
            .build();
    }

    private void checkLoginFailCount(String email) {
        String key = LOGIN_FAIL_KEY_PREFIX + email;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr != null && Integer.parseInt(countStr) >= MAX_LOGIN_FAIL_COUNT) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
    }

    private void incrementLoginFailCount(String email) {
        String key = LOGIN_FAIL_KEY_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }

    private void resetLoginFailCount(String email) {
        String key = LOGIN_FAIL_KEY_PREFIX + email;
        redisTemplate.delete(key);
    }

    private String generateSlug(String tenantName) {
        String base = tenantName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (base.isEmpty()) {
            base = "team";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
