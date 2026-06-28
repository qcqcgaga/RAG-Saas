package com.docchat.module_stat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用量概览响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatOverviewResponse {

    /** 周期内总调用次数 */
    private long totalCalls;

    /** 周期内总 Token 消耗 */
    private long totalTokens;

    /** 周期内总会话数（按 apiKeyId 去重） */
    private long totalConversations;

    /** 日均调用次数 */
    private long avgDailyCalls;

    /** 日均 Token 消耗 */
    private long avgDailyTokens;

    /** 日均会话数 */
    private long avgDailyConversations;

    /** 今日已调用次数 */
    private long todayCalls;

    /** 今日调用限额（0 表示无限制） */
    private long todayLimit;

    /** 今日剩余调用次数 */
    private long todayRemaining;

    /** 统计周期：7d / 30d */
    private String period;
}
