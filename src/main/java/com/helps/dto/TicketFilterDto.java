package com.helps.dto;

public record TicketFilterDto(
        String status,
        String category,
        String search,
        String sortBy,
        String sortDirection
) {}