package com.helps.dto;

import com.helps.domain.model.AuditLog;
import com.helps.domain.model.Ticket;

import java.util.List;

public record UserActivitySummaryDto(
        Long userId,
        String username,
        String name,
        int openTicketsCount,
        int ticketsInProgressCount,
        int recentActionsCount,
        double averageTicketsPerDay,
        List<Ticket> openTickets,
        List<Ticket> ticketsInProgress,
        List<AuditLog> recentActions
) {}