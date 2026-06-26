package com.docchat.module_task.worker;

import com.docchat.module_knowledge.entity.Document;
import com.docchat.module_knowledge.repository.DocumentRepository;
import com.docchat.module_knowledge.repository.MilvusRepository;
import com.docchat.module_knowledge.service.DocumentChunker;
import com.docchat.module_knowledge.service.DocumentParser;
import com.docchat.module_knowledge.service.EmbeddingService;
import com.docchat.module_task.entity.AsyncTask;
import com.docchat.module_task.service.TaskQueueService;
import com.docchat.module_task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务执行器
 *
 * 使用 @Scheduled 轮询 Redis 队列，拉取并执行任务。
 * MVP 阶段单实例运行，未来可扩展为多实例 + 分布式锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskWorker {

    private final TaskQueueService taskQueueService;
    private final TaskService taskService;
    private final DocumentRepository documentRepository;
    private final DocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    private final MilvusRepository milvusRepository;

    @Value("${docchat.storage.path:./uploads}")
    private String storagePath;

    private static final String[] SUPPORTED_TYPES = {"CHUNK_AND_EMBED", "DELETE_VECTORS"};
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    /**
     * 每 5 秒轮询一次任务队列
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void pollTasks() {
        for (String taskType : SUPPORTED_TYPES) {
            try {
                AsyncTask task = taskQueueService.popTask(taskType);
                if (task != null) {
                    processTask(task);
                }
            } catch (Exception e) {
                log.error("任务轮询异常: type={}", taskType, e);
            }
        }
    }

    private void processTask(AsyncTask task) {
        if (!taskQueueService.acquireLock(task.getId())) {
            log.warn("任务已被其他实例处理: taskId={}", task.getId());
            return;
        }

        try {
            taskService.updateTaskStatus(task.getId(), "PROCESSING", null);
            onTask(task);
            taskService.updateTaskStatus(task.getId(), "COMPLETED", null);
            log.info("任务执行完成: taskId={}", task.getId());
        } catch (Exception e) {
            taskService.updateTaskStatus(task.getId(), "FAILED", e.getMessage());
            log.error("任务执行失败: taskId={}", task.getId(), e);
        } finally {
            taskQueueService.releaseLock(task.getId());
        }
    }

    /**
     * 根据任务类型分发执行逻辑
     */
    private void onTask(AsyncTask task) {
        log.info("处理任务: taskId={}, type={}, documentId={}",
            task.getId(), task.getTaskType(), task.getDocumentId());

        switch (task.getTaskType()) {
            case "CHUNK_AND_EMBED" -> handleChunkAndEmbed(task);
            case "DELETE_VECTORS" -> handleDeleteVectors(task);
            default -> log.warn("未知任务类型: {}", task.getTaskType());
        }
    }

    private void handleChunkAndEmbed(AsyncTask task) {
        Long documentId = task.getDocumentId();
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));

        taskService.updateTaskProgress(task.getId(), 10);

        // 1. 解析文件内容
        String text = parseDocument(doc);
        taskService.updateTaskProgress(task.getId(), 30);

        // 2. 切分文档
        List<String> chunks = documentChunker.chunkFixed(
                text, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
        taskService.updateTaskProgress(task.getId(), 40);

        // 3. 生成向量
        List<float[]> vectors = embeddingService.embed(chunks);
        taskService.updateTaskProgress(task.getId(), 70);

        // 4. 确保租户 collection 存在
        String collectionName = milvusRepository.getCollectionName(doc.getTenantId());
        milvusRepository.createCollection(collectionName);
        taskService.updateTaskProgress(task.getId(), 80);

        // 5. 构建并插入向量数据
        List<com.google.gson.JsonObject> data = buildInsertData(
                doc, chunks, vectors);
        milvusRepository.insert(collectionName, data);
        taskService.updateTaskProgress(task.getId(), 90);

        // 6. 更新文档状态
        doc.setChunkCount(chunks.size());
        doc.setStatus("COMPLETED");
        documentRepository.save(doc);
        taskService.updateTaskProgress(task.getId(), 100);
    }

    private String parseDocument(Document doc) {
        Path filePath = Path.of(storagePath, doc.getStoredPath());
        try {
            return documentParser.parse(filePath, doc.getFileType());
        } catch (IOException e) {
            throw new RuntimeException("文件解析失败: " + doc.getStoredPath(), e);
        }
    }

    private List<com.google.gson.JsonObject> buildInsertData(
            Document doc, List<String> chunks, List<float[]> vectors) {
        List<com.google.gson.JsonObject> data = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = doc.getId() + "_chunk_" + i;
            com.google.gson.JsonObject row = milvusRepository.buildRow(
                    chunkId, doc.getId(), doc.getOriginalName(),
                    i, chunks.get(i), vectors.get(i));
            data.add(row);
        }
        return data;
    }

    private void handleDeleteVectors(AsyncTask task) {
        Long documentId = task.getDocumentId();
        // 文档记录可能已在 deleteDocument 事务中被删除，直接使用 tenantId
        Long tenantId = task.getTenantId();
        String collectionName = milvusRepository.getCollectionName(tenantId);
        milvusRepository.deleteByDocumentId(collectionName, documentId);
        log.info("已删除文档 {} 的向量数据", documentId);
    }
}
