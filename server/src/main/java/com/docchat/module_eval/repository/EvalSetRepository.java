package com.docchat.module_eval.repository;

import com.docchat.module_eval.entity.EvalSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalSetRepository extends JpaRepository<EvalSet, Long> {

    List<EvalSet> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    long countByTenantId(Long tenantId);
}
