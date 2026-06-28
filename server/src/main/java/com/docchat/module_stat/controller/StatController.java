package com.docchat.module_stat.controller;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.response.R;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_stat.dto.StatDailyResponse;
import com.docchat.module_stat.dto.StatOverviewResponse;
import com.docchat.module_stat.dto.StatTrendResponse;
import com.docchat.module_stat.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 用量统计接口
 *
 * 所有接口需 JWT 鉴权（管理后台调用）。
 * 租户ID 从 Token 中解析，禁止从请求参数传入。
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    /**
     * 用量概览
     *
     * @param period 统计周期：7d / 30d，默认 7d
     */
    @GetMapping("/overview")
    public R<StatOverviewResponse> getOverview(
            @RequestParam(defaultValue = "7d") String period) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(statService.getOverview(tenantId, period));
    }

    /**
     * 每日统计
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     */
    @GetMapping("/daily")
    public R<List<StatDailyResponse>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(statService.getDailyStats(tenantId, startDate, endDate));
    }

    /**
     * 趋势统计
     *
     * @param period 统计周期：7d / 30d，默认 7d
     * @param metric 指标名称：calls / tokens / conversations，默认 calls
     */
    @GetMapping("/trend")
    public R<StatTrendResponse> getTrend(
            @RequestParam(defaultValue = "7d") String period,
            @RequestParam(defaultValue = "calls") String metric) {
        Long tenantId = requireCurrentTenantId();
        return R.ok(statService.getTrend(tenantId, period, metric));
    }

    /**
     * 要求当前租户ID，为空则抛出401业务异常
     */
    private Long requireCurrentTenantId() {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        return tenantId;
    }
}
