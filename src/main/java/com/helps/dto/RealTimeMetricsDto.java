package com.helps.dto;

import java.time.LocalDateTime;

public record RealTimeMetricsDto(
        long openTickets,
        long inProgressTickets,
        long ticketsLastHour,
        double ticketsPerMinute,
        long unassignedTickets,
        long totalTicketsSinceRestart,
        LocalDateTime timestamp
) {}