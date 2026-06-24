package com.docchat.module_knowledge.repository;

import com.docchat.module_knowledge.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByKnowledgeId(Long knowledgeId, Pageable pageable);

    Page<Document> findByKnowledgeIdAndStatus(Long knowledgeId, String status, Pageable pageable);

    Page<Document> findByKnowledgeIdAndOriginalNameContaining(
            Long knowledgeId, String keyword, Pageable pageable);

    long countByKnowledgeId(Long knowledgeId);
}
