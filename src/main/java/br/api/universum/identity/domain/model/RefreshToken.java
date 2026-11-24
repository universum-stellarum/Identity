package br.api.universum.identity.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static br.api.universum.identity.domain.model.Guard.requireNonNull;
import static br.api.universum.identity.domain.model.Guard.requireText;

public class RefreshToken {

    /** Assigned by the persistence layer; {@code null} until the token is saved for the first time. */
    private final UUID id;

    /** SHA-256 em hex do refresh token. O valor original nunca é persistido. */
    private final String tokenHash;
    private final UUID userId;
    private final Instant expiryDate;
    private boolean revoked;

    private RefreshToken(UUID id, String tokenHash, UUID userId, Instant expiryDate, boolean revoked) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiryDate = expiryDate;
        this.revoked = revoked;
    }

    public static RefreshToken issue(String tokenHash, UUID userId, Instant now, Duration validFor) {
        return new RefreshToken(
                null,
                requireText(tokenHash, "Token hash is required."),
                requireNonNull(userId, "User is required."),
                now.plus(validFor),
                false
        );
    }

    /** Rebuilds a token that was already persisted. The stored state is trusted as is. */
    public static RefreshToken restore(UUID id, String tokenHash, UUID userId, Instant expiryDate, boolean revoked) {
        return new RefreshToken(id, tokenHash, userId, expiryDate, revoked);
    }

    public boolean isExpired(Instant now) {
        return !expiryDate.isAfter(now);
    }

    public void revoke() {
        this.revoked = true;
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
