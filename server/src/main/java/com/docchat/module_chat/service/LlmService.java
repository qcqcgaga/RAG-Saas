package com.docchat.module_chat.service;

import com.docchat.module_chat.entity.TenantLlmConfig;
import com.docchat.module_chat.repository.TenantLlmConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 服务
 *
 * V1 变更：支持租户级 LLM 配置。
 * 优先使用租户自定义的 LLM API，未配置时使用系统默认。
 * 支持 Anthropic Messages API 协议。
 *
 * V1 R5 修复：从 SSE 响应中提取 token 使用量，供 ChatStatAspect 记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final TenantLlmConfigRepository tenantLlmConfigRepository;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${docchat.llm.api-url:}")
    private String defaultApiUrl;

    @Value("${docchat.llm.api-key:}")
    private String defaultApiKey;

    @Value("${docchat.llm.model-name:claude-sonnet-4-20250514}")
    private String defaultModelName;

    @Value("${docchat.llm.max-tokens:2048}")
    private int maxTokens;

    /**
     * 流式调用 LLM，逐 token 通过 consumer 回调推送。
     * 优先使用租户自定义 LLM 配置，未配置时使用系统默认。
     *
     * @return TokenUsage 包含 prompt_tokens 和 completion_tokens
     */
    public TokenUsage streamChat(String prompt, Long tenantId,
                           Consumer<String> tokenConsumer)
            throws InterruptedException {
        LlmConfig config = resolveLlmConfig(tenantId);
        log.info("调用 LLM API: tenantId={}, model={}, apiUrl length={}",
                tenantId, config.modelName, config.apiUrl.length());

        return callAnthropicApi(config, prompt, tokenConsumer);
    }

    /**
     * 调用 Anthropic Messages API（流式）
     *
     * @return TokenUsage 包含本次调用的 token 消耗
     */
    private TokenUsage callAnthropicApi(LlmConfig config, String prompt,
                                  Consumer<String> tokenConsumer) throws InterruptedException {
        TokenUsage usage = new TokenUsage();
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", config.modelName,
                    "max_tokens", maxTokens,
                    "stream", true,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", config.apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("LLM API 调用失败: status={}, body={}",
                        response.statusCode(),
                        response.body().length() > 500
                                ? response.body().substring(0, 500) : response.body());
                throw new RuntimeException("LLM API 返回错误: " + response.statusCode());
            }

            // 解析 SSE 流式响应，同时提取 token 使用量
            parseSseResponse(response.body(), tokenConsumer, usage);

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM API 调用异常", e);
            throw new RuntimeException("LLM 服务调用失败: " + e.getMessage(), e);
        }
        return usage;
    }

    /**
     * 解析 Anthropic SSE 流式响应
     *
     * V1 R5: 同时提取 message_delta 中的 usage 信息
     */
    private void parseSseResponse(String responseBody, Consumer<String> tokenConsumer, TokenUsage usage) {
        String[] lines = responseBody.split("\n");
        for (String line : lines) {
            if (!line.startsWith("data: ")) continue;
            String data = line.substring(6).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) continue;

            try {
                JsonNode node = objectMapper.readTree(data);
                String type = node.path("type").asText();

                if ("content_block_delta".equals(type)) {
                    JsonNode delta = node.path("delta");
                    if ("text_delta".equals(delta.path("type").asText())) {
                        String text = delta.path("text").asText("");
                        if (!text.isEmpty()) {
                            tokenConsumer.accept(text);
                        }
                    }
                } else if ("message_start".equals(type)) {
                    // Anthropic API 在 message_start 中包含 input tokens
                    JsonNode msgUsage = node.path("message").path("usage");
                    if (!msgUsage.isMissingNode()) {
                        usage.promptTokens = msgUsage.path("input_tokens").asInt(0);
                    }
                } else if ("message_delta".equals(type)) {
                    // Anthropic API 在 message_delta 中包含 output tokens
                    JsonNode deltaUsage = node.path("usage");
                    if (!deltaUsage.isMissingNode()) {
                        usage.completionTokens = deltaUsage.path("output_tokens").asInt(0);
                    }
                }
            } catch (Exception e) {
                log.debug("解析 SSE 行失败: {}", data.substring(0, Math.min(data.length(), 100)));
            }
        }
    }

    /**
     * 解析 LLM 配置：租户自定义 > 系统默认
     */
    private LlmConfig resolveLlmConfig(Long tenantId) {
        if (tenantId != null) {
            var tenantConfig = tenantLlmConfigRepository
                    .findByTenantIdAndStatus(tenantId, 1);
            if (tenantConfig.isPresent()) {
                TenantLlmConfig cfg = tenantConfig.get();
                String apiKey = cfg.getApiKeyEncrypted();
                return new LlmConfig(cfg.getApiUrl(), apiKey, cfg.getModelName());
            }
        }
        return new LlmConfig(defaultApiUrl, defaultApiKey, defaultModelName);
    }

    /**
     * 获取租户 LLM 配置（供管理接口使用）
     */
    public TenantLlmConfig getTenantLlmConfig(Long tenantId) {
        return tenantLlmConfigRepository.findByTenantId(tenantId).orElse(null);
    }

    /**
     * 保存租户 LLM 配置
     */
    public TenantLlmConfig saveTenantLlmConfig(TenantLlmConfig config) {
        // TODO: 加密 api_key
        return tenantLlmConfigRepository.save(config);
    }

    /**
     * 删除租户 LLM 配置（恢复系统默认）
     */
    public void deleteTenantLlmConfig(Long tenantId) {
        tenantLlmConfigRepository.findByTenantId(tenantId)
                .ifPresent(tenantLlmConfigRepository::delete);
    }

    /**
     * 测试 LLM 连通性
     */
    public LlmTestResult testConnection(String apiUrl, String apiKey, String modelName) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", modelName,
                    "max_tokens", 10,
                    "messages", List.of(Map.of("role", "user", "content", "Hi"))
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            long responseTime = System.currentTimeMillis() - startTime;
            boolean connected = response.statusCode() == 200;

            if (!connected) {
                log.warn("LLM 连通性测试失败: status={}, body={}",
                        response.statusCode(),
                        response.body().length() > 200
                                ? response.body().substring(0, 200) : response.body());
            }

            return new LlmTestResult(connected, modelName, responseTime);
        } catch (Exception e) {
            log.error("LLM 连通性测试异常", e);
            return new LlmTestResult(false, modelName,
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * LLM 配置内部类
     */
    private record LlmConfig(String apiUrl, String apiKey, String modelName) {
    }

    /**
     * LLM 连通性测试结果
     */
    public record LlmTestResult(boolean connected, String modelName, long responseTimeMs) {
    }

    /**
     * Token 使用量（V1 R5 新增）
     *
     * 从 LLM SSE 响应中提取，供 ChatStatAspect 记录到用量统计。
     */
    public static class TokenUsage {
        public int promptTokens = 0;
        public int completionTokens = 0;

        public int totalTokens() {
            return promptTokens + completionTokens;
        }
    }
}
