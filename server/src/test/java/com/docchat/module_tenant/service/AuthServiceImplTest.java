package com.docchat.module_tenant.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks private AuthServiceImpl authService;

    @Test
    @DisplayName("register - 正常注册返回AuthResponse")
    void register_happyPath() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setTenantName("TestTeam");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(jwtUtil.generateToken(anyLong(), anyLong(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(tenantRepository).save(any());
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("register - 邮箱已存在抛AUTH_EMAIL_EXISTS")
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("exists@example.com");
        request.setPassword("password123");
        request.setTenantName("Team");

        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.AUTH_EMAIL_EXISTS.getCode()));
    }

    @Test
    @DisplayName("register - 特殊字符租户名生成team前缀slug")
    void register_specialCharTenantName_generatesTeamSlug() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("special@example.com");
        request.setPassword("password123");
        request.setTenantName("中文团队!!!");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(1L);
            assertThat(t.getSlug()).startsWith("team-");
            return t;
        });
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtUtil.generateToken(anyLong(), anyLong(), anyString())).thenReturn("token");

        authService.register(request);
        verify(tenantRepository).save(any());
    }

    @Test
    @DisplayName("login - 正常登录返回AuthResponse")
    void login_happyPath() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = User.builder()
            .id(10L).tenantId(1L).email("test@example.com")
            .passwordHash("$2a$10$hashed").role("ADMIN").status((short) 1)
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 无锁定
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashed")).thenReturn(true);
        when(jwtUtil.generateToken(10L, 1L, "ADMIN")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(10L);
        verify(redisTemplate).delete(anyString()); // 重置失败计数
    }

    @Test
    @DisplayName("login - 邮箱不存在增加失败计数")
    void login_emailNotFound_incrementsFailCount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@example.com");
        request.setPassword("wrong");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BizException.class);
        verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("login - 密码错误增加失败计数")
    void login_wrongPassword_incrementsFailCount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        User user = User.builder()
            .id(10L).tenantId(1L).email("test@example.com")
            .passwordHash("$2a$10$hashed").role("ADMIN").status((short) 1)
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BizException.class);
        verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("login - 账户被锁定抛AUTH_ACCOUNT_LOCKED")
    void login_accountLocked_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@example.com");
        request.setPassword("password123");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("5"); // 失败5次

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.AUTH_ACCOUNT_LOCKED.getCode()));
    }

    @Test
    @DisplayName("login - 禁用账户抛AUTH_ACCOUNT_DISABLED")
    void login_disabledAccount_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("disabled@example.com");
        request.setPassword("password123");

        User user = User.builder()
            .id(10L).tenantId(1L).email("disabled@example.com")
            .passwordHash("$2a$10$hashed").role("ADMIN").status((short) 0)
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.AUTH_ACCOUNT_DISABLED.getCode()));
        // 禁用账户不应增加失败计数
        verify(valueOperations, never()).increment(anyString());
    }
}
