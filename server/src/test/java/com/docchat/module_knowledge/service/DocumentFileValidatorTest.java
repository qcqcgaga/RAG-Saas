package com.docchat.module_knowledge.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentFileValidatorTest {

    private DocumentFileValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentFileValidator();
    }

    private MultipartFile mockFile(String filename, long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getSize()).thenReturn(size);
        return file;
    }

    private MultipartFile mockFileWithHeader(String filename, byte[] header) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(header));
        return file;
    }

    // ===== validateFileType =====

    @Test
    @DisplayName("validateFileType - PDF文件通过")
    void validateFileType_pdf_passes() {
        MultipartFile file = mockFile("test.pdf", 1024);
        validator.validateFileType(file); // 不抛异常即通过
    }

    @Test
    @DisplayName("validateFileType - MD文件通过")
    void validateFileType_md_passes() {
        MultipartFile file = mockFile("readme.md", 1024);
        validator.validateFileType(file);
    }

    @Test
    @DisplayName("validateFileType - TXT文件通过")
    void validateFileType_txt_passes() {
        MultipartFile file = mockFile("notes.txt", 1024);
        validator.validateFileType(file);
    }

    @Test
    @DisplayName("validateFileType - 不支持的文件类型抛异常")
    void validateFileType_unsupportedType_throws() {
        MultipartFile file = mockFile("malware.exe", 1024);
        assertThatThrownBy(() -> validator.validateFileType(file))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.KNOWLEDGE_FILE_TYPE_NOT_ALLOWED.getCode()));
    }

    @Test
    @DisplayName("validateFileType - null文件名抛异常")
    void validateFileType_nullFilename_throws() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        assertThatThrownBy(() -> validator.validateFileType(file))
            .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("validateFileType - 无扩展名抛异常")
    void validateFileType_noExtension_throws() {
        MultipartFile file = mockFile("noextension", 1024);
        assertThatThrownBy(() -> validator.validateFileType(file))
            .isInstanceOf(BizException.class);
    }

    // ===== validateFileSize =====

    @Test
    @DisplayName("validateFileSize - 文件大小在限制内通过")
    void validateFileSize_withinLimit_passes() {
        MultipartFile file = mockFile("test.pdf", 1024);
        validator.validateFileSize(file);
    }

    @Test
    @DisplayName("validateFileSize - 超过大小限制抛异常")
    void validateFileSize_exceedsLimit_throws() {
        MultipartFile file = mockFile("test.pdf", 100L * 1024 * 1024 + 1); // 50MB + 1
        assertThatThrownBy(() -> validator.validateFileSize(file))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.KNOWLEDGE_FILE_TOO_LARGE.getCode()));
    }

    // ===== validateFileHeader =====

    @Test
    @DisplayName("validateFileHeader - 有效PDF文件头通过")
    void validateFileHeader_validPdf_passes() throws IOException {
        byte[] pdfHeader = {0x25, 0x50, 0x44, 0x46}; // %PDF
        MultipartFile file = mockFileWithHeader("test.pdf", pdfHeader);
        validator.validateFileHeader(file);
    }

    @Test
    @DisplayName("validateFileHeader - PDF文件头不匹配抛异常")
    void validateFileHeader_invalidPdf_throws() throws IOException {
        byte[] fakeHeader = {0x49, 0x44, 0x33, 0x04}; // ID3 (MP3 header)
        MultipartFile file = mockFileWithHeader("test.pdf", fakeHeader);
        assertThatThrownBy(() -> validator.validateFileHeader(file))
            .isInstanceOf(BizException.class)
            .satisfies(ex -> assertThat(((BizException) ex).getCode())
                .isEqualTo(ErrorCode.KNOWLEDGE_FILE_HEADER_MISMATCH.getCode()));
    }

    @Test
    @DisplayName("validateFileHeader - MD文件跳过文件头校验")
    void validateFileHeader_mdFile_skips() throws IOException {
        MultipartFile file = mockFileWithHeader("readme.md", new byte[0]);
        validator.validateFileHeader(file);
    }

    @Test
    @DisplayName("validateFileHeader - TXT文件跳过文件头校验")
    void validateFileHeader_txtFile_skips() throws IOException {
        MultipartFile file = mockFileWithHeader("notes.txt", new byte[0]);
        validator.validateFileHeader(file);
    }

    @Test
    @DisplayName("validateFileHeader - null文件名跳过校验")
    void validateFileHeader_nullFilename_skips() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        validator.validateFileHeader(file);
    }

    // ===== generateStoredName =====

    @Test
    @DisplayName("generateStoredName - 正常文件名生成UUID+扩展名")
    void generateStoredName_withExtension() {
        String result = validator.generateStoredName("document.pdf");
        assertThat(result).endsWith(".pdf");
        assertThat(result).hasSize(36 + 4); // UUID(36) + .pdf(4)
    }

    @Test
    @DisplayName("generateStoredName - 无扩展名只生成UUID")
    void generateStoredName_noExtension() {
        String result = validator.generateStoredName("noextension");
        assertThat(result).doesNotContain(".");
        assertThat(result).hasSize(36); // pure UUID
    }

    @Test
    @DisplayName("generateStoredName - 每次调用生成不同UUID")
    void generateStoredName_uniquePerCall() {
        String name1 = validator.generateStoredName("test.pdf");
        String name2 = validator.generateStoredName("test.pdf");
        assertThat(name1).isNotEqualTo(name2);
    }
}
