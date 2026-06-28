package com.docchat.module_eval.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.module_chat.service.RetrievalService;
import com.docchat.module_eval.dto.*;
import com.docchat.module_eval.entity.EvalPair;
import com.docchat.module_eval.entity.EvalResult;
import com.docchat.module_eval.entity.EvalSet;
import com.docchat.module_eval.repository.EvalPairRepository;
import com.docchat.module_eval.repository.EvalResultRepository;
import com.docchat.module_eval.repository.EvalSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 评测服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class EvalServiceImplTest {

    @Mock private EvalSetRepository evalSetRepository;
    @Mock private EvalPairRepository evalPairRepository;
    @Mock private EvalResultRepository evalResultRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private RetrievalService retrievalService;
    @InjectMocks private EvalServiceImpl evalService;

    /** 真实 ObjectMapper，替换 @InjectMocks 注入的 mock */
    private final ObjectMapper realObjectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 用真实 ObjectMapper 替换 mock，使 JSON 序列化/反序列化正常工作
        ReflectionTestUtils.setField(evalService, "objectMapper", realObjectMapper);
    }

    // ========== createSet ==========

    @Test
    @DisplayName("createSet - 正常创建评测集")
    void createSet_happyPath() {
        when(evalSetRepository.countByTenantId(1L)).thenReturn(0L);
        when(evalSetRepository.save(any())).thenAnswer(inv -> {
            EvalSet s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        EvalSetResponse response = evalService.createSet(1L, "测试集", "描述");

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("测试集");
        assertThat(response.getPairCount()).isEqualTo(0);
        verify(evalSetRepository).save(any());
    }

    @Test
    @DisplayName("createSet - 达到上限10个抛EVAL_SET_LIMIT_EXCEEDED")
    void createSet_exceedLimit_throws() {
        when(evalSetRepository.countByTenantId(1L)).thenReturn(10L);

        assertThatThrownBy(() -> evalService.createSet(1L, "超限集", ""))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_SET_LIMIT_EXCEEDED.getCode()));
    }

    @Test
    @DisplayName("createSet - 上限边界9个时仍可创建")
    void createSet_atLimit9_stillAllowed() {
        when(evalSetRepository.countByTenantId(1L)).thenReturn(9L);
        when(evalSetRepository.save(any())).thenAnswer(inv -> {
            EvalSet s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });

        EvalSetResponse response = evalService.createSet(1L, "第10集", "");
        assertThat(response.getId()).isEqualTo(11L);
    }

    // ========== listSets ==========

    @Test
    @DisplayName("listSets - 返回租户评测集列表")
    void listSets_happyPath() {
        EvalSet set1 = buildEvalSet(1L, 1L, "集1", 3);
        EvalSet set2 = buildEvalSet(2L, 1L, "集2", 0);
        when(evalSetRepository.findByTenantIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(set2, set1));

        List<EvalSetResponse> result = evalService.listSets(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("集2");
    }

    @Test
    @DisplayName("listSets - 无评测集时返回空列表")
    void listSets_empty() {
        when(evalSetRepository.findByTenantIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<EvalSetResponse> result = evalService.listSets(1L);

        assertThat(result).isEmpty();
    }

    // ========== getSet ==========

    @Test
    @DisplayName("getSet - 正常获取评测集详情")
    void getSet_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 5);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));

        EvalSetResponse response = evalService.getSet(1L, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPairCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("getSet - 不存在抛EVAL_SET_NOT_FOUND")
    void getSet_notFound_throws() {
        when(evalSetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evalService.getSet(1L, 999L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_SET_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("getSet - 非本租户抛EVAL_SET_NOT_FOUND")
    void getSet_otherTenant_throws() {
        EvalSet set = buildEvalSet(1L, 2L, "别人的集", 0);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));

        assertThatThrownBy(() -> evalService.getSet(1L, 1L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_SET_NOT_FOUND.getCode()));
    }

    // ========== updateSet ==========

    @Test
    @DisplayName("updateSet - 更新名称和描述")
    void updateSet_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "旧名", 0);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalSetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EvalSetResponse response = evalService.updateSet(1L, 1L, "新名", "新描述");

        assertThat(response.getName()).isEqualTo("新名");
        assertThat(response.getDescription()).isEqualTo("新描述");
    }

    @Test
    @DisplayName("updateSet - 只更新名称（描述为null则不更新）")
    void updateSet_onlyName() {
        EvalSet set = buildEvalSet(1L, 1L, "旧名", 0);
        set.setDescription("保留描述");
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalSetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EvalSetResponse response = evalService.updateSet(1L, 1L, "新名", null);

        assertThat(response.getName()).isEqualTo("新名");
    }

    // ========== deleteSet ==========

    @Test
    @DisplayName("deleteSet - 正常删除评测集及其问答对")
    void deleteSet_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "待删集", 3);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));

        evalService.deleteSet(1L, 1L);

        verify(evalPairRepository).deleteByEvalSetId(1L);
        verify(evalSetRepository).deleteById(1L);
    }

    // ========== addPair ==========

    @Test
    @DisplayName("addPair - 正常添加问答对")
    void addPair_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 0);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(0L);
        when(evalPairRepository.save(any())).thenAnswer(inv -> {
            EvalPair p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(evalSetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EvalPairResponse response = evalService.addPair(1L, 1L, "什么是RAG?", "rag-guide.pdf");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getQuestion()).isEqualTo("什么是RAG?");
        assertThat(response.getExpectedDocument()).isEqualTo("rag-guide.pdf");
        assertThat(response.getSortOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("addPair - 达到上限50个抛EVAL_PAIR_LIMIT_EXCEEDED")
    void addPair_exceedLimit_throws() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 50);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(50L);

        assertThatThrownBy(() -> evalService.addPair(1L, 1L, "Q?", "doc.pdf"))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_PAIR_LIMIT_EXCEEDED.getCode()));
    }

    // ========== listPairs ==========

    @Test
    @DisplayName("listPairs - 返回评测集下问答对列表")
    void listPairs_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 2);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        EvalPair p1 = buildEvalPair(1L, 1L, "Q1", "doc1.pdf", 0);
        EvalPair p2 = buildEvalPair(2L, 1L, "Q2", "doc2.pdf", 1);
        when(evalPairRepository.findByEvalSetIdOrderBySortOrder(1L))
                .thenReturn(List.of(p1, p2));

        List<EvalPairResponse> result = evalService.listPairs(1L, 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getQuestion()).isEqualTo("Q1");
    }

    // ========== deletePair ==========

    @Test
    @DisplayName("deletePair - 删除问答对并更新pairCount")
    void deletePair_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 2);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(1L);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalSetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        evalService.deletePair(1L, 1L, 2L);

        verify(evalPairRepository).deleteById(2L);
        verify(evalSetRepository).save(argThat(s -> s.getPairCount() == 1));
    }

    // ========== importPairs ==========

    @Test
    @DisplayName("importPairs - 正常批量导入")
    void importPairs_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 0);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(0L);
        when(evalPairRepository.save(any())).thenAnswer(inv -> {
            EvalPair p = inv.getArgument(0);
            p.setId((long) p.getSortOrder() + 1);
            return p;
        });
        when(evalSetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ImportPairsRequest.PairItem> items = List.of(
                buildPairItem("Q1", "doc1.pdf"),
                buildPairItem("Q2", "doc2.pdf")
        );

        ImportResult result = evalService.importPairs(1L, 1L, items);

        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("importPairs - 导入后超过上限抛EVAL_PAIR_LIMIT_EXCEEDED")
    void importPairs_exceedLimit_throws() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 48);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(48L);

        List<ImportPairsRequest.PairItem> items = List.of(
                buildPairItem("Q1", "doc1.pdf"),
                buildPairItem("Q2", "doc2.pdf"),
                buildPairItem("Q3", "doc3.pdf") // 48+3=51 > 50
        );

        assertThatThrownBy(() -> evalService.importPairs(1L, 1L, items))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_PAIR_LIMIT_EXCEEDED.getCode()));
    }

    // ========== runEval ==========

    @Test
    @DisplayName("runEval - 空评测集抛EVAL_SET_EMPTY")
    void runEval_emptySet_throws() {
        EvalSet set = buildEvalSet(1L, 1L, "空集", 0);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> evalService.runEval(1L, 1L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_SET_EMPTY.getCode()));
    }

    @Test
    @DisplayName("runEval - 防重复执行锁抛EVAL_ALREADY_RUNNING")
    void runEval_alreadyRunning_throws() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 5);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(5L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any()))
                .thenReturn(false); // 已锁定

        assertThatThrownBy(() -> evalService.runEval(1L, 1L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_ALREADY_RUNNING.getCode()));
    }

    @Test
    @DisplayName("runEval - 正常启动评测返回RUNNING状态")
    void runEval_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 3);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        when(evalPairRepository.countByEvalSetId(1L)).thenReturn(3L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any()))
                .thenReturn(true); // 获取锁成功
        when(evalResultRepository.save(any())).thenAnswer(inv -> {
            EvalResult r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        EvalRunResponse response = evalService.runEval(1L, 1L);

        assertThat(response.getResultId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("RUNNING");
    }

    // ========== listResults ==========

    @Test
    @DisplayName("listResults - 返回评测结果列表")
    void listResults_happyPath() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 3);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        EvalResult r1 = buildEvalResult(1L, 1L, BigDecimal.valueOf(80.00), 3, 2, "COMPLETED");
        when(evalResultRepository.findByEvalSetIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(r1));

        List<EvalResultResponse> result = evalService.listResults(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHitRate()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
        assertThat(result.get(0).getHitCount()).isEqualTo(2);
    }

    // ========== getResult ==========

    @Test
    @DisplayName("getResult - 正常获取评测结果详情")
    void getResult_happyPath() throws Exception {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 3);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        EvalResult r = buildEvalResult(1L, 1L, BigDecimal.valueOf(66.67), 3, 2, "COMPLETED");
        r.setDetailJson("[{\"pairId\":1,\"question\":\"Q1\",\"expectedDocument\":\"doc1.pdf\",\"hit\":true,\"retrievedDocuments\":[\"doc1.pdf\"]}]");
        when(evalResultRepository.findById(1L)).thenReturn(Optional.of(r));

        EvalResultDetailResponse response = evalService.getResult(1L, 1L, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getHitRate()).isEqualByComparingTo(BigDecimal.valueOf(66.67));
        assertThat(response.getDetail()).hasSize(1);
    }

    @Test
    @DisplayName("getResult - 结果不属于指定评测集抛EVAL_SET_NOT_FOUND")
    void getResult_wrongSet_throws() {
        EvalSet set = buildEvalSet(1L, 1L, "测试集", 3);
        when(evalSetRepository.findById(1L)).thenReturn(Optional.of(set));
        EvalResult r = buildEvalResult(1L, 2L, BigDecimal.ZERO, 3, 0, "COMPLETED"); // 属于set 2
        when(evalResultRepository.findById(1L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> evalService.getResult(1L, 1L, 1L))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.EVAL_SET_NOT_FOUND.getCode()));
    }

    // ========== 辅助方法 ==========

    private EvalSet buildEvalSet(Long id, Long tenantId, String name, int pairCount) {
        EvalSet set = new EvalSet();
        set.setId(id);
        set.setTenantId(tenantId);
        set.setName(name);
        set.setDescription("");
        set.setPairCount(pairCount);
        set.setCreatedAt(OffsetDateTime.now());
        set.setUpdatedAt(OffsetDateTime.now());
        return set;
    }

    private EvalPair buildEvalPair(Long id, Long evalSetId, String question, String expectedDoc, int sortOrder) {
        EvalPair pair = new EvalPair();
        pair.setId(id);
        pair.setEvalSetId(evalSetId);
        pair.setTenantId(1L);
        pair.setQuestion(question);
        pair.setExpectedDocument(expectedDoc);
        pair.setSortOrder(sortOrder);
        pair.setCreatedAt(OffsetDateTime.now());
        return pair;
    }

    private EvalResult buildEvalResult(Long id, Long evalSetId, BigDecimal hitRate, int totalPairs, int hitCount, String status) {
        EvalResult result = new EvalResult();
        result.setId(id);
        result.setEvalSetId(evalSetId);
        result.setTenantId(1L);
        result.setHitRate(hitRate);
        result.setTotalPairs(totalPairs);
        result.setHitCount(hitCount);
        result.setStatus(status);
        result.setDurationMs(500);
        result.setCreatedAt(OffsetDateTime.now());
        return result;
    }

    private ImportPairsRequest.PairItem buildPairItem(String question, String expectedDoc) {
        ImportPairsRequest.PairItem item = new ImportPairsRequest.PairItem();
        item.setQuestion(question);
        item.setExpectedDocument(expectedDoc);
        return item;
    }
}
