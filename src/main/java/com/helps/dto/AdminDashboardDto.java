package com.helps.dto;

import java.util.List;

public record AdminDashboardDto(
        TicketStatisticsDto statistics,
        List<HelperPerformanceDto> helperPerformance,
        long solvedTicketsThisMonth,
        long pendingTicketsThisMonth
) {}