package com.helps.dto;

import java.util.Map;

public record ActivityStatsDto(
        Long totalSessions,
        Long activeSessions,
        Long totalLogins24h,
        Long totalUsers,
        Long activeUsers,
        Map<String, Long> loginsByHour,
        Map<String, Long> activitiesByType
) {}