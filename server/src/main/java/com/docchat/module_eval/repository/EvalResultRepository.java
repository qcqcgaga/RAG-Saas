package com.docchat.module_eval.repository;

import com.docchat.module_eval.entity.EvalResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvalResultRepository extends JpaRepository<EvalResult, Long> {

    List<EvalResult> findByEvalSetIdOrderByCreatedAtDesc(Long evalSetId);

    Optional<EvalResult> findTopByEvalSetIdOrderByCreatedAtDesc(Long evalSetId);
}
