package com.helps.dto;

import java.time.LocalDateTime;

public record TicketResponseDto(
        Long id,
        String title,
        String description,
        String status,
        String category,
        LocalDateTime openingDate,
        LocalDateTime startDate,
        LocalDateTime closingDate,
        String imagePath,
        UserResponseDto user,
        UserResponseDto helper
) {}