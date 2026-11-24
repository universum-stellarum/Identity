package br.api.universum.identity.domain.exception;

/** Indica que o e-mail ou a senha informados no login são inválidos. */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("Invalid credentials.");
    }
}
