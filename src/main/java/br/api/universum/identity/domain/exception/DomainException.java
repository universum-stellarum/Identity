package br.api.universum.identity.domain.exception;

/**
 * Base for every rule violation raised by the domain or by a use case.
 *
 * <p>A domain exception is an expected outcome, not a failure: the unit of work commits what the
 * use case recorded before throwing it (failed login attempts, revoked sessions).</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
