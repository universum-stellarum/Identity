package br.api.universum.identity.domain.exception;

/** Indica que um recurso não foi encontrado. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
