package com.docchat;

import com.docchat.module_chat.service.LlmService;
import com.docchat.module_chat.service.RetrievalService;
import com.docchat.module_knowledge.repository.MilvusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.milvus.v2.client.MilvusClientV2;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 集成测试基类
 *
 * 策略：
 * - PostgreSQL: H2 内存数据库（PostgreSQL 兼容模式，避免 Docker 依赖）
 * - Redis: MockBean（登录失败计数、任务队列等缓存功能）
 * - Milvus: MockBean（外部向量数据库）
 * - LLM/Retrieval: MockBean（外部 API 调用）
 *
 * H2 兼容性说明：
 * - 使用 PostgreSQL 兼容模式，支持大部分语法
 * - Flyway 迁移脚本在 H2 上执行，验证 DDL 正确性
 * - 不测试 PostgreSQL 特有功能（如 JSONB、数组类型等）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // === Mock Redis ===
    @MockBean
    protected RedisConnectionFactory redisConnectionFactory;

    @MockBean
    protected ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockBean
    protected StringRedisTemplate redisTemplate;

    @MockBean
    protected ValueOperations<String, String> valueOperations;

    // === Mock 外部依赖 ===

    @MockBean
    protected MilvusClientV2 milvusClient;

    @MockBean
    protected MilvusRepository milvusRepository;

    @MockBean
    protected RetrievalService retrievalService;

    @MockBean
    protected LlmService llmService;

    @BeforeEach
    void setupRedisMock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.keys(anyString())).thenReturn(java.util.Collections.emptySet());
        when(redisTemplate.opsForList()).thenReturn(mock(org.springframework.data.redis.core.ListOperations.class));
    }
}
