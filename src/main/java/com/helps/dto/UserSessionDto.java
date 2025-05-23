package com.helps.dto;

import java.time.LocalDateTime;

public record UserSessionDto(
        Long id,
        Long userId,
        String username,
        String userDisplayName,
        String sessionId,
        LocalDateTime loginTime,
        LocalDateTime logoutTime,
        LocalDateTime lastActivity,
        String ipAddress,
        String userAgent,
        Boolean isActive,
        Long durationMinutes
) {}