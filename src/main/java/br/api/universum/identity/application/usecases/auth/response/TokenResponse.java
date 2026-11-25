package br.api.universum.identity.application.usecases.auth.response;

/**
 * O par de tokens entregue ao cliente após o login ou o refresh.
 *
 * @param accessToken  JWT de curta duração, enviado em toda requisição
 * @param refreshToken valor opaco de longa duração, usado só para pedir um novo par
 * @param tokenType    sempre {@code Bearer}
 * @param expiresIn    validade do access token, em segundos
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
