package br.api.universum.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static br.api.universum.identity.domain.model.Guard.requireNonNull;
import static br.api.universum.identity.domain.model.Guard.requireText;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken extends AuditableEntity {

    /** Gerado no primeiro save; {@code null} até lá. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** SHA-256 em hex do refresh token. O valor original nunca é persistido. */
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expiry_date", nullable = false, updatable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    public static RefreshToken issue(String tokenHash, UUID userId, Instant now, Duration validFor) {
        return new RefreshToken(
                null,
                requireText(tokenHash, "Token hash is required."),
                requireNonNull(userId, "User is required."),
                now.plus(validFor),
                false
        );
    }

    /** Reconstrói um token num estado conhecido, como viria do armazenamento. */
    public static RefreshToken restore(UUID id, String tokenHash, UUID userId, Instant expiryDate, boolean revoked) {
        return new RefreshToken(id, tokenHash, userId, expiryDate, revoked);
    }

    public boolean isExpired(Instant now) {
        return !expiryDate.isAfter(now);
    }

    public void revoke() {
        this.revoked = true;
    }
}
