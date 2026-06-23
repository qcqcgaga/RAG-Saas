package com.docchat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 基础上下文加载测试
 *
 * 验证 Spring 应用上下文可以正常启动。
 * 集成测试需配合 TestContainers，骨架阶段仅验证上下文加载。
 */
@SpringBootTest
class DocChatApplicationTest {

    @Test
    void contextLoads() {
        // 如果应用上下文无法加载，此测试会失败
    }
}
