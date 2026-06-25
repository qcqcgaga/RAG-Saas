package com.docchat;

import org.junit.jupiter.api.Test;

/**
 * 基础上下文加载测试
 *
 * 验证 Spring 应用上下文可以正常启动。
 * 继承 BaseIntegrationTest 获取 H2 + MockBean 配置。
 */
class DocChatApplicationTest extends BaseIntegrationTest {

    @Test
    void contextLoads() {
        // 如果应用上下文无法加载，此测试会失败
    }
}
