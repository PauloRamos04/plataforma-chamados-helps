package com.helps.dto;

import java.time.LocalDateTime;

public record TicketListDto(
        Long id,
        String title,
        String description,
        String status,
        String category,
        LocalDateTime openingDate,
        LocalDateTime startDate,
        LocalDateTime closingDate,
        String imagePath,
        SimpleUserDto user,
        SimpleUserDto helper
) {}