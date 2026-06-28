package com.docchat.module_eval.service;

import com.docchat.common.response.ErrorCode;
import com.docchat.module_chat.dto.SourceReference;
import com.docchat.module_chat.service.RetrievalService;
import com.docchat.module_eval.dto.*;
import com.docchat.module_eval.entity.EvalPair;
import com.docchat.module_eval.entity.EvalResult;
import com.docchat.module_eval.entity.EvalSet;
import com.docchat.module_eval.repository.EvalPairRepository;
import com.docchat.module_eval.repository.EvalResultRepository;
import com.docchat.module_eval.repository.EvalSetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 评测服务实现
 *
 * V1 R5 修复要点：
 * 1. 虚拟线程中 JPA 操作使用独立事务（@Transactional(propagation = REQUIRES_NEW)）
 * 2. retrievalService.retrieve() 完全容错：Milvus 不可用时所有 pair 标记为未命中
 * 3. Redis 锁增加 TTL 兜底 + 多重清理保证
 * 4. 主线程 runEval() save 后立即返回，不在主线程做任何可能抛异常的操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalServiceImpl implements EvalService {

    private static final int MAX_SETS_PER_TENANT = 10;
    private static final int MAX_PAIRS_PER_SET = 50;
    private static final int EVAL_TOP_K = 5;
    private static final String LOCK_KEY_PREFIX = "docchat:eval:running:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final EvalSetRepository evalSetRepository;
    private final EvalPairRepository evalPairRepository;
    private final EvalResultRepository evalResultRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RetrievalService retrievalService;
    private final EvalSelfService evalSelfService;

    @Override
    @Transactional
    public EvalSetResponse createSet(Long tenantId, String name, String description) {
        long count = evalSetRepository.countByTenantId(tenantId);
        if (count >= MAX_SETS_PER_TENANT) {
            throw ErrorCode.EVAL_SET_LIMIT_EXCEEDED.asBizException(
                    "每个租户最多 " + MAX_SETS_PER_TENANT + " 个评测集");
        }
        EvalSet set = new EvalSet();
        set.setTenantId(tenantId);
        set.setName(name);
        set.setDescription(description);
        set.setPairCount(0);
        evalSetRepository.save(set);
        log.info("评测集创建: tenantId={}, setId={}", tenantId, set.getId());
        return toSetResponse(set);
    }

    @Override
    public List<EvalSetResponse> listSets(Long tenantId) {
        return evalSetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toSetResponse).toList();
    }

    @Override
    public EvalSetResponse getSet(Long tenantId, Long setId) {
        EvalSet set = findSetOrFail(tenantId, setId);
        return toSetResponse(set);
    }

    @Override
    @Transactional
    public EvalSetResponse updateSet(Long tenantId, Long setId,
                                      String name, String description) {
        EvalSet set = findSetOrFail(tenantId, setId);
        if (name != null) set.setName(name);
        if (description != null) set.setDescription(description);
        evalSetRepository.save(set);
        return toSetResponse(set);
    }

    @Override
    @Transactional
    public void deleteSet(Long tenantId, Long setId) {
        findSetOrFail(tenantId, setId);
        evalPairRepository.deleteByEvalSetId(setId);
        evalSetRepository.deleteById(setId);
        log.info("评测集删除: tenantId={}, setId={}", tenantId, setId);
    }

    @Override
    @Transactional
    public EvalPairResponse addPair(Long tenantId, Long setId,
                                     String question, String expectedDocument) {
        EvalSet set = findSetOrFail(tenantId, setId);
        long pairCount = evalPairRepository.countByEvalSetId(setId);
        if (pairCount >= MAX_PAIRS_PER_SET) {
            throw ErrorCode.EVAL_PAIR_LIMIT_EXCEEDED.asBizException(
                    "每个评测集最多 " + MAX_PAIRS_PER_SET + " 个问答对");
        }
        EvalPair pair = new EvalPair();
        pair.setEvalSetId(setId);
        pair.setTenantId(tenantId);
        pair.setQuestion(question);
        pair.setExpectedDocument(expectedDocument);
        pair.setSortOrder((int) pairCount);
        evalPairRepository.save(pair);

        set.setPairCount((int) pairCount + 1);
        evalSetRepository.save(set);

        return toPairResponse(pair);
    }

    @Override
    public List<EvalPairResponse> listPairs(Long tenantId, Long setId) {
        findSetOrFail(tenantId, setId);
        return evalPairRepository.findByEvalSetIdOrderBySortOrder(setId)
                .stream().map(this::toPairResponse).toList();
    }

    @Override
    @Transactional
    public void deletePair(Long tenantId, Long setId, Long pairId) {
        findSetOrFail(tenantId, setId);
        evalPairRepository.deleteById(pairId);
        // 更新 pairCount
        long remaining = evalPairRepository.countByEvalSetId(setId);
        EvalSet set = evalSetRepository.findById(setId).orElseThrow();
        set.setPairCount((int) remaining);
        evalSetRepository.save(set);
    }

    @Override
    @Transactional
    public ImportResult importPairs(Long tenantId, Long setId,
                                     List<ImportPairsRequest.PairItem> pairs) {
        EvalSet set = findSetOrFail(tenantId, setId);
        long currentCount = evalPairRepository.countByEvalSetId(setId);
        if (currentCount + pairs.size() > MAX_PAIRS_PER_SET) {
            throw ErrorCode.EVAL_PAIR_LIMIT_EXCEEDED.asBizException(
                    "导入后超过上限 " + MAX_PAIRS_PER_SET);
        }

        int sortOrder = (int) currentCount;
        for (ImportPairsRequest.PairItem item : pairs) {
            EvalPair pair = new EvalPair();
            pair.setEvalSetId(setId);
            pair.setTenantId(tenantId);
            pair.setQuestion(item.getQuestion());
            pair.setExpectedDocument(item.getExpectedDocument());
            pair.setSortOrder(sortOrder++);
            evalPairRepository.save(pair);
        }

        set.setPairCount(sortOrder);
        evalSetRepository.save(set);
        return new ImportResult(pairs.size(), sortOrder);
    }

    @Override
    public EvalRunResponse runEval(Long tenantId, Long setId) {
        findSetOrFail(tenantId, setId);
        long pairCount = evalPairRepository.countByEvalSetId(setId);
        if (pairCount == 0) {
            throw ErrorCode.EVAL_SET_EMPTY.asBizException();
        }

        // 防重复执行 — 使用短 TTL 兜底，避免锁泄漏
        String lockKey = LOCK_KEY_PREFIX + setId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_TTL);
        if (locked == null || !locked) {
            throw ErrorCode.EVAL_ALREADY_RUNNING.asBizException();
        }

        // 创建运行中的结果 — 在主线程事务中保存
        EvalResult result = new EvalResult();
        result.setEvalSetId(setId);
        result.setTenantId(tenantId);
        result.setHitRate(BigDecimal.ZERO);
        result.setTotalPairs((int) pairCount);
        result.setHitCount(0);
        result.setStatus("RUNNING");
        result.setDurationMs(0);
        evalResultRepository.save(result);

        final Long resultId = result.getId();
        log.info("评测已启动: setId={}, resultId={}, pairCount={}", setId, resultId, pairCount);

        // 使用虚拟线程异步执行评测
        // 通过 evalSelfService（Spring 代理）执行事务操作，确保独立事务
        Thread.startVirtualThread(() -> {
            long startTime = System.currentTimeMillis();
            try {
                executeEval(tenantId, setId, resultId, startTime);
            } catch (Exception e) {
                // 虚拟线程顶层异常兜底
                log.error("评测虚拟线程异常: setId={}, resultId={}", setId, resultId, e);
                try {
                    int elapsed = (int) (System.currentTimeMillis() - startTime);
                    evalSelfService.markAsFailed(resultId, elapsed);
                } catch (Exception ex) {
                    log.error("标记评测FAILED失败: resultId={}", resultId, ex);
                }
            } finally {
                // 确保锁一定被清理 — 多重保证
                safeDeleteLock(setId);
            }
        });

        return new EvalRunResponse(resultId, "RUNNING");
    }

    /**
     * 执行评测（虚拟线程中运行）
     *
     * 关键设计：
     * 1. retrievalService.retrieve() 完全容错 — Milvus 不可用时返回空列表而非抛异常
     * 2. JPA 状态更新通过 evalSelfService（REQUIRES_NEW 事务）执行
     * 3. 单个 pair 检索失败不影响整体评测
     */
    private void executeEval(Long tenantId, Long setId, Long resultId, long startTime) {
        List<EvalPair> pairs = evalPairRepository.findByEvalSetIdOrderBySortOrder(setId);
        int hitCount = 0;
        List<EvalResultDetailResponse.PairDetail> details = new ArrayList<>();

        for (EvalPair pair : pairs) {
            try {
                // 调用 RetrievalService 检索 top-K 文档
                // 完全容错：Milvus 不可用 / collection 不存在 / 检索异常 → 返回空列表
                List<SourceReference> sources = safeRetrieve(pair.getQuestion(), tenantId);

                // 检查期望文档是否出现在检索结果中
                List<String> retrievedDocs = sources.stream()
                        .map(SourceReference::getDocumentName)
                        .toList();

                boolean hit = retrievedDocs.stream()
                        .anyMatch(doc -> doc.equals(pair.getExpectedDocument()));
                if (hit) hitCount++;

                details.add(new EvalResultDetailResponse.PairDetail(
                        pair.getId(), pair.getQuestion(),
                        pair.getExpectedDocument(), hit, retrievedDocs));
            } catch (Exception e) {
                // 单个问答对检索失败不影响整体评测
                log.warn("评测对处理失败: pairId={}, question={}", pair.getId(), pair.getQuestion(), e);
                details.add(new EvalResultDetailResponse.PairDetail(
                        pair.getId(), pair.getQuestion(),
                        pair.getExpectedDocument(), false, List.of()));
            }
        }

        // 计算命中率
        BigDecimal hitRate = pairs.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(hitCount)
                .divide(BigDecimal.valueOf(pairs.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        int durationMs = (int) (System.currentTimeMillis() - startTime);
        String detailJson = toJson(details);

        // 通过 evalSelfService 在独立事务中更新结果
        evalSelfService.updateEvalResult(resultId, "COMPLETED",
                hitRate, hitCount, durationMs, detailJson);

        log.info("评测完成: setId={}, hitRate={}%, hitCount={}/{}",
                setId, hitRate, hitCount, pairs.size());
    }

    /**
     * 安全检索 — Milvus 不可用 / collection 不存在时返回空列表而非抛异常
     *
     * 这是评测执行"三轮未修复"的根本对策：
     * retrievalService.retrieve() 依赖 Milvus，如果 Milvus 不可用或 collection 不存在，
     * 会直接抛异常导致整个评测流程中断。改为容错模式：检索失败视为"未命中"。
     */
    private List<SourceReference> safeRetrieve(String question, Long tenantId) {
        try {
            return retrievalService.retrieve(question, tenantId, EVAL_TOP_K);
        } catch (Exception e) {
            log.warn("检索失败（视为未命中）: tenantId={}, question={}, error={}",
                    tenantId, question.substring(0, Math.min(question.length(), 50)),
                    e.getMessage());
            return List.of();
        }
    }

    /**
     * 安全清理 Redis 锁
     */
    private void safeDeleteLock(Long setId) {
        try {
            redisTemplate.delete(LOCK_KEY_PREFIX + setId);
        } catch (Exception e) {
            log.error("清理评测锁失败: setId={}", setId, e);
        }
    }

    @Override
    public void clearLock(Long tenantId, Long setId) {
        findSetOrFail(tenantId, setId);
        safeDeleteLock(setId);
        log.info("评测锁已清理: tenantId={}, setId={}", tenantId, setId);
    }

    @Override
    public List<EvalResultResponse> listResults(Long tenantId, Long setId) {
        findSetOrFail(tenantId, setId);
        return evalResultRepository.findByEvalSetIdOrderByCreatedAtDesc(setId)
                .stream().map(this::toResultResponse).toList();
    }

    @Override
    public EvalResultDetailResponse getResult(Long tenantId, Long setId, Long resultId) {
        findSetOrFail(tenantId, setId);
        EvalResult result = evalResultRepository.findById(resultId)
                .filter(r -> r.getEvalSetId().equals(setId))
                .orElseThrow(() -> ErrorCode.EVAL_SET_NOT_FOUND.asBizException("评测结果不存在"));

        List<EvalResultDetailResponse.PairDetail> details = new ArrayList<>();
        if (result.getDetailJson() != null) {
            try {
                details = objectMapper.readValue(result.getDetailJson(),
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class, EvalResultDetailResponse.PairDetail.class));
            } catch (JsonProcessingException e) {
                log.warn("解析评测详情失败", e);
            }
        }

        return EvalResultDetailResponse.builder()
                .id(result.getId())
                .evalSetId(result.getEvalSetId())
                .hitRate(result.getHitRate())
                .totalPairs(result.getTotalPairs())
                .hitCount(result.getHitCount())
                .status(result.getStatus())
                .durationMs(result.getDurationMs())
                .createdAt(result.getCreatedAt())
                .detail(details)
                .build();
    }

    private EvalSet findSetOrFail(Long tenantId, Long setId) {
        return evalSetRepository.findById(setId)
                .filter(s -> s.getTenantId().equals(tenantId))
                .orElseThrow(() -> ErrorCode.EVAL_SET_NOT_FOUND.asBizException());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化失败", e);
            return "[]";
        }
    }

    private EvalSetResponse toSetResponse(EvalSet set) {
        return EvalSetResponse.builder()
                .id(set.getId()).name(set.getName())
                .description(set.getDescription())
                .pairCount(set.getPairCount())
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }

    private EvalPairResponse toPairResponse(EvalPair pair) {
        return EvalPairResponse.builder()
                .id(pair.getId()).evalSetId(pair.getEvalSetId())
                .question(pair.getQuestion())
                .expectedDocument(pair.getExpectedDocument())
                .sortOrder(pair.getSortOrder())
                .createdAt(pair.getCreatedAt())
                .build();
    }

    private EvalResultResponse toResultResponse(EvalResult r) {
        return EvalResultResponse.builder()
                .id(r.getId()).evalSetId(r.getEvalSetId())
                .hitRate(r.getHitRate())
                .totalPairs(r.getTotalPairs())
                .hitCount(r.getHitCount())
                .status(r.getStatus())
                .durationMs(r.getDurationMs())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
