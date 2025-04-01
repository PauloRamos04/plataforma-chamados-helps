package com.helps.dto;

import org.springframework.web.multipart.MultipartFile;

public record TicketDto(
        String title,
        String description,
        String category,
        String type,
        MultipartFile image
) {}