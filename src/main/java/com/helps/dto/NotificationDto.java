package com.helps.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String message,
        String type,
        boolean read,
        Long ticketId,
        LocalDateTime createdAt
) {}