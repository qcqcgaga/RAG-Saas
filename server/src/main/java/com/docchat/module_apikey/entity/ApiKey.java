package com.docchat.module_apikey.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * API Key 实体
 *
 * 存储 API Key 的加密值、哈希值和元数据。
 * 完整 Key 仅在创建时展示一次，之后只展示脱敏值。
 */
@Getter
@Setter
@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** SHA-256 哈希，用于快速查找 */
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    /** AES-256 加密的完整 Key */
    @Column(name = "key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String keyEncrypted;

    /** Key 前缀（如 dc_a1b2），用于脱敏展示 */
    @Column(name = "key_prefix", nullable = false, length = 10)
    private String keyPrefix;

    /** Key 名称（管理员自定义） */
    @Column(name = "name", length = 50)
    private String name;

    /** 状态：1-有效 0-已吊销 */
    @Column(name = "status", nullable = false)
    private Short status = 1;

    /** 最后使用时间 */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 吊销时间 */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
