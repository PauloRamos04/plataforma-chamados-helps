package com.helps.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(
        String type,
        Long ticketId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime timestamp
) {}