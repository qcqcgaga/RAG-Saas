package com.docchat.module_chat.repository;

import com.docchat.module_chat.entity.TenantLlmConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 租户 LLM 配置 Repository
 */
public interface TenantLlmConfigRepository extends JpaRepository<TenantLlmConfig, Long> {

    Optional<TenantLlmConfig> findByTenantIdAndStatus(Long tenantId, Integer status);

    Optional<TenantLlmConfig> findByTenantId(Long tenantId);
}
