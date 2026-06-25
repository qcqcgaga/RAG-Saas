package com.docchat.module_task.controller;

import com.docchat.BaseIntegrationTest;
import com.docchat.common.util.JwtUtil;
import com.docchat.module_knowledge.entity.Document;
import com.docchat.module_knowledge.entity.KnowledgeBase;
import com.docchat.module_knowledge.repository.DocumentRepository;
import com.docchat.module_knowledge.repository.KnowledgeBaseRepository;
import com.docchat.module_task.entity.AsyncTask;
import com.docchat.module_task.repository.AsyncTaskRepository;
import com.docchat.module_tenant.entity.Tenant;
import com.docchat.module_tenant.entity.User;
import com.docchat.module_tenant.repository.TenantRepository;
import com.docchat.module_tenant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 异步任务模块集成测试
 *
 * 覆盖：任务列表、任务详情、任务重试、多租户隔离
 */
class TaskControllerIT extends BaseIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AsyncTaskRepository asyncTaskRepository;

    private Long tenantIdA;
    private String tokenA;
    private Long tenantIdB;
    private String tokenB;
    private Long documentIdA;
    private AsyncTask taskA;

    @BeforeEach
    void setupData() {
        asyncTaskRepository.deleteAll();
        documentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 租户 A
        Tenant tenantA = Tenant.builder().name("任务租户A").slug("task-a-001").status((short) 1).build();
        tenantA = tenantRepository.save(tenantA);
        tenantIdA = tenantA.getId();
        User userA = User.builder().tenantId(tenantIdA).email("taska@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userA = userRepository.save(userA);
        tokenA = jwtUtil.generateToken(userA.getId(), tenantIdA, "ADMIN");

        // 创建知识库和文档
        KnowledgeBase kb = KnowledgeBase.builder().tenantId(tenantIdA).name("任务测试KB").build();
        kb = knowledgeBaseRepository.save(kb);
        Document doc = Document.builder().knowledgeId(kb.getId()).tenantId(tenantIdA)
                .originalName("test.txt").storedName("uuid.txt").fileType("txt")
                .fileSize(100L).storedPath("1/uuid.txt").build();
        doc = documentRepository.save(doc);
        documentIdA = doc.getId();

        // 创建任务
        taskA = AsyncTask.builder().tenantId(tenantIdA).documentId(documentIdA)
                .taskType("CHUNK_AND_EMBED").status("FAILED").progress((short) 50)
                .maxRetry((short) 3).retryCount((short) 1)
                .errorMessage("模拟失败").build();
        taskA = asyncTaskRepository.save(taskA);

        // 租户 B
        Tenant tenantB = Tenant.builder().name("任务租户B").slug("task-b-001").status((short) 1).build();
        tenantB = tenantRepository.save(tenantB);
        tenantIdB = tenantB.getId();
        User userB = User.builder().tenantId(tenantIdB).email("taskb@example.com")
                .passwordHash("$2a$10$dummy").role("ADMIN").status((short) 1).build();
        userB = userRepository.save(userB);
        tokenB = jwtUtil.generateToken(userB.getId(), tenantIdB, "ADMIN");
    }

    private String auth(String token) {
        return "Bearer " + token;
    }

    @Nested
    @DisplayName("GET /api/v1/tasks — 获取任务列表")
    class ListTasksTests {

        @Test
        @DisplayName("返回当前租户的任务列表")
        void listTasks_success() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.list").isArray())
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.list[0].taskType").value("CHUNK_AND_EMBED"))
                    .andExpect(jsonPath("$.data.list[0].status").value("FAILED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tasks/{taskId} — 获取任务详情")
    class GetTaskTests {

        @Test
        @DisplayName("返回任务详细信息")
        void getTask_success() throws Exception {
            mockMvc.perform(get("/api/v1/tasks/{taskId}", taskA.getId())
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(taskA.getId()))
                    .andExpect(jsonPath("$.data.documentId").value(documentIdA))
                    .andExpect(jsonPath("$.data.taskType").value("CHUNK_AND_EMBED"))
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.progress").value(50))
                    .andExpect(jsonPath("$.data.retryCount").value(1))
                    .andExpect(jsonPath("$.data.errorMessage").value("模拟失败"));
        }

        @Test
        @DisplayName("任务不存在：返回40500")
        void getTask_notFound() throws Exception {
            mockMvc.perform(get("/api/v1/tasks/{taskId}", 99999L)
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40500));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tasks/{taskId}/retry — 重试失败任务")
    class RetryTaskTests {

        @Test
        @DisplayName("重试FAILED任务：状态变为PENDING")
        void retryTask_success() throws Exception {
            mockMvc.perform(post("/api/v1/tasks/{taskId}/retry", taskA.getId())
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.retryCount").value(2));

            // 验证数据库更新
            AsyncTask updated = asyncTaskRepository.findById(taskA.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo("PENDING");
            assertThat(updated.getRetryCount()).isEqualTo((short) 2);
        }

        @Test
        @DisplayName("重试非FAILED任务：返回40501")
        void retryTask_notFailed() throws Exception {
            // 创建一个 COMPLETED 任务
            AsyncTask completedTask = AsyncTask.builder().tenantId(tenantIdA).documentId(documentIdA)
                    .taskType("CHUNK_AND_EMBED").status("COMPLETED").progress((short) 100)
                    .maxRetry((short) 3).retryCount((short) 0).build();
            completedTask = asyncTaskRepository.save(completedTask);

            mockMvc.perform(post("/api/v1/tasks/{taskId}/retry", completedTask.getId())
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40501));
        }

        @Test
        @DisplayName("超过最大重试次数：返回40502")
        void retryTask_maxRetryExceeded() throws Exception {
            // 创建已达到最大重试次数的任务
            AsyncTask maxRetryTask = AsyncTask.builder().tenantId(tenantIdA).documentId(documentIdA)
                    .taskType("CHUNK_AND_EMBED").status("FAILED").progress((short) 50)
                    .maxRetry((short) 3).retryCount((short) 3).build();
            maxRetryTask = asyncTaskRepository.save(maxRetryTask);

            mockMvc.perform(post("/api/v1/tasks/{taskId}/retry", maxRetryTask.getId())
                            .header("Authorization", auth(tokenA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40502));
        }
    }

    @Nested
    @DisplayName("多租户任务隔离")
    class TaskIsolationTests {

        @Test
        @DisplayName("租户B无法查看租户A的任务详情")
        void getTask_crossTenant() throws Exception {
            mockMvc.perform(get("/api/v1/tasks/{taskId}", taskA.getId())
                            .header("Authorization", auth(tokenB)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40300));
        }

        @Test
        @DisplayName("租户B无法重试租户A的任务")
        void retryTask_crossTenant() throws Exception {
            mockMvc.perform(post("/api/v1/tasks/{taskId}/retry", taskA.getId())
                            .header("Authorization", auth(tokenB)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(40300));
        }

        @Test
        @DisplayName("租户B任务列表不包含租户A的任务")
        void listTasks_isolated() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", auth(tokenB)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }
}
