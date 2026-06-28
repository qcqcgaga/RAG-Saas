package com.docchat.module_stat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 趋势统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatTrendResponse {

    /** 指标名称：calls / tokens / conversations */
    private String metric;

    /** 统计周期：7d / 30d */
    private String period;

    /** 趋势数据点 */
    private List<TrendPoint> points;

    /**
     * 趋势数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {

        /** 日期 */
        private LocalDate date;

        /** 指标值 */
        private long value;
    }
}
