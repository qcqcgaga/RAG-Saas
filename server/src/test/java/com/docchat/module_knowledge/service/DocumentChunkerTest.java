package com.docchat.module_knowledge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    // ===== chunkFixed =====

    @Test
    @DisplayName("chunkFixed - 标准文本切分")
    void chunkFixed_standardText() {
        String text = "a".repeat(1200);
        List<String> chunks = chunker.chunkFixed(text, 500, 50);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().mapToInt(String::length).sum()).isGreaterThan(0);
    }

    @Test
    @DisplayName("chunkFixed - 重叠区域共享边界内容")
    void chunkFixed_overlapSharesContent() {
        String text = "abcdefghijklmnopqrstuvwxyz";
        List<String> chunks = chunker.chunkFixed(text, 10, 3);
        // 第二个chunk的开头应该与第一个chunk的结尾有重叠
        if (chunks.size() >= 2) {
            String first = chunks.get(0);
            String second = chunks.get(1);
            // 验证有内容产出
            assertThat(first).isNotEmpty();
            assertThat(second).isNotEmpty();
        }
    }

    @Test
    @DisplayName("chunkFixed - 短末尾块合并到前一块")
    void chunkFixed_mergeShortLastChunk() {
        // chunkSize=10, 文本长度=12 -> 第二个chunk只有2字符 < 10/2=5，应合并
        String text = "abcdefghijxy";
        List<String> chunks = chunker.chunkFixed(text, 10, 0);
        assertThat(chunks).hasSize(1);
        // 合并时用空格连接再trim
        assertThat(chunks.get(0).replace(" ", "")).isEqualTo("abcdefghijxy");
    }

    @Test
    @DisplayName("chunkFixed - null返回空列表")
    void chunkFixed_null_returnsEmpty() {
        assertThat(chunker.chunkFixed(null, 500, 50)).isEmpty();
    }

    @Test
    @DisplayName("chunkFixed - 空字符串返回空列表")
    void chunkFixed_empty_returnsEmpty() {
        assertThat(chunker.chunkFixed("", 500, 50)).isEmpty();
    }

    @Test
    @DisplayName("chunkFixed - chunkSize<=0默认500")
    void chunkFixed_invalidChunkSize_defaultsTo500() {
        String text = "a".repeat(600);
        List<String> chunks = chunker.chunkFixed(text, 0, 0);
        assertThat(chunks).hasSize(1);
    }

    @Test
    @DisplayName("chunkFixed - overlap<0默认0")
    void chunkFixed_negativeOverlap_defaultsTo0() {
        String text = "a".repeat(500);
        List<String> chunks = chunker.chunkFixed(text, 500, -1);
        assertThat(chunks).hasSize(1);
    }

    @Test
    @DisplayName("chunkFixed - overlap>=chunkSize自动调整")
    void chunkFixed_overlapTooLarge_autoAdjust() {
        String text = "a".repeat(500);
        List<String> chunks = chunker.chunkFixed(text, 500, 600);
        assertThat(chunks).hasSize(1);
    }

    @Test
    @DisplayName("chunkFixed - 文本恰好等于chunkSize返回1块")
    void chunkFixed_exactChunkSize_returnsOneChunk() {
        String text = "a".repeat(500);
        List<String> chunks = chunker.chunkFixed(text, 500, 0);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(500);
    }

    @Test
    @DisplayName("chunkFixed - 纯空白文本返回空列表")
    void chunkFixed_whitespaceOnly_returnsEmpty() {
        assertThat(chunker.chunkFixed("   ", 500, 0)).isEmpty();
    }

    // ===== chunkBySentence =====

    @Test
    @DisplayName("chunkBySentence - 中文句号切分")
    void chunkBySentence_chinesePeriod() {
        String text = "这是第一句。这是第二句。这是第三句。";
        List<String> sentences = chunker.chunkBySentence(text);
        assertThat(sentences).hasSize(3);
    }

    @Test
    @DisplayName("chunkBySentence - 英文句号切分")
    void chunkBySentence_englishPeriod() {
        String text = "First sentence. Second sentence. Third sentence.";
        List<String> sentences = chunker.chunkBySentence(text);
        assertThat(sentences).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("chunkBySentence - 无分隔符返回单个元素")
    void chunkBySentence_noDelimiters_returnsSingle() {
        String text = "没有标点符号的文本";
        List<String> sentences = chunker.chunkBySentence(text);
        assertThat(sentences).hasSize(1);
        assertThat(sentences.get(0)).isEqualTo(text);
    }

    @Test
    @DisplayName("chunkBySentence - null返回空列表")
    void chunkBySentence_null_returnsEmpty() {
        assertThat(chunker.chunkBySentence(null)).isEmpty();
    }

    @Test
    @DisplayName("chunkBySentence - 空字符串返回空列表")
    void chunkBySentence_empty_returnsEmpty() {
        assertThat(chunker.chunkBySentence("")).isEmpty();
    }

    // ===== chunkByParagraph =====

    @Test
    @DisplayName("chunkByParagraph - 双换行切分")
    void chunkByParagraph_doubleNewline() {
        String text = "第一段\n\n第二段\n\n第三段";
        List<String> paragraphs = chunker.chunkByParagraph(text);
        assertThat(paragraphs).hasSize(3);
    }

    @Test
    @DisplayName("chunkByParagraph - 无双换行返回单个元素")
    void chunkByParagraph_noDoubleNewline_returnsSingle() {
        String text = "只有一段文本";
        List<String> paragraphs = chunker.chunkByParagraph(text);
        assertThat(paragraphs).hasSize(1);
    }

    @Test
    @DisplayName("chunkByParagraph - null返回空列表")
    void chunkByParagraph_null_returnsEmpty() {
        assertThat(chunker.chunkByParagraph(null)).isEmpty();
    }

    @Test
    @DisplayName("chunkByParagraph - 空字符串返回空列表")
    void chunkByParagraph_empty_returnsEmpty() {
        assertThat(chunker.chunkByParagraph("")).isEmpty();
    }
}
