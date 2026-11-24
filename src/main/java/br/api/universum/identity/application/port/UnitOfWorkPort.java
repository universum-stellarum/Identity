package br.api.universum.identity.application.port;

import java.util.function.Supplier;

/**
 * Transaction boundary of a use case.
 *
 * <p>Everything done inside the block is committed or rolled back together. A
 * {@link br.api.universum.identity.domain.exception.DomainException} is an expected outcome, so it
 * <strong>commits</strong> what was recorded before it and is then rethrown: a failed login keeps the
 * attempt counter, and reusing a revoked refresh token keeps the revocation of every session. Any
 * other exception rolls the block back.</p>
 */
public interface UnitOfWorkPort {

    <T> T execute(Supplier<T> work);

    void run(Runnable work);

    /** Same as {@link #execute(Supplier)}, for blocks that only read. */
    <T> T read(Supplier<T> query);
}
