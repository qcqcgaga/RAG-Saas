package com.docchat.module_stat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 对话用量日志实体
 *
 * 每次 API_KEY 鉴权的对话调用记录一条日志，
 * 用于用量统计和限额计算。
 * JWT 鉴权（预览对话）不记录。
 */
@Getter
@Setter
@Entity
@Table(name = "chat_usage_logs")
public class ChatUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    /** 鉴权类型：API_KEY / WIDGET / JWT */
    @Column(name = "auth_type", nullable = false, length = 10)
    private String authType;

    /** LLM 模型名称 */
    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens = 0;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
