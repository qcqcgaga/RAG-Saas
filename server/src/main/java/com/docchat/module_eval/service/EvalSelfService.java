package com.docchat.module_eval.service;

import com.docchat.module_eval.entity.EvalResult;
import com.docchat.module_eval.repository.EvalResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 评测自引用服务
 *
 * 解决虚拟线程中 JPA 操作无事务管理的问题。
 * Spring AOP 代理要求同类内部调用通过另一个 Bean 才能触发 @Transactional，
 * 因此将需要在虚拟线程中执行的事务操作抽取到独立 Service。
 *
 * 所有方法使用 REQUIRES_NEW 传播级别，确保每次调用都有独立事务，
 * 不受主线程事务状态影响。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalSelfService {

    private final EvalResultRepository evalResultRepository;

    /**
     * 在独立事务中更新评测结果状态
     *
     * @param resultId 评测结果ID
     * @param status   目标状态（COMPLETED / FAILED）
     * @param hitRate  命中率（COMPLETED 时传入，FAILED 时传 null）
     * @param hitCount 命中数（COMPLETED 时传入，FAILED 时传 null）
     * @param durationMs 耗时毫秒
     * @param detailJson 详情JSON（COMPLETED 时传入，FAILED 时传 null）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEvalResult(Long resultId, String status,
                                  BigDecimal hitRate, Integer hitCount,
                                  int durationMs, String detailJson) {
        try {
            EvalResult result = evalResultRepository.findById(resultId).orElse(null);
            if (result == null) {
                log.warn("评测结果不存在，无法更新: resultId={}", resultId);
                return;
            }
            result.setStatus(status);
            result.setDurationMs(durationMs);
            if (hitRate != null) {
                result.setHitRate(hitRate);
            }
            if (hitCount != null) {
                result.setHitCount(hitCount);
            }
            if (detailJson != null) {
                result.setDetailJson(detailJson);
            }
            evalResultRepository.save(result);
            log.info("评测结果已更新: resultId={}, status={}, hitRate={}%",
                    resultId, status, hitRate);
        } catch (Exception e) {
            log.error("更新评测结果失败: resultId={}, status={}", resultId, status, e);
        }
    }

    /**
     * 在独立事务中标记评测结果为 FAILED
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long resultId, int durationMs) {
        updateEvalResult(resultId, "FAILED", null, null, durationMs, null);
    }
}
