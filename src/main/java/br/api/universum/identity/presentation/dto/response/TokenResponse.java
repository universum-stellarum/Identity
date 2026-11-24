package br.api.universum.identity.presentation.dto.response;

import br.api.universum.identity.application.dto.AuthenticationResult;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public static TokenResponse from(AuthenticationResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken(), "Bearer", result.expiresIn());
    }
}
