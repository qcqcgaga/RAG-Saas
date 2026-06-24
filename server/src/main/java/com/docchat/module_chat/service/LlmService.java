package com.docchat.module_chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * LLM 服务
 *
 * MVP 阶段使用讯飞 Coding Plan API。
 * 当前为占位实现，返回模拟的流式文本。
 */
@Slf4j
@Service
public class LlmService {

    @Value("${docchat.llm.api-url:}")
    private String apiUrl;

    @Value("${docchat.llm.api-key:}")
    private String apiKey;

    /**
     * 流式调用 LLM，逐 token 通过 consumer 回调推送。
     * 在当前线程同步执行（由 ChatService 在虚拟线程中调用）。
     */
    public void streamChat(String prompt, Consumer<String> tokenConsumer)
            throws InterruptedException {
        log.info("调用 LLM API (prompt length={})", prompt.length());

        // TODO: 替换为讯飞 Coding Plan API 真实调用
        String mockResponse = "根据文档内容，我找到了相关信息：\n\n"
                + "这是一个基于文档的智能回答。由于当前处于开发阶段，"
                + "此回答为模拟内容。实际部署后将接入讯飞 Coding Plan API "
                + "提供真实的 RAG 对话能力。";

        int len = mockResponse.length();
        for (int i = 0; i < len; i += 3) {
            int end = Math.min(i + 3, len);
            String token = mockResponse.substring(i, end);
            tokenConsumer.accept(token);
            Thread.sleep(30);
        }
    }
}
