package com.docchat.module_knowledge.repository;

import com.docchat.module_knowledge.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    Optional<KnowledgeBase> findByTenantId(Long tenantId);
}
