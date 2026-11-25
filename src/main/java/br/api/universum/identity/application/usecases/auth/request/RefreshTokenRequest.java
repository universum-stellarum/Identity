package br.api.universum.identity.application.usecases.auth.request;

import jakarta.validation.constraints.NotBlank;

/** Corpo do refresh e do logout: o refresh token recebido no login (ou no último refresh). */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required.")
        String refreshToken) {
}
