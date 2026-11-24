package br.api.universum.identity.domain.exception;

/** Indicates that the given refresh token is invalid, already revoked, or expired. */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, revoked, or expired.");
    }
}
