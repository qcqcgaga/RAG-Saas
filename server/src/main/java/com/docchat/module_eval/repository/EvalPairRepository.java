package com.docchat.module_eval.repository;

import com.docchat.module_eval.entity.EvalPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalPairRepository extends JpaRepository<EvalPair, Long> {

    List<EvalPair> findByEvalSetIdOrderBySortOrder(Long evalSetId);

    long countByEvalSetId(Long evalSetId);

    void deleteByEvalSetId(Long evalSetId);
}
