package com.helps.dto;

import java.time.LocalDateTime;

public record MessageDto(
        Long id,
        Long ticketId,
        UserDto sender,
        String content,
        LocalDateTime sentDate,
        String imagePath
) {

    public MessageDto(String content) {
        this(null, null, null, content, null, null);
    }
}