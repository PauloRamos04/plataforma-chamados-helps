package com.helps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketTypeDto(
        Long id,

        @NotBlank(message = "Nome do tipo é obrigatório")
        @Size(min = 2, max = 50, message = "Nome deve ter entre 2 e 50 caracteres")
        String name,

        @Size(max = 500, message = "Descrição não pode exceder 500 caracteres")
        String description,

        boolean active,

        int priorityLevel,

        int slaMinutes
) {}