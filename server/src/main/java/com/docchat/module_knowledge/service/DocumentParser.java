package com.docchat.module_knowledge.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档解析器
 *
 * 支持解析 PDF、Markdown、TXT 为纯文本。
 * 后续可扩展支持 DOCX、HTML 等格式。
 */
@Component
public class DocumentParser {

    /** 解析文件内容为纯文本 */
    public String parse(Path filePath, String fileType) throws IOException {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> parsePdf(filePath);
            case "md", "txt" -> parseText(filePath);
            default -> throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        };
    }

    private String parsePdf(Path filePath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String parseText(Path filePath) throws IOException {
        return Files.readString(filePath);
    }
}
