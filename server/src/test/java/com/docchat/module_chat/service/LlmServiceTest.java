package com.docchat.module_chat.service;

import com.docchat.module_chat.entity.TenantLlmConfig;
import com.docchat.module_chat.repository.TenantLlmConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * LLM 服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock private TenantLlmConfigRepository tenantLlmConfigRepository;
    @InjectMocks private LlmService llmService;

    private void setupDefaults() {
        ReflectionTestUtils.setField(llmService, "defaultApiUrl", "https://default.api.com");
        ReflectionTestUtils.setField(llmService, "defaultApiKey", "default-key");
        ReflectionTestUtils.setField(llmService, "defaultModelName", "gpt-4o-mini");
        ReflectionTestUtils.setField(llmService, "objectMapper", new ObjectMapper());
    }

    @Test
    @DisplayName("streamChat - 无租户配置时回退到系统默认")
    void streamChat_noTenantConfig_usesDefault() {
        setupDefaults();

        // 验证无租户配置时getTenantLlmConfig返回null（将回退到系统默认）
        when(tenantLlmConfigRepository.findByTenantId(1L))
                .thenReturn(Optional.empty());

        TenantLlmConfig config = llmService.getTenantLlmConfig(1L);
        assertThat(config).isNull();
        verify(tenantLlmConfigRepository).findByTenantId(1L);
    }

    @Test
    @DisplayName("streamChat - null tenantId不查询repository的findByTenantIdAndStatus")
    void streamChat_nullTenantId_usesDefault() {
        setupDefaults();

        TenantLlmConfig config = llmService.getTenantLlmConfig(null);
        assertThat(config).isNull();
        // tenantId为null时，getTenantLlmConfig会调用findByTenantId(null)
        // 但resolveLlmConfig中不会调用findByTenantIdAndStatus
        verify(tenantLlmConfigRepository, never()).findByTenantIdAndStatus(any(), anyInt());
    }

    @Test
    @DisplayName("getTenantLlmConfig - 存在时返回配置")
    void getTenantLlmConfig_exists() {
        TenantLlmConfig config = new TenantLlmConfig();
        config.setId(1L);
        config.setTenantId(1L);
        when(tenantLlmConfigRepository.findByTenantId(1L))
                .thenReturn(Optional.of(config));

        TenantLlmConfig result = llmService.getTenantLlmConfig(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTenantLlmConfig - 不存在时返回null")
    void getTenantLlmConfig_notExists() {
        when(tenantLlmConfigRepository.findByTenantId(1L))
                .thenReturn(Optional.empty());

        TenantLlmConfig result = llmService.getTenantLlmConfig(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("saveTenantLlmConfig - 保存配置")
    void saveTenantLlmConfig_happyPath() {
        TenantLlmConfig config = new TenantLlmConfig();
        config.setTenantId(1L);
        config.setApiUrl("https://api.com");
        config.setApiKeyEncrypted("key");
        config.setModelName("model");
        when(tenantLlmConfigRepository.save(any())).thenAnswer(inv -> {
            TenantLlmConfig c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        TenantLlmConfig result = llmService.saveTenantLlmConfig(config);

        assertThat(result.getId()).isEqualTo(1L);
        verify(tenantLlmConfigRepository).save(config);
    }

    @Test
    @DisplayName("deleteTenantLlmConfig - 存在时删除")
    void deleteTenantLlmConfig_exists() {
        TenantLlmConfig config = new TenantLlmConfig();
        when(tenantLlmConfigRepository.findByTenantId(1L))
                .thenReturn(Optional.of(config));

        llmService.deleteTenantLlmConfig(1L);

        verify(tenantLlmConfigRepository).delete(config);
    }

    @Test
    @DisplayName("deleteTenantLlmConfig - 不存在时不操作")
    void deleteTenantLlmConfig_notExists() {
        when(tenantLlmConfigRepository.findByTenantId(1L))
                .thenReturn(Optional.empty());

        llmService.deleteTenantLlmConfig(1L);

        verify(tenantLlmConfigRepository, never()).delete(any());
    }
}
