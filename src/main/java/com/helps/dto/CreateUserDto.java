package com.helps.dto;

public record CreateUserDto(String username, String password, String name) {
    public CreateUserDto(String username, String password) {
        this(username, password, null);
    }
}