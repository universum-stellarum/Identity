package br.api.universum.identity.application.dto;

/** The token pair handed to the client after login or refresh. */
public record AuthenticationResult(String accessToken, String refreshToken, long expiresIn) {
}
