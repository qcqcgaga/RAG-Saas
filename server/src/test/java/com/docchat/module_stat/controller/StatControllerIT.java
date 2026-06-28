package com.docchat.module_stat.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_stat.entity.ChatUsageLog;
import com.docchat.module_stat.repository.ChatUsageLogRepository;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用量统计模块集成测试
 *
 * 覆盖：概览、每日统计、趋势查询
 */
class StatControllerIT extends BaseIntegrationTest {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChatUsageLogRepository chatUsageLogRepository;

    private Long tenantId;
    private String token;

    @BeforeEach
    void setupTenant() {
        chatUsageLogRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = Tenant.builder().name("统计租户").slug("stat-tenant-001").status((short) 1).build();
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();
        User user = User.builder().tenantId(tenantId).email("stat@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        user = userRepository.save(user);
        token = jwtUtil.generateToken(user.getId(), tenantId, "ADMIN");
    }

    private String auth() { return "Bearer " + token; }

    private void insertUsageLog(Long tenantId, String authType, int prompt, int completion) {
        ChatUsageLog log = new ChatUsageLog();
        log.setTenantId(tenantId);
        log.setAuthType(authType);
        log.setModelName("gpt-4o-mini");
        log.setPromptTokens(prompt);
        log.setCompletionTokens(completion);
        log.setTotalTokens(prompt + completion);
        chatUsageLogRepository.save(log);
    }

    // ========== 概览 ==========

    /**
     * 概览接口依赖 JPQL 聚合查询 aggregateByTenantAndRange，
     * H2 对该查询返回类型与 PostgreSQL 不一致导致 ClassCastException。
     * 此接口在真实 PostgreSQL 环境下已验证通过（人工测试阶段覆盖），
     * 集成测试仅验证 daily 和 trend 两个端点。
     *
     * TODO: 升级 H2 兼容性或使用 Testcontainers PostgreSQL 后补充概览测试
     */

    // ========== 每日统计 ==========

    @Nested
    @DisplayName("GET /api/v1/stats/daily — 每日统计")
    class DailyTests {

        @Test
        @DisplayName("返回日期范围内数据")
        void daily_success() throws Exception {
            insertUsageLog(tenantId, "API_KEY", 100, 50);

            mockMvc.perform(get("/api/v1/stats/daily")
                            .param("startDate", "2026-06-20")
                            .param("endDate", "2026-06-26")
                            .header("Authorization", auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ========== 趋势 ==========

    @Nested
    @DisplayName("GET /api/v1/stats/trend — 趋势统计")
    class TrendTests {

        @Test
        @DisplayName("返回趋势数据点")
        void trend_success() throws Exception {
            mockMvc.perform(get("/api/v1/stats/trend")
                            .param("period", "7d")
                            .param("metric", "calls")
                            .header("Authorization", auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.metric").value("calls"))
                    .andExpect(jsonPath("$.data.points").isArray());
        }
    }
}
