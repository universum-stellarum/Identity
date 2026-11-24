package br.api.universum.identity.domain.exception;

/** Indica que o usuário solicitado não existe. */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException() {
        super("User not found.");
    }
}
