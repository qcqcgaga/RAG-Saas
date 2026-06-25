package com.docchat.module_widget.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import com.docchat.module_widget.entity.WidgetConfig;
import com.docchat.module_widget.repository.WidgetConfigRepository;
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
 * 聊天组件模块集成测试
 *
 * 覆盖：组件配置CRUD、嵌入脚本生成、Token重新生成、权限控制
 */
class WidgetControllerIT extends BaseIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WidgetConfigRepository widgetConfigRepository;

    private Long tenantId;
    private Long userId;
    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setupData() {
        widgetConfigRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = Tenant.builder().name("组件租户").slug("widget-001").status((short) 1).build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        // 管理员
        User admin = User.builder().tenantId(tenantId).email("widget-admin@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        admin = userRepository.save(admin);
        userId = admin.getId();
        adminToken = jwtUtil.generateToken(admin.getId(), tenantId, "ADMIN");

        // 普通成员
        User member = User.builder().tenantId(tenantId).email("widget-member@example.com")
                .passwordHash("$2a$10$dummy").role("MEMBER").status((short) 1).build();
        member = userRepository.save(member);
        memberToken = jwtUtil.generateToken(member.getId(), tenantId, "MEMBER");
    }

    private String auth(String token) {
        return "Bearer " + token;
    }

    // ========== 获取组件配置（Widget Token 认证）==========

    @Nested
    @DisplayName("GET /api/v1/widget/config?token=xxx — 获取组件配置")
    class GetConfigTests {

        @Test
        @DisplayName("有效Widget Token：返回配置")
        void getConfig_validToken() throws Exception {
            // 先创建 WidgetConfig
            WidgetConfig config = new WidgetConfig();
            config.setTenantId(tenantId);
            config.setWidgetToken("test-widget-token-123");
            widgetConfigRepository.save(config);

            mockMvc.perform(get("/api/v1/widget/config")
                            .param("token", "test-widget-token-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.brandColor").isString())
                    .andExpect(jsonPath("$.data.welcomeMessage").isString())
                    .andExpect(jsonPath("$.data.enabled").isNumber());
        }

        @Test
        @DisplayName("无效Widget Token：返回40701")
        void getConfig_invalidToken() throws Exception {
            mockMvc.perform(get("/api/v1/widget/config")
                            .param("token", "invalid-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40701));
        }
    }

    // ========== 更新组件配置（JWT 认证）==========

    @Nested
    @DisplayName("PUT /api/v1/widget/config — 更新组件配置")
    class UpdateConfigTests {

        @Test
        @DisplayName("更新品牌色和欢迎语")
        void updateConfig_success() throws Exception {
            String body = """
                    {
                        "brandColor": "#ff0000",
                        "welcomeMessage": "你好，欢迎使用！"
                    }
                    """;

            mockMvc.perform(put("/api/v1/widget/config")
                            .header("Authorization", auth(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.brandColor").value("#ff0000"))
                    .andExpect(jsonPath("$.data.welcomeMessage").value("你好，欢迎使用！"));

            // 验证数据库持久化
            WidgetConfig config = widgetConfigRepository.findByTenantId(tenantId).orElseThrow();
            assertThat(config.getBrandColor()).isEqualTo("#ff0000");
            assertThat(config.getWelcomeMessage()).isEqualTo("你好，欢迎使用！");
        }

        @Test
        @DisplayName("禁用组件")
        void updateConfig_disable() throws Exception {
            String body = """
                    {
                        "enabled": 0
                    }
                    """;

            mockMvc.perform(put("/api/v1/widget/config")
                            .header("Authorization", auth(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.enabled").value(0));
        }
    }

    // ========== 嵌入脚本 ==========

    @Nested
    @DisplayName("GET /api/v1/widget/embed-script — 获取嵌入脚本")
    class GetEmbedScriptTests {

        @Test
        @DisplayName("返回包含Widget Token的嵌入脚本")
        void getEmbedScript_success() throws Exception {
            // 先确保有 WidgetConfig
            WidgetConfig config = new WidgetConfig();
            config.setTenantId(tenantId);
            config.setWidgetToken("embed-test-token");
            widgetConfigRepository.save(config);

            mockMvc.perform(get("/api/v1/widget/embed-script")
                            .header("Authorization", auth(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.script").isString())
                    .andExpect(jsonPath("$.data.previewUrl").isString());
        }

        @Test
        @DisplayName("无Widget配置：返回40702")
        void getEmbedScript_noConfig() throws Exception {
            // 不创建 WidgetConfig
            mockMvc.perform(get("/api/v1/widget/embed-script")
                            .header("Authorization", auth(adminToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40702));
        }
    }

    // ========== Token 重新生成 ==========

    @Nested
    @DisplayName("POST /api/v1/widget/regenerate-token — 重新生成Token")
    class RegenerateTokenTests {

        @Test
        @DisplayName("管理员重新生成Token：返回新Token")
        void regenerateToken_adminSuccess() throws Exception {
            WidgetConfig config = new WidgetConfig();
            config.setTenantId(tenantId);
            config.setWidgetToken("old-token");
            config = widgetConfigRepository.save(config);

            mockMvc.perform(post("/api/v1/widget/regenerate-token")
                            .header("Authorization", auth(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.token").isString());

            // 验证数据库中 Token 已更新
            WidgetConfig updated = widgetConfigRepository.findById(config.getId()).orElseThrow();
            assertThat(updated.getWidgetToken()).isNotEqualTo("old-token");
        }

        @Test
        @DisplayName("普通成员无权重新生成Token：返回403")
        void regenerateToken_memberForbidden() throws Exception {
            WidgetConfig config = new WidgetConfig();
            config.setTenantId(tenantId);
            config.setWidgetToken("member-test-token");
            widgetConfigRepository.save(config);

            mockMvc.perform(post("/api/v1/widget/regenerate-token")
                            .header("Authorization", auth(memberToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40300));
        }
    }
}
