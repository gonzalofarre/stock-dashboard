package com.stockdashboard.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(Long id, String firstName, String lastName, String email) {
    }
}
