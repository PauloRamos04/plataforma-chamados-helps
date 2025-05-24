package com.helps.dto;

import java.util.Map;

public record ReportDataDto(
        Long totalTickets,
        Map<String, Long> dailyStats,
        Map<String, Long> categoryStats,
        Map<String, Long> statusStats,
        Double avgResolutionTime,
        Double slaCompliance
) {}
