package com.helps.dto;

import java.util.List;
import java.util.Map;

public record DashboardStatsDto(
        Map<String, Object> generalStats,
        Map<String, Object> timeStats,
        Map<String, Object> statusDistribution,
        Map<String, Object> categoryStats,
        Map<String, Object> priorityStats,
        List<TicketTrendDto> weeklyTrends,
        List<HelperPerformanceDto> helperPerformance,
        Map<String, Object> slaStats,
        Map<String, Object> workloadStats
) {}