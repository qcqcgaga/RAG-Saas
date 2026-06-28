package com.docchat.module_chat.aop;

import com.docchat.module_stat.service.StatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 对话统计切面
 *
 * AOP 拦截 ChatService.converse() 方法，根据鉴权类型决定是否记录用量。
 * - API_KEY 鉴权：记录用量到 chat_usage_logs + 递增 Redis 限额计数器
 * - WIDGET 鉴权：聊天组件对话（嵌入外部网站），记录用量（计入统计面板）
 * - JWT 鉴权（预览对话）：跳过记录
 *
 * V1 R5: 从 AuthContext 读取 promptTokens/completionTokens（由 ChatServiceImpl 设置），
 * 不再记录固定的 0 值。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ChatStatAspect {

    private final StatService statService;

    /**
     * 拦截对话方法，采集用量数据。
     *
     * 鉴权类型决策：
     * - API_KEY：正式访客对话，记录用量 + Redis 限额计数器已在鉴权阶段递增
     * - WIDGET：聊天组件对话（嵌入外部网站），记录用量（计入统计面板）
     * - JWT：管理后台预览对话，跳过记录（不计入用量）
     */
    @Around("execution(* com.docchat.module_chat.service.ChatService.converse(..))")
    public Object aroundConverse(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行对话
        Object result = joinPoint.proceed();

        // 判断鉴权类型 — JWT 预览对话跳过记录
        String authType = AuthContext.getAuthType();
        log.info("ChatStatAspect: authType={}, tenantId={}", authType, AuthContext.getTenantId());
        if ("JWT".equals(authType)) {
            log.debug("预览对话，跳过用量记录");
            return result;
        }

        // API_KEY / WIDGET：记录用量到数据库
        try {
            Long tenantId = AuthContext.getTenantId();
            Long apiKeyId = AuthContext.getApiKeyId();
            String modelName = AuthContext.getModelName();

            // V1 R5: 从 AuthContext 读取实际 token 使用量
            int promptTokens = AuthContext.getPromptTokens();
            int completionTokens = AuthContext.getCompletionTokens();

            statService.recordUsage(tenantId, apiKeyId, authType, modelName,
                    promptTokens, completionTokens);
            log.info("记录用量: tenantId={}, apiKeyId={}, authType={}, tokens={}/{}",
                    tenantId, apiKeyId, authType, promptTokens, completionTokens);
        } catch (Exception e) {
            // 用量记录失败不影响对话，仅记录日志
            log.warn("用量记录失败", e);
        }

        return result;
    }
}
