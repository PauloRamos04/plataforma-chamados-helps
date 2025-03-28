package com.helps.dto;

public record LoginResponse(String accessToken, Long expiresIn) {
}