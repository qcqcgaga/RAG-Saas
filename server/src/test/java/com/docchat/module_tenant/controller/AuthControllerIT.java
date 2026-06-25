package com.docchat.module_tenant.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.module_tenant.dto.LoginRequest;
import com.docchat.module_tenant.dto.RegisterRequest;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证模块集成测试
 *
 * 覆盖：注册 → 登录 → 登录失败 → 账户锁定 → 账户禁用 → 重复注册
 * 验证：HTTP 链路、数据库持久化、JWT Token 生成
 *
 * 注意：Redis 为 MockBean，登录失败计数/锁定逻辑通过 Mock 行为模拟
 */
class AuthControllerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    // ========== 注册接口测试 ==========

    @Nested
    @DisplayName("POST /api/v1/auth/register — 用户注册")
    class RegisterTests {

        @Test
        @DisplayName("正常注册：创建租户+用户，返回JWT Token")
        void register_success() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("test@example.com");
            req.setPassword("Pass1234");
            req.setTenantName("测试团队");

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").isNumber())
                    .andExpect(jsonPath("$.data.tenantId").isNumber())
                    .andExpect(jsonPath("$.data.role").value("ADMIN"))
                    .andExpect(jsonPath("$.data.token").isString())
                    .andExpect(jsonPath("$.data.expiresIn").value(86400))
                    .andReturn();

            // 验证数据库持久化
            assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
            assertThat(tenantRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);

            // 验证用户关联租户
            User user = userRepository.findByEmail("test@example.com").orElseThrow();
            assertThat(user.getRole()).isEqualTo("ADMIN");
            assertThat(user.getStatus()).isEqualTo((short) 1);
            assertThat(user.getTenantId()).isNotNull();

            // 验证租户 slug 生成
            Tenant tenant = tenantRepository.findById(user.getTenantId()).orElseThrow();
            assertThat(tenant.getName()).isEqualTo("测试团队");
            assertThat(tenant.getSlug()).isNotBlank();
        }

        @Test
        @DisplayName("重复邮箱注册：返回40104错误")
        void register_duplicateEmail() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("dup@example.com");
            req.setPassword("Pass1234");
            req.setTenantName("团队A");
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            RegisterRequest req2 = new RegisterRequest();
            req2.setEmail("dup@example.com");
            req2.setPassword("Pass5678");
            req2.setTenantName("团队B");
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40104))
                    .andExpect(jsonPath("$.msg").value("邮箱已注册"));
        }

        @Test
        @DisplayName("密码强度不足：返回参数校验失败")
        void register_weakPassword() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("weak@example.com");
            req.setPassword("12345678");  // 纯数字，不含字母
            req.setTenantName("弱密码团队");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        }

        @Test
        @DisplayName("邮箱格式错误：返回参数校验失败")
        void register_invalidEmail() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("not-an-email");
            req.setPassword("Pass1234");
            req.setTenantName("错误邮箱");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        }

        @Test
        @DisplayName("空请求体：返回参数校验失败")
        void register_emptyBody() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40000));
        }
    }

    // ========== 登录接口测试 ==========

    @Nested
    @DisplayName("POST /api/v1/auth/login — 用户登录")
    class LoginTests {

        private String registeredEmail = "login@example.com";
        private String registeredPassword = "Pass1234";

        void registerTestUser() throws Exception {
            // 先清理可能已存在的用户（测试间数据隔离）
            userRepository.findByEmail(registeredEmail)
                    .ifPresent(u -> userRepository.delete(u));
            tenantRepository.findAll().forEach(t -> tenantRepository.delete(t));

            RegisterRequest req = new RegisterRequest();
            req.setEmail(registeredEmail);
            req.setPassword(registeredPassword);
            req.setTenantName("登录测试团队");
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("正常登录：返回JWT Token")
        void login_success() throws Exception {
            registerTestUser();

            LoginRequest req = new LoginRequest();
            req.setEmail(registeredEmail);
            req.setPassword(registeredPassword);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userId").isNumber())
                    .andExpect(jsonPath("$.data.tenantId").isNumber())
                    .andExpect(jsonPath("$.data.role").value("ADMIN"))
                    .andExpect(jsonPath("$.data.token").isString())
                    .andExpect(jsonPath("$.data.expiresIn").value(86400));
        }

        @Test
        @DisplayName("密码错误：返回40101，Redis失败计数+1")
        void login_wrongPassword() throws Exception {
            registerTestUser();

            LoginRequest req = new LoginRequest();
            req.setEmail(registeredEmail);
            req.setPassword("WrongPass1");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40101));

            // 验证 Redis increment 被调用（失败计数+1）
            org.mockito.Mockito.verify(valueOperations)
                    .increment(startsWith("docchat:auth:login_fail:"));
        }

        @Test
        @DisplayName("邮箱不存在：返回40101")
        void login_emailNotFound() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("nonexist@example.com");
            req.setPassword("Pass1234");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40101));
        }

        @Test
        @DisplayName("账户锁定：Redis计数>=5时返回40102")
        void login_accountLocked() throws Exception {
            registerTestUser();

            // Mock Redis 返回失败计数 >= 5，模拟账户锁定状态
            when(valueOperations.get(startsWith("docchat:auth:login_fail:")))
                    .thenReturn("5");

            LoginRequest req = new LoginRequest();
            req.setEmail(registeredEmail);
            req.setPassword("WrongPass1");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40102))
                    .andExpect(jsonPath("$.msg").value("账户已锁定，请30分钟后重试"));
        }

        @Test
        @DisplayName("账户禁用：返回40103")
        void login_accountDisabled() throws Exception {
            registerTestUser();

            // 直接修改数据库禁用账户
            User user = userRepository.findByEmail(registeredEmail).orElseThrow();
            user.setStatus((short) 0);
            userRepository.save(user);

            LoginRequest req = new LoginRequest();
            req.setEmail(registeredEmail);
            req.setPassword(registeredPassword);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40103))
                    .andExpect(jsonPath("$.msg").value("账户已禁用"));
        }

        @Test
        @DisplayName("登录成功后Redis清除失败计数")
        void login_successClearsFailCount() throws Exception {
            registerTestUser();

            LoginRequest correctReq = new LoginRequest();
            correctReq.setEmail(registeredEmail);
            correctReq.setPassword(registeredPassword);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(correctReq)))
                    .andExpect(status().isOk());

            // 验证 Redis delete 被调用（清除失败计数）
            org.mockito.Mockito.verify(redisTemplate)
                    .delete(startsWith("docchat:auth:login_fail:"));
        }
    }

    // ========== 注册→登录端到端流程 ==========

    @Nested
    @DisplayName("注册→登录端到端流程")
    class RegisterLoginE2E {

        @Test
        @DisplayName("注册后Token可访问受保护接口")
        void registerThenAccessProtectedApi() throws Exception {
            // 1. 注册
            RegisterRequest regReq = new RegisterRequest();
            regReq.setEmail("e2e@example.com");
            regReq.setPassword("Pass1234");
            regReq.setTenantName("E2E团队");

            MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            // 2. 提取 Token
            String responseBody = regResult.getResponse().getContentAsString();
            var responseNode = objectMapper.readTree(responseBody);
            String token = responseNode.get("data").get("token").asText();
            assertThat(token).isNotBlank();

            // 3. 用 Token 访问受保护接口（获取当前租户信息）
            mockMvc.perform(get("/api/v1/tenants/current")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("E2E团队"));
        }
    }
}
