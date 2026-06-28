package com.docchat.module_eval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 评测结果详情响应（含每个问答对的检索结果）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalResultDetailResponse {

    private Long id;
    private Long evalSetId;
    private BigDecimal hitRate;
    private int totalPairs;
    private int hitCount;
    private String status;
    private int durationMs;
    private OffsetDateTime createdAt;
    private List<PairDetail> detail;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PairDetail {

        private Long pairId;
        private String question;
        private String expectedDocument;
        private boolean hit;
        private List<String> retrievedDocuments;
    }
}
