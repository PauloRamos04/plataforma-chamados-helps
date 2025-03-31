package com.helps.dto;

import java.util.Map;

public record UserMetricsDto(
        Long userId,
        String username,
        String name,
        long totalTicketsCreated,
        long totalTicketsHelped,
        Map<String, Long> ticketsByStatus,
        Map<String, Long> helpedTicketsByStatus,
        double averageResponseTimeMinutes,
        double averageResolutionTimeMinutes
) {}