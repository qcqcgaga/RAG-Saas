package com.docchat.module_knowledge.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切分器
 *
 * 支持三种切分策略：固定大小、按句子、按段落。
 * MVP 阶段仅实现固定大小切分（含重叠），后续可扩展语义切分。
 */
@Component
public class DocumentChunker {

    /** 固定大小切分（含重叠） */
    public List<String> chunkFixed(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) return List.of();
        if (chunkSize <= 0) chunkSize = 500;
        if (overlap < 0) overlap = 0;
        if (overlap >= chunkSize) overlap = chunkSize / 4;

        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int pos = 0;

        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());
            String chunk = text.substring(pos, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            pos += step;
            if (pos >= text.length()) break;
            if (end == text.length()) break;
        }

        mergeShortLastChunk(chunks, chunkSize);
        return chunks;
    }

    /** 按句子切分 */
    public List<String> chunkBySentence(String text) {
        if (text == null || text.isEmpty()) return List.of();
        String[] sentences = text.split("(?<=[。！？.!?])\\s*");
        List<String> result = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /** 按段落切分 */
    public List<String> chunkByParagraph(String text) {
        if (text == null || text.isEmpty()) return List.of();
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> result = new ArrayList<>();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private void mergeShortLastChunk(List<String> chunks, int chunkSize) {
        if (chunks.size() > 1) {
            String last = chunks.get(chunks.size() - 1);
            if (last.length() < chunkSize / 2) {
                String prev = chunks.get(chunks.size() - 2);
                String merged = prev + " " + last;
                chunks.set(chunks.size() - 2, merged.trim());
                chunks.remove(chunks.size() - 1);
            }
        }
    }
}
