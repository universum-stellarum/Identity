package br.api.universum.identity.domain.exception;

/** Indicates a generic business rule violation. */
public class BusinessException extends DomainException {

    public BusinessException(String message) {
        super(message);
    }
}
