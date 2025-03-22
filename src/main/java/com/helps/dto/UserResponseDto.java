package com.helps.dto;

import java.util.List;

public record UserResponseDto(
        Long id,
        String username,
        String name,
        boolean enabled,
        List<String> roles
) {}