package com.docchat.module_knowledge.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.response.PageResult;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_knowledge.dto.*;
import com.docchat.module_knowledge.entity.Document;
import com.docchat.module_knowledge.entity.DocumentVersion;
import com.docchat.module_knowledge.entity.KnowledgeBase;
import com.docchat.module_knowledge.repository.DocumentRepository;
import com.docchat.module_knowledge.repository.DocumentVersionRepository;
import com.docchat.module_knowledge.repository.KnowledgeBaseRepository;
import com.docchat.module_knowledge.repository.MilvusRepository;
import com.docchat.module_task.entity.AsyncTask;
import com.docchat.module_task.service.TaskService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentFileValidator fileValidator;
    private final TaskService taskService;
    private final EntityManager entityManager;
    private final MilvusRepository milvusRepository;

    @Value("${docchat.storage.path:./uploads}")
    private String storagePath;

    @Override
    public KnowledgeResponse getKnowledge() {
        KnowledgeBase kb = getOrCreateKnowledgeBase();
        return toKnowledgeResponse(kb);
    }

    @Override
    @Transactional
    public KnowledgeResponse updateKnowledge(UpdateKnowledgeRequest request) {
        KnowledgeBase kb = getOrCreateKnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        knowledgeBaseRepository.save(kb);
        return toKnowledgeResponse(kb);
    }

    @Override
    public PageResult<DocumentDetailResponse> listDocuments(
            int page, int size, String keyword, String status) {
        KnowledgeBase kb = getOrCreateKnowledgeBase();
        Page<Document> docPage = findDocuments(
                kb.getId(), page, size, keyword, status);
        List<DocumentDetailResponse> list = docPage.getContent().stream()
                .map(this::toDocumentDetailResponse)
                .toList();
        return PageResult.of(list, docPage.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public DocumentUploadResponse uploadDocument(
            MultipartFile file, String strategy,
            Integer chunkSize, Integer overlap) {
        fileValidator.validateFileType(file);
        fileValidator.validateFileSize(file);
        fileValidator.validateFileHeader(file);

        KnowledgeBase kb = getOrCreateKnowledgeBase();
        String storedName = fileValidator.generateStoredName(
                file.getOriginalFilename());
        String relativePath = saveFileToDisk(file, kb.getTenantId(), storedName);

        Document doc = createDocumentRecord(
                kb, file, storedName, relativePath);
        createDocumentVersion(doc, strategy, chunkSize, overlap);

        AsyncTask task = taskService.createTask(
                "CHUNK_AND_EMBED", doc.getId());

        log.info("文档上传成功: docId={}, taskId={}", doc.getId(), task.getId());
        return DocumentUploadResponse.builder()
                .documentId(doc.getId())
                .taskId(task.getId())
                .status(doc.getStatus())
                .build();
    }

    @Override
    public DocumentDetailResponse getDocument(Long documentId) {
        Document doc = findDocumentAndCheckOwnership(documentId);
        return toDocumentDetailResponse(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId, boolean confirm) {
        if (!confirm) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "删除文档需要二次确认，请传入 confirm=true");
        }
        Document doc = findDocumentAndCheckOwnership(documentId);

        // 1. 先删除关联的异步任务（避免外键约束冲突）
        taskService.deleteTasksByDocumentId(documentId);

        // 2. 删除文档版本
        documentVersionRepository.findByDocumentIdOrderByVersionDesc(
                documentId).forEach(dv ->
                documentVersionRepository.delete(dv));

        // 3. 删除文档记录
        documentRepository.delete(doc);

        // 4. 立即 flush，确保文档记录和关联数据已从数据库删除
        entityManager.flush();

        // 5. 同步删除 Milvus 向量（文档记录已删除，不再创建异步任务避免外键冲突）
        deleteVectors(doc.getTenantId(), documentId);

        // 6. 删除磁盘文件
        deleteFileFromDisk(doc.getStoredPath());

        log.info("文档已删除: docId={}", documentId);
    }

    @Override
    public List<DocumentVersionResponse> listVersions(Long documentId) {
        findDocumentAndCheckOwnership(documentId);
        return documentVersionRepository
                .findByDocumentIdOrderByVersionDesc(documentId).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentVersionResponse rollbackVersion(
            Long documentId, Long versionId) {
        Document doc = findDocumentAndCheckOwnership(documentId);

        DocumentVersion targetVersion = documentVersionRepository
                .findById(versionId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND,
                        "版本不存在"));

        // 验证版本归属：版本必须属于该文档
        if (!targetVersion.getDocumentId().equals(documentId)) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "版本不属于该文档");
        }

        // 验证租户隔离：版本必须属于当前租户
        Long currentTenantId = SecurityUtil.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        if (!targetVersion.getTenantId().equals(currentTenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 计算新版本号（当前最大版本 + 1）
        Integer maxVersion = documentVersionRepository
                .findMaxVersionByDocumentId(documentId);
        int newVersionNum = (maxVersion != null ? maxVersion : 0) + 1;

        // 使用目标版本的切分策略参数创建新版本记录
        DocumentVersion newVersion = DocumentVersion.builder()
                .documentId(documentId)
                .tenantId(currentTenantId)
                .version(newVersionNum)
                .chunkingStrategy(targetVersion.getChunkingStrategy())
                .chunkSize(targetVersion.getChunkSize())
                .chunkOverlap(targetVersion.getChunkOverlap())
                .build();
        newVersion = documentVersionRepository.save(newVersion);

        // 更新文档状态和版本号
        doc.setVersion(newVersionNum);
        doc.setStatus("PENDING");
        doc.setChunkCount(0);
        documentRepository.save(doc);

        // 创建 CHUNK_AND_EMBED 异步任务
        taskService.createTask("CHUNK_AND_EMBED", documentId);

        log.info("版本回滚: docId={}, targetVersion={}, newVersion={}",
                documentId, targetVersion.getVersion(), newVersionNum);
        return toVersionResponse(newVersion);
    }

    // ========== 私有方法 ==========

    private KnowledgeBase getOrCreateKnowledgeBase() {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        return knowledgeBaseRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultKnowledgeBase(tenantId));
    }

    private KnowledgeBase createDefaultKnowledgeBase(Long tenantId) {
        KnowledgeBase kb = KnowledgeBase.builder()
                .tenantId(tenantId)
                .name("默认知识库")
                .description("")
                .build();
        return knowledgeBaseRepository.save(kb);
    }

    private Page<Document> findDocuments(
            Long knowledgeId, int page, int size,
            String keyword, String status) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        if (keyword != null && !keyword.isBlank()) {
            return documentRepository
                    .findByKnowledgeIdAndOriginalNameContaining(
                            knowledgeId, keyword, pageable);
        }
        if (status != null && !status.isBlank()) {
            return documentRepository
                    .findByKnowledgeIdAndStatus(knowledgeId, status, pageable);
        }
        return documentRepository.findByKnowledgeId(knowledgeId, pageable);
    }

    private String saveFileToDisk(
            MultipartFile file, Long tenantId, String storedName) {
        Path tenantDir = Paths.get(storagePath, String.valueOf(tenantId));
        try {
            Files.createDirectories(tenantDir);
            Path targetPath = tenantDir.resolve(storedName);
            file.transferTo(targetPath.toFile());
            return tenantId + "/" + storedName;
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR,
                    "文件保存失败: " + e.getMessage());
        }
    }

    private Document createDocumentRecord(
            KnowledgeBase kb, MultipartFile file,
            String storedName, String relativePath) {
        String ext = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                        .substring(file.getOriginalFilename()
                                .lastIndexOf(".") + 1).toLowerCase()
                : "";
        Document doc = Document.builder()
                .knowledgeId(kb.getId())
                .tenantId(kb.getTenantId())
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .fileType(ext)
                .fileSize(file.getSize())
                .storedPath(relativePath)
                .build();
        doc = documentRepository.save(doc);

        kb.setDocumentCount(
                (int) documentRepository.countByKnowledgeId(kb.getId()));
        knowledgeBaseRepository.save(kb);

        return doc;
    }

    private void createDocumentVersion(
            Document doc, String strategy,
            Integer chunkSize, Integer overlap) {
        DocumentVersion version = DocumentVersion.builder()
                .documentId(doc.getId())
                .tenantId(doc.getTenantId())
                .version(doc.getVersion())
                .chunkingStrategy(strategy != null ? strategy : "FIXED_SIZE")
                .chunkSize(chunkSize != null ? chunkSize : 500)
                .chunkOverlap(overlap != null ? overlap : 50)
                .build();
        documentVersionRepository.save(version);
    }

    private Document findDocumentAndCheckOwnership(Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new BizException(
                                ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND));
        Long currentTenantId = SecurityUtil.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        if (!doc.getTenantId().equals(currentTenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return doc;
    }

    private void deleteFileFromDisk(String storedPath) {
        try {
            Path filePath = Paths.get(storagePath, storedPath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("磁盘文件删除失败: path={}, error={}", storedPath,
                    e.getMessage());
        }
    }

    private void deleteVectors(Long tenantId, Long documentId) {
        try {
            String collectionName = milvusRepository.getCollectionName(tenantId);
            milvusRepository.deleteByDocumentId(collectionName, documentId);
            log.info("已删除文档 {} 的向量数据", documentId);
        } catch (Exception e) {
            log.warn("向量删除失败（不影响文档删除）: docId={}, error={}",
                    documentId, e.getMessage());
        }
    }

    private KnowledgeResponse toKnowledgeResponse(KnowledgeBase kb) {
        return KnowledgeResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .documentCount(kb.getDocumentCount())
                .chunkCount(kb.getChunkCount())
                .createdAt(kb.getCreatedAt())
                .build();
    }

    private DocumentDetailResponse toDocumentDetailResponse(Document doc) {
        return DocumentDetailResponse.builder()
                .id(doc.getId())
                .originalName(doc.getOriginalName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .chunkCount(doc.getChunkCount())
                .version(doc.getVersion())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion dv) {
        return DocumentVersionResponse.builder()
                .id(dv.getId())
                .version(dv.getVersion())
                .chunkingStrategy(dv.getChunkingStrategy())
                .chunkSize(dv.getChunkSize())
                .chunkOverlap(dv.getChunkOverlap())
                .chunkCount(dv.getChunkCount())
                .status(dv.getStatus())
                .createdAt(dv.getCreatedAt())
                .build();
    }
}
