package br.api.universum.identity.domain.exception;

/** Indica que o refresh token informado é inválido, já foi revogado ou expirou. */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, revoked, or expired.");
    }
}
