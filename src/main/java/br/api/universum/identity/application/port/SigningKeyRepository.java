package br.api.universum.identity.application.port;

import br.api.universum.identity.domain.model.SigningKey;

import java.util.Optional;

public interface SigningKeyRepository {

    /** The active key, or empty while no pair was generated yet. */
    Optional<SigningKey> findActive();

    /**
     * Loads the key, initialized or not, and locks it until the unit of work ends. This serializes
     * initialization and rotation across every instance of the service. Empty when the storage for
     * the key does not exist (missing migration).
     */
    Optional<SigningKey> findForUpdate();

    void save(SigningKey key);
}
