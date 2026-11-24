package br.api.universum.identity.domain.exception;

/** Indicates a business rule conflict. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
