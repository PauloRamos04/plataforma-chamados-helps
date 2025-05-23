package com.helps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserWithRoleDto(
        @NotBlank(message = "Nome de usuário é obrigatório")
        @Size(min = 3, max = 50, message = "Nome de usuário deve ter entre 3 e 50 caracteres")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
        String password,

        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String name,

        @NotBlank(message = "Role é obrigatória")
        String role
) {}