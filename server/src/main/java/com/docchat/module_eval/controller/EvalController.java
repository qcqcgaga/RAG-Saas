package com.docchat.module_eval.controller;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.response.R;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_eval.dto.*;
import com.docchat.module_eval.service.EvalService;
import com.docchat.module_eval.service.ImportResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评测集管理控制器
 */
@RestController
@RequestMapping("/api/v1/eval")
@RequiredArgsConstructor
public class EvalController {

    private final EvalService evalService;

    @PostMapping("/sets")
    public R<EvalSetResponse> createSet(
            @Valid @RequestBody CreateEvalSetRequest request) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.createSet(tenantId,
                request.getName(), request.getDescription()));
    }

    @GetMapping("/sets")
    public R<List<EvalSetResponse>> listSets() {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.listSets(tenantId));
    }

    @GetMapping("/sets/{setId}")
    public R<EvalSetResponse> getSet(@PathVariable Long setId) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.getSet(tenantId, setId));
    }

    @PutMapping("/sets/{setId}")
    public R<EvalSetResponse> updateSet(
            @PathVariable Long setId,
            @Valid @RequestBody CreateEvalSetRequest request) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.updateSet(tenantId, setId,
                request.getName(), request.getDescription()));
    }

    @DeleteMapping("/sets/{setId}")
    public R<Void> deleteSet(@PathVariable Long setId) {
        Long tenantId = requireCurrentTenantId();
        evalService.deleteSet(tenantId, setId);
        return R.ok(null);
    }

    @PostMapping("/sets/{setId}/pairs")
    public R<EvalPairResponse> addPair(
            @PathVariable Long setId,
            @Valid @RequestBody AddEvalPairRequest request) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.addPair(tenantId, setId,
                request.getQuestion(), request.getExpectedDocument()));
    }

    @GetMapping("/sets/{setId}/pairs")
    public R<List<EvalPairResponse>> listPairs(@PathVariable Long setId) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.listPairs(tenantId, setId));
    }

    @DeleteMapping("/sets/{setId}/pairs/{pairId}")
    public R<Void> deletePair(
            @PathVariable Long setId,
            @PathVariable Long pairId) {
        Long tenantId = requireCurrentTenantId();
        evalService.deletePair(tenantId, setId, pairId);
        return R.ok(null);
    }

    @PostMapping("/sets/{setId}/pairs/import")
    public R<ImportResult> importPairs(
            @PathVariable Long setId,
            @Valid @RequestBody ImportPairsRequest request) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.importPairs(tenantId, setId, request.getPairs()));
    }

    @PostMapping("/sets/{setId}/run")
    public R<EvalRunResponse> runEval(@PathVariable Long setId) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.runEval(tenantId, setId));
    }

    @GetMapping("/sets/{setId}/results")
    public R<List<EvalResultResponse>> listResults(@PathVariable Long setId) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.listResults(tenantId, setId));
    }

    @GetMapping("/sets/{setId}/results/{resultId}")
    public R<EvalResultDetailResponse> getResult(
            @PathVariable Long setId,
            @PathVariable Long resultId) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(evalService.getResult(tenantId, setId, resultId));
    }

    /**
     * 强制清理评测锁
     *
     * 当评测卡在"已在执行中"状态时，允许管理员手动清理 Redis 锁。
     * 这是一种安全兜底机制，锁本身有 TTL（5分钟），此接口用于紧急情况。
     */
    @DeleteMapping("/sets/{setId}/lock")
    public R<Void> clearLock(@PathVariable Long setId) {
        Long tenantId = requireCurrentTenantId();
        evalService.clearLock(tenantId, setId);
        return R.ok(null);
    }

    /**
     * 要求当前租户ID，为空则抛出401业务异常
     */
    private Long requireCurrentTenantId() {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        return tenantId;
    }
}
