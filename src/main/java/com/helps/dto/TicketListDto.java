package com.helps.dto;

import java.time.LocalDateTime;

public record TicketListDto(
        Long id,
        String title,
        String status,
        String category,
        String type,
        LocalDateTime openingDate,
        Long userId,
        String userName,
        Long helperId,
        String helperName
) {}