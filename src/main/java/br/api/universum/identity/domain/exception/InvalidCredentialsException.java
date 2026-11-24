package br.api.universum.identity.domain.exception;

/** Indicates that the email or password given at login is invalid. */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("Invalid credentials.");
    }
}
