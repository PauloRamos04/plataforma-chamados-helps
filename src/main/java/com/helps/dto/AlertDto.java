package com.helps.dto;

import java.time.LocalDateTime;

public record AlertDto(
        String type,
        String title,
        String message,
        String severity,
        LocalDateTime timestamp
) {}