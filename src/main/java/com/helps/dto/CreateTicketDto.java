package com.helps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CreateTicketDto(
        @NotBlank(message = "Título é obrigatório")
        @Size(min = 5, max = 100, message = "Título deve ter entre 5 e 100 caracteres")
        String title,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 10, max = 1000, message = "Descrição deve ter entre 10 e 1000 caracteres")
        String description,

        @NotBlank(message = "Categoria é obrigatória")
        String category,

        @NotBlank(message = "Tipo é obrigatório")
        String type,

        MultipartFile image
) {}