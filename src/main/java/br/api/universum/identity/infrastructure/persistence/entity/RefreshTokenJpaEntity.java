package br.api.universum.identity.infrastructure.persistence.entity;

import br.api.universum.identity.domain.model.RefreshToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenJpaEntity extends AuditableJpaEntity<UUID> {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** SHA-256 em hex do refresh token. O valor original nunca é persistido. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    public static RefreshTokenJpaEntity newFrom(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.tokenHash = token.getTokenHash();
        entity.userId = token.getUserId();
        entity.expiryDate = token.getExpiryDate();
        entity.copyFrom(token);
        return entity;
    }

    /** Copies the mutable domain state onto this entity: only revocation changes after issue. */
    public void copyFrom(RefreshToken token) {
        this.revoked = token.isRevoked();
    }

    public RefreshToken toDomain() {
        return RefreshToken.restore(id, tokenHash, userId, expiryDate, revoked);
    }
}
