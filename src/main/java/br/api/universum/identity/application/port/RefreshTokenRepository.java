package br.api.universum.identity.application.port;

import br.api.universum.identity.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    /**
     * Finds the token and locks it until the unit of work ends, so that concurrent requests
     * cannot consume the same refresh token twice.
     */
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    /** Inserts a new token (no id yet) or updates an existing one. */
    RefreshToken save(RefreshToken token);

    void revokeAllByUserId(UUID userId);
}
