package com.docchat.module_eval.dto;

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
public class EvalResultResponse {

    private Long id;
    private Long evalSetId;
    private BigDecimal hitRate;
    private int totalPairs;
    private int hitCount;
    private String status;
    private int durationMs;
    private OffsetDateTime createdAt;
}
