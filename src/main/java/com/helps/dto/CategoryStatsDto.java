package com.helps.dto;

public record CategoryStatsDto(
        String category,
        Long count,
        Double avgResolutionTime,
        Long openCount
) {}