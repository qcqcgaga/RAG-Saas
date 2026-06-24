package com.docchat.module_knowledge.service;

import com.docchat.common.constant.CommonConstant;
import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Component
public class DocumentFileValidator {

    /** 文件头魔数映射 */
    private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
        "pdf", new byte[]{0x25, 0x50, 0x44, 0x46}  // %PDF
    );

    /** 校验文件类型 */
    public void validateFileType(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new BizException(ErrorCode.KNOWLEDGE_FILE_TYPE_NOT_ALLOWED);
        }
        String ext = originalName.substring(
                originalName.lastIndexOf(".") + 1).toLowerCase();
        boolean allowed = false;
        for (String allowedType : CommonConstant.ALLOWED_FILE_TYPES) {
            if (allowedType.equals(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new BizException(ErrorCode.KNOWLEDGE_FILE_TYPE_NOT_ALLOWED);
        }
    }

    /** 校验文件大小 */
    public void validateFileSize(MultipartFile file) {
        if (file.getSize() > CommonConstant.MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.KNOWLEDGE_FILE_TOO_LARGE);
        }
    }

    /** 校验文件头（仅对 PDF 做校验，MD/TXT 无固定文件头） */
    public void validateFileHeader(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) return;

        String ext = originalName.substring(
                originalName.lastIndexOf(".") + 1).toLowerCase();
        byte[] expectedSignature = FILE_SIGNATURES.get(ext);
        if (expectedSignature == null) return;

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[expectedSignature.length];
            int read = is.read(header);
            if (read < expectedSignature.length) {
                throw new BizException(ErrorCode.KNOWLEDGE_FILE_HEADER_MISMATCH);
            }
            for (int i = 0; i < expectedSignature.length; i++) {
                if (header[i] != expectedSignature[i]) {
                    throw new BizException(
                            ErrorCode.KNOWLEDGE_FILE_HEADER_MISMATCH);
                }
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.KNOWLEDGE_FILE_HEADER_MISMATCH);
        }
    }

    /** 生成存储文件名（UUID） */
    public String generateStoredName(String originalName) {
        String ext = originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf("."))
            : "";
        return UUID.randomUUID().toString() + ext;
    }
}
