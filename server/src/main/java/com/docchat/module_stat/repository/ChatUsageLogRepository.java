package com.docchat.module_stat.repository;

import com.docchat.module_stat.entity.ChatUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * 对话用量日志数据访问
 *
 * 所有聚合查询使用 native query，避免 JPQL 在 Hibernate + PostgreSQL 上的兼容性问题。
 * JPQL 聚合查询在 PostgreSQL 上可能返回嵌套 Object[] 或类型不匹配的值。
 */
public interface ChatUsageLogRepository extends JpaRepository<ChatUsageLog, Long> {

    /**
     * 按租户和日期范围聚合统计（API_KEY + WIDGET 鉴权均计入用量）
     *
     * 使用 native query 避免 JPQL 在 PostgreSQL 上返回嵌套 Object[] 的问题。
     *
     * @param tenantId 租户ID
     * @param start    起始时间（含）
     * @param end      结束时间（不含）
     * @return 单行结果 [calls(bigint), pt(bigint), ct(bigint), tt(bigint)]
     */
    @Query(value = """
        SELECT COUNT(*) AS calls,
               COALESCE(SUM(prompt_tokens), 0) AS pt,
               COALESCE(SUM(completion_tokens), 0) AS ct,
               COALESCE(SUM(total_tokens), 0) AS tt
        FROM chat_usage_logs
        WHERE tenant_id = :tenantId
          AND auth_type <> 'JWT'
          AND created_at >= :start AND created_at < :end
    """, nativeQuery = true)
    List<Object[]> aggregateByTenantAndRange(@Param("tenantId") Long tenantId,
                                              @Param("start") Instant start,
                                              @Param("end") Instant end);

    /**
     * 按日期分组聚合（API_KEY + WIDGET 鉴权均计入用量）
     *
     * @param tenantId 租户ID
     * @param start    起始时间（含）
     * @param end      结束时间（不含）
     * @return 每日聚合列表 [date(sql.Date), calls(bigint), pt(bigint), ct(bigint), tt(bigint)]
     */
    @Query(value = """
        SELECT DATE(created_at) AS d,
               COUNT(*) AS calls,
               COALESCE(SUM(prompt_tokens), 0) AS pt,
               COALESCE(SUM(completion_tokens), 0) AS ct,
               COALESCE(SUM(total_tokens), 0) AS tt
        FROM chat_usage_logs
        WHERE tenant_id = :tenantId
          AND auth_type <> 'JWT'
          AND created_at >= :start AND created_at < :end
        GROUP BY DATE(created_at)
        ORDER BY DATE(created_at)
    """, nativeQuery = true)
    List<Object[]> dailyAggregateByTenantAndRange(@Param("tenantId") Long tenantId,
                                                   @Param("start") Instant start,
                                                   @Param("end") Instant end);

    /**
     * 统计今日调用量（API_KEY + WIDGET 鉴权均计入用量）
     */
    @Query("""
        SELECT COUNT(l)
        FROM ChatUsageLog l
        WHERE l.tenantId = :tenantId
          AND l.authType <> 'JWT'
          AND l.createdAt >= :startOfDay
    """)
    long countTodayCalls(@Param("tenantId") Long tenantId, @Param("startOfDay") Instant startOfDay);
}
