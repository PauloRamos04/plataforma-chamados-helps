package com.helps.dto;

public record TicketTrendDto(
        String date,
        String dayOfWeek,
        Long created,
        Long resolved
) {}