package com.docchat.module_chat.service;

import com.docchat.module_chat.dto.ChatEvent;
import com.docchat.module_chat.dto.ChatRequest;
import com.docchat.module_chat.dto.SourceReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * 对话服务实现
 *
 * 编排 RAG 对话流程：向量检索 -> 构造 Prompt -> 流式 LLM 生成。
 * 通过 SseEmitter 推送 token/done/error 事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final RetrievalService retrievalService;
    private final LlmService llmService;

    private static final int TOP_K = 5;
    private static final long SSE_TIMEOUT_MS = 120_000L;
    private static final int MAX_PROMPT_LENGTH = 4000;

    private static final String SYSTEM_PROMPT = """
            你是 DocChat 智能客服，基于以下文档内容回答用户问题。
            请遵守以下规则：
            1. 只基于提供的文档内容回答，不要编造信息
            2. 如果文档中没有相关信息，请诚实回答"抱歉，文档中没有相关信息"
            3. 回答时引用来源文档名
            4. 使用中文回答
            """;

    @Override
    public SseEmitter converse(ChatRequest request, Long tenantId) {
        log.info("RAG 对话: tenantId={}, question={}", tenantId, request.getQuestion());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        Thread.startVirtualThread(() -> executeConversation(
                request, tenantId, emitter));

        return emitter;
    }

    private void executeConversation(
            ChatRequest request, Long tenantId, SseEmitter emitter) {
        try {
            // 1. 向量检索
            List<SourceReference> sources = retrievalService.retrieve(
                    request.getQuestion(), tenantId, TOP_K);

            // 2. 构造 Prompt
            String prompt = buildPrompt(request.getQuestion(), sources);

            // 3. 流式调用 LLM，逐 token 推送
            llmService.streamChat(prompt, token -> emitToken(emitter, token));

            // 4. 发送 done 事件（含来源引用）
            emitDone(emitter, sources);
            emitter.complete();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            emitError(emitter, "对话被中断");
        } catch (Exception e) {
            log.error("RAG 对话异常: tenantId={}", tenantId, e);
            emitError(emitter, "对话服务异常");
        }
    }

    private String buildPrompt(String question, List<SourceReference> sources) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
        sb.append("\n\n--- 文档参考内容 ---\n");

        for (SourceReference source : sources) {
            sb.append("[来源: ").append(source.getDocumentName())
                    .append(" 第").append(source.getChunkIndex()).append("段]\n")
                    .append(source.getContent()).append("\n\n");
        }

        sb.append("--- 用户问题 ---\n").append(question);

        if (sb.length() > MAX_PROMPT_LENGTH) {
            return sb.substring(0, MAX_PROMPT_LENGTH);
        }
        return sb.toString();
    }

    private void emitToken(SseEmitter emitter, String token) {
        try {
            ChatEvent event = ChatEvent.builder()
                    .type("token")
                    .content(token)
                    .build();
            emitter.send(SseEmitter.event().name("token").data(event));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void emitDone(SseEmitter emitter, List<SourceReference> sources) {
        try {
            ChatEvent event = ChatEvent.builder()
                    .type("done")
                    .content("")
                    .sources(sources)
                    .build();
            emitter.send(SseEmitter.event().name("done").data(event));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void emitError(SseEmitter emitter, String message) {
        try {
            ChatEvent event = ChatEvent.builder()
                    .type("error")
                    .content(message)
                    .build();
            emitter.send(SseEmitter.event().name("error").data(event));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        } finally {
            emitter.complete();
        }
    }
}
