package com.helps.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(
        String type,
        Long chamadoId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime timestamp,
        String imagePath
) {
    public ChatMessageDto(
            String type,
            Long chamadoId,
            Long senderId,
            String senderName,
            String content,
            LocalDateTime timestamp
    ) {
        this(type, chamadoId, senderId, senderName, content, timestamp, null);
    }
}