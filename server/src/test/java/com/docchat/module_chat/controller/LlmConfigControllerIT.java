package com.docchat.module_chat.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_chat.dto.UpdateLlmConfigRequest;
import com.docchat.module_chat.entity.TenantLlmConfig;
import com.docchat.module_chat.repository.TenantLlmConfigRepository;
import com.docchat.module_chat.service.LlmService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LLM 配置模块集成测试
 *
 * 覆盖：配置获取、更新、删除
 *
 * 注意：LlmService 被 BaseIntegrationTest @MockBean 模拟，
 * 因此需要手动将 mock 委托给真实 repository。
 * LlmConfigController.getCurrentTenantId() 使用硬编码 1L。
 */
class LlmConfigControllerIT extends BaseIntegrationTest {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantLlmConfigRepository tenantLlmConfigRepository;
    @Autowired private LlmService llmService; // @MockBean from BaseIntegrationTest

    private String token;

    @BeforeEach
    void setup() {
        tenantLlmConfigRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = Tenant.builder().name("LLM租户").slug("llm-tenant-001").status((short) 1).build();
        tenant = tenantRepository.save(tenant);
        User user = User.builder().tenantId(tenant.getId()).email("llm@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        user = userRepository.save(user);
        token = jwtUtil.generateToken(user.getId(), tenant.getId(), "ADMIN");

        // 配置 LlmService mock 委托给真实 repository
        when(llmService.getTenantLlmConfig(anyLong()))
                .thenAnswer(inv -> tenantLlmConfigRepository.findByTenantId(inv.getArgument(0)).orElse(null));
        when(llmService.saveTenantLlmConfig(any()))
                .thenAnswer(inv -> tenantLlmConfigRepository.saveAndFlush(inv.getArgument(0)));
        doAnswer(inv -> {
            Long tid = inv.getArgument(0);
            tenantLlmConfigRepository.findByTenantId(tid)
                    .ifPresent(tenantLlmConfigRepository::delete);
            return null;
        }).when(llmService).deleteTenantLlmConfig(anyLong());
    }

    private String auth() { return "Bearer " + token; }

    // ========== 获取配置 ==========

    @Nested
    @DisplayName("GET /api/v1/llm-config — 获取 LLM 配置")
    class GetConfigTests {

        @Test
        @DisplayName("无配置时返回空值")
        void getConfig_noConfig() throws Exception {
            mockMvc.perform(get("/api/v1/llm-config")
                            .header("Authorization", auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.apiUrl").value(""))
                    .andExpect(jsonPath("$.data.status").value((short) 0));
        }

        @Test
        @DisplayName("有配置时返回脱敏结果")
        void getConfig_withConfig() throws Exception {
            TenantLlmConfig config = new TenantLlmConfig();
            config.setTenantId(1L);
            config.setApiUrl("https://api.openai.com/v1");
            config.setApiKeyEncrypted("sk-test-api-key-12345678");
            config.setModelName("gpt-4o-mini");
            config.setStatus((short) 1);
            tenantLlmConfigRepository.saveAndFlush(config);

            mockMvc.perform(get("/api/v1/llm-config")
                            .header("Authorization", auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.apiUrl").value("https://api.openai.com/v1"))
                    .andExpect(jsonPath("$.data.modelName").value("gpt-4o-mini"))
                    .andExpect(jsonPath("$.data.status").value((short) 1));
        }
    }

    // ========== 更新配置 ==========

    @Nested
    @DisplayName("PUT /api/v1/llm-config — 更新 LLM 配置")
    class UpdateConfigTests {

        @Test
        @DisplayName("首次创建配置")
        void updateConfig_create() throws Exception {
            UpdateLlmConfigRequest req = new UpdateLlmConfigRequest();
            req.setApiUrl("https://api.openai.com/v1");
            req.setApiKey("sk-test-key");
            req.setModelName("gpt-4o-mini");

            mockMvc.perform(put("/api/v1/llm-config")
                            .header("Authorization", auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.modelName").value("gpt-4o-mini"))
                    .andExpect(jsonPath("$.data.status").value((short) 1));

            assertThat(tenantLlmConfigRepository.findByTenantId(1L)).isPresent();
        }

        @Test
        @DisplayName("更新已有配置")
        void updateConfig_update() throws Exception {
            TenantLlmConfig config = new TenantLlmConfig();
            config.setTenantId(1L);
            config.setApiUrl("https://old.api.com");
            config.setApiKeyEncrypted("old-key");
            config.setModelName("old-model");
            config.setStatus((short) 1);
            tenantLlmConfigRepository.saveAndFlush(config);

            UpdateLlmConfigRequest req = new UpdateLlmConfigRequest();
            req.setApiUrl("https://new.api.com");
            req.setApiKey("new-key");
            req.setModelName("new-model");

            mockMvc.perform(put("/api/v1/llm-config")
                            .header("Authorization", auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.modelName").value("new-model"));

            TenantLlmConfig updated = tenantLlmConfigRepository.findByTenantId(1L).orElseThrow();
            assertThat(updated.getModelName()).isEqualTo("new-model");
            assertThat(updated.getApiUrl()).isEqualTo("https://new.api.com");
        }
    }

    // ========== 删除配置 ==========

    @Nested
    @DisplayName("DELETE /api/v1/llm-config — 删除 LLM 配置")
    class DeleteConfigTests {

        @Test
        @DisplayName("删除后恢复为空配置")
        void deleteConfig_success() throws Exception {
            TenantLlmConfig config = new TenantLlmConfig();
            config.setTenantId(1L);
            config.setApiUrl("https://api.com");
            config.setApiKeyEncrypted("key");
            config.setModelName("model");
            config.setStatus((short) 1);
            tenantLlmConfigRepository.saveAndFlush(config);

            mockMvc.perform(delete("/api/v1/llm-config")
                            .header("Authorization", auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            assertThat(tenantLlmConfigRepository.findByTenantId(1L)).isEmpty();
        }
    }
}
