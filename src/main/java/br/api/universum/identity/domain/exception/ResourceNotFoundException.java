package br.api.universum.identity.domain.exception;

/** Indicates that a resource was not found. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
