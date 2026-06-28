package com.docchat.module_apikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * API Key 响应
 *
 * key 字段仅在创建时返回完整值，之后只展示脱敏值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    private Long id;
    private String name;

    /** 完整 Key，仅在创建时返回 */
    private String key;

    /** Key 前缀（如 dc_a1b2c） */
    private String keyPrefix;

    /** 脱敏 Key（如 dc_a1b2c****3f4g） */
    private String keyMasked;

    /** 状态：1-有效 0-已吊销 */
    private Short status;

    /** 每日调用限额 */
    private Long dailyLimit;

    /** 最后使用时间 */
    private Instant lastUsedAt;

    private Instant createdAt;
}
