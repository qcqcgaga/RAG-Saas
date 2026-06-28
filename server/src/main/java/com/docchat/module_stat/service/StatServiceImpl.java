package com.docchat.module_stat.service;

import com.docchat.module_stat.dto.StatDailyResponse;
import com.docchat.module_stat.dto.StatOverviewResponse;
import com.docchat.module_stat.dto.StatTrendResponse;
import com.docchat.module_stat.dto.StatTrendResponse.TrendPoint;
import com.docchat.module_stat.entity.ChatUsageLog;
import com.docchat.module_stat.repository.ChatUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用量统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final ChatUsageLogRepository chatUsageLogRepository;

    @Override
    public StatOverviewResponse getOverview(Long tenantId, String period) {
        int days = parsePeriodDays(period);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant rangeStart = today.minusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant rangeEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();

        // 周期聚合 — 安全解析，兼容不同JPA驱动的返回格式
        long totalCalls = 0;
        long totalTokens = 0;
        try {
            List<Object[]> aggList = chatUsageLogRepository.aggregateByTenantAndRange(tenantId, rangeStart, rangeEnd);
            if (aggList != null && !aggList.isEmpty()) {
                Object[] agg = aggList.get(0);
                // agg = [calls, promptTokens, completionTokens, totalTokens]
                if (agg != null && agg.length >= 1) {
                    totalCalls = toLong(agg[0]);
                    totalTokens = agg.length >= 4 ? toLong(agg[3]) : 0;
                }
            }
        } catch (Exception e) {
            log.warn("用量聚合查询异常, 使用默认值0: tenantId={}, error={}", tenantId, e.getMessage());
        }

        // 会话数：按 apiKeyId 去重近似（同一 apiKeyId 多次调用视为同一会话来源）
        // 简化处理：使用 totalCalls 作为会话数近似，后续可按 apiKeyId 去重
        long totalConversations = totalCalls;

        // 日均
        long avgDailyCalls = days > 0 ? totalCalls / days : 0;
        long avgDailyTokens = days > 0 ? totalTokens / days : 0;
        long avgDailyConversations = days > 0 ? totalConversations / days : 0;

        // 今日调用
        long todayCalls = chatUsageLogRepository.countTodayCalls(tenantId, todayStart);

        // TODO: 从租户配置读取限额，当前默认无限制
        long todayLimit = 0;
        long todayRemaining = 0;

        return StatOverviewResponse.builder()
                .totalCalls(totalCalls)
                .totalTokens(totalTokens)
                .totalConversations(totalConversations)
                .avgDailyCalls(avgDailyCalls)
                .avgDailyTokens(avgDailyTokens)
                .avgDailyConversations(avgDailyConversations)
                .todayCalls(todayCalls)
                .todayLimit(todayLimit)
                .todayRemaining(todayRemaining)
                .period(period)
                .build();
    }

    @Override
    public List<StatDailyResponse> getDailyStats(Long tenantId, LocalDate start, LocalDate end) {
        Instant rangeStart = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant rangeEnd = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = chatUsageLogRepository.dailyAggregateByTenantAndRange(
                tenantId, rangeStart, rangeEnd);

        // 将查询结果转为 Map，便于填充无数据的日期
        Map<LocalDate, Object[]> dateMap = rows.stream()
                .collect(Collectors.toMap(
                        row -> convertToDate(row[0]),
                        row -> row
                ));

        // 填充日期范围内所有日期（无数据补零）
        List<StatDailyResponse> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Object[] row = dateMap.get(date);
            if (row != null) {
                result.add(StatDailyResponse.builder()
                        .date(date)
                        .calls(toLong(row[1]))
                        .tokens(toLong(row[4]))
                        .conversations(toLong(row[1]))
                        .build());
            } else {
                result.add(StatDailyResponse.builder()
                        .date(date)
                        .calls(0)
                        .tokens(0)
                        .conversations(0)
                        .build());
            }
        }

        return result;
    }

    @Override
    public StatTrendResponse getTrend(Long tenantId, String period, String metric) {
        int days = parsePeriodDays(period);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = today.minusDays(days - 1);
        LocalDate end = today;

        List<StatDailyResponse> dailyStats = getDailyStats(tenantId, start, end);

        List<TrendPoint> points = dailyStats.stream()
                .map(d -> TrendPoint.builder()
                        .date(d.getDate())
                        .value(extractMetricValue(d, metric))
                        .build())
                .toList();

        return StatTrendResponse.builder()
                .metric(metric)
                .period(period)
                .points(points)
                .build();
    }

    @Override
    @Transactional
    public void recordUsage(Long tenantId, Long apiKeyId, String authType,
                            String modelName, int promptTokens, int completionTokens) {
        ChatUsageLog usageLog = new ChatUsageLog();
        usageLog.setTenantId(tenantId);
        usageLog.setApiKeyId(apiKeyId);
        usageLog.setAuthType(authType);
        usageLog.setModelName(modelName);
        usageLog.setPromptTokens(promptTokens);
        usageLog.setCompletionTokens(completionTokens);
        usageLog.setTotalTokens(promptTokens + completionTokens);

        chatUsageLogRepository.save(usageLog);

        log.debug("用量记录: tenantId={}, apiKeyId={}, authType={}, model={}, tokens={}/{}",
                tenantId, apiKeyId, authType, modelName, promptTokens, completionTokens);
    }

    /**
     * 将查询聚合结果安全转为 long
     * H2 可能返回 Integer/Long/BigInteger/BigDecimal，PostgreSQL 可能返回 Object[]
     * 统一处理，遇到异常返回0
     */
    private long toLong(Object value) {
        if (value == null) return 0L;
        try {
            if (value instanceof Number num) return num.longValue();
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            log.warn("聚合结果转long失败: value={}, type={}", value, value.getClass().getSimpleName());
            return 0L;
        }
    }

    /**
     * 解析周期字符串为天数
     */
    private int parsePeriodDays(String period) {
        if ("30d".equals(period)) {
            return 30;
        }
        return 7;
    }

    /**
     * 从每日统计中提取指定指标值
     */
    private long extractMetricValue(StatDailyResponse daily, String metric) {
        return switch (metric) {
            case "tokens" -> daily.getTokens();
            case "conversations" -> daily.getConversations();
            default -> daily.getCalls();
        };
    }

    /**
     * 将查询结果的日期列转为 LocalDate
     * 兼容多种返回类型：java.sql.Date / LocalDate / java.sql.Timestamp / String
     */
    private LocalDate convertToDate(Object dateObj) {
        if (dateObj == null) {
            return LocalDate.now(ZoneOffset.UTC);
        }
        if (dateObj instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (dateObj instanceof LocalDate localDate) {
            return localDate;
        }
        if (dateObj instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (dateObj instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        try {
            return LocalDate.parse(dateObj.toString());
        } catch (Exception e) {
            log.warn("日期转换失败: value={}, type={}", dateObj, dateObj.getClass().getSimpleName());
            return LocalDate.now(ZoneOffset.UTC);
        }
    }
}
