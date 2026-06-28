package com.docchat.module_apikey.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.module_apikey.dto.ApiKeyResponse;
import com.docchat.module_apikey.dto.ApiKeyValidationResult;
import com.docchat.module_apikey.entity.ApiKey;
import com.docchat.module_apikey.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * API Key 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final int MAX_KEYS_PER_TENANT = 5;
    private static final String KEY_PREFIX = "dc_";
    private static final int KEY_RANDOM_LENGTH = 32;
    private static final String QUOTA_KEY_PREFIX = "docchat:quota:";
    private static final long QUOTA_TTL_SECONDS = 86400L;

    private final ApiKeyRepository apiKeyRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ApiKeyResponse createKey(Long tenantId, String name) {
        checkKeyLimit(tenantId);

        String key = generateKey();
        String keyHash = sha256(key);
        String keyEncrypted = encryptKey(key);
        String keyPrefix = key.substring(0, 7);

        ApiKey entity = new ApiKey();
        entity.setTenantId(tenantId);
        entity.setKeyHash(keyHash);
        entity.setKeyEncrypted(keyEncrypted);
        entity.setKeyPrefix(keyPrefix);
        entity.setName(name);
        entity.setStatus((short) 1);

        entity = apiKeyRepository.save(entity);
        log.info("API Key 创建成功: tenantId={}, keyId={}, prefix={}", tenantId, entity.getId(), keyPrefix);

        return toResponse(entity, key);
    }

    @Override
    public List<ApiKeyResponse> listKeys(Long tenantId) {
        return apiKeyRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, 1)
                .stream()
                .map(key -> toResponse(key, null))
                .toList();
    }

    @Override
    @Transactional
    public ApiKeyResponse revokeKey(Long tenantId, Long keyId) {
        ApiKey entity = findAndVerifyOwnership(tenantId, keyId);

        if (entity.getStatus() == (short) 0) {
            throw new BizException(ErrorCode.APIKEY_ALREADY_REVOKED);
        }

        entity.setStatus((short) 0);
        entity.setRevokedAt(Instant.now());
        entity = apiKeyRepository.save(entity);

        log.info("API Key 吊销成功: tenantId={}, keyId={}", tenantId, keyId);
        return toResponse(entity, null);
    }

    @Override
    @Transactional
    public ApiKeyResponse renameKey(Long tenantId, Long keyId, String name) {
        ApiKey entity = findAndVerifyOwnership(tenantId, keyId);
        entity.setName(name);
        entity = apiKeyRepository.save(entity);

        log.info("API Key 重命名: tenantId={}, keyId={}", tenantId, keyId);
        return toResponse(entity, null);
    }

    @Override
    @Transactional
    public ApiKeyValidationResult validateKey(String keyHash) {
        ApiKey entity = apiKeyRepository.findByKeyHash(keyHash).orElse(null);

        if (entity == null || entity.getStatus() != (short) 1) {
            return new ApiKeyValidationResult(false, null, null, null);
        }

        entity.setLastUsedAt(Instant.now());
        apiKeyRepository.save(entity);

        return new ApiKeyValidationResult(true, entity.getTenantId(), entity.getId(), "apikey");
    }

    @Override
    public boolean checkQuota(Long tenantId) {
        String quotaKey = buildQuotaKey(tenantId);
        String countStr = stringRedisTemplate.opsForValue().get(quotaKey);
        return countStr == null || Long.parseLong(countStr) < getDefaultQuotaLimit();
    }

    @Override
    public void incrementQuota(Long tenantId) {
        String quotaKey = buildQuotaKey(tenantId);
        Long count = stringRedisTemplate.opsForValue().increment(quotaKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(quotaKey, java.time.Duration.ofSeconds(QUOTA_TTL_SECONDS));
        }
    }

    // ========== 私有方法 ==========

    private void checkKeyLimit(Long tenantId) {
        long activeCount = apiKeyRepository.countByTenantIdAndStatus(tenantId, 1);
        if (activeCount >= MAX_KEYS_PER_TENANT) {
            throw new BizException(ErrorCode.APIKEY_LIMIT_EXCEEDED,
                    "每个租户最多创建" + MAX_KEYS_PER_TENANT + "个 API Key");
        }
    }

    private ApiKey findAndVerifyOwnership(Long tenantId, Long keyId) {
        ApiKey entity = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new BizException(ErrorCode.APIKEY_NOT_FOUND));

        if (!entity.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.APIKEY_NOT_FOUND);
        }
        return entity;
    }

    private String generateKey() {
        byte[] bytes = new byte[KEY_RANDOM_LENGTH / 2];
        secureRandom.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(KEY_RANDOM_LENGTH);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return KEY_PREFIX + hex;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 加密 Key — MVP 阶段使用 Base64 编码
     * TODO: 替换为 AES-256-GCM 加密，密钥从配置中心获取
     */
    private String encryptKey(String key) {
        return Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }

    private String buildQuotaKey(Long tenantId) {
        String today = LocalDate.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        return QUOTA_KEY_PREFIX + tenantId + ":" + today;
    }

    private long getDefaultQuotaLimit() {
        // TODO: 从租户套餐配置读取
        return 1000L;
    }

    private ApiKeyResponse toResponse(ApiKey entity, String rawKey) {
        String keyMasked = entity.getKeyPrefix() + "****"
                + entity.getKeyEncrypted().substring(entity.getKeyEncrypted().length() - 4);

        return ApiKeyResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .key(rawKey)
                .keyPrefix(entity.getKeyPrefix())
                .keyMasked(keyMasked)
                .status(entity.getStatus())
                .dailyLimit(getDefaultQuotaLimit())
                .lastUsedAt(entity.getLastUsedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
