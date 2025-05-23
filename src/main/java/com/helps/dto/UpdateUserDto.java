package com.helps.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserDto(
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String name,

        @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
        String password,

        String role
) {}