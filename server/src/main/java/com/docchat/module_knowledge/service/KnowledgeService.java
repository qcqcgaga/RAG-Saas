package com.docchat.module_knowledge.service;

import com.docchat.common.response.PageResult;
import com.docchat.module_knowledge.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService {

    KnowledgeResponse getKnowledge();

    KnowledgeResponse updateKnowledge(UpdateKnowledgeRequest request);

    PageResult<DocumentDetailResponse> listDocuments(
            int page, int size, String keyword, String status);

    DocumentUploadResponse uploadDocument(
            MultipartFile file, String strategy,
            Integer chunkSize, Integer overlap);

    DocumentDetailResponse getDocument(Long documentId);

    void deleteDocument(Long documentId, boolean confirm);

    java.util.List<DocumentVersionResponse> listVersions(Long documentId);

    DocumentVersionResponse rollbackVersion(
            Long documentId, Long versionId);
}
