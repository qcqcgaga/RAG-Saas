package com.docchat.module_knowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_versions")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "chunking_strategy", nullable = false, length = 30)
    private String chunkingStrategy;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    @Column(name = "chunk_overlap", nullable = false)
    private Integer chunkOverlap;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (chunkingStrategy == null) chunkingStrategy = "FIXED_SIZE";
        if (chunkSize == null) chunkSize = 500;
        if (chunkOverlap == null) chunkOverlap = 50;
        if (chunkCount == null) chunkCount = 0;
        if (status == null) status = "PENDING";
    }
}
