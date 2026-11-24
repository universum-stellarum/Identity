package br.api.universum.identity.application.usecases.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required.")
        String refreshToken) {
}
