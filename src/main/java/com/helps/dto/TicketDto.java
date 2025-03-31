package com.helps.dto;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public record TicketDto(
        Long id,
        String title,
        String description,
        String status,
        CategoryDto category,
        TicketTypeDto type,
        LocalDateTime openingDate,
        LocalDateTime startDate,
        LocalDateTime closingDate,
        UserDto user,
        UserDto helper,
        String imagePath,
        MultipartFile image
) {
    // Construtor para o controller (linha 49)
    public TicketDto(String title, String description, String category, String type, MultipartFile image) {
        this(
                null, // id
                title,
                description,
                null, // status
                category != null ? new CategoryDto(null, category, null, true) : null,
                type != null ? new TicketTypeDto(null, type, null, true, 0, 0) : null,
                null, // openingDate
                null, // startDate
                null, // closingDate
                null, // user
                null, // helper
                null, // imagePath
                image // Armazena a imagem
        );
    }

    // Construtor para manter compatibilidade com código existente
    public TicketDto(
            Long id,
            String title,
            String description,
            String status,
            CategoryDto category,
            TicketTypeDto type,
            LocalDateTime openingDate,
            LocalDateTime startDate,
            LocalDateTime closingDate,
            UserDto user,
            UserDto helper,
            String imagePath
    ) {
        this(id, title, description, status, category, type, openingDate, startDate, closingDate, user, helper, imagePath, null);
    }

    public TicketListDto toListDto() {
        return new TicketListDto(
                id,
                title,
                status,
                category != null ? category.name() : null,
                type != null ? type.name() : null,
                openingDate,
                user != null ? user.id() : null,
                user != null ? user.name() : null,
                helper != null ? helper.id() : null,
                helper != null ? helper.name() : null
        );
    }
}