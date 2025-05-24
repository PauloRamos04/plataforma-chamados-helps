package com.helps.dto;

public record PerformanceMetricsDto(
        Long userId,
        String userName,
        Long totalTickets,
        Long resolvedTickets,
        Double resolutionRate,
        Double avgResolutionTime,
        Double satisfactionScore
) {}