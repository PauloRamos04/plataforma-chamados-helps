package com.helps.dto;

public record HelperPerformanceDto(
        Long id,
        String name,
        Long totalAssigned,
        Long recentTickets,
        Long resolvedTickets,
        Double resolutionRate,
        Double avgResolutionTime,
        Long activeTickets
) {}