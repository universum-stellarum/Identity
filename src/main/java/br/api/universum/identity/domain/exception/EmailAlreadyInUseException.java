package br.api.universum.identity.domain.exception;

/** Indica que o e-mail informado já está cadastrado para outro usuário. */
public class EmailAlreadyInUseException extends ConflictException {

    public EmailAlreadyInUseException() {
        super("Email is already in use.");
    }
}
