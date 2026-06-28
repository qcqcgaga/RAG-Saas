package com.docchat.module_apikey.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_apikey.entity.ApiKey;
import com.docchat.module_apikey.repository.ApiKeyRepository;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Key 模块集成测试
 *
 * 覆盖：Key CRUD、限额校验、多租户隔离、鉴权校验
 */
class ApiKeyControllerIT extends BaseIntegrationTest {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ApiKeyRepository apiKeyRepository;

    private Long tenantIdA;
    private String tokenA;
    private Long tenantIdB;
    private String tokenB;

    @BeforeEach
    void setupTenants() {
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenantA = Tenant.builder().name("API租户A").slug("api-tenant-a-001").status((short) 1).build();
        tenantA = tenantRepository.save(tenantA);
        tenantIdA = tenantA.getId();
        User userA = User.builder().tenantId(tenantIdA).email("apia@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userA = userRepository.save(userA);
        tokenA = jwtUtil.generateToken(userA.getId(), tenantIdA, "ADMIN");

        Tenant tenantB = Tenant.builder().name("API租户B").slug("api-tenant-b-001").status((short) 1).build();
        tenantB = tenantRepository.save(tenantB);
        tenantIdB = tenantB.getId();
        User userB = User.builder().tenantId(tenantIdB).email("apib@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userB = userRepository.save(userB);
        tokenB = jwtUtil.generateToken(userB.getId(), tenantIdB, "ADMIN");
    }

    private String authHeader(String token) {
        return "Bearer " + token;
    }

    // ========== 创建 API Key ==========

    @Nested
    @DisplayName("POST /api/v1/api-keys — 创建 API Key")
    class CreateKeyTests {

        @Test
        @DisplayName("正常创建：返回完整Key")
        void createKey_success() throws Exception {
            mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"测试Key\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.key").value(org.hamcrest.Matchers.startsWith("dc_")))
                    .andExpect(jsonPath("$.data.keyPrefix").isNotEmpty())
                    .andExpect(jsonPath("$.data.status").value(1))
                    .andExpect(jsonPath("$.data.name").value("测试Key"));
        }

        @Test
        @DisplayName("未认证：返回403")
        void createKey_unauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/api-keys")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Key\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("达到上限5个：返回40913")
        void createKey_exceedLimit() throws Exception {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/v1/api-keys")
                                .header("Authorization", authHeader(tokenA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Key" + i + "\"}"))
                        .andExpect(status().isOk());
            }
            // 第6个应失败
            mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Key6\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40913));
        }

        @Test
        @DisplayName("不同租户各自计数：A有5个不影响B")
        void createKey_tenantIsolation_limitIndependent() throws Exception {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/v1/api-keys")
                                .header("Authorization", authHeader(tokenA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"AKey" + i + "\"}"))
                        .andExpect(status().isOk());
            }
            // 租户B仍可创建
            mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"BKey1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    // ========== 列表 API Key ==========

    @Nested
    @DisplayName("GET /api/v1/api-keys — 列表 API Key")
    class ListKeysTests {

        @Test
        @DisplayName("返回当前租户有效Key列表")
        void listKeys_success() throws Exception {
            mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Key1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].key").isEmpty()); // 列表不返回完整Key
        }

        @Test
        @DisplayName("多租户隔离：A看不到B的Key")
        void listKeys_tenantIsolation() throws Exception {
            mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"AKey\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"BKey\"}"))
                    .andExpect(status().isOk());

            // 租户A只能看到1个
            mockMvc.perform(get("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }
    }

    // ========== 吊销 API Key ==========

    @Nested
    @DisplayName("DELETE /api/v1/api-keys/{keyId} — 吊销 API Key")
    class RevokeKeyTests {

        @Test
        @DisplayName("正常吊销：status变为0")
        void revokeKey_success() throws Exception {
            String response = mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"待吊销\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Long keyId = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build().readTree(response).get("data").get("id").asLong();

            mockMvc.perform(delete("/api/v1/api-keys/{keyId}", keyId)
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"confirm\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(0));
        }

        @Test
        @DisplayName("吊销后列表不再包含")
        void revokeKey_notInList() throws Exception {
            String response = mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"待吊销\"}"))
                    .andReturn().getResponse().getContentAsString();

            Long keyId = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build().readTree(response).get("data").get("id").asLong();

            mockMvc.perform(delete("/api/v1/api-keys/{keyId}", keyId)
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"confirm\":true}"))
                    .andExpect(status().isOk());

            // 列表应只包含有效Key
            mockMvc.perform(get("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    // ========== 重命名 API Key ==========

    @Nested
    @DisplayName("PUT /api/v1/api-keys/{keyId}/name — 重命名 API Key")
    class RenameKeyTests {

        @Test
        @DisplayName("正常重命名")
        void renameKey_success() throws Exception {
            String response = mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"旧名\"}"))
                    .andReturn().getResponse().getContentAsString();

            Long keyId = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build().readTree(response).get("data").get("id").asLong();

            mockMvc.perform(put("/api/v1/api-keys/{keyId}/name", keyId)
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"新名\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("新名"));
        }
    }

    // ========== 数据库持久化验证 ==========

    @Nested
    @DisplayName("数据库持久化验证")
    class PersistenceTests {

        @Test
        @DisplayName("创建的Key正确持久化到数据库")
        void createKey_persistedToDb() throws Exception {
            mockMvc.perform(post("/api/v1/api-keys")
                            .header("Authorization", authHeader(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"持久化Key\"}"))
                    .andExpect(status().isOk());

            List<ApiKey> keys = apiKeyRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantIdA, 1);
            assertThat(keys).hasSize(1);
            assertThat(keys.get(0).getName()).isEqualTo("持久化Key");
            assertThat(keys.get(0).getKeyHash()).isNotBlank();
            assertThat(keys.get(0).getKeyPrefix()).startsWith("dc_");
        }
    }
}
