package com.docchat.module_eval.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_eval.entity.EvalPair;
import com.docchat.module_eval.entity.EvalSet;
import com.docchat.module_eval.repository.EvalPairRepository;
import com.docchat.module_eval.repository.EvalSetRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 评测模块集成测试
 *
 * 覆盖：评测集CRUD、问答对管理、批量导入、多租户隔离
 */
class EvalControllerIT extends BaseIntegrationTest {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EvalSetRepository evalSetRepository;
    @Autowired private EvalPairRepository evalPairRepository;

    private Long tenantIdA;
    private String tokenA;
    private Long tenantIdB;
    private String tokenB;

    @BeforeEach
    void setupTenants() {
        evalPairRepository.deleteAll();
        evalSetRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenantA = Tenant.builder().name("评测租户A").slug("eval-tenant-a-001").status((short) 1).build();
        tenantA = tenantRepository.save(tenantA);
        tenantIdA = tenantA.getId();
        User userA = User.builder().tenantId(tenantIdA).email("evala@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userA = userRepository.save(userA);
        tokenA = jwtUtil.generateToken(userA.getId(), tenantIdA, "ADMIN");

        Tenant tenantB = Tenant.builder().name("评测租户B").slug("eval-tenant-b-001").status((short) 1).build();
        tenantB = tenantRepository.save(tenantB);
        tenantIdB = tenantB.getId();
        User userB = User.builder().tenantId(tenantIdB).email("evalb@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userB = userRepository.save(userB);
        tokenB = jwtUtil.generateToken(userB.getId(), tenantIdB, "ADMIN");
    }

    private String auth(String token) { return "Bearer " + token; }

    // ========== 创建评测集 ==========

    @Nested
    @DisplayName("POST /api/v1/eval/sets — 创建评测集")
    class CreateSetTests {

        @Test
        @DisplayName("正常创建")
        void createSet_success() throws Exception {
            mockMvc.perform(post("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"测试集\",\"description\":\"描述\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value("测试集"))
                    .andExpect(jsonPath("$.data.pairCount").value(0));
        }

        @Test
        @DisplayName("达到上限10个：返回41002")
        void createSet_exceedLimit() throws Exception {
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/v1/eval/sets")
                                .header("Authorization", auth(tokenA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"集" + i + "\"}"))
                        .andExpect(status().isOk());
            }
            mockMvc.perform(post("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"超限集\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(41002));
        }
    }

    // ========== 评测集列表 ==========

    @Nested
    @DisplayName("GET /api/v1/eval/sets — 评测集列表")
    class ListSetsTests {

        @Test
        @DisplayName("返回当前租户评测集")
        void listSets_success() throws Exception {
            mockMvc.perform(post("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"集1\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("多租户隔离：A看不到B的评测集")
        void listSets_tenantIsolation() throws Exception {
            mockMvc.perform(post("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"A集\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenB)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    // ========== 删除评测集 ==========

    @Nested
    @DisplayName("DELETE /api/v1/eval/sets/{setId} — 删除评测集")
    class DeleteSetTests {

        @Test
        @DisplayName("正常删除")
        void deleteSet_success() throws Exception {
            String resp = mockMvc.perform(post("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"待删集\"}"))
                    .andReturn().getResponse().getContentAsString();

            Long setId = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build().readTree(resp).get("data").get("id").asLong();

            mockMvc.perform(delete("/api/v1/eval/sets/{setId}", setId)
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isOk());

            assertThat(evalSetRepository.findById(setId)).isEmpty();
        }
    }

    // ========== 添加问答对 ==========

    @Nested
    @DisplayName("POST /api/v1/eval/sets/{setId}/pairs — 添加问答对")
    class AddPairTests {

        private Long setIdA;

        @BeforeEach
        void createSet() throws Exception {
            String resp = mockMvc.perform(post("/api/v1/eval/sets")
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"问答对测试集\"}"))
                    .andReturn().getResponse().getContentAsString();
            setIdA = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build().readTree(resp).get("data").get("id").asLong();
        }

        @Test
        @DisplayName("正常添加问答对")
        void addPair_success() throws Exception {
            mockMvc.perform(post("/api/v1/eval/sets/{setId}/pairs", setIdA)
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"什么是RAG?\",\"expectedDocument\":\"rag-guide.pdf\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.question").value("什么是RAG?"))
                    .andExpect(jsonPath("$.data.expectedDocument").value("rag-guide.pdf"));
        }

        @Test
        @DisplayName("达到上限50个：返回41003")
        void addPair_exceedLimit() throws Exception {
            for (int i = 0; i < 50; i++) {
                mockMvc.perform(post("/api/v1/eval/sets/{setId}/pairs", setIdA)
                                .header("Authorization", auth(tokenA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"Q" + i + "\",\"expectedDocument\":\"doc" + i + ".pdf\"}"))
                        .andExpect(status().isOk());
            }
            mockMvc.perform(post("/api/v1/eval/sets/{setId}/pairs", setIdA)
                            .header("Authorization", auth(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"超限Q\",\"expectedDocument\":\"doc.pdf\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(41003));
        }
    }

    // ========== 评测集不存在 ==========

    @Nested
    @DisplayName("评测集不存在场景")
    class NotFoundTests {

        @Test
        @DisplayName("获取不存在的评测集：返回41001")
        void getSet_notFound() throws Exception {
            mockMvc.perform(get("/api/v1/eval/sets/{setId}", 99999L)
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(41001));
        }
    }
}
