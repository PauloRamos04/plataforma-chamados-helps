package com.helps.dto;

import java.util.List;

public record UserDto(
        Long id,
        String username,
        String name,
        Boolean enabled,
        List<String> roles
) {

    public UserDto sanitized() {
        return new UserDto(
                id,
                username,
                name,
                enabled,
                roles
        );
    }
}