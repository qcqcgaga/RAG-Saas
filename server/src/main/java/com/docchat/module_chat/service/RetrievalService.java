package com.docchat.module_chat.service;

import com.docchat.module_chat.dto.SourceReference;
import com.docchat.module_knowledge.repository.MilvusRepository;
import com.docchat.module_knowledge.service.EmbeddingService;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量检索服务
 *
 * 负责将用户问题转为向量，检索 Milvus 中最相关的文档片段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final MilvusRepository milvusRepository;
    private final EmbeddingService embeddingService;

    private static final int DEFAULT_TOP_K = 5;
    private static final float MIN_SCORE = 0.5f;

    /** 向量检索相关文档片段 */
    public List<SourceReference> retrieve(String question, Long tenantId, int topK) {
        if (topK <= 0) {
            topK = DEFAULT_TOP_K;
        }

        String collectionName = milvusRepository.getCollectionName(tenantId);
        float[] queryVector = embeddingService.embed(question);

        List<SearchResp.SearchResult> results = milvusRepository.search(
                collectionName, queryVector, topK
        );

        return results.stream()
                .filter(r -> r.getScore() != null && r.getScore() >= MIN_SCORE)
                .map(this::toSourceReference)
                .toList();
    }

    private SourceReference toSourceReference(SearchResp.SearchResult result) {
        var entity = result.getEntity();
        return SourceReference.builder()
                .documentName((String) entity.get("document_name"))
                .chunkIndex(entity.get("chunk_index") instanceof Number n
                        ? n.intValue() : 0)
                .content((String) entity.get("content"))
                .score(result.getScore())
                .build();
    }
}
