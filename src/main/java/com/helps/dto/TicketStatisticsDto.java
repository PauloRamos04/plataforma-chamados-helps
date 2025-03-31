package com.helps.dto;

import java.util.Map;

public record TicketStatisticsDto(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long closedTickets,
        long ticketsCreatedToday,
        long ticketsCreatedThisWeek,
        long ticketsCreatedThisMonth,
        double averageResolutionTimeMinutes,
        double averageResponseTimeMinutes,
        Map<String, Long> ticketsByCategory,
        Map<String, Long> ticketsByDay
) {}