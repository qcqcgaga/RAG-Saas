package com.docchat.module_chat.controller;

import com.docchat.module_chat.dto.ChatRequest;
import com.docchat.module_chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 对话控制器
 *
 * 提供基于 SSE 的流式对话接口。
 * 通过 widget_token 识别租户，不需要 JWT。
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发起对话（SSE 流式）
     *
     * 通过 Authorization header 中的 widget_token 识别租户。
     * MVP 简化：假设 token 格式为 "Bearer <tenantId>"。
     * 后续 TASK-011 会实现 Widget Token 验证。
     */
    @PostMapping(value = "/conversations",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter converse(
            @RequestHeader("Authorization") String widgetToken,
            @Valid @RequestBody ChatRequest request) {
        Long tenantId = parseTenantIdFromToken(widgetToken);
        return chatService.converse(request, tenantId);
    }

    private Long parseTenantIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return Long.parseLong(authHeader.substring(7));
            } catch (NumberFormatException e) {
                return 1L;
            }
        }
        return 1L;
    }
}
