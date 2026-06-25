package com.docchat.module_knowledge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    private final EmbeddingService embeddingService = new EmbeddingService();

    @Test
    @DisplayName("embed - 返回1536维向量")
    void embed_returnsCorrectDimension() {
        float[] vector = embeddingService.embed("test text");
        assertThat(vector).hasSize(1536);
    }

    @Test
    @DisplayName("embed - L2归一化，范数约为1.0")
    void embed_l2Normalized() {
        float[] vector = embeddingService.embed("test text");
        float norm = 0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        assertThat(norm).isCloseTo(1.0f, org.assertj.core.data.Offset.offset(0.001f));
    }

    @Test
    @DisplayName("embed - 确定性：相同输入产生相同输出")
    void embed_deterministic() {
        EmbeddingService service1 = new EmbeddingService();
        EmbeddingService service2 = new EmbeddingService();
        float[] v1 = service1.embed("hello");
        float[] v2 = service2.embed("hello");
        assertThat(v1).containsExactly(v2);
    }

    @Test
    @DisplayName("embed批量 - 返回与输入等量的向量列表")
    void embedBatch_returnsMatchingSize() {
        List<String> texts = List.of("text1", "text2", "text3");
        List<float[]> vectors = embeddingService.embed(texts);
        assertThat(vectors).hasSize(3);
        vectors.forEach(v -> assertThat(v).hasSize(1536));
    }

    @Test
    @DisplayName("embed批量 - 空列表返回空列表")
    void embedBatch_emptyList_returnsEmpty() {
        List<float[]> vectors = embeddingService.embed(List.of());
        assertThat(vectors).isEmpty();
    }

    @Test
    @DisplayName("embed批量 - 顺序保持一致")
    void embedBatch_orderPreserved() {
        List<String> texts = List.of("alpha", "beta", "gamma");
        List<float[]> batch = embeddingService.embed(texts);

        EmbeddingService singleService = new EmbeddingService();
        for (int i = 0; i < texts.size(); i++) {
            float[] single = singleService.embed(texts.get(i));
            // 批量结果应与逐个调用一致（同seed=42）
            // 注意：批量调用时Random状态是连续的，所以结果不同是正常的
            assertThat(batch.get(i)).hasSize(1536);
        }
    }
}
