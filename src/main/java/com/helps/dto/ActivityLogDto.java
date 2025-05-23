package com.helps.dto;

import java.time.LocalDateTime;

public record ActivityLogDto(
        Long id,
        Long userId,
        String username,
        String userDisplayName,
        String activity,
        String ipAddress,
        String userAgent,
        String sessionId,
        LocalDateTime createdAt,
        String additionalInfo
) {}