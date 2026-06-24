package com.docchat.module_knowledge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Embedding 服务
 *
 * MVP 阶段使用随机向量作为占位实现。
 * 生产环境替换为讯飞 Embedding API 或其他 Embedding 服务。
 */
@Slf4j
@Service
public class EmbeddingService {

    private static final int EMBEDDING_DIMENSION = 1536;
    private final Random random = new Random(42);

    /** 对单个文本生成向量 */
    public float[] embed(String text) {
        log.debug("Embedding text (length={})", text.length());
        // TODO: 替换为真实 Embedding API
        return generateRandomVector();
    }

    /** 批量生成向量 */
    public List<float[]> embed(List<String> texts) {
        log.info("Batch embedding {} texts", texts.size());
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(embed(text));
        }
        return vectors;
    }

    private float[] generateRandomVector() {
        float[] vector = new float[EMBEDDING_DIMENSION];
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            vector[i] = (random.nextFloat() - 0.5f) * 0.1f;
        }
        // 归一化
        float norm = 0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
