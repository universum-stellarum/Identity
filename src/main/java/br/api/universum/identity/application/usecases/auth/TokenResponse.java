package br.api.universum.identity.application.usecases.auth;

/** O par de tokens entregue ao cliente após o login ou o refresh. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
