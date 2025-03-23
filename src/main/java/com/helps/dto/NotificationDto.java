package com.helps.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String message,
        String type,
        boolean read,
        Long chamadoId,
        LocalDateTime createdAt
) {}