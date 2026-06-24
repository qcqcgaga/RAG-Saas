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
public class KnowledgeResponse {
    private Long id;
    private String name;
    private String description;
    private Integer documentCount;
    private Integer chunkCount;
    private OffsetDateTime createdAt;
}
