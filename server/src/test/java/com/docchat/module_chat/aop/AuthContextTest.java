package com.docchat.module_chat.aop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 鉴权上下文（ThreadLocal）单元测试
 */
class AuthContextTest {

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("set/get - 正确存取鉴权上下文")
    void setAndGet_happyPath() {
        AuthContext.set(1L, 5L, "API_KEY", "gpt-4o-mini");

        assertThat(AuthContext.getTenantId()).isEqualTo(1L);
        assertThat(AuthContext.getApiKeyId()).isEqualTo(5L);
        assertThat(AuthContext.getAuthType()).isEqualTo("API_KEY");
        assertThat(AuthContext.getModelName()).isEqualTo("gpt-4o-mini");
    }

    @Test
    @DisplayName("clear - 清除后所有值为null")
    void clear_allValuesNull() {
        AuthContext.set(1L, 5L, "API_KEY", "gpt-4o-mini");
        AuthContext.clear();

        assertThat(AuthContext.getTenantId()).isNull();
        assertThat(AuthContext.getApiKeyId()).isNull();
        assertThat(AuthContext.getAuthType()).isNull();
        assertThat(AuthContext.getModelName()).isNull();
    }

    @Test
    @DisplayName("get - 未设置时返回null")
    void get_withoutSet_returnsNull() {
        assertThat(AuthContext.getTenantId()).isNull();
        assertThat(AuthContext.getApiKeyId()).isNull();
        assertThat(AuthContext.getAuthType()).isNull();
        assertThat(AuthContext.getModelName()).isNull();
    }

    @Test
    @DisplayName("ThreadLocal隔离 - 不同线程互不影响")
    void threadLocal_isolation() throws Exception {
        AuthContext.set(1L, 5L, "API_KEY", "model-a");

        Thread other = new Thread(() -> {
            // 子线程不应看到父线程的值
            assertThat(AuthContext.getTenantId()).isNull();
            AuthContext.set(2L, 10L, "JWT", "model-b");
            assertThat(AuthContext.getTenantId()).isEqualTo(2L);
        });
        other.start();
        other.join();

        // 父线程值不受影响
        assertThat(AuthContext.getTenantId()).isEqualTo(1L);
        assertThat(AuthContext.getAuthType()).isEqualTo("API_KEY");
    }

    @Test
    @DisplayName("set - JWT鉴权时apiKeyId为null")
    void set_jwtAuth_apiKeyIdNull() {
        AuthContext.set(1L, null, "JWT", "gpt-4o-mini");

        assertThat(AuthContext.getTenantId()).isEqualTo(1L);
        assertThat(AuthContext.getApiKeyId()).isNull();
        assertThat(AuthContext.getAuthType()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("clear后再set - 可以重新设置")
    void clearThenSet_resetsValues() {
        AuthContext.set(1L, 5L, "API_KEY", "model-a");
        AuthContext.clear();
        AuthContext.set(2L, 10L, "JWT", "model-b");

        assertThat(AuthContext.getTenantId()).isEqualTo(2L);
        assertThat(AuthContext.getApiKeyId()).isEqualTo(10L);
        assertThat(AuthContext.getAuthType()).isEqualTo("JWT");
        assertThat(AuthContext.getModelName()).isEqualTo("model-b");
    }
}
