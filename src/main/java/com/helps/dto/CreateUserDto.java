package com.helps.dto;

public record CreateUserDto(String username, String password, String role) {
    public CreateUserDto {
        if (role == null) {
            role = "OPERADOR";
        }
    }

    public CreateUserDto(String username, String password) {
        this(username, password, "OPERADOR");
    }
}