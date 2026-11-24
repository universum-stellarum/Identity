package br.api.universum.identity.application.dto;

/** O par de tokens entregue ao cliente após o login ou o refresh. */
public record AuthenticationResult(String accessToken, String refreshToken, long expiresIn) {
}
