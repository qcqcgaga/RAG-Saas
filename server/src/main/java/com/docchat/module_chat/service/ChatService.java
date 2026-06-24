package com.docchat.module_chat.service;

import com.docchat.module_chat.dto.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 对话服务接口
 *
 * 编排 RAG 对话流程：向量检索 + LLM 生成，通过 SSE 推送结果。
 */
public interface ChatService {

    /**
     * 执行 RAG 对话，通过 SseEmitter 推送事件流。
     *
     * @param request  对话请求
     * @param tenantId 租户 ID
     * @return SseEmitter 用于 SSE 流式响应
     */
    SseEmitter converse(ChatRequest request, Long tenantId);
}
