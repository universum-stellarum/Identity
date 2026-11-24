package br.api.universum.identity.domain.exception;

/** Indica a violação de uma regra de negócio genérica. */
public class BusinessException extends DomainException {

    public BusinessException(String message) {
        super(message);
    }
}
