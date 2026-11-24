package br.api.universum.identity.infrastructure.persistence.entity;

import br.api.universum.identity.domain.model.SigningKey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * The single row that holds the signing key. The row is created by migration V2 (with every key
 * column empty), so that {@code SELECT ... FOR UPDATE} can serialize the first key generation.
 */
@Getter
@Entity
@Table(name = "jwt_signing_key")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SigningKeyJpaEntity extends AuditableJpaEntity<Integer> {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "key_id")
    private UUID keyId;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    public void copyFrom(SigningKey key) {
        this.keyId = key.getKeyId();
        this.publicKey = key.getPublicKey();
        this.privateKey = key.getPrivateKey();
        this.rotatedAt = key.getRotatedAt();
    }

    public SigningKey toDomain() {
        return SigningKey.restore(keyId, publicKey, privateKey, rotatedAt);
    }
}
