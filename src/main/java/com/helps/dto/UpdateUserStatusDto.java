package com.helps.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusDto(
        @NotNull(message = "Status é obrigatório")
        Boolean enabled
) {}