package com.docchat.module_tenant.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import com.docchat.module_tenant.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 租户模块集成测试
 *
 * 覆盖：获取当前租户、更新租户、成员管理、角色权限、多租户隔离
 */
class TenantControllerIT extends BaseIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    private Long tenantIdA;
    private Long userIdA;
    private String tokenA;

    private Long tenantIdB;
    private Long userIdB;
    private String tokenB;

    /**
     * 初始化两个租户，用于多租户隔离测试
     */
    @BeforeEach
    void setupTenants() throws Exception {
        // 清理数据库
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 租户 A（管理员）
        Tenant tenantA = Tenant.builder().name("租户A").slug("tenant-a-001").status((short) 1).build();
        tenantA = tenantRepository.save(tenantA);
        tenantIdA = tenantA.getId();

        User userA = User.builder().tenantId(tenantIdA).email("adminA@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userA = userRepository.save(userA);
        userIdA = userA.getId();
        tokenA = jwtUtil.generateToken(userIdA, tenantIdA, "ADMIN");

        // 租户 B（管理员）
        Tenant tenantB = Tenant.builder().name("租户B").slug("tenant-b-001").status((short) 1).build();
        tenantB = tenantRepository.save(tenantB);
        tenantIdB = tenantB.getId();

        User userB = User.builder().tenantId(tenantIdB).email("adminB@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userB = userRepository.save(userB);
        userIdB = userB.getId();
        tokenB = jwtUtil.generateToken(userIdB, tenantIdB, "ADMIN");
    }

    private String authHeader(String token) {
        return "Bearer " + token;
    }

    // ========== 获取当前租户信息 ==========

    @Nested
    @DisplayName("GET /api/v1/tenants/current — 获取当前租户信息")
    class GetCurrentTenantTests {

        @Test
        @DisplayName("正常获取：返回租户名称和slug")
        void getCurrentTenant_success() throws Exception {
            mockMvc.perform(get("/api/v1/tenants/current")
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(tenantIdA))
                    .andExpect(jsonPath("$.data.name").value("租户A"))
                    .andExpect(jsonPath("$.data.slug").value("tenant-a-001"))
                    .andExpect(jsonPath("$.data.status").value(1))
                    .andExpect(jsonPath("$.data.memberCount").isNumber());
        }

        @Test
        @DisplayName("未认证：返回403")
        void getCurrentTenant_unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/tenants/current"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== 更新租户信息 ==========

    @Nested
    @DisplayName("PUT /api/v1/tenants/current — 更新租户信息")
    class UpdateTenantTests {

        @Test
        @DisplayName("管理员更新：返回新名称")
        void updateTenant_adminSuccess() throws Exception {
            UpdateTenantRequest req = new UpdateTenantRequest();
            req.setName("租户A新名称");

            mockMvc.perform(put("/api/v1/tenants/current")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("租户A新名称"));

            // 验证数据库更新
            Tenant updated = tenantRepository.findById(tenantIdA).orElseThrow();
            assertThat(updated.getName()).isEqualTo("租户A新名称");
        }
    }

    // ========== 成员管理 ==========

    @Nested
    @DisplayName("成员管理接口")
    class MemberManagementTests {

        @Test
        @DisplayName("邀请成员：创建新用户并关联租户")
        void inviteMember_success() throws Exception {
            InviteMemberRequest req = new InviteMemberRequest();
            req.setEmail("member@example.com");
            req.setRole("MEMBER");

            mockMvc.perform(post("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.email").value("member@example.com"))
                    .andExpect(jsonPath("$.data.role").value("MEMBER"));

            // 验证数据库持久化
            User member = userRepository.findByEmailAndTenantId("member@example.com", tenantIdA).orElseThrow();
            assertThat(member.getRole()).isEqualTo("MEMBER");
            assertThat(member.getStatus()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("邀请重复邮箱：返回40301")
        void inviteMember_duplicateEmail() throws Exception {
            InviteMemberRequest req = new InviteMemberRequest();
            req.setEmail("adminA@example.com");  // 已存在的邮箱
            req.setRole("MEMBER");

            mockMvc.perform(post("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40301));
        }

        @Test
        @DisplayName("成员列表：返回当前租户所有成员")
        void listMembers_success() throws Exception {
            // 先邀请一个成员
            InviteMemberRequest req = new InviteMemberRequest();
            req.setEmail("listmember@example.com");
            req.setRole("READONLY");
            mockMvc.perform(post("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)));

            mockMvc.perform(get("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.list").isArray())
                    .andExpect(jsonPath("$.data.list.length()").value(2))  // admin + 1 member
                    .andExpect(jsonPath("$.data.total").value(2));
        }

        @Test
        @DisplayName("修改成员角色：成功")
        void updateMemberRole_success() throws Exception {
            // 邀请成员
            InviteMemberRequest inviteReq = new InviteMemberRequest();
            inviteReq.setEmail("rolechange@example.com");
            inviteReq.setRole("MEMBER");
            mockMvc.perform(post("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inviteReq)));

            User member = userRepository.findByEmailAndTenantId("rolechange@example.com", tenantIdA).orElseThrow();

            UpdateRoleRequest updateReq = new UpdateRoleRequest();
            updateReq.setRole("READONLY");

            mockMvc.perform(put("/api/v1/tenants/members/{userId}/role", member.getId())
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 验证数据库更新
            User updated = userRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getRole()).isEqualTo("READONLY");
        }

        @Test
        @DisplayName("移除成员：成功删除")
        void removeMember_success() throws Exception {
            InviteMemberRequest inviteReq = new InviteMemberRequest();
            inviteReq.setEmail("remove@example.com");
            inviteReq.setRole("MEMBER");
            mockMvc.perform(post("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inviteReq)));

            User member = userRepository.findByEmailAndTenantId("remove@example.com", tenantIdA).orElseThrow();

            mockMvc.perform(delete("/api/v1/tenants/members/{userId}", member.getId())
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk());

            // 验证数据库删除
            assertThat(userRepository.findById(member.getId())).isEmpty();
        }

        @Test
        @DisplayName("不能移除自己：返回403")
        void removeSelf_forbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/tenants/members/{userId}", userIdA)
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40300));
        }
    }

    // ========== 多租户隔离测试 ==========

    @Nested
    @DisplayName("多租户数据隔离")
    class TenantIsolationTests {

        @Test
        @DisplayName("租户A看不到租户B的成员列表")
        void listMembers_isolated() throws Exception {
            // 租户B邀请成员
            InviteMemberRequest req = new InviteMemberRequest();
            req.setEmail("bmember@example.com");
            req.setRole("MEMBER");
            mockMvc.perform(post("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)));

            // 租户A查询成员列表——不应包含B的成员
            mockMvc.perform(get("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.list.length()").value(1))  // 只有A自己的admin
                    .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        @DisplayName("租户A无法修改租户B成员的角色")
        void updateMemberRole_crossTenant() throws Exception {
            // 租户B邀请成员
            InviteMemberRequest req = new InviteMemberRequest();
            req.setEmail("bmember2@example.com");
            req.setRole("MEMBER");
            mockMvc.perform(post("/api/v1/tenants/members")
                            .header("Authorization", authHeader(tokenB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)));

            User bMember = userRepository.findByEmailAndTenantId("bmember2@example.com", tenantIdB).orElseThrow();

            // 租户A尝试修改B成员角色——应返回403
            UpdateRoleRequest updateReq = new UpdateRoleRequest();
            updateReq.setRole("ADMIN");
            mockMvc.perform(put("/api/v1/tenants/members/{userId}/role", bMember.getId())
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40300));
        }

        @Test
        @DisplayName("租户A获取到的是自己的租户信息，不是B的")
        void getCurrentTenant_isolated() throws Exception {
            mockMvc.perform(get("/api/v1/tenants/current")
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(tenantIdA))
                    .andExpect(jsonPath("$.data.name").value("租户A"));

            mockMvc.perform(get("/api/v1/tenants/current")
                            .header("Authorization", authHeader(tokenB)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(tenantIdB))
                    .andExpect(jsonPath("$.data.name").value("租户B"));
        }
    }
}
