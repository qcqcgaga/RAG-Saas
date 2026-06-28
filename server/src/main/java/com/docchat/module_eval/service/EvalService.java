package com.docchat.module_eval.service;

import com.docchat.module_eval.dto.*;

import java.util.List;

/**
 * 评测服务接口
 */
public interface EvalService {

    EvalSetResponse createSet(Long tenantId, String name, String description);

    List<EvalSetResponse> listSets(Long tenantId);

    EvalSetResponse getSet(Long tenantId, Long setId);

    EvalSetResponse updateSet(Long tenantId, Long setId, String name, String description);

    void deleteSet(Long tenantId, Long setId);

    EvalPairResponse addPair(Long tenantId, Long setId, String question, String expectedDocument);

    List<EvalPairResponse> listPairs(Long tenantId, Long setId);

    void deletePair(Long tenantId, Long setId, Long pairId);

    ImportResult importPairs(Long tenantId, Long setId, List<ImportPairsRequest.PairItem> pairs);

    EvalRunResponse runEval(Long tenantId, Long setId);

    List<EvalResultResponse> listResults(Long tenantId, Long setId);

    EvalResultDetailResponse getResult(Long tenantId, Long setId, Long resultId);

    /**
     * 强制清理评测锁（当评测卡在"已在执行中"时使用）
     */
    void clearLock(Long tenantId, Long setId);
}
