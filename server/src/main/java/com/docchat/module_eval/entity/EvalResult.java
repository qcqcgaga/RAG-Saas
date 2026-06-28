package com.docchat.module_eval.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "eval_results")
public class EvalResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eval_set_id", nullable = false)
    private Long evalSetId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "hit_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal hitRate;

    @Column(name = "total_pairs", nullable = false)
    private int totalPairs;

    @Column(name = "hit_count", nullable = false)
    private int hitCount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "detail_json", columnDefinition = "jsonb")
    private String detailJson;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null) {
            status = "COMPLETED";
        }
    }
}
