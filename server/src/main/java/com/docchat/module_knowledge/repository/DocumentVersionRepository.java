package com.docchat.module_knowledge.repository;

import com.docchat.module_knowledge.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByVersionDesc(Long documentId);

    @Query("SELECT MAX(dv.version) FROM DocumentVersion dv WHERE dv.documentId = :documentId")
    Integer findMaxVersionByDocumentId(Long documentId);
}
