package com.example.bankcards.dto.responce;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
