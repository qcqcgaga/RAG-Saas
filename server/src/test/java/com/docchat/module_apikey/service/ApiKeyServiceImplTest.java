package com.docchat.module_apikey.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.module_apikey.dto.ApiKeyResponse;
import com.docchat.module_apikey.dto.ApiKeyValidationResult;
import com.docchat.module_apikey.entity.ApiKey;
import com.docchat.module_apikey.repository.ApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * API Key 服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceImplTest {

    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @InjectMocks private ApiKeyServiceImpl apiKeyService;

    // ========== createKey ==========

    @Test
    @DisplayName("createKey - 正常创建返回完整Key")
    void createKey_happyPath() {
        when(apiKeyRepository.countByTenantIdAndStatus(1L, 1)).thenReturn(0L);
        when(apiKeyRepository.save(any())).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            k.setId(100L);
            return k;
        });

        ApiKeyResponse response = apiKeyService.createKey(1L, "测试Key");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getKey()).startsWith("dc_");
        assertThat(response.getKey()).hasSize(35); // dc_ + 32 hex
        assertThat(response.getKeyPrefix()).hasSize(7);
        assertThat(response.getStatus()).isEqualTo((short) 1);
        assertThat(response.getName()).isEqualTo("测试Key");
        verify(apiKeyRepository).save(any());
    }

    @Test
    @DisplayName("createKey - 达到上限5个抛APIKEY_LIMIT_EXCEEDED")
    void createKey_exceedLimit_throws() {
        when(apiKeyRepository.countByTenantIdAndStatus(1L, 1)).thenReturn(5L);

        assertThatThrownBy(() -> apiKeyService.createKey(1L, "Key6"))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.APIKEY_LIMIT_EXCEEDED.getCode()));
    }

    @Test
    @DisplayName("createKey - 上限边界4个时仍可创建")
    void createKey_atLimit4_stillAllowed() {
        when(apiKeyRepository.countByTenantIdAndStatus(1L, 1)).thenReturn(4L);
        when(apiKeyRepository.save(any())).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            k.setId(101L);
            return k;
        });

        ApiKeyResponse response = apiKeyService.createKey(1L, "Key5");

        assertThat(response.getId()).isEqualTo(101L);
        verify(apiKeyRepository).save(any());
    }

    // ========== listKeys ==========

    @Test
    @DisplayName("listKeys - 返回租户下有效Key列表")
    void listKeys_happyPath() {
        ApiKey key1 = buildApiKey(1L, 1L, "dc_a1b2", "key1", 1);
        ApiKey key2 = buildApiKey(2L, 1L, "dc_c3d4", "key2", 1);
        when(apiKeyRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(1L, 1))
                .thenReturn(List.of(key2, key1));

        List<ApiKeyResponse> result = apiKeyService.listKeys(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("key2");
        assertThat(result.get(1).getName()).isEqualTo("key1");
        // listKeys 返回的 key 字段应为 null（不暴露完整Key）
        assertThat(result.get(0).getKey()).isNull();
        assertThat(result.get(1).getKey()).isNull();
    }

    @Test
    @DisplayName("listKeys - 无Key时返回空列表")
    void listKeys_empty() {
        when(apiKeyRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(1L, 1))
                .thenReturn(List.of());

        List<ApiKeyResponse> result = apiKeyService.listKeys(1L);

        assertThat(result).isEmpty();
    }

    // ========== revokeKey ==========

    @Test
    @DisplayName("revokeKey - 正常吊销设置status=0")
    void revokeKey_happyPath() {
        ApiKey key = buildApiKey(1L, 1L, "dc_a1b2", "myKey", 1);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyResponse response = apiKeyService.revokeKey(1L, 1L);

        assertThat(response.getStatus()).isEqualTo((short) 0);
        verify(apiKeyRepository).save(any());
    }

    @Test
    @DisplayName("revokeKey - Key不存在抛APIKEY_NOT_FOUND")
    void revokeKey_notFound_throws() {
        when(apiKeyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revokeKey(1L, 999L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.APIKEY_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("revokeKey - 非本租户Key抛APIKEY_NOT_FOUND")
    void revokeKey_otherTenant_throws() {
        ApiKey key = buildApiKey(1L, 2L, "dc_a1b2", "otherKey", 1);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> apiKeyService.revokeKey(1L, 1L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.APIKEY_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("revokeKey - 重复吊销已吊销Key抛APIKEY_ALREADY_REVOKED")
    void revokeKey_alreadyRevoked_throws() {
        ApiKey key = buildApiKey(1L, 1L, "dc_a1b2", "myKey", 0); // 已吊销
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> apiKeyService.revokeKey(1L, 1L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.APIKEY_ALREADY_REVOKED.getCode()));
    }

    // ========== renameKey ==========

    @Test
    @DisplayName("renameKey - 正常重命名")
    void renameKey_happyPath() {
        ApiKey key = buildApiKey(1L, 1L, "dc_a1b2", "oldName", 1);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyResponse response = apiKeyService.renameKey(1L, 1L, "newName");

        assertThat(response.getName()).isEqualTo("newName");
        verify(apiKeyRepository).save(any());
    }

    // ========== validateKey ==========

    @Test
    @DisplayName("validateKey - 有效Key返回成功结果")
    void validateKey_validKey_returnsSuccess() {
        ApiKey key = buildApiKey(1L, 1L, "dc_a1b2", "myKey", 1);
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any())).thenReturn(key);

        ApiKeyValidationResult result = apiKeyService.validateKey("someHash");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getTenantId()).isEqualTo(1L);
        assertThat(result.getApiKeyId()).isEqualTo(1L);
        assertThat(result.getAuthType()).isEqualTo("apikey");
        verify(apiKeyRepository).save(any()); // 更新 lastUsedAt
    }

    @Test
    @DisplayName("validateKey - 不存在的Key返回失败")
    void validateKey_notFound_returnsInvalid() {
        when(apiKeyRepository.findByKeyHash("badHash")).thenReturn(Optional.empty());

        ApiKeyValidationResult result = apiKeyService.validateKey("badHash");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getTenantId()).isNull();
    }

    @Test
    @DisplayName("validateKey - 已吊销的Key返回失败")
    void validateKey_revokedKey_returnsInvalid() {
        ApiKey key = buildApiKey(1L, 1L, "dc_a1b2", "myKey", 0); // 已吊销
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.of(key));

        ApiKeyValidationResult result = apiKeyService.validateKey("someHash");

        assertThat(result.isValid()).isFalse();
    }

    // ========== checkQuota ==========

    @Test
    @DisplayName("checkQuota - 未使用时允许")
    void checkQuota_noUsage_allowed() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        boolean result = apiKeyService.checkQuota(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("checkQuota - 未达限额时允许")
    void checkQuota_belowLimit_allowed() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("500");

        boolean result = apiKeyService.checkQuota(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("checkQuota - 达到限额时拒绝")
    void checkQuota_atLimit_denied() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("1000");

        boolean result = apiKeyService.checkQuota(1L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("checkQuota - 超过限额时拒绝")
    void checkQuota_overLimit_denied() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("1500");

        boolean result = apiKeyService.checkQuota(1L);

        assertThat(result).isFalse();
    }

    // ========== incrementQuota ==========

    @Test
    @DisplayName("incrementQuota - 首次递增设置过期时间")
    void incrementQuota_firstIncrement_setsExpiry() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        apiKeyService.incrementQuota(1L);

        verify(valueOperations).increment(anyString());
        verify(stringRedisTemplate).expire(anyString(), any());
    }

    @Test
    @DisplayName("incrementQuota - 非首次递增不设置过期时间")
    void incrementQuota_subsequentIncrement_noExpiry() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        apiKeyService.incrementQuota(1L);

        verify(valueOperations).increment(anyString());
        verify(stringRedisTemplate, never()).expire(anyString(), any());
    }

    // ========== 辅助方法 ==========

    private ApiKey buildApiKey(Long id, Long tenantId, String prefix, String name, int status) {
        ApiKey key = new ApiKey();
        key.setId(id);
        key.setTenantId(tenantId);
        key.setKeyHash("hash_" + id);
        key.setKeyEncrypted("encrypted_value_" + id);
        key.setKeyPrefix(prefix);
        key.setName(name);
        key.setStatus((short) status);
        key.setCreatedAt(Instant.now());
        return key;
    }
}
