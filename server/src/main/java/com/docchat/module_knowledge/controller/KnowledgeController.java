package com.docchat.module_knowledge.controller;

import com.docchat.common.response.PageResult;
import com.docchat.common.response.R;
import com.docchat.module_knowledge.dto.*;
import com.docchat.module_knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public R<KnowledgeResponse> getKnowledge() {
        return R.ok(knowledgeService.getKnowledge());
    }

    @PutMapping
    public R<KnowledgeResponse> updateKnowledge(
            @Valid @RequestBody UpdateKnowledgeRequest request) {
        return R.ok(knowledgeService.updateKnowledge(request));
    }

    @GetMapping("/documents")
    public R<PageResult<DocumentDetailResponse>> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return R.ok(knowledgeService.listDocuments(page, size, keyword, status));
    }

    @PostMapping("/documents")
    public R<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "strategy", required = false)
                    String strategy,
            @RequestParam(value = "chunkSize", required = false)
                    Integer chunkSize,
            @RequestParam(value = "overlap", required = false)
                    Integer overlap) {
        return R.ok(knowledgeService.uploadDocument(
                file, strategy, chunkSize, overlap));
    }

    @GetMapping("/documents/{documentId}")
    public R<DocumentDetailResponse> getDocument(
            @PathVariable Long documentId) {
        return R.ok(knowledgeService.getDocument(documentId));
    }

    @DeleteMapping("/documents/{documentId}")
    public R<Void> deleteDocument(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean confirm) {
        knowledgeService.deleteDocument(documentId, confirm);
        return R.ok();
    }

    @GetMapping("/documents/{documentId}/versions")
    public R<List<DocumentVersionResponse>> listVersions(
            @PathVariable Long documentId) {
        return R.ok(knowledgeService.listVersions(documentId));
    }

    @PostMapping("/documents/{documentId}/versions/{versionId}/rollback")
    public R<DocumentVersionResponse> rollbackVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        return R.ok(knowledgeService.rollbackVersion(documentId, versionId));
    }
}
