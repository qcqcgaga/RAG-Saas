package com.docchat.module_stat.service;

import com.docchat.module_stat.dto.StatDailyResponse;
import com.docchat.module_stat.dto.StatOverviewResponse;
import com.docchat.module_stat.dto.StatTrendResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 用量统计服务
 */
public interface StatService {

    /**
     * 获取用量概览
     *
     * @param tenantId 租户ID
     * @param period   统计周期：7d / 30d
     * @return 概览数据
     */
    StatOverviewResponse getOverview(Long tenantId, String period);

    /**
     * 获取每日统计
     *
     * @param tenantId 租户ID
     * @param start    起始日期（含）
     * @param end      结束日期（含）
     * @return 每日统计列表
     */
    List<StatDailyResponse> getDailyStats(Long tenantId, LocalDate start, LocalDate end);

    /**
     * 获取趋势数据
     *
     * @param tenantId 租户ID
     * @param period   统计周期：7d / 30d
     * @param metric   指标名称：calls / tokens / conversations
     * @return 趋势数据
     */
    StatTrendResponse getTrend(Long tenantId, String period, String metric);

    /**
     * 记录一次用量
     *
     * @param tenantId         租户ID
     * @param apiKeyId         API Key ID（可为 null）
     * @param authType         鉴权类型：API_KEY / JWT
     * @param modelName        模型名称
     * @param promptTokens     输入 Token 数
     * @param completionTokens 输出 Token 数
     */
    void recordUsage(Long tenantId, Long apiKeyId, String authType,
                     String modelName, int promptTokens, int completionTokens);
}
