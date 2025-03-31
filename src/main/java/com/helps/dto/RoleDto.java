package com.helps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleDto(
        Long id,

        @NotBlank(message = "Nome da role é obrigatório")
        @Size(min = 2, max = 50, message = "Nome deve ter entre 2 e 50 caracteres")
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "Nome da role deve conter apenas letras maiúsculas, números e underscores")
        String name,

        @Size(max = 500, message = "Descrição não pode exceder 500 caracteres")
        String description,

        boolean active
) {}