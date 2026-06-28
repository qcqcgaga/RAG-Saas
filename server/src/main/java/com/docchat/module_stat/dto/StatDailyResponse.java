package com.docchat.module_stat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 每日统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatDailyResponse {

    /** 日期 */
    private LocalDate date;

    /** 当日调用次数 */
    private long calls;

    /** 当日 Token 消耗 */
    private long tokens;

    /** 当日会话数 */
    private long conversations;
}
