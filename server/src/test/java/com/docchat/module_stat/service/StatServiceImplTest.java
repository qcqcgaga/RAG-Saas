package com.docchat.module_stat.service;

import com.docchat.module_stat.dto.StatDailyResponse;
import com.docchat.module_stat.dto.StatOverviewResponse;
import com.docchat.module_stat.dto.StatTrendResponse;
import com.docchat.module_stat.repository.ChatUsageLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 用量统计服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class StatServiceImplTest {

    @Mock private ChatUsageLogRepository chatUsageLogRepository;
    @InjectMocks private StatServiceImpl statService;

    // ========== getOverview ==========

    @Test
    @DisplayName("getOverview - 7天周期返回正确聚合数据")
    void getOverview_7d_returnsAggregatedData() {
        Long tenantId = 1L;
        Object[] aggResult = new Object[]{100L, 5000L, 3000L, 8000L};
        when(chatUsageLogRepository.aggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(aggResult));
        when(chatUsageLogRepository.countTodayCalls(eq(tenantId), any(Instant.class)))
                .thenReturn(15L);

        StatOverviewResponse response = statService.getOverview(tenantId, "7d");

        assertThat(response.getTotalCalls()).isEqualTo(100);
        assertThat(response.getTotalTokens()).isEqualTo(8000);
        assertThat(response.getAvgDailyCalls()).isEqualTo(100 / 7);
        assertThat(response.getAvgDailyTokens()).isEqualTo(8000 / 7);
        assertThat(response.getTodayCalls()).isEqualTo(15);
        assertThat(response.getPeriod()).isEqualTo("7d");
    }

    @Test
    @DisplayName("getOverview - 30天周期返回正确聚合数据")
    void getOverview_30d_returnsAggregatedData() {
        Long tenantId = 1L;
        Object[] aggResult = new Object[]{3000L, 150000L, 90000L, 240000L};
        when(chatUsageLogRepository.aggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(aggResult));
        when(chatUsageLogRepository.countTodayCalls(eq(tenantId), any(Instant.class)))
                .thenReturn(100L);

        StatOverviewResponse response = statService.getOverview(tenantId, "30d");

        assertThat(response.getTotalCalls()).isEqualTo(3000);
        assertThat(response.getTotalTokens()).isEqualTo(240000);
        assertThat(response.getAvgDailyCalls()).isEqualTo(3000 / 30);
        assertThat(response.getPeriod()).isEqualTo("30d");
    }

    @Test
    @DisplayName("getOverview - 无数据时返回零值")
    void getOverview_noData_returnsZeros() {
        Long tenantId = 1L;
        Object[] aggResult = new Object[]{0L, 0L, 0L, 0L};
        when(chatUsageLogRepository.aggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(aggResult));
        when(chatUsageLogRepository.countTodayCalls(eq(tenantId), any(Instant.class)))
                .thenReturn(0L);

        StatOverviewResponse response = statService.getOverview(tenantId, "7d");

        assertThat(response.getTotalCalls()).isEqualTo(0);
        assertThat(response.getTotalTokens()).isEqualTo(0);
        assertThat(response.getTodayCalls()).isEqualTo(0);
    }

    @Test
    @DisplayName("getOverview - 无效周期参数默认为7天")
    void getOverview_invalidPeriod_defaultsTo7d() {
        Long tenantId = 1L;
        Object[] aggResult = new Object[]{0L, 0L, 0L, 0L};
        when(chatUsageLogRepository.aggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(aggResult));
        when(chatUsageLogRepository.countTodayCalls(eq(tenantId), any(Instant.class)))
                .thenReturn(0L);

        StatOverviewResponse response = statService.getOverview(tenantId, "invalid");

        assertThat(response.getPeriod()).isEqualTo("invalid"); // 保留原始参数，但按7天计算
    }

    // ========== getDailyStats ==========

    @Test
    @DisplayName("getDailyStats - 有数据日期返回实际值")
    void getDailyStats_withData_returnsActualValues() {
        Long tenantId = 1L;
        LocalDate start = LocalDate.of(2026, 6, 24);
        LocalDate end = LocalDate.of(2026, 6, 24);

        Object[] row = new Object[]{java.sql.Date.valueOf(start), 50L, 2000L, 1000L, 3000L};
        when(chatUsageLogRepository.dailyAggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(row));

        List<StatDailyResponse> result = statService.getDailyStats(tenantId, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(start);
        assertThat(result.get(0).getCalls()).isEqualTo(50);
        assertThat(result.get(0).getTokens()).isEqualTo(3000);
    }

    @Test
    @DisplayName("getDailyStats - 无数据日期补零")
    void getDailyStats_missingDates_filledWithZeros() {
        Long tenantId = 1L;
        LocalDate start = LocalDate.of(2026, 6, 22);
        LocalDate end = LocalDate.of(2026, 6, 24);

        when(chatUsageLogRepository.dailyAggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        List<StatDailyResponse> result = statService.getDailyStats(tenantId, start, end);

        assertThat(result).hasSize(3); // 3天
        assertThat(result.stream().allMatch(d -> d.getCalls() == 0)).isTrue();
        assertThat(result.stream().allMatch(d -> d.getTokens() == 0)).isTrue();
    }

    @Test
    @DisplayName("getDailyStats - 同一天起止返回1条记录")
    void getDailyStats_sameDay_returnsOneRecord() {
        Long tenantId = 1L;
        LocalDate day = LocalDate.of(2026, 6, 24);

        when(chatUsageLogRepository.dailyAggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        List<StatDailyResponse> result = statService.getDailyStats(tenantId, day, day);

        assertThat(result).hasSize(1);
    }

    // ========== getTrend ==========

    @Test
    @DisplayName("getTrend - calls指标返回调用趋势")
    void getTrend_callsMetric_returnsCallsTrend() {
        Long tenantId = 1L;
        when(chatUsageLogRepository.dailyAggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        StatTrendResponse response = statService.getTrend(tenantId, "7d", "calls");

        assertThat(response.getMetric()).isEqualTo("calls");
        assertThat(response.getPeriod()).isEqualTo("7d");
        assertThat(response.getPoints()).hasSize(7);
    }

    @Test
    @DisplayName("getTrend - tokens指标返回Token趋势")
    void getTrend_tokensMetric_returnsTokensTrend() {
        Long tenantId = 1L;
        when(chatUsageLogRepository.dailyAggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        StatTrendResponse response = statService.getTrend(tenantId, "7d", "tokens");

        assertThat(response.getMetric()).isEqualTo("tokens");
        assertThat(response.getPoints()).hasSize(7);
    }

    @Test
    @DisplayName("getTrend - 未知指标默认返回calls")
    void getTrend_unknownMetric_defaultsToCalls() {
        Long tenantId = 1L;
        when(chatUsageLogRepository.dailyAggregateByTenantAndRange(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        StatTrendResponse response = statService.getTrend(tenantId, "7d", "unknown");

        assertThat(response.getMetric()).isEqualTo("unknown");
        assertThat(response.getPoints()).hasSize(7);
    }

    // ========== recordUsage ==========

    @Test
    @DisplayName("recordUsage - 正确记录用量日志")
    void recordUsage_happyPath() {
        when(chatUsageLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        statService.recordUsage(1L, 5L, "API_KEY", "gpt-4o-mini", 100, 50);

        verify(chatUsageLogRepository).save(argThat(log ->
                log.getTenantId().equals(1L)
                && log.getApiKeyId().equals(5L)
                && log.getAuthType().equals("API_KEY")
                && log.getModelName().equals("gpt-4o-mini")
                && log.getPromptTokens() == 100
                && log.getCompletionTokens() == 50
                && log.getTotalTokens() == 150
        ));
    }

    @Test
    @DisplayName("recordUsage - totalTokens为promptTokens+completionTokens")
    void recordUsage_totalTokensCalculated() {
        when(chatUsageLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        statService.recordUsage(1L, 5L, "API_KEY", "gpt-4o-mini", 200, 300);

        verify(chatUsageLogRepository).save(argThat(log ->
                log.getTotalTokens() == 500
        ));
    }
}
