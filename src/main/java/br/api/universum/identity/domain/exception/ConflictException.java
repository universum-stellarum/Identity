package br.api.universum.identity.domain.exception;

/** Indica um conflito com uma regra de negócio. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
