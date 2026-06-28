package com.docchat.module_chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 租户 LLM 配置实体
 *
 * 每个租户可配置自己的 LLM API 端点和密钥。
 * 未配置时使用系统默认（application.yml）。
 */
@Getter
@Setter
@Entity
@Table(name = "tenant_llm_configs")
public class TenantLlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    /** LLM API 端点 URL */
    @Column(name = "api_url", nullable = false, length = 500)
    private String apiUrl;

    /** AES-256 加密的 API Key */
    @Column(name = "api_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    /** 模型名称 */
    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    /** 状态：1-启用 0-禁用 */
    @Column(name = "status", nullable = false)
    private Short status = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
