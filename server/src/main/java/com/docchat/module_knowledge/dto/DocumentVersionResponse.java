package com.docchat.module_knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionResponse {
    private Long id;
    private Integer version;
    private String chunkingStrategy;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer chunkCount;
    private String status;
    private OffsetDateTime createdAt;
}
